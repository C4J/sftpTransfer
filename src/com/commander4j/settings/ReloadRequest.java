package com.commander4j.settings;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * The reload request file - how the desktop tells a service running on the
 * same machine that the settings on disk have changed. Its existence is the
 * signal; what it holds is of no importance, so an empty file dropped into
 * the folder by hand works just as well. The desktop writes it on Apply, after
 * the six settings files have been saved, so a request always points at whole
 * files. The service polls for it, removes it, then re-reads the settings and
 * hands them to its threads exactly as the desktop's Apply does. A request
 * left over at service start-up is removed unread - the settings were just
 * read - and the desktop removes its own request if no service collects it.
 */
public class ReloadRequest
{
	public static final String FILENAME = "reload.request";

	private static final File file = new File("./xml/config", FILENAME);

	public static File getFile()
	{
		return file;
	}

	/**
	 * Creates the request. Returns false if the file could not be written.
	 */
	public static boolean request()
	{
		try
		{
			FileWriter writer = new FileWriter(file);
			writer.write("Settings reload requested " + LocalDateTime.now().toString() + System.lineSeparator());
			writer.close();
			return true;
		}
		catch (IOException e)
		{
			return false;
		}
	}

	public static boolean isPending()
	{
		return file.isFile();
	}

	/**
	 * Removes the request. Returns true if there was one to remove.
	 */
	public static boolean clear()
	{
		if (file.isFile())
		{
			return file.delete();
		}

		return false;
	}
}
