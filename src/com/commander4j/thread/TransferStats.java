package com.commander4j.thread;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

import com.commander4j.util.JFormat;

/**
 * Running totals for the daily statistics email. The transfer threads add to
 * them as files go through; the stats thread reads and zeroes them once a
 * day. Held in memory only - a restart starts a new period.
 */
public final class TransferStats
{
	public static final AtomicLong putFiles = new AtomicLong();
	public static final AtomicLong putBytes = new AtomicLong();
	public static final AtomicLong putFailures = new AtomicLong();
	public static final AtomicLong getFiles = new AtomicLong();
	public static final AtomicLong getBytes = new AtomicLong();
	public static final AtomicLong getFailures = new AtomicLong();
	public static final AtomicLong errorsRaised = new AtomicLong();
	public static final AtomicLong errorsEmailed = new AtomicLong();

	private static volatile Instant periodStart = Instant.now();

	private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private TransferStats()
	{
	}

	public static void putOk(long bytes)
	{
		putFiles.incrementAndGet();
		putBytes.addAndGet(bytes);
	}

	public static void getOk(long bytes)
	{
		getFiles.incrementAndGet();
		getBytes.addAndGet(bytes);
	}

	public static void putFailed()
	{
		putFailures.incrementAndGet();
	}

	public static void getFailed()
	{
		getFailures.incrementAndGet();
	}

	public static final class Snapshot
	{
		public final long putFiles, putBytes, putFailures, getFiles, getBytes, getFailures, errorsRaised, errorsEmailed;
		public final Instant from, to;

		private Snapshot(long putFiles, long putBytes, long putFailures, long getFiles, long getBytes, long getFailures, long errorsRaised, long errorsEmailed, Instant from, Instant to)
		{
			this.putFiles = putFiles;
			this.putBytes = putBytes;
			this.putFailures = putFailures;
			this.getFiles = getFiles;
			this.getBytes = getBytes;
			this.getFailures = getFailures;
			this.errorsRaised = errorsRaised;
			this.errorsEmailed = errorsEmailed;
			this.from = from;
			this.to = to;
		}
	}

	/** Reads every counter and zeroes it, and starts the next period now. */
	public static synchronized Snapshot snapshotAndReset()
	{
		Instant now = Instant.now();

		Snapshot s = new Snapshot(putFiles.getAndSet(0), putBytes.getAndSet(0), putFailures.getAndSet(0), getFiles.getAndSet(0), getBytes.getAndSet(0), getFailures.getAndSet(0), errorsRaised.getAndSet(0), errorsEmailed.getAndSet(0), periodStart, now);

		periodStart = now;

		return s;
	}

	/** A read without reset, for the test email and the log. */
	public static synchronized Snapshot snapshot()
	{
		return new Snapshot(putFiles.get(), putBytes.get(), putFailures.get(), getFiles.get(), getBytes.get(), getFailures.get(), errorsRaised.get(), errorsEmailed.get(), periodStart, Instant.now());
	}

	public static String[] reportLines(Snapshot s)
	{
		return new String[] {
				"Period : " + stamp(s.from) + " to " + stamp(s.to),
				"Put files sent     : " + s.putFiles,
				"Put bytes sent     : " + JFormat.bytes(s.putBytes),
				"Put failures       : " + s.putFailures,
				"Get files received : " + s.getFiles,
				"Get bytes received : " + JFormat.bytes(s.getBytes),
				"Get failures       : " + s.getFailures,
				"Errors raised      : " + s.errorsRaised,
				"Errors emailed     : " + s.errorsEmailed };
	}

	private static String stamp(Instant i)
	{
		return LocalDateTime.ofInstant(i, ZoneId.systemDefault()).format(STAMP);
	}
}
