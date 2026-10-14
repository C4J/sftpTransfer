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
import com.commander4j.email.Notify;
import com.commander4j.gui.frame.JFrameSFTPTransfer;
import com.commander4j.jsch.JschCommands;
import com.commander4j.log.JLogPanel;
import com.commander4j.jsch.JschRecord;
import com.commander4j.settings.ReloadRequest;
import com.commander4j.settings.ServiceLock;
import com.commander4j.settings.SettingUtil;
import com.commander4j.settings.SettingsCommon;
import com.commander4j.settings.SettingsGet;
import com.commander4j.settings.SettingsPut;
import com.commander4j.thread.ArchiveThread;
import com.commander4j.thread.EmailThread;
import com.commander4j.thread.ReloadWatcher;
import com.commander4j.thread.ShutdownHook;
import com.commander4j.thread.StatsThread;
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

	public static String version = "6.12";

	public static TransferPUT transferPut;
	public static TransferGET transferGet;
	public static EmailThread emailthread;
	public static ArchiveThread archiveThread;
	public static StatsThread statsThread;

	// Whether the transfers are running, as far as the Started / Stopped emails are concerned.
	private static boolean processingRunning = false;

	public static JFrameSFTPTransfer gui;

	// Service mode only - collects the desktop's reload requests. Null in desktop mode.
	public static ReloadWatcher reloadWatcher;

	// Read-only web log viewer - null while it is off. Title shown on the page.
	public static WebServer webServer;
	public static String webTitle = "";

	// What the settings last asked of the viewer, so a service can retry a port which was busy at the time.
	private static boolean webWantedEnabled = false;
	private static int webWantedPort = WebServer.DEFAULT_PORT;

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

			// Read once here for the stats thread, the email subjects and the web viewer below.
			SettingsCommon settingsCommon = new SettingUtil().readSFTPCommonFromXml();
			Notify.configure(settingsCommon);

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

			statsThread = new StatsThread(settingsCommon);
			statsThread.start();

			if (args[0].equals("service"))
			{
				JschCommands syslog = new JschCommands(JschCommands.LogDestination_SYS);

				// The lock is how a desktop on this machine knows the service is running - see ServiceLock.
				if (ServiceLock.acquire())
				{
					syslog.writeToLog("Service lock taken : " + ServiceLock.getFile().getPath() + " - a desktop on this machine will leave the transfers and the web viewer to this service", JLogPanel.INFO);
				}
				else
				{
					syslog.writeToLog("Service lock NOT taken : " + ServiceLock.getFile().getPath() + " - " + ServiceLock.getLastAcquireError() + " - another service already running, or no permission ? A desktop on this machine will not know this service is here", JLogPanel.WARN);
				}

				// A request left over from before this start is pointless - the settings were just read.
				if (ReloadRequest.clear())
				{
					new JschCommands(JschCommands.LogDestination_SYS).writeToLog("Stale " + ReloadRequest.FILENAME + " removed - the settings have just been read", JLogPanel.INFO);
				}

				reloadWatcher = new ReloadWatcher();
				reloadWatcher.start();

				// A service processes from the moment it starts; the desktop only when its Run button is pressed.
				processingStateChanged(true, "service started");
			}

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
	 * Hands a complete set of settings to the four threads and the web viewer.
	 * The desktop's Apply passes copies of what is on screen; a service reload
	 * passes what it has just read from disk. Each thread takes its settings up
	 * itself, never part way through a transfer pass; the viewer changes at once.
	 */
	public static void handOverSettings(SettingsCommon common, SettingsPut put, SettingsGet get, HashMap<String, EmailRecord> email, HashMap<String, DistributionRecord> dist, HashMap<String, JschRecord> jsch)
	{
		Notify.configure(common);
		emailthread.requestSettings(common, email, dist);
		statsThread.requestSettings(common);
		archiveThread.requestSettings(put);
		transferPut.requestSettings(put, common, jsch);
		transferGet.requestSettings(get, common, jsch);

		applyWebSettings(Boolean.valueOf(common.webEnabled.data), WebServer.parsePort(common.webPort.data, WebServer.DEFAULT_PORT), common.title.data);
	}

	/**
	 * Service mode, on a reload request: re-reads the six settings files and
	 * hands them over. A file which does not parse stops the whole reload - the
	 * readers would return defaults for it, and defaults must never reach
	 * running threads - and the request is simply dropped; the next Apply
	 * writes another.
	 */
	public static synchronized void reloadSettingsFromDisk()
	{
		JschCommands syslog = new JschCommands(JschCommands.LogDestination_SYS);
		SettingUtil settingUtil = new SettingUtil();

		String badFile = settingUtil.checkConfigFiles();

		if (badFile != null)
		{
			syslog.writeToLog("Settings reload requested but " + badFile + " could not be parsed - settings unchanged, request dropped", JLogPanel.ERROR);
			return;
		}

		syslog.writeToLog("Settings reload requested - re-reading xml/config and notifying threads of new configuration", JLogPanel.INFO);

		handOverSettings(settingUtil.readSFTPCommonFromXml(), settingUtil.readSFTPPutFromXml(), settingUtil.readSFTPGetFromXml(), settingUtil.readEmailPropertiesFromXml(), settingUtil.readDistributionListFromXml(), settingUtil.readJschPropertiesFromXml());
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
		webWantedEnabled = enabled;
		webWantedPort = port;

		// A desktop beside a running service leaves the viewer to the service - two listeners would fight for the port.
		if (enabled && ServiceLock.isHeldElsewhere())
		{
			if (webServer != null)
			{
				webServer.stop();
				syslog.writeToLog("Web log viewer stopped on port " + webServer.getPort() + " - a service on this machine owns the viewer", JLogPanel.INFO);
				webServer = null;
			}
			else
			{
				syslog.writeToLog("Web log viewer not started - a service on this machine owns the viewer", JLogPanel.INFO);
			}

			return;
		}

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

	/**
	 * Service mode, from the reload watcher: a viewer wanted but not listening
	 * (its port was busy when the settings were applied - a desktop's viewer,
	 * most likely, since released) is tried again, quietly. Only success is
	 * logged; the failure was logged when it happened.
	 */
	public static synchronized void retryWebViewer()
	{
		if (webWantedEnabled && (webServer == null))
		{
			try
			{
				WebServer server = new WebServer(webWantedPort);
				server.start();
				webServer = server;
				new JschCommands(JschCommands.LogDestination_SYS).writeToLog("Web log viewer listening on port " + webWantedPort + " - http://<this host>:" + webWantedPort + "/ (the port has become free)", JLogPanel.INFO);
			}
			catch (Exception e)
			{
				// Still busy - tried again on the next round.
			}
		}
	}

	public static void requestServiceShutdown()
	{
			// No reload may start once the threads are being stopped.
			if (reloadWatcher != null)
			{
				reloadWatcher.shutdown();
				reloadWatcher = null;
			}

			if (webServer != null)
			{
				webServer.stop();
				webServer = null;
			}

			// Queued before the email thread is told to stop, so it goes out in the drain.
			processingStateChanged(false, "shutdown");

			transferPut.setRunMode(TransferPUT.Mode_SHUTDOWN);

			transferGet.setRunMode(TransferGET.Mode_SHUTDOWN);

			archiveThread.shutdown();

			statsThread.shutdown();

			emailthread.shutdown();

			waitforServicesShutdown();

			// Released only now, so a desktop on this machine does not take over until the transfers have really stopped.
			ServiceLock.release();

			LogManager.getLogger(Start.class).info("sftpTransfer Stopped");

	}

	/**
	 * The Started / Stopped emails to the Monitor list. "Processing" means the
	 * transfer threads are running: a service from start to shutdown, the
	 * desktop while its Run button is down. The latch makes a repeat (window
	 * close after the button was released, say) a no-op.
	 */
	public static synchronized void processingStateChanged(boolean running, String reason)
	{
		if (running == processingRunning)
		{
			return;
		}

		processingRunning = running;

		Notify.monitor(running ? "Started" : "Stopped", "Reason : " + reason);
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
