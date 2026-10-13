package com.commander4j.thread;

import com.commander4j.jsch.JschCommands;
import com.commander4j.log.JLogPanel;
import com.commander4j.settings.ReloadRequest;
import com.commander4j.sftp.Start;

/**
 * Service mode only: polls for the reload request file once a second and,
 * when it appears, removes it and has Start re-read the settings from disk.
 * The request is removed before the re-read, so a second request landing
 * during the reload is seen on the next poll rather than lost. A daemon
 * thread, polled rather than a file-system watch - Windows raises duplicate
 * events and an editor's atomic save shows as a delete and a create, while a
 * poll has no platform quirks and the rest of the program polls anyway.
 */
public class ReloadWatcher extends Thread
{
	private volatile boolean run = true;

	public ReloadWatcher()
	{
		setName("ReloadWatcher");
		setDaemon(true);
	}

	public void shutdown()
	{
		run = false;
	}

	public void run()
	{
		// A request which cannot be removed is reported once, not once a second, and never acted on -
		// acting on it would reload (and reconnect) on every poll until someone removed it by hand.
		boolean removalFailed = false;
		int tick = 0;

		while (run)
		{
			// Every 5 s: a viewer whose port was busy is tried again.
			if (++tick % 5 == 0)
			{
				Start.retryWebViewer();
			}

			if (ReloadRequest.isPending())
			{
				if (ReloadRequest.clear())
				{
					removalFailed = false;

					Start.reloadSettingsFromDisk();
				}
				else if (removalFailed == false)
				{
					removalFailed = true;

					new JschCommands(JschCommands.LogDestination_SYS).writeToLog("Settings reload requested but " + ReloadRequest.getFile().getPath() + " could not be removed (permissions, or another program holding it) - ignored until it can be", JLogPanel.WARN);
				}
			}
			else
			{
				removalFailed = false;
			}

			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e)
			{
				run = false;
			}
		}
	}
}
