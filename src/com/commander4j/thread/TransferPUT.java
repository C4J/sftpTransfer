package com.commander4j.thread;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.commander4j.jsch.JschCommands;
import com.commander4j.jsch.JschRecord;
import com.commander4j.log.JLogPanel;
import com.commander4j.settings.SettingUtil;
import com.commander4j.settings.SettingsCommon;
import com.commander4j.settings.SettingsPut;

public class TransferPUT extends Thread
{

	public static final int Mode_NONE = 0;
	public static final int Mode_RUN = 1;
	public static final int Mode_PAUSE = 2;
	public static final int Mode_SHUTDOWN = 3;
	public static final int Mode_CONFIG_UPDATE = 4;

	private static final String[] modeName = new String[] { "None", "Run", "Pause", "Shutdown", "Configuration Update" };

	@SuppressWarnings("unused")
	private int modeNone = Mode_NONE;
	private int modeActive = Mode_PAUSE;
	private int modeRequest = Mode_NONE;
	private int modePrevious = Mode_NONE;

	private boolean run = true;

	// Settings
	private HashMap<String, JschRecord> jschConfig;

	private SettingUtil settingsUtil = new SettingUtil();
	private SettingsCommon settingsCommon = new SettingsCommon();
	private SettingsPut settingsPut = new SettingsPut();
	private JschCommands jcmd;

	Properties cfg = new Properties();

	TemporalAmount pollingFrequancy = Duration.ofSeconds(10);

	Instant due = Instant.now();
	Instant now = Instant.now();

	int logDestination = 0;


	public TransferPUT(int destination)
	{
		this.logDestination = destination;
		
		jcmd = new JschCommands(logDestination);
		loadSettings();

	}

	public void loadSettings()
	{
		jcmd.writeToSystemLog("sftpPUT Thread loading settings.", JLogPanel.INFO);
		
		settingsPut = settingsUtil.readSFTPPutFromXml();

		pollingFrequancy = Duration.ofSeconds(Long.valueOf(settingsPut.pollFrequencySeconds.data));

		settingsCommon = settingsUtil.readSFTPCommonFromXml();
		jschConfig = settingsUtil.readJschPropertiesFromXml();

		jcmd.assignCommonSettings(settingsCommon);
		jcmd.assignJschConfig(jschConfig);
	}

	public void requestMode(int mode)
	{
		this.modeRequest = mode;
	}

	public int getRequestMode()
	{
		return this.modeRequest;
	}

	public int getRunMode()
	{
		return this.modeActive;
	}

	public void setRunMode(int mode)
	{
		if (mode != Mode_NONE)
		{
			this.modePrevious = this.modeActive;
			this.modeActive = mode;
			this.modeRequest = Mode_NONE;
			jcmd.writeToSystemLog("sftpPUT Thread run mode : " + modeName[this.modeActive], JLogPanel.INFO);
		}
	}

	public void run()
	{
		jcmd.writeToSystemLog("sftpPUT Thread started.", JLogPanel.INFO);

		while (run)
		{

			if (getRunMode() == Mode_RUN)
			{
				now = Instant.now();

				if (now.compareTo(due) > 0)
				{
					transferLocalToRemote();
					due = now.plus(pollingFrequancy);
				}
			}

			if (getRunMode() == Mode_CONFIG_UPDATE)
			{
				loadSettings();

				setRunMode(modePrevious);
			}

			if (getRunMode() == Mode_SHUTDOWN)
			{
				run = false;
			}

			if (getRunMode() == Mode_PAUSE)
			{
				if (getRunMode() != getRequestMode())
				{
					setRunMode(getRequestMode());
				}
			}

			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
				run = false;
			}
		}
		
		jcmd.writeToSystemLog("sftpPUT Thread stopped.", JLogPanel.INFO);
	}

	private void transferLocalToRemote()
	{

		File sourceDirectory = new File(settingsPut.localDir.data);
		FileFilter fileFilter = WildcardFileFilter.builder().setWildcards(settingsPut.localFileMask.data).setIoCase(IOCase.INSENSITIVE).get();
		File[] filesNames = sourceDirectory.listFiles(fileFilter);

		if (filesNames.length > 0)
		{
			jcmd.writeToLog("Files found :" + filesNames.length, JLogPanel.INFO);

			if (jcmd.isConnected() == false)
			{
				jcmd.connect();
			}
			if (jcmd.isConnected())
			{

				for (int i = 0; i < filesNames.length; i++)
				{

					File sourceFile = filesNames[i];

					jcmd.writeToLog("Processing = " + String.valueOf(i + 1) + " of " + filesNames.length + " " + sourceFile.getName(), JLogPanel.INFO);

					if (sourceFile.isFile())
					{

						if (Boolean.valueOf(settingsPut.backupEnabled.data)==true)
						{
							if (settingsPut.backupDir.data.equals("") == false)
							{
								File backupFile = new File(settingsPut.backupDir.data + File.separator + filesNames[i].getName());
	
								try
								{
									FileUtils.copyFile(sourceFile, backupFile, false);
	
								}
								catch (IOException e)
								{
									jcmd.writeToLog(e.getMessage(), JLogPanel.ERROR);
								}
								finally
								{
									backupFile = null;
								}
							}
						}

						try
						{

							String filename = sourceFile.getName();

							sourceFile = null;

							jcmd.rm(settingsPut.remoteDir.data + "/" + filename);

							jcmd.put(settingsPut.localDir.data + File.separator + filename, settingsPut.remoteDir.data + "/" + filename + settingsPut.tempFileExtension.data);

							jcmd.rename(settingsPut.remoteDir.data + "/" + filename + settingsPut.tempFileExtension.data, settingsPut.remoteDir.data + "/" + filename);

							FileUtils.deleteQuietly(new File(settingsPut.localDir.data + File.separator + filename));

						}
						catch (Exception e)
						{
							jcmd.writeToLog(e.getMessage(), JLogPanel.ERROR);
						}
						finally
						{

						}

					}

				}

			}
			//jcmd.disconnect();

		}

		if (getRunMode() != getRequestMode())
		{
			setRunMode(getRequestMode());
		}

	}

}
