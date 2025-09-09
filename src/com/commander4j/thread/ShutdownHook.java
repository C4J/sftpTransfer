package com.commander4j.thread;

import org.apache.logging.log4j.LogManager;

import com.commander4j.sftp.Start;


public class ShutdownHook extends Thread
{

	@Override
	public void run()
	{

		Start.requestServiceShutdown();
	
		LogManager.shutdown();
		
	}

}
