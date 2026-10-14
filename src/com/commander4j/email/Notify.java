package com.commander4j.email;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.commander4j.email.core.Email;
import com.commander4j.jsch.JschCommands;
import com.commander4j.settings.SettingsCommon;
import com.commander4j.sftp.Start;
import com.commander4j.thread.EmailThread;
import com.commander4j.thread.TransferStats;

/**
 * The one way the program raises an email. Builds the subject the same way
 * every time - "SFTP Transfer <version> <Put|Get ><event> for [<title>] on
 * <host>" - so the distribution list's frequency limit, which is keyed on
 * list + subject, suppresses a repeat of the same event. Anything that
 * varies from one occurrence to the next (file name, reason) belongs in the
 * body lines, never in the event text.
 *
 * Everything goes through the checked EmailThread.addToQueue(Email), so the
 * Email Notifications switch and the queue cap apply to every sender.
 */
public final class Notify
{
	public static final String LIST_ERROR = "Error";
	public static final String LIST_MONITOR = "Monitor";

	private static volatile String title = "";
	private static volatile String host = null;

	private Notify()
	{
	}

	/**
	 * Called at start-up and whenever settings are handed over. The host is
	 * resolved once; the lookup can take a while on a machine with no DNS, so
	 * it is done here, off the Swing thread, rather than on the first email.
	 */
	public static void configure(SettingsCommon common)
	{
		String t = (common == null || common.title.data == null) ? "" : common.title.data.trim();
		title = t;

		if (host == null)
		{
			host = resolveHost();
		}
	}

	/** An error of one direction's transfers; the list is "Error". */
	public static void error(int logDestination, String event, String... detail)
	{
		TransferStats.errorsRaised.incrementAndGet();
		send(LIST_ERROR, direction(logDestination) + event, detail);
	}

	/** Start, stop and the daily statistics; the list is "Monitor". */
	public static void monitor(String event, String... detail)
	{
		send(LIST_MONITOR, event, detail);
	}

	public static String subject(String event)
	{
		String t = title.isEmpty() ? "SFTP Transfer" : title;
		return "SFTP Transfer " + Start.version + " " + event + " for [" + t + "] on " + host();
	}

	/** The body every email starts with, before the event's own lines. */
	public static String body(String event, String... detail)
	{
		StringBuilder b = new StringBuilder();
		b.append("Event : ").append(event).append('\n');
		b.append("Title : ").append(title).append('\n');
		b.append("Host  : ").append(host()).append('\n');
		b.append("Mode  : ").append(Start.gui == null ? "service" : "desktop").append('\n');
		b.append("Time  : ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append('\n');

		for (String line : detail)
		{
			if (line != null)
			{
				b.append(line).append('\n');
			}
		}

		return b.toString();
	}

	/** Exception class and message for a Reason line - an IOException's message is often just a path. */
	public static String reason(Throwable e)
	{
		if (e == null)
		{
			return "";
		}

		String msg = e.getMessage();

		return e.getClass().getSimpleName() + (((msg == null) || msg.isBlank()) ? "" : " - " + msg.trim());
	}

	private static String direction(int logDestination)
	{
		switch (logDestination)
		{
		case JschCommands.LogDestination_PUT:
			return "Put ";
		case JschCommands.LogDestination_GET:
			return "Get ";
		default:
			return "";
		}
	}

	private static void send(String list, String event, String... detail)
	{
		EmailThread thread = Start.emailthread;

		if (thread == null)
		{
			// The window is built before the threads - nothing to queue on yet.
			return;
		}

		thread.addToQueue(new Email(list, subject(event), body(event, detail), ""));
	}

	private static String host()
	{
		String h = host;

		if (h == null)
		{
			h = resolveHost();
			host = h;
		}

		return h;
	}

	private static String resolveHost()
	{
		try
		{
			String h = InetAddress.getLocalHost().getHostName();

			if ((h != null) && (h.isBlank() == false))
			{
				return h;
			}
		}
		catch (Exception e)
		{
			// Fall through to the environment.
		}

		String h = System.getenv("COMPUTERNAME");

		if ((h == null) || h.isBlank())
		{
			h = System.getenv("HOSTNAME");
		}

		return ((h == null) || h.isBlank()) ? "unknown" : h;
	}
}
