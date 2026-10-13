package com.commander4j.thread;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;

import com.commander4j.jsch.JschCommands;
import com.commander4j.jsch.JschRecord;
import com.commander4j.log.JLogPanel;
import com.commander4j.settings.SettingUtil;
import com.commander4j.settings.SettingsCommon;
import com.commander4j.settings.SettingsGet;
import com.commander4j.util.JFileMask;
import com.commander4j.util.JFormat;
import com.commander4j.util.JSafeFile;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;

public class TransferGET extends Thread
{

	public static final int Mode_NONE = 0;
	public static final int Mode_RUN = 1;
	public static final int Mode_PAUSE = 2;
	public static final int Mode_SHUTDOWN = 3;
	public static final int Mode_CONFIG_UPDATE = 4;

	private static final String[] modeName = new String[] { "None", "Run", "Pause", "Shutdown", "Configuration Update" };
	
	@SuppressWarnings("unused")
	private int modeNone = Mode_NONE;
	private int modeActive = Mode_PAUSE;
	private int modeRequest = Mode_NONE;
	private int modePrevious = Mode_NONE;

	private boolean run = true;

	// Settings
	private HashMap<String, JschRecord> jschConfig;

	private SettingUtil settingsUtil = new SettingUtil();
	private SettingsCommon settingsCommon = new SettingsCommon();
	private SettingsGet settingsGet = new SettingsGet();
	private JschCommands jcmd;

	// Settings handed over from the screen, taken up between transfer passes.
	private AtomicReference<PendingSettings> pendingSettings = new AtomicReference<PendingSettings>();
	private volatile boolean applyingSettings = false;

	private static class PendingSettings
	{
		SettingsGet settingsGet;
		SettingsCommon settingsCommon;
		HashMap<String, JschRecord> jschConfig;
	}

	Properties cfg = new Properties();

	TemporalAmount pollingFrequancy = Duration.ofSeconds(10);

	Instant due = Instant.now();
	Instant now = Instant.now();

	Session jschSession;

	int logDestination = 0;

	// Local files with no remote counterpart, removed at the end of a completed sync.
	private ArrayList<File> syncDeletions = new ArrayList<File>();
	// Local folders with no remote counterpart, cleared out at the end of a completed sync.
	private ArrayList<File> syncFolderDeletions = new ArrayList<File>();

	// Each missing safe file is reported once, not on every poll.
	private boolean localSafeFileMissing = false;
	private boolean remoteSafeFileMissing = false;

	// Counts for the pass summary.
	private int receivedCount = 0;
	private int failedCount = 0;
	private int skippedCount = 0;

	public TransferGET(int destination)
	{
		this.logDestination = destination;
		
		jcmd = new JschCommands(logDestination);
		loadSettings();
	}

	public void loadSettings()
	{
		jcmd.writeToSystemLog("sftpGET Thread loading settings.", JLogPanel.INFO);
		
		assignSettings(settingsUtil.readSFTPGetFromXml(), settingsUtil.readSFTPCommonFromXml(), settingsUtil.readJschPropertiesFromXml());
	}

	private void assignSettings(SettingsGet newGet, SettingsCommon newCommon, HashMap<String, JschRecord> newJschConfig)
	{
		settingsGet = newGet;

		pollingFrequancy = Duration.ofSeconds(Long.valueOf(settingsGet.pollFrequencySeconds.data));

		settingsCommon = newCommon;
		jschConfig = newJschConfig;

		// An open session would carry on using the old host and login.
		jcmd.resetConnection();

		jcmd.assignCommonSettings(settingsCommon);
		jcmd.assignJschConfig(jschConfig);

		// Start the new settings straight away rather than at the end of the old polling interval.
		due = Instant.now();
		localSafeFileMissing = false;
		remoteSafeFileMissing = false;

		if (Boolean.valueOf(settingsGet.enabled.data) == false)
		{
			jcmd.writeToSystemLog("sftpGET is disabled in sftp_get.xml - no files will be received.", JLogPanel.WARN);
		}
	}

