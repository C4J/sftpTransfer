package com.commander4j.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;

/**
 * A file mask setting holds one or more wildcard masks separated by ';', for
 * example "*.xml;*.csv". Each mask uses '*' and '?' only. Spaces around a mask
 * and empty masks are ignored, so "*.xml; *.csv;" is the same as "*.xml;*.csv".
 * A blank setting matches nothing.
 */
public class JFileMask
{

	public static final String separator = ";";

	/**
	 * The individual masks in a setting, trimmed, with empty ones dropped.
	 */
	public static List<String> split(String maskSetting)
	{
		ArrayList<String> masks = new ArrayList<String>();

		if (maskSetting != null)
		{
			for (String mask : maskSetting.split(separator))
			{
				mask = mask.trim();

				if (mask.equals("") == false)
				{
					masks.add(mask);
				}
			}
		}

		return masks;
	}

	/**
	 * True if the name matches any of the masks.
	 */
	public static boolean matches(String filename, List<String> masks, IOCase ioCase)
	{
		for (String mask : masks)
		{
			if (FilenameUtils.wildcardMatch(filename, mask, ioCase))
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * True if the name matches any of the masks in the setting.
	 */
	public static boolean matches(String filename, String maskSetting, IOCase ioCase)
	{
		return matches(filename, split(maskSetting), ioCase);
	}

	/**
	 * The masks joined with the separator, as shown in a log line.
	 */
	public static String describe(List<String> masks)
	{
		return String.join(separator, masks);
	}

}
