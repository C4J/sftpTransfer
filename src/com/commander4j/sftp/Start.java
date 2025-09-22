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
import com.commander4j.thread.ArchiveThread;
import com.commander4j.thread.EmailThread;
import com.commander4j.thread.ShutdownHook;
import com.commander4j.thread.TransferGET;
import com.commander4j.thread.TransferPUT;
import com.commander4j.util.JWait;

public class Start
{

	public Logger logger = org.apache.logging.log4j.LogManager.getLogger((TransferPUT.class));

	public LoggerContextFactory factory = LogManager.getFactory();

	public static JWait wait = new JWait();

	// Static variables;

	public static String version = "5.01";

	public static TransferPUT transferPut;
	public static TransferGET transferGet;
	public static EmailThread emailthread;
	public static ArchiveThread archiveThread;

	public static JFrameSFTPTransfer gui;

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

			if (args[0].equals("desktop"))
			{
				gui.setVisible(true);
			}

		}
		else
		{
			logger.info("sftpTransfer No Parameter Specified");
		}

		logger.info("sftpTransfer Stopped");
	}

	public static void requestServiceShutdown()
	{

			emailthread.addToQueue("Monitor", "Shutdown", "SFTP Transfer has stopped", "");

			transferPut.setRunMode(TransferPUT.Mode_SHUTDOWN);

			transferGet.setRunMode(TransferGET.Mode_SHUTDOWN);

			archiveThread.shutdown();

			emailthread.shutdown();

			waitforServicesShutdown();


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