	/**
	 * Hands over settings from the screen. They are taken up by the transfer
	 * thread itself, never part way through a transfer pass.
	 */
	public void requestSettings(SettingsGet newGet, SettingsCommon newCommon, HashMap<String, JschRecord> newJschConfig)
	{
		PendingSettings pending = new PendingSettings();
		pending.settingsGet = newGet;
		pending.settingsCommon = newCommon;
		pending.jschConfig = newJschConfig;

		pendingSettings.set(pending);
	}

	public boolean isSettingsPending()
	{
		return (pendingSettings.get() != null) || applyingSettings;
	}

	private void applyPendingSettings()
	{
		applyingSettings = true;

		PendingSettings pending = pendingSettings.getAndSet(null);

		if (pending != null)
		{
			jcmd.writeToSystemLog("sftpGET Thread applying new settings.", JLogPanel.INFO);

			assignSettings(pending.settingsGet, pending.settingsCommon, pending.jschConfig);
		}

		applyingSettings = false;
	}

	public void requestMode(int mode)
	{
		this.modeRequest = mode;
	}

	public int getRequestMode()
	{
		return this.modeRequest;
	}

	public int getRunMode()
	{
		return this.modeActive;
	}

	public void setRunMode(int mode)
	{
		if (mode != Mode_NONE)
		{
			this.modePrevious = this.modeActive;
			this.modeActive = mode;
			this.modeRequest = Mode_NONE;
			jcmd.writeToSystemLog("sftpGET Thread run mode : " + modeName[this.modeActive], JLogPanel.INFO);
		}
	}

	public void run()
	{
		jcmd.writeToSystemLog("sftpGET Thread started.", JLogPanel.INFO);

		while (run)
		{
			// Between passes is the only place new settings are taken up.
			applyPendingSettings();

			if (getRunMode() == Mode_RUN)
			{
				now = Instant.now();

				if (now.compareTo(due) > 0)
				{
					if (Boolean.valueOf(settingsGet.enabled.data))
					{
						transferRemoteToLocal();
					}
					else
					{
						// Nothing to transfer while disabled, but a pause or new settings still have to be picked up.
						if (getRunMode() != getRequestMode())
						{
							setRunMode(getRequestMode());
						}
					}
					due = now.plus(pollingFrequancy);
				}
			}

			if (getRunMode() == Mode_CONFIG_UPDATE)
			{
				loadSettings();

				setRunMode(modePrevious);
			}

			if (getRunMode() == Mode_SHUTDOWN)
			{
				run = false;
			}

			if (getRunMode() == Mode_PAUSE)
			{
				// Nothing is fetched while paused, so the session is closed rather than left for the server to time out.
				if (jcmd.isConnected())
				{
					jcmd.disconnect();
				}

				if (getRunMode() != getRequestMode())
				{
					setRunMode(getRequestMode());
				}
			}

			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
				run = false;
			}
		}
		if (jcmd.isConnected())
		{
			jcmd.disconnect();
		}

