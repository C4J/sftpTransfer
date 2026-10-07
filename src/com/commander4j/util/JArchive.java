package com.commander4j.util;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

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

				// Backups of files sent from sub folders are held in matching sub folders.
				try (Stream<Path> stream = Files.walk(folder))
				{
					for (Path entry : (Iterable<Path>) stream::iterator)
					{
						if (Files.isRegularFile(entry))
						{
							if ((entry.getFileName().endsWith(directoryValidation) == false) && (JSafeFile.isSafeFile(entry.getFileName().toString()) == false))
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
				catch (IOException | UncheckedIOException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

}
