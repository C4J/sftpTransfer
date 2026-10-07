package com.commander4j.thread;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.Logger;

import com.commander4j.jsch.JschCommands;
import com.commander4j.log.JLogPanel;
import com.commander4j.settings.SettingUtil;
import com.commander4j.settings.SettingsPut;
import com.commander4j.util.JArchive;
import com.commander4j.util.JWait;


public class ArchiveThread extends Thread
{
	private JWait wait = new com.commander4j.util.JWait();
	private SettingsPut settingsPut = new SettingsPut();
	private SettingUtil settingUtil = new SettingUtil();
	private JArchive archive = new JArchive();
	private int archiveRetention = 7;
	private boolean loadConfig = true;
	private boolean run=true;
	
	// Settings handed over from the screen, taken up between clean-up passes.
	private AtomicReference<SettingsPut> pendingSettings = new AtomicReference<SettingsPut>();
	
	private JschCommands jcmd;

	Logger logger = org.apache.logging.log4j.LogManager.getLogger((ArchiveThread.class));
	
	int logDestination = 0;

	public ArchiveThread(int destination)
	{
		super();
		
		this.logDestination = destination;
		
		jcmd = new JschCommands(logDestination);
	}
	
	public void shutdown()
	{

		loadConfig = false;
		run = false;
	}

	public void run()
	{

		jcmd.writeToSystemLog("Archive Thread started.", JLogPanel.INFO);
		
		loadConfigFromXML();
		
		while (run)
		{
			if (loadConfig)
			{
				loadConfigFromXML();
			}

			SettingsPut pending = pendingSettings.getAndSet(null);

			if (pending != null)
			{
				jcmd.writeToSystemLog("Archive Thread applying new settings.", JLogPanel.INFO);

				assignSettings(pending);
			}

			archive.archiveBackupFiles(settingsPut.backupDir.data,archiveRetention,"backup.folder");

			wait.manySec(1);
			
		}
		
		jcmd.writeToSystemLog("Archive Thread stopped.", JLogPanel.INFO);
	}
	
	public void loadConfig()
	{
		loadConfig = true;
	}

	/**
	 * Hands over settings from the screen, taken up by the archive thread itself.
	 */
	public void requestSettings(SettingsPut newPut)
	{
		pendingSettings.set(newPut);
	}
	
	private void loadConfigFromXML()
	{
		jcmd.writeToSystemLog("Archive Thread loading config.", JLogPanel.INFO);
		
		assignSettings(settingUtil.readSFTPPutFromXml());
		
		loadConfig=false;
	}
	
	private void assignSettings(SettingsPut newPut)
	{
		settingsPut = newPut;
		
		try
		{
			archiveRetention = Integer.parseInt(settingsPut.backupRetention.data);
		}
		catch (NumberFormatException ex)
		{
			archiveRetention = 7;
		}
	}
}
