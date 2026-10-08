package com.commander4j.sftp;

import java.io.File;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.util.DefaultShutdownCallbackRegistry;
import org.apache.logging.log4j.spi.LoggerContextFactory;

import com.commander4j.email.DistributionRecord;
import com.commander4j.email.EmailRecord;
import com.commander4j.gui.frame.JFrameSFTPTransfer;
import com.commander4j.jsch.JschCommands;
import com.commander4j.log.JLogPanel;
import com.commander4j.settings.SettingUtil;
import com.commander4j.settings.SettingsCommon;
import com.commander4j.thread.ArchiveThread;
import com.commander4j.thread.EmailThread;
import com.commander4j.thread.ShutdownHook;
import com.commander4j.thread.TransferGET;
import com.commander4j.thread.TransferPUT;
import com.commander4j.util.JWait;
import com.commander4j.web.WebServer;

public class Start
{

	public Logger logger = org.apache.logging.log4j.LogManager.getLogger((Start.class));

	public LoggerContextFactory factory = LogManager.getFactory();

	public static JWait wait = new JWait();

	// Static variables;

	public static String version = "6.10";

	public static TransferPUT transferPut;
	public static TransferGET transferGet;
	public static EmailThread emailthread;
	public static ArchiveThread archiveThread;

	public static JFrameSFTPTransfer gui;

	// Read-only web log viewer - null while it is off. Title shown on the page.
	public static WebServer webServer;
	public static String webTitle = "";

	public HashMap<String, EmailRecord> emailConfig = new HashMap<String, EmailRecord>();
	public HashMap<String, DistributionRecord> distConfig = new HashMap<String, DistributionRecord>();

	public static void main(String[] args)
	{

		Start start = new Start();
		start.Begin(args);

	}

	public void Begin(String[] args)
	{
		initLogging("");

		logger.info("sftpTransfer Starting");

		if (args.length == 1)
		{

			int putThreadRunMode = TransferPUT.Mode_PAUSE;
			int getThreadRunMode = TransferGET.Mode_PAUSE;
			int emailLogDestination = JschCommands.LogDestination_NoGUI;
			int archiveLogDestination = JschCommands.LogDestination_NoGUI;
			int putLogDestination = JschCommands.LogDestination_NoGUI;
			int getLogDestination = JschCommands.LogDestination_NoGUI;

			// Pass JFrame to Services for GUI mode
			if (args[0].equals("desktop"))
			{
				gui = new JFrameSFTPTransfer();


				putThreadRunMode = TransferPUT.Mode_PAUSE;

				getThreadRunMode = TransferGET.Mode_PAUSE;
			}

			// No GUI in service mode
			if (args[0].equals("service"))
			{
				ShutdownHook shutdownHook = new ShutdownHook();
				Runtime.getRuntime().addShutdownHook(shutdownHook);

				putThreadRunMode = TransferPUT.Mode_RUN;

				getThreadRunMode = TransferGET.Mode_RUN;
			}

			putLogDestination = JschCommands.LogDestination_PUT;

			getLogDestination = JschCommands.LogDestination_GET;

			emailLogDestination = JschCommands.LogDestination_SYS;

			archiveLogDestination = JschCommands.LogDestination_SYS;

			// Start Services

			emailthread = new EmailThread(emailLogDestination);
			emailthread.setName("EmailThread");
			emailthread.start();

			archiveThread = new ArchiveThread(archiveLogDestination);
			archiveThread.setName("ArchiveThread");
			archiveThread.start();

			transferPut = new TransferPUT(putLogDestination);
			transferPut.setName("PutThread");
			transferPut.setRunMode(putThreadRunMode);
			transferPut.start();

			transferGet = new TransferGET(getLogDestination);
			transferGet.setName("GetThread");
			transferGet.setRunMode(getThreadRunMode);
			transferGet.start();

			emailthread.addToQueue("Monitor", "Starting", "SFTP Transfer has started", "");

			SettingsCommon settingsCommon = new SettingUtil().readSFTPCommonFromXml();
			applyWebSettings(Boolean.valueOf(settingsCommon.webEnabled.data), WebServer.parsePort(settingsCommon.webPort.data, WebServer.DEFAULT_PORT), settingsCommon.title.data);

			if (args[0].equals("desktop"))
			{
				gui.setVisible(true);
			}

			logger.info("sftpTransfer Started");

		}
		else
		{
			logger.info("sftpTransfer No Parameter Specified");

			logger.info("sftpTransfer Stopped");
		}
	}

	/**
	 * Starts, stops or moves the web log viewer to match the settings. Called at
	 * start-up and whenever settings are applied from the desktop. A port which
	 * cannot be opened is reported on the System log and the transfers carry on
	 * without the viewer.
	 */
	public static synchronized void applyWebSettings(boolean enabled, int port, String title)
	{
		JschCommands syslog = new JschCommands(JschCommands.LogDestination_SYS);

		webTitle = title == null ? "" : title;

		if ((webServer != null) && ((enabled == false) || (webServer.getPort() != port)))
		{
			webServer.stop();
			syslog.writeToLog("Web log viewer stopped on port " + webServer.getPort(), JLogPanel.INFO);
			webServer = null;
		}

		if (enabled && (webServer == null))
		{
			try
			{
				WebServer server = new WebServer(port);
				server.start();
				webServer = server;
				syslog.writeToLog("Web log viewer listening on port " + port + " - http://<this host>:" + port + "/", JLogPanel.INFO);
			}
			catch (Exception e)
			{
				syslog.writeToLog("Web log viewer could not open port " + port + " - " + e.getMessage(), JLogPanel.WARN);
			}
		}
	}

	public static void requestServiceShutdown()
	{
			if (webServer != null)
			{
				webServer.stop();
				webServer = null;
			}

			emailthread.addToQueue("Monitor", "Shutdown", "SFTP Transfer has stopped", "");

			transferPut.setRunMode(TransferPUT.Mode_SHUTDOWN);

			transferGet.setRunMode(TransferGET.Mode_SHUTDOWN);

			archiveThread.shutdown();

			emailthread.shutdown();

			waitforServicesShutdown();

			LogManager.getLogger(Start.class).info("sftpTransfer Stopped");

	}

	public static void waitforServicesShutdown()
	{
		while (transferPut.isAlive())
		{
			wait.milliSec(100);
		}

		while (transferGet.isAlive())
		{
			wait.milliSec(100);
		}

		while (archiveThread.isAlive())
		{
			wait.milliSec(100);
		}

		while (emailthread.isAlive())
		{
			wait.milliSec(100);
		}

	}

	public void initLogging(String filename)
	{
		if (filename.isEmpty())
		{
			filename = System.getProperty("user.dir") + File.separator + "xml" + File.separator + "config" + File.separator + "log4j2.xml";
		}

		LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
		File file = new File(filename);

		context.setConfigLocation(file.toURI());

		if (factory instanceof Log4jContextFactory)
		{

			Log4jContextFactory contextFactory = (Log4jContextFactory) factory;

			((DefaultShutdownCallbackRegistry) contextFactory.getShutdownCallbackRegistry()).stop();
		}

	}

}
