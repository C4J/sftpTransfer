package com.commander4j.settings;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.time.LocalDateTime;

/**
 * How the desktop knows a service is running on this machine. The service
 * takes an operating-system lock on this file for its whole life; the
 * desktop tries the same lock every couple of seconds and, when it cannot
 * have it, knows the service is there. Nothing is configured and nothing can
 * go stale: the lock is released the moment the service process ends,
 * however it ends. The file itself is left behind - it holds nothing of
 * value, and removing it under another process would fail on Windows anyway.
 */
public class ServiceLock
{
	public static final String FILENAME = "service.lock";

	private static final File file = new File("./xml/config", FILENAME);

	private static RandomAccessFile raf = null;
	private static FileChannel channel = null;
	private static FileLock lock = null;

	// Why the last acquire() or isHeldElsewhere() failed, for the caller to log; null when the last call went through.
	private static String lastAcquireError = null;
	private static String lastProbeError = null;

	public static synchronized String getLastAcquireError()
	{
		return lastAcquireError;
	}

	public static synchronized String getLastProbeError()
	{
		return lastProbeError;
	}

	public static File getFile()
	{
		return file;
	}

	/**
	 * Service mode: takes the lock for the life of the process. Returns false
	 * if another process already holds it.
	 */
	public static synchronized boolean acquire()
	{
		if (lock != null)
		{
			return true;
		}

		try
		{
			lastAcquireError = null;

			raf = new RandomAccessFile(file, "rw");
			channel = raf.getChannel();
			lock = channel.tryLock();

			if (lock == null)
			{
				lastAcquireError = "already locked by another process";
				close();
				return false;
			}

			raf.setLength(0);
			raf.write(("Held by the sftpTransfer service, process " + ProcessHandle.current().pid() + ", since " + LocalDateTime.now() + System.lineSeparator()).getBytes("UTF-8"));

			return true;
		}
		catch (Exception e)
		{
			lastAcquireError = e.toString();
			close();
			return false;
		}
	}

	public static synchronized void release()
	{
		try
		{
			if (lock != null)
			{
				lock.release();
			}
		}
		catch (Exception e)
		{
			// Nothing to do - the process is ending and the system drops the lock anyway.
		}

		close();
	}

	public static synchronized boolean isHeldByThisProcess()
	{
		return lock != null;
	}

	/**
	 * Desktop: true when another process holds the lock, which is to say a
	 * service is running on this machine. False when there is no lock file,
	 * when the lock is free, when this process holds it, or when the file
	 * system cannot lock at all - in which case nothing is known and the
	 * desktop carries on as if alone.
	 */
	public static synchronized boolean isHeldElsewhere()
	{
		if (lock != null)
		{
			return false;
		}

		if (file.isFile() == false)
		{
			return false;
		}

		try (RandomAccessFile probe = new RandomAccessFile(file, "rw"); FileChannel probeChannel = probe.getChannel())
		{
			FileLock probeLock = probeChannel.tryLock();

			lastProbeError = null;

			if (probeLock == null)
			{
				return true;
			}

			probeLock.release();
			return false;
		}
		catch (Exception e)
		{
			// Nothing is known - the caller should say so, since "alone" is then only an assumption.
			lastProbeError = e.toString();
			return false;
		}
	}

	private static void close()
	{
		lock = null;

		try
		{
			if (channel != null)
			{
				channel.close();
			}
		}
		catch (Exception e)
		{
		}

		try
		{
			if (raf != null)
			{
				raf.close();
			}
		}
		catch (Exception e)
		{
		}

		channel = null;
		raf = null;
	}
}