		jcmd.writeToSystemLog("sftpGET Thread stopped.", JLogPanel.INFO);
	}

	private void transferRemoteToLocal()
	{
		// Nothing is fetched or removed unless the local folder holds its safe file.
		if (isLocalSafeFilePresent())
		{
			if (jcmd.isConnected() == false)
			{
				jcmd.connect();
			}

			// The listing which proved the safe file is the remote folder as it is now; the top folder is processed from it.
			JschCommands.RemoteListing topListing = jcmd.isConnected() ? remoteSafeFileListing() : null;

			if (jcmd.isConnected() && (topListing != null))
			{
				boolean includeSubFolders = Boolean.valueOf(settingsGet.includeSubFolders.data);
				boolean sync = Boolean.valueOf(settingsGet.syncEnabled.data);

				syncDeletions.clear();
				syncFolderDeletions.clear();
				receivedCount = 0;
				failedCount = 0;
				skippedCount = 0;
				Instant passStarted = Instant.now();

				receiveFolder(settingsGet.remoteDir.data, new File(settingsGet.localDir.data), includeSubFolders, sync, topListing);

				if ((receivedCount + failedCount + skippedCount) > 0)
				{
					jcmd.writeToLog("Transfer finished - " + receivedCount + " received, " + failedCount + " failed, " + skippedCount + " skipped in " + JFormat.seconds(Duration.between(passStarted, Instant.now()).toMillis()), JLogPanel.INFO);
				}

				if (jcmd.isConnected() == false)
				{
					jcmd.writeToLog("Transfer abandoned, not connected - retried on the next poll", JLogPanel.ERROR);
				}
				else if (getRunMode() != Mode_SHUTDOWN)
				{
					// The safe file has shown the remote folder to be the real one, so an empty remote folder empties the local folder too.
					for (File localFile : syncDeletions)
					{
						jcmd.writeToLog("Sync delete = " + localFile.getPath(), JLogPanel.INFO);

						if (localFile.delete() == false)
						{
							jcmd.writeToLog("Unable to delete " + localFile.getPath(), JLogPanel.ERROR);
						}
					}

					for (File localFolder : syncFolderDeletions)
					{
						removeLocalFolder(localFolder);
					}
				}

				//jcmd.disconnect();

			}
		}

		if (getRunMode() != getRequestMode())
		{
			setRunMode(getRequestMode());
		}

	}

	/**
	 * The safe file is the proof that the local folder is the one the files
	 * are meant for - and the one files may be deleted from.
	 */
	private boolean isLocalSafeFilePresent()
	{
		File safeFile = new File(settingsGet.localDir.data, JSafeFile.local);

		boolean present = safeFile.isFile();

		if (present)
		{
			if (localSafeFileMissing)
			{
				jcmd.writeToLog("Safe file found : " + safeFile.getPath(), JLogPanel.INFO);
			}
		}
		else
		{
			if (localSafeFileMissing == false)
			{
				jcmd.writeToLog("Nothing received, safe file not found : " + safeFile.getPath(), JLogPanel.WARN);
			}
		}

		localSafeFileMissing = (present == false);

		return present;
	}

	/**
	 * The safe file is the proof that the remote folder is the right one and
	 * is really there - a wrong path can look like an empty folder.
	 */
	/**
	 * Returns the listing of the remote folder when the safe file is in it,
	 * null when it is not (or the folder could not be listed). The pass
	 * processes the top folder from this same listing instead of listing it
	 * again - it is returned, not kept, so it lives for one pass.
	 */
	private JschCommands.RemoteListing remoteSafeFileListing()
	{
		String safeFile = remotePath(settingsGet.remoteDir.data, JSafeFile.remote);

		// The remote folder is listed because a server may answer a question about one file from a cache.
		JschCommands.RemoteListing listing = jcmd.listFolder(settingsGet.remoteDir.data, false);

		boolean present = (listing != null) && listing.hasFile(JSafeFile.remote);

		if (present)
		{
			if (remoteSafeFileMissing)
			{
				jcmd.writeToLog("Safe file found : " + safeFile, JLogPanel.INFO);
			}
		}
		else
		{
			if (remoteSafeFileMissing == false)
			{
				jcmd.writeToLog("Nothing received, safe file not found : " + safeFile, JLogPanel.WARN);
			}
		}

		remoteSafeFileMissing = (present == false);

		return present ? listing : null;
	}

	/**
	 * Processes one remote folder from one listing of it: the files matching
	 * the mask, the sync deletions and the sub folders all come from the same
	 * read. The top folder arrives with the listing taken for the safe file
	 * check; a sub folder is listed here. A folder which cannot be listed is
	 * reported and left alone - nothing fetched, nothing deleted, no descent.
	 */
	private void receiveFolder(String remoteFolder, File localFolder, boolean includeSubFolders, boolean sync, JschCommands.RemoteListing listing)
	{
		// The listing is a poll - a DEBUG row only - so at the Normal level nothing shows until a file to fetch is found.
		if (listing == null)
		{
			listing = jcmd.listFolder(remoteFolder, true);

			if (listing == null)
			{
				return;
			}
		}

		// One or more masks separated by ';' - a file matching any of them is fetched.
		Vector<ChannelSftp.LsEntry> fileNames = listing.matching(JFileMask.split(settingsGet.remoteFileMask.data));

		// Stability check: the listing above is the "before" picture of this folder; the folder is listed
		// again - a genuine fresh read, never the listing in hand - after the wait, just before the first
		// file is fetched from it.
		Instant listedAt = Instant.now();
		HashMap<String, SftpATTRS> freshEntries = null;

		if (sync && Boolean.valueOf(settingsGet.syncDeleteEnabled.data))
		{
			collectSyncDeletions(listing.entries(), localFolder, includeSubFolders && Boolean.valueOf(settingsGet.syncDeleteFoldersEnabled.data));
		}

		for (ChannelSftp.LsEntry entry : fileNames)
		{
			if ((getRunMode() == Mode_SHUTDOWN) || (jcmd.isConnected() == false))
			{
				// A lost session is reported once the pass is over; the rest waits for the next poll.
				return;
			}

			if (entry.getAttrs().isDir() || (isSafeName(entry.getFilename()) == false))
			{
				// A remote folder whose name matches the file mask is not a file to download.
				continue;
			}

			if (JSafeFile.isSafeFile(entry.getFilename()) || isTempFile(entry.getFilename()))
			{
				// The safe files are never fetched, whatever the file mask, and nor is a file still being written under the temporary extension.
				continue;
			}

			String remoteFile = remotePath(remoteFolder, entry.getFilename());
			File localFile = new File(localFolder, entry.getFilename());

			if (localFile.isDirectory())
			{
				jcmd.writeToLog("Download skipped, a local folder has the same name " + localFile.getPath(), JLogPanel.ERROR);
				continue;
			}

			if (sync && isLocalCopyCurrent(localFile, entry.getAttrs()))
			{
				continue;
			}

			if (Boolean.valueOf(settingsGet.stabilityCheckEnabled.data))
			{
				if (freshEntries == null)
				{
					if (awaitStability(listedAt) == false)
					{
						return;
					}

					freshEntries = jcmd.lsEntries(remoteFolder);

					if (freshEntries == null)
					{
						jcmd.writeToLog("Stability check abandoned, folder could not be listed again : " + remoteFolder + " - retried on the next poll", JLogPanel.ERROR);
						return;
					}
				}

				if (isStable(entry, freshEntries.get(entry.getFilename()), remoteFile) == false)
				{
					skippedCount++;
					continue;
				}
			}

			long size = entry.getAttrs().getSize();

			if (sync)
			{
				jcmd.writeToLog("Sync receiving " + remoteFile + " (" + JFormat.bytes(size) + ")", JLogPanel.INFO);
			}
			else
			{
				// A move shows the listing entry of each file it is about to fetch - the safe and temporary files above are never shown.
				jcmd.writeToLog(entry.toString(), JLogPanel.DIRECTORY);
				jcmd.writeToLog("Receiving " + remoteFile + " (" + JFormat.bytes(size) + ")", JLogPanel.INFO);
			}

			ensureLocalFolder(localFolder);

			Instant fileStarted = Instant.now();

			if (receiveFile(remoteFile, localFile))
			{
				if (sync)
				{
					// Carry the modified time across so the next poll sees the two copies as identical.
					localFile.setLastModified(entry.getAttrs().getMTime() * 1000L);
				}
				else
				{
					jcmd.rm(remoteFile);
				}

				receivedCount++;
				jcmd.writeToLog("Received " + localFile.getPath() + " (" + JFormat.bytes(size) + ") in " + JFormat.seconds(Duration.between(fileStarted, Instant.now()).toMillis()), JLogPanel.INFO);
			}
			else
			{
				failedCount++;
				jcmd.writeToLog("Download failed, remote file retained " + remoteFile, JLogPanel.ERROR);
			}
		}

		if (includeSubFolders)
		{
			for (String folderName : listing.folders())
			{
				if (getRunMode() == Mode_SHUTDOWN)
				{
					return;
				}

				if (isSafeName(folderName))
				{
					File localSubFolder = new File(localFolder, folderName);

					if (sync)
					{
						// A sync mirrors the folder structure, including folders with nothing to download.
						ensureLocalFolder(localSubFolder);
					}

					receiveFolder(remotePath(remoteFolder, folderName), localSubFolder, includeSubFolders, sync, null);
				}
			}
		}
	}

	/**
	 * Stability check, part one: waits until the folder listing is the
	 * configured number of seconds old. Done once per folder, just before the
	 * first file is fetched from it, so a folder with nothing to fetch never
	 * waits. Returns false if a shutdown arrived during the wait.
	 */
	private boolean awaitStability(Instant listedAt)
	{
		long waitMillis = stabilitySeconds() * 1000L - Duration.between(listedAt, Instant.now()).toMillis();

		if (waitMillis > 0)
		{
			jcmd.writeToLog("Stability check - waiting " + JFormat.seconds(waitMillis) + " before the first transfer", JLogPanel.INFO);

			Instant until = Instant.now().plusMillis(waitMillis);

			while (Instant.now().isBefore(until))
			{
				if ((getRunMode() == Mode_SHUTDOWN) || (getRequestMode() == Mode_SHUTDOWN))
				{
					return false;
				}

				try
				{
					Thread.sleep(100);
				}
				catch (InterruptedException e)
				{
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Stability check, part two: a file whose size or modified time differs
	 * between the two listings (or which has gone) is still being written, so
	 * it is left for the next poll - nothing is fetched or removed.
	 */
	private boolean isStable(ChannelSftp.LsEntry before, SftpATTRS after, String remoteFile)
	{
		if ((after == null) || after.isDir())
		{
			jcmd.writeToLog("Skipped, no longer there : " + remoteFile, JLogPanel.WARN);
			return false;
		}

		if ((before.getAttrs().getSize() != after.getSize()) || (before.getAttrs().getMTime() != after.getMTime()))
		{
			jcmd.writeToLog("Skipped, still changing : " + remoteFile + " (" + JFormat.bytes(before.getAttrs().getSize()) + " -> " + JFormat.bytes(after.getSize()) + ") - retried on the next poll", JLogPanel.WARN);
			return false;
		}

		return true;
	}

	private int stabilitySeconds()
	{
		try
		{
			return Math.max(1, Integer.parseInt(settingsGet.stabilitySeconds.data.trim()));
		}
		catch (Exception e)
		{
			return 5;
		}
	}

	/**
	 * Notes the local files which no longer exist in the matching remote
	 * folder and, when asked, the local folders too. Only files the transfer
	 * could have put there are ever removed - the name has to match the file
	 * mask and must not be a part-finished download. Nothing is noted if the
	 * remote folder cannot be listed.
	 */
	private void collectSyncDeletions(HashMap<String, SftpATTRS> remoteEntries, File localFolder, boolean syncDeleteFolders)
	{
		File[] localFiles = localFolder.listFiles();

		if ((remoteEntries != null) && (localFiles != null))
		{
			for (File localFile : localFiles)
			{
				String filename = localFile.getName();

				if (localFile.isFile())
				{
					SftpATTRS remoteEntry = remoteEntries.get(filename);

					if (((remoteEntry == null) || remoteEntry.isDir()) && isSyncDeleteCandidate(filename))
					{
						syncDeletions.add(localFile);
					}
				}
				else
				{
					// Links are not followed, and anything of that name on the server (whatever its case) keeps the local folder.
					if (syncDeleteFolders && localFile.isDirectory() && (Files.isSymbolicLink(localFile.toPath()) == false) && (isRemoteNamePresent(remoteEntries, filename) == false))
					{
						syncFolderDeletions.add(localFile);
					}
				}
			}
		}
	}

	private boolean isRemoteNamePresent(HashMap<String, SftpATTRS> remoteEntries, String name)
	{
		for (String remoteName : remoteEntries.keySet())
		{
			if (remoteName.equalsIgnoreCase(name))
			{
				return true;
			}
		}

		return false;
	}

	private boolean isSyncDeleteCandidate(String filename)
	{
		if (JSafeFile.isSafeFile(filename))
		{
			return false;
		}

		if (isTempFile(filename))
		{
			return false;
		}

		return JFileMask.matches(filename, settingsGet.remoteFileMask.data, IOCase.SENSITIVE);
	}

	/**
	 * A file carrying the temporary extension is one somebody is still
	 * writing, so it is left alone whatever the file mask says.
	 */
	private boolean isTempFile(String filename)
	{
		String tempFileExtension = settingsGet.tempFileExtension.data;

		return (tempFileExtension.equals("") == false) && filename.endsWith(tempFileExtension);
	}

	/**
	 * Clears out a local folder which no longer exists on the server. Only
	 * files the transfer could have put there are removed, and a folder is
	 * only removed once nothing is left in it. Returns true if the folder has
	 * gone.
	 */
	private boolean removeLocalFolder(File folder)
	{
		boolean removed = false;

		File[] entries = folder.listFiles();

		if (entries != null)
		{
			boolean empty = true;

			for (File entry : entries)
			{
				if (entry.isDirectory())
				{
					// Links are not followed.
					if (Files.isSymbolicLink(entry.toPath()) || (removeLocalFolder(entry) == false))
					{
						empty = false;
					}
				}
				else
				{
					if (isSyncDeleteCandidate(entry.getName()))
					{
						jcmd.writeToLog("Sync delete = " + entry.getPath(), JLogPanel.INFO);

						if (entry.delete() == false)
						{
							jcmd.writeToLog("Unable to delete " + entry.getPath(), JLogPanel.ERROR);
							empty = false;
						}
					}
					else
					{
						empty = false;
					}
				}
			}

			if (empty)
			{
				jcmd.writeToLog("Sync delete folder = " + folder.getPath(), JLogPanel.INFO);

				removed = folder.delete();

				if (removed == false)
				{
					jcmd.writeToLog("Unable to delete " + folder.getPath(), JLogPanel.ERROR);
				}
			}
		}

		return removed;
	}

	private boolean receiveFile(String remoteFile, File localFile)
	{
		File localTempFile = new File(localFile.getPath() + settingsGet.tempFileExtension.data);

		boolean received = false;

		FileUtils.deleteQuietly(localTempFile);

		if (jcmd.get(remoteFile, localTempFile.getPath()))
		{
			if (localTempFile.equals(localFile))
			{
				// No temporary extension configured - the download is already under the final name.
				received = true;
			}
			else
			{
				// Only replace the existing local copy once the download is complete.
				FileUtils.deleteQuietly(localFile);

				try
				{
					FileUtils.moveFile(localTempFile, localFile);

					received = true;
				}
				catch (IOException e)
				{
					jcmd.writeToLog("sftpGET Thread error renaming " + e.getMessage(), JLogPanel.ERROR);
				}
			}
		}
		else
		{
			// Remove the partial download.
			FileUtils.deleteQuietly(localTempFile);
		}

		return received;
	}

	private boolean isLocalCopyCurrent(File localFile, SftpATTRS remoteFile)
	{
		boolean result = false;

		if (localFile.isFile())
		{
			// SFTP holds modified times in whole seconds.
			if ((localFile.length() == remoteFile.getSize()) && (remoteFile.getMTime() <= (localFile.lastModified() / 1000)))
			{
				result = true;
			}
		}

		return result;
	}

	/**
	 * Creates a missing sub folder beneath the local folder. The local folder
	 * itself is never created.
	 */
	private void ensureLocalFolder(File folder)
	{
		if (folder.isDirectory() == false)
		{
			if (new File(settingsGet.localDir.data).isDirectory())
			{
				folder.mkdirs();
			}
		}
	}

	/**
	 * Names come from the server, so anything which could step outside the
	 * local folder is ignored.
	 */
	private boolean isSafeName(String name)
	{
		return (name.equals("") == false) && (name.equals(".") == false) && (name.equals("..") == false) && (name.indexOf('/') < 0) && (name.indexOf('\\') < 0);
	}

	private String remotePath(String folder, String name)
	{
		return folder.endsWith("/") ? folder + name : folder + "/" + name;
	}

}
