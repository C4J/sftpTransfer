package com.commander4j.thread;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;

import org.apache.commons.io.FileUtils;

import com.commander4j.jsch.JschCommands;
import com.commander4j.jsch.JschRecord;
import com.commander4j.log.JLogPanel;
import com.commander4j.settings.SettingUtil;
import com.commander4j.settings.SettingsCommon;
import com.commander4j.settings.SettingsGet;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;

public class TransferGET extends Thread
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
	private SettingsGet settingsGet = new SettingsGet();
	private JschCommands jcmd;

	Properties cfg = new Properties();

	TemporalAmount pollingFrequancy = Duration.ofSeconds(10);

	Instant due = Instant.now();
	Instant now = Instant.now();

	Session jschSession;

	int logDestination = 0;

	public TransferGET(int destination)
	{
		this.logDestination = destination;
		
		jcmd = new JschCommands(logDestination);
		loadSettings();
	}

	public void loadSettings()
	{
		jcmd.writeToSystemLog("smtpGET Thread loading settings.", JLogPanel.INFO);
		
		settingsGet = settingsUtil.readSFTPGetFromXml();

		pollingFrequancy = Duration.ofSeconds(Long.valueOf(settingsGet.pollFrequencySeconds.data));

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
			jcmd.writeToSystemLog("smtpGET Thread run mode : " + modeName[this.modeActive], JLogPanel.INFO);
		}
	}

	public void run()
	{
		jcmd.writeToSystemLog("smtpGET Thread started.", JLogPanel.INFO);

		while (run)
		{

			if (getRunMode() == Mode_RUN)
			{
				now = Instant.now();

				if (now.compareTo(due) > 0)
				{
					transferRemoteToLocal();
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
		jcmd.writeToSystemLog("smtpGET Thread stopped.", JLogPanel.INFO);
	}

	private void transferRemoteToLocal()
	{
		
		if (jcmd.isConnected() == false)
		{
			jcmd.connect();
		}

		if (jcmd.isConnected())
		{
			Vector<ChannelSftp.LsEntry> fileNames = jcmd.ls(settingsGet.remoteDir.data, settingsGet.remoteFileMask.data);

			for (ChannelSftp.LsEntry entry : fileNames)
			{

				FileUtils.deleteQuietly(new File(settingsGet.localDir.data + File.separator + entry.getFilename() + settingsGet.tempFileExtension.data));

				FileUtils.deleteQuietly(new File(settingsGet.localDir.data + File.separator + entry.getFilename()));

				jcmd.get(settingsGet.remoteDir.data + "/" + entry.getFilename(), settingsGet.localDir.data + File.separator + entry.getFilename() + settingsGet.tempFileExtension.data);

				try
				{
					FileUtils.moveFile(new File(settingsGet.localDir.data + File.separator + entry.getFilename() + settingsGet.tempFileExtension.data), new File(settingsGet.localDir.data + File.separator + entry.getFilename()));

				}
				catch (IOException e)
				{
					jcmd.writeToLog("smtpGET Thread error renaming " + e.getMessage(), JLogPanel.ERROR);
				}

				jcmd.rm(settingsGet.remoteDir.data + "/" + entry.getFilename());
			}

			//jcmd.disconnect();

		}

		if (getRunMode() != getRequestMode())
		{
			setRunMode(getRequestMode());
		}

	}

}
