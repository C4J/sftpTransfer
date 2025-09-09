package com.commander4j.thread;

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

			archive.archiveBackupFiles(settingsPut.backupDir.data,archiveRetention,"backup.folder");

			wait.manySec(1);
			
		}
		
		jcmd.writeToSystemLog("Archive Thread stopped.", JLogPanel.INFO);
	}
	
	public void loadConfig()
	{
		loadConfig = true;
	}
	
	private void loadConfigFromXML()
	{
		jcmd.writeToSystemLog("Archive Thread loading config.", JLogPanel.INFO);
		
		settingsPut = settingUtil.readSFTPPutFromXml();
		
		try
		{
			archiveRetention = Integer.parseInt(settingsPut.backupRetention.data);
		}
		catch (NumberFormatException ex)
		{
			archiveRetention = 7;
		}
		
		loadConfig=false;
	}
}
