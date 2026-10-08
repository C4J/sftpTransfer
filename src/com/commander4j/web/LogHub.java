package com.commander4j.web;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.commander4j.jsch.JschCommands;

/**
 * In-memory copy of the three log windows (Put, Get, System) so the web page
 * can show them when there is no window at all - the service case.
 *
 * <p>{@link JschCommands} publishes here on the same path as it writes to the
 * log file, so the page sees exactly what the desktop tabs see, with the same
 * five row types, in service mode as well as desktop mode. Each destination
 * keeps the last {@link #CAPACITY} entries; the sequence number is global so a
 * page which reconnects can ask for only what it missed.
 *
 * <p>The listener (the web server's SSE hub) is optional and is called on the
 * logging thread, so it must never block - {@link SseHub#broadcast} only offers
 * to per-connection queues.
 */
public final class LogHub
{
	/** Entries kept per destination. */
	public static final int CAPACITY = 500;

	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	public static final class Entry
	{
		public final long seq;
		public final String time;
		public final int destination;
		public final int level;
		public final String message;

		Entry(long seq, String time, int destination, int level, String message)
		{
			this.seq = seq;
			this.time = time;
			this.destination = destination;
			this.level = level;
			this.message = message;
		}
	}

	private static final Object lock = new Object();
	private static final List<ArrayDeque<Entry>> buffers = new ArrayList<ArrayDeque<Entry>>();
	private static long nextSeq = 1;
	private static volatile Consumer<Entry> listener = null;

	static
	{
		for (int i = 0; i <= JschCommands.LogDestination_SYS; i++)
		{
			buffers.add(new ArrayDeque<Entry>(CAPACITY));
		}
	}

	private LogHub()
	{
	}

	/**
	 * Records one line. Lines for {@link JschCommands#LogDestination_NoGUI} are
	 * dropped, as the desktop does with them.
	 */
	public static void add(int destination, int level, String message)
	{
		if ((destination <= JschCommands.LogDestination_NoGUI) || (destination > JschCommands.LogDestination_SYS))
		{
			return;
		}

		Entry entry;

		synchronized (lock)
		{
			entry = new Entry(nextSeq++, LocalDateTime.now().format(TIME_FORMAT), destination, level, message == null ? "" : message);

			ArrayDeque<Entry> buffer = buffers.get(destination);

			if (buffer.size() >= CAPACITY)
			{
				buffer.pollFirst();
			}

			buffer.addLast(entry);
		}

		Consumer<Entry> l = listener;

		if (l != null)
		{
			try
			{
				l.accept(entry);
			}
			catch (RuntimeException e)
			{
				// A broken viewer must never disturb a transfer.
			}
		}
	}

	/** Entries for one destination with a sequence number above {@code afterSeq}, oldest first. */
	public static List<Entry> backlog(int destination, long afterSeq)
	{
		List<Entry> result = new ArrayList<Entry>();

		if ((destination <= JschCommands.LogDestination_NoGUI) || (destination > JschCommands.LogDestination_SYS))
		{
			return result;
		}

		synchronized (lock)
		{
			for (Entry entry : buffers.get(destination))
			{
				if (entry.seq > afterSeq)
				{
					result.add(entry);
				}
			}
		}

		return result;
	}

	/** At most one listener - the running web server, or null when it is stopped. */
	public static void setListener(Consumer<Entry> l)
	{
		listener = l;
	}
}
