package com.commander4j.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.apache.commons.io.FileUtils;

public class JArchive
{

	JUtility util = new JUtility();

	public void archiveBackupFiles(String directory, int days, String directoryValidation)
	{
		{

			Path folder = Paths.get(directory);

			File validate = new File(directory + File.separator + directoryValidation);

			if (validate.isFile())
			{

				if (!Files.isDirectory(folder))
				{
					System.out.println("Not a directory: " + folder);
					return;
				}

				// Calculate the cutoff instant
				Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);

				try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder))
				{
					for (Path entry : stream)
					{
						if (Files.isRegularFile(entry))
						{
							if (entry.getFileName().endsWith(directoryValidation) == false)
							{
								FileTime lastModifiedTime = Files.getLastModifiedTime(entry);
								Instant fileInstant = lastModifiedTime.toInstant();

								if (fileInstant.isBefore(cutoff))
								{
									FileUtils.deleteQuietly(entry.toFile());
								}
							}
						}
					}
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

}
