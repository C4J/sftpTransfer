package com.commander4j.thread;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;

import com.commander4j.email.Notify;
import com.commander4j.jsch.JschCommands;
import com.commander4j.log.JLogPanel;
import com.commander4j.settings.ServiceLock;
import com.commander4j.settings.SettingsCommon;
import com.commander4j.sftp.Start;

/**
 * Sends the daily statistics email to the Monitor list at the configured
 * time of day (this machine's clock), once per day, and zeroes the counters.
 * Its own thread rather than a job of the email thread, because that thread
 * can sit inside one SMTP send for as long as the mail server takes, and
 * only takes new settings when its queue is empty.
 */
public class StatsThread extends Thread
{
	public static final LocalTime DEFAULT_TIME = LocalTime.of(6, 0);
	public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

	private volatile boolean run = true;
	private final AtomicReference<SettingsCommon> pending = new AtomicReference<SettingsCommon>();

	private boolean enabled = false;
	private LocalTime reportTime = DEFAULT_TIME;

	// The day of the last report (or skip) - one attempt per day.
	private LocalDate lastRunDate = null;

	private final JschCommands jcmd = new JschCommands(JschCommands.LogDestination_SYS);

	public StatsThread(SettingsCommon common)
	{
		super();
		setName("StatsThread");
		setDaemon(true);
		apply(common);
	}

	public void shutdown()
	{
		run = false;
	}

	/** Hands over settings from the screen or a reload; taken up on the next loop. */
	public void requestSettings(SettingsCommon common)
	{
		pending.set(common);
	}

	/** The HH:mm setting, or the default when blank or malformed. */
	public static LocalTime parseTime(String text, LocalTime fallback)
	{
		try
		{
			return LocalTime.parse(text.trim(), TIME_FORMAT);
		}
		catch (Exception e)
		{
			return fallback;
		}
	}

	private void apply(SettingsCommon common)
	{
		enabled = Boolean.valueOf(common.statsReportEnabled.data);
		reportTime = parseTime(common.statsReportTime.data, DEFAULT_TIME);

		// A start (or a settings change) after today's time has passed must not send a report for an empty period.
		if (LocalTime.now().isBefore(reportTime))
		{
			lastRunDate = null;
		}
		else
		{
			lastRunDate = LocalDate.now();
		}

		jcmd.writeToSystemLog("Daily statistics email " + (enabled ? "at " + reportTime.format(TIME_FORMAT) : "off"), JLogPanel.INFO);
	}

	public void run()
	{
		jcmd.writeToSystemLog("Stats Thread started.", JLogPanel.INFO);

		while (run)
		{
			SettingsCommon common = pending.getAndSet(null);

			if (common != null)
			{
				jcmd.writeToSystemLog("Stats Thread applying new settings.", JLogPanel.INFO);
				apply(common);
			}

			if (enabled)
			{
				LocalDate today = LocalDate.now();

				if ((today.equals(lastRunDate) == false) && (LocalTime.now().isBefore(reportTime) == false))
				{
					lastRunDate = today;

					if ((Start.gui != null) && ServiceLock.isHeldElsewhere())
					{
						jcmd.writeToSystemLog("Daily statistics email skipped - a service on this machine runs the transfers.", JLogPanel.INFO);
					}
					else
					{
						TransferStats.Snapshot s = TransferStats.snapshotAndReset();
						Notify.monitor("Daily Statistics", TransferStats.reportLines(s));
						jcmd.writeToSystemLog("Daily statistics email queued, counters reset.", JLogPanel.INFO);
					}
				}
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

		jcmd.writeToSystemLog("Stats Thread stopped.", JLogPanel.INFO);
	}
}
