package com.commander4j.util;

/**
 * Short human-readable forms for the transfer log lines.
 */
public class JFormat
{
	/** 0 B, 512 B, 12.3 KB, 4.0 MB, 1.2 GB */
	public static String bytes(long size)
	{
		if (size < 1024)
		{
			return size + " B";
		}

		double value = size / 1024.0;
		String[] units = new String[] { "KB", "MB", "GB", "TB" };
		int unit = 0;

		while ((value >= 1024.0) && (unit < units.length - 1))
		{
			value = value / 1024.0;
			unit++;
		}

		return String.format("%.1f %s", value, units[unit]);
	}

	/** 0.4 s, 12.0 s, 1 m 05 s */
	public static String seconds(long millis)
	{
		if (millis < 60000)
		{
			return String.format("%.1f s", millis / 1000.0);
		}

		long seconds = millis / 1000;

		return String.format("%d m %02d s", seconds / 60, seconds % 60);
	}
}
