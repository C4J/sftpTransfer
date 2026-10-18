package com.commander4j.email;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
	public static final String LIST_TRANSFER = "Transfer";

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

	/**
	 * The files one direction sent or received in a pass; the list is
	 * "Transfer". Sent only when the list exists and is enabled - a quiet
	 * skip, not the error or warning the other lists get, because most
	 * installs will leave it off. The subject is the same for every pass of
	 * a direction, so a frequency limit on this list would drop whole passes;
	 * it is meant to stay at 0.
	 */
	public static void transfer(int logDestination, String event, String... detail)
	{
		EmailThread thread = Start.emailthread;

		if ((thread == null) || (thread.isListActive(LIST_TRANSFER) == false))
		{
			return;
		}

		send(LIST_TRANSFER, direction(logDestination) + event, detail);
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

	/**
	 * The body of every email: an HTML table with the labels in one column
	 * and the values in the other. The standard rows (event, title, host,
	 * mode, time) come first, then the event's own lines. A detail line is
	 * "Label : value"; a line with no " : " spans both columns as plain text;
	 * a line starting with white space belongs to the row above (a file under
	 * the "Files :" heading); a blank line is a short gap. Everything is
	 * escaped, so an odd file name or exception message cannot break the
	 * layout. A few empty paragraphs follow the table to keep a gap between
	 * our text and any disclaimer a corporate mail system appends.
	 */
	public static String body(String event, String... detail)
	{
		ArrayList<String> lines = new ArrayList<String>();
		lines.add("Event : " + event);
		lines.add("Title : " + title);
		lines.add("Host : " + host());
		lines.add("Mode : " + (Start.gui == null ? "service" : "desktop"));
		// UTC, like the daily statistics schedule, so the same moment reads the same from every host.
		lines.add("Time : " + LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " UTC");

		if ((detail != null) && (detail.length > 0))
		{
			lines.add("");

			for (String line : detail)
			{
				if (line != null)
				{
					lines.add(line);
				}
			}
		}

		return html(lines);
	}

	private static final String FONT = "font-family:Arial,Helvetica,sans-serif;font-size:13px;line-height:18px;";
	private static final String LABEL_STYLE = FONT + "color:#555555;padding:3px 16px 3px 0;vertical-align:top;white-space:nowrap;";
	private static final String VALUE_STYLE = FONT + "color:#111111;padding:3px 0;vertical-align:top;word-break:break-all;";
	private static final String TEXT_STYLE = FONT + "color:#111111;padding:3px 0;";
	private static final String MONO = "font-family:Consolas,Menlo,'Courier New',monospace;font-size:12px;";

	/**
	 * Paths, file names and server messages read better in a fixed-width
	 * face; counts, sizes and host names stay in the body face. Decided from
	 * the value, not the label, so the daily statistics counts do not change
	 * face because a label happens to say "files".
	 */
	private static boolean monospaced(String label, String value)
	{
		return value.contains("/") || value.contains("\\") || label.equalsIgnoreCase("Reason");
	}

	/** The detail lines as the HTML table. Package-private so a probe can render it without a mail session. */
	static String html(List<String> lines)
	{
		StringBuilder b = new StringBuilder();
		b.append("<html><body style=\"margin:0;padding:12px;background-color:#ffffff;\">\n");
		b.append("<table cellspacing=\"0\" cellpadding=\"0\" border=\"0\" style=\"border-collapse:collapse;\">\n");

		boolean first = true;
		StringBuilder continuation = null;
		// False while the value cell is still empty (a "Files :" heading), so the first file does not start with a line break.
		boolean continuationStarted = false;

		for (int i = 0; i < lines.size(); i++)
		{
			String line = lines.get(i);

			if ((line.length() > 0) && Character.isWhitespace(line.charAt(0)) && (continuation != null))
			{
				// A line under a heading such as "Files :" - one line of the value cell above.
				String text = line.trim();

				if (continuationStarted)
				{
					continuation.append("<br>");
				}

				continuation.append("<span style=\"").append(MONO).append("\">").append(text.startsWith("...") ? "<i>" + escape(text) + "</i>" : escape(text)).append("</span>");
				continuationStarted = true;
				continue;
			}

			if (continuation != null)
			{
				b.append(continuation).append("</td></tr>\n");
				continuation = null;
			}

			if (line.trim().isEmpty())
			{
				b.append("<tr><td colspan=\"2\" style=\"height:8px;font-size:4px;line-height:4px;\">&nbsp;</td></tr>\n");
				continue;
			}

			int sep = line.indexOf(" : ");

			if ((sep < 0) && line.trim().endsWith(":"))
			{
				// "Files :" - a heading whose value is the indented lines which follow.
				sep = line.lastIndexOf(':');
			}

			if (sep < 0)
			{
				b.append("<tr><td colspan=\"2\" style=\"").append(TEXT_STYLE).append("\">").append(escape(line.trim())).append("</td></tr>\n");
				continue;
			}

			String label = line.substring(0, sep).trim();
			String value = line.substring(sep + 1).replaceFirst("^:", "").trim();

			String valueStyle = VALUE_STYLE + (monospaced(label, value) ? MONO : "");
			String valueHtml = escape(value);

			if (first)
			{
				// The event name is the one thing the reader wants first.
				valueHtml = "<b>" + valueHtml + "</b>";
				first = false;
			}

			b.append("<tr><td style=\"").append(LABEL_STYLE).append("\">").append(escape(label)).append("</td>");
			b.append("<td style=\"").append(valueStyle).append("\">").append(valueHtml);

			// The row stays open in case indented lines follow it.
			continuation = new StringBuilder();
			continuationStarted = (value.isEmpty() == false);
		}

		if (continuation != null)
		{
			b.append(continuation).append("</td></tr>\n");
		}

		b.append("</table>\n");

		// A gap before anything the receiving mail system appends. Outlook collapses repeated <br>, so empty paragraphs are used.
		for (int i = 0; i < 4; i++)
		{
			b.append("<p style=\"").append(FONT).append("margin:0;\">&nbsp;</p>\n");
		}

		b.append("</body></html>");

		return b.toString();
	}

	/** HTML-escaped, with a line break kept as one - an exception message can run to more than one line. */
	private static String escape(String text)
	{
		StringBuilder b = new StringBuilder(text.length() + 16);

		text = text.replace("\r", "");

		for (int i = 0; i < text.length(); i++)
		{
			char c = text.charAt(i);

			switch (c)
			{
			case '&':
				b.append("&amp;");
				break;
			case '<':
				b.append("&lt;");
				break;
			case '>':
				b.append("&gt;");
				break;
			case '"':
				b.append("&quot;");
				break;
			case '\'':
				b.append("&#39;");
				break;
			case '\n':
				b.append("<br>");
				break;
			default:
				b.append(c);
			}
		}

		return b.toString();
	}

	/** The most file lines a Transfer email carries - a sync pass can move thousands. */
	public static final int MAX_FILE_LINES = 500;

	/**
	 * Adds a "Files :" heading and one indented line per file to the detail
	 * lines of a Transfer email, stopping at MAX_FILE_LINES with a count of
	 * the rest.
	 */
	public static void appendFiles(List<String> detail, List<String> files)
	{
		detail.add("");
		detail.add("Files :");

		int shown = Math.min(files.size(), MAX_FILE_LINES);

		for (int i = 0; i < shown; i++)
		{
			detail.add("  " + files.get(i));
		}

		if (files.size() > shown)
		{
			detail.add("  ... and " + (files.size() - shown) + " more");
		}
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
