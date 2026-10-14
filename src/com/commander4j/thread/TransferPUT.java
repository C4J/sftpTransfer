package com.commander4j.thread;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.commander4j.email.Notify;
import com.commander4j.jsch.JschCommands;
import com.commander4j.jsch.JschRecord;
import com.commander4j.log.JLogPanel;
import com.commander4j.settings.SettingUtil;
import com.commander4j.settings.SettingsCommon;
import com.commander4j.settings.SettingsPut;
import com.commander4j.util.JFileMask;
import com.commander4j.util.JFormat;
import com.commander4j.util.JSafeFile;
import com.jcraft.jsch.SftpATTRS;

public class TransferPUT extends Thread
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
	private SettingsPut settingsPut = new SettingsPut();
	private JschCommands jcmd;

	// Settings handed over from the screen, taken up between transfer passes.
	private AtomicReference<PendingSettings> pendingSettings = new AtomicReference<PendingSettings>();
	private volatile boolean applyingSettings = false;

	private static class PendingSettings
	{
		SettingsPut settingsPut;
		SettingsCommon settingsCommon;
		HashMap<String, JschRecord> jschConfig;
	}

	Properties cfg = new Properties();

	TemporalAmount pollingFrequancy = Duration.ofSeconds(10);

	Instant due = Instant.now();
	Instant now = Instant.now();

	int logDestination = 0;

	// Each missing safe file is reported once, not on every poll.
	private boolean localSafeFileMissing = false;
	private boolean remoteSafeFileMissing = false;

	// Stability check: the size and modified time of every candidate file as it was found,
	// and when. The wait runs once per pass, just before the first file is sent.
	private HashMap<File, long[]> stabilitySnapshot = new HashMap<File, long[]>();
	private Instant collectedAt = Instant.now();
	private boolean stabilityWaited = false;

	// The local folder whose heading has been written this pass - see announceFolder().
	private String announcedFolder = null;

	// Reported once, not on every poll.
	private boolean localFolderMissing = false;

	// Why sendFile() last returned false when no SFTP command threw, for the email.
	private String lastSendError = "";

	// Counts for the pass summary, and the files sent for the Transfer email.
	private int sentCount = 0;
	private int failedCount = 0;
	private int skippedCount = 0;
	private final ArrayList<String> sentFiles = new ArrayList<String>();


	public TransferPUT(int destination)
	{
		this.logDestination = destination;
		
		jcmd = new JschCommands(logDestination);
		loadSettings();

	}

	public void loadSettings()
	{
		jcmd.writeToSystemLog("sftpPUT Thread loading settings.", JLogPanel.INFO);
		
		assignSettings(settingsUtil.readSFTPPutFromXml(), settingsUtil.readSFTPCommonFromXml(), settingsUtil.readJschPropertiesFromXml());
	}

	private void assignSettings(SettingsPut newPut, SettingsCommon newCommon, HashMap<String, JschRecord> newJschConfig)
	{
		settingsPut = newPut;

		pollingFrequancy = Duration.ofSeconds(Long.valueOf(settingsPut.pollFrequencySeconds.data));

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

		if (Boolean.valueOf(settingsPut.enabled.data) == false)
		{
			jcmd.writeToSystemLog("sftpPUT is disabled in sftp_put.xml - no files will be sent.", JLogPanel.WARN);
		}
	}

	/**
	 * Hands over settings from the screen. They are taken up by the transfer
	 * thread itself, never part way through a transfer pass.
	 */
	public void requestSettings(SettingsPut newPut, SettingsCommon newCommon, HashMap<String, JschRecord> newJschConfig)
	{
		PendingSettings pending = new PendingSettings();
		pending.settingsPut = newPut;
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
			jcmd.writeToSystemLog("sftpPUT Thread applying new settings.", JLogPanel.INFO);

			assignSettings(pending.settingsPut, pending.settingsCommon, pending.jschConfig);
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
			jcmd.writeToSystemLog("sftpPUT Thread run mode : " + modeName[this.modeActive], JLogPanel.INFO);
		}
	}

	public void run()
	{
		jcmd.writeToSystemLog("sftpPUT Thread started.", JLogPanel.INFO);

		while (run)
		{
			// Between passes is the only place new settings are taken up.
			applyPendingSettings();

			if (getRunMode() == Mode_RUN)
			{
				now = Instant.now();

				if (now.compareTo(due) > 0)
				{
					if (Boolean.valueOf(settingsPut.enabled.data))
					{
						transferLocalToRemote();
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
				// Nothing is sent while paused, so the session is closed rather than left for the server to time out.
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

		jcmd.writeToSystemLog("sftpPUT Thread stopped.", JLogPanel.INFO);
	}

	private void transferLocalToRemote()
	{
		// Nothing is sent or removed unless the local folder holds its safe file.
		if (isLocalSafeFilePresent())
		{
			sendLocalFolder();
		}

		if (getRunMode() != getRequestMode())
		{
			setRunMode(getRequestMode());
		}

	}

	/**
	 * The safe file is the proof that the local folder is the right one and
	 * is really there - a share which has not mounted looks like an empty
	 * folder.
	 */
	private boolean isLocalSafeFilePresent()
	{
		File safeFile = new File(settingsPut.localDir.data, JSafeFile.local);

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
				jcmd.writeToLog("Nothing sent, safe file not found : " + safeFile.getPath(), JLogPanel.WARN);
				Notify.error(logDestination, "Safe file missing", "Safe file : " + safeFile.getPath(), "Side : local");
			}
		}

		localSafeFileMissing = (present == false);

		return present;
	}

	/**
	 * The safe file is the proof that the remote folder is the one the files
	 * are meant for - and the one files may be deleted from. Returns the
	 * listing of the remote folder when the safe file is in it, null when it
	 * is not (or the folder could not be listed). The caller's sync pass takes
	 * its view of the remote folder from this same listing instead of listing
	 * the folder again - it is returned, not kept, so it lives for one pass.
	 */
	private JschCommands.RemoteListing remoteSafeFileListing()
	{
		String safeFile = remotePath(settingsPut.remoteDir.data, JSafeFile.remote);

		// The remote folder is listed because a server may answer a question about one file from a cache.
		JschCommands.RemoteListing listing = jcmd.listFolder(settingsPut.remoteDir.data, false);

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
				jcmd.writeToLog("Nothing sent, safe file not found : " + safeFile, JLogPanel.WARN);
				Notify.error(logDestination, "Safe file missing", "Safe file : " + safeFile, "Side : remote");
			}
		}

		remoteSafeFileMissing = (present == false);

		return present ? listing : null;
	}

	private void sendLocalFolder()
	{

		boolean includeSubFolders = Boolean.valueOf(settingsPut.includeSubFolders.data);
		boolean sync = Boolean.valueOf(settingsPut.syncEnabled.data);
		boolean syncDelete = sync && Boolean.valueOf(settingsPut.syncDeleteEnabled.data);
		boolean syncDeleteFolders = syncDelete && includeSubFolders && Boolean.valueOf(settingsPut.syncDeleteFoldersEnabled.data);

		announcedFolder = null;
		stabilitySnapshot.clear();
		collectedAt = Instant.now();
		stabilityWaited = false;
		sentCount = 0;
		failedCount = 0;
		skippedCount = 0;
		sentFiles.clear();
		Instant passStarted = Instant.now();

		File sourceDirectory = new File(settingsPut.localDir.data);
		// One or more masks separated by ';' - a file matching any of them is sent.
		FileFilter fileFilter = WildcardFileFilter.builder().setWildcards(JFileMask.split(settingsPut.localFileMask.data)).setIoCase(IOCase.INSENSITIVE).get();

		// Relative folder ("" is the local folder itself) and the files in it which match the mask.
		LinkedHashMap<String, ArrayList<File>> folders = new LinkedHashMap<String, ArrayList<File>>();

		if (sourceDirectory.isDirectory())
		{
			collectLocalFiles(sourceDirectory, "", fileFilter, includeSubFolders, folders);
			localFolderMissing = false;
		}
		else
		{
			jcmd.writeToLog("Local folder not found " + settingsPut.localDir.data, JLogPanel.ERROR);

			if (localFolderMissing == false)
			{
				localFolderMissing = true;
				Notify.error(logDestination, "Local folder not found", "Folder : " + settingsPut.localDir.data);
			}
		}

		int fileCount = 0;

		for (ArrayList<File> files : folders.values())
		{
			fileCount = fileCount + files.size();
		}

		// A move only needs the server when there are files waiting. A sync also has to create any sub folders missing from the server, and look for files to remove from it.
		if ((fileCount > 0) || (sync && ((folders.size() > 1) || syncDelete)))
		{
			if (jcmd.isConnected() == false)
			{
				jcmd.connect();
			}
			// The listing which proved the safe file is the remote folder as it is now; the top folder's sync pass uses it below.
			JschCommands.RemoteListing topListing = jcmd.isConnected() ? remoteSafeFileListing() : null;

			if (jcmd.isConnected() && (topListing != null))
			{
				if (sync == false)
				{
					jcmd.writeToLog("Files found :" + fileCount, JLogPanel.INFO);
				}

				int fileNumber = 0;

				for (Map.Entry<String, ArrayList<File>> folder : folders.entrySet())
				{
					String relativeFolder = folder.getKey();
					String remoteFolder = remotePath(settingsPut.remoteDir.data, relativeFolder);
					File localFolder = relativeFolder.equals("") ? sourceDirectory : new File(sourceDirectory, relativeFolder.replace("/", File.separator));

					HashMap<String, SftpATTRS> remoteFiles = null;

					// This folder's remote files and folders with no local counterpart - removed once its files are up, before the next folder is started.
					ArrayList<String> syncDeletions = new ArrayList<String>();
					ArrayList<String> syncFolderDeletions = new ArrayList<String>();

					if (sync)
					{
						// One read of the remote folder serves the file comparison and the folder comparison below.
						// The top folder was read a moment ago for the safe file check; a sub folder is read here.
						JschCommands.RemoteListing listing = relativeFolder.equals("") ? topListing : jcmd.listFolder(remoteFolder, false);

						if (listing == null)
						{
							if (relativeFolder.equals("") == false)
							{
								jcmd.mkdirs(settingsPut.remoteDir.data, relativeFolder);
							}

							remoteFiles = new HashMap<String, SftpATTRS>();
						}
						else
						{
							remoteFiles = listing.files();
						}

						if (syncDelete)
						{
							for (String remoteName : remoteFiles.keySet())
							{
								if (isSyncDeleteCandidate(remoteName) && (new File(localFolder, remoteName).exists() == false))
								{
									syncDeletions.add(remotePath(remoteFolder, remoteName));
								}
							}

							if (syncDeleteFolders)
							{
								HashMap<String, SftpATTRS> remoteEntries = (listing == null) ? null : listing.entries();

								if (remoteEntries != null)
								{
									for (Map.Entry<String, SftpATTRS> remoteEntry : remoteEntries.entrySet())
									{
										// Anything of that name on the local side (a link, an unreadable folder, the backup folder) keeps the remote folder.
										if (remoteEntry.getValue().isDir() && (new File(localFolder, remoteEntry.getKey()).exists() == false))
										{
											syncFolderDeletions.add(remotePath(remoteFolder, remoteEntry.getKey()));
										}
									}
								}
							}
						}
					}
					else
					{
						if ((relativeFolder.equals("") == false) && (folder.getValue().size() > 0))
						{
							jcmd.mkdirs(settingsPut.remoteDir.data, relativeFolder);
						}
					}

					for (File sourceFile : folder.getValue())
					{
						if (getRunMode() == Mode_SHUTDOWN)
						{
							return;
						}

						if (jcmd.isConnected() == false)
						{
							// The session has gone part way through - the rest waits for the next poll and a new connection.
							jcmd.writeToLog("Transfer abandoned, not connected - remaining files are retried on the next poll", JLogPanel.ERROR);
							return;
						}

						fileNumber++;

						String filename = sourceFile.getName();
						String relativeFile = remotePath(relativeFolder, filename);

						boolean replacingNewer = false;

						if (sync)
						{
							int state = remoteCopyState(sourceFile, remoteFiles.get(filename));

							if (state == COPY_IDENTICAL)
							{
								continue;
							}

							if (state == COPY_NEWER)
							{
								if (Boolean.valueOf(settingsPut.overwriteNewer.data) == false)
								{
									// Polling detail - this file is kept on every poll until the local copy catches up.
									jcmd.writeToLog("Kept, remote copy is newer : " + relativeFile, JLogPanel.DEBUG);
									continue;
								}

								replacingNewer = true;
							}
						}

						// Something is about to happen in this folder, so its heading goes first.
						announceFolder(localFolder);

						// Stability check - the wait happens once, before the first file is actually sent.
						if (Boolean.valueOf(settingsPut.stabilityCheckEnabled.data))
						{
							if (awaitStability() == false)
							{
								return;
							}

							if (isStable(sourceFile, relativeFile) == false)
							{
								skippedCount++;
								continue;
							}
						}

						long size = sourceFile.length();

						if (sync)
						{
							jcmd.writeToLog("Sync sending " + relativeFile + " (" + JFormat.bytes(size) + ")" + (replacingNewer ? " - replacing the newer remote copy" : ""), JLogPanel.INFO);
						}
						else
						{
							jcmd.writeToLog("Sending " + String.valueOf(fileNumber) + " of " + fileCount + " : " + relativeFile + " (" + JFormat.bytes(size) + ")", JLogPanel.INFO);
						}

						if (sourceFile.isFile())
						{
							Instant fileStarted = Instant.now();


							// A backup is a copy of a file about to be moved away. In sync mode the source stays, so none is taken
							// (the form greys the row out, keeping the tick for when sync is switched off again).
							if ((sync == false) && (Boolean.valueOf(settingsPut.backupEnabled.data) == true))
							{
								if (settingsPut.backupDir.data.equals("") == false)
								{
									File backupFile = new File(settingsPut.backupDir.data + File.separator + relativeFile.replace("/", File.separator));

									try
									{
										FileUtils.copyFile(sourceFile, backupFile, false);

									}
									catch (IOException e)
									{
										jcmd.writeToLog("Backup copy failed " + backupFile.getPath() + " - " + Notify.reason(e), JLogPanel.ERROR);
										Notify.error(logDestination, "Backup copy failed", "File : " + sourceFile.getPath(), "Backup : " + backupFile.getPath(), "Reason : " + Notify.reason(e));
									}
									finally
									{
										backupFile = null;
									}
								}
							}

							try
							{

								if (sendFile(sourceFile, remoteFolder))
								{
									if (sync)
									{
										// Carry the modified time across so the next poll sees the two copies as identical.
										jcmd.setMtime(remotePath(remoteFolder, filename), (int) (sourceFile.lastModified() / 1000));
									}
									else
									{
										if (FileUtils.deleteQuietly(sourceFile) == false)
										{
											jcmd.writeToLog("Unable to delete " + sourceFile.getPath() + " - it will be sent again on the next poll", JLogPanel.ERROR);
											Notify.error(logDestination, "Local delete failed", "Path : " + sourceFile.getPath());
										}
									}

									sentCount++;
									sentFiles.add(relativeFile + " (" + JFormat.bytes(size) + ")");
									TransferStats.putOk(size);
									jcmd.writeToLog("Sent " + relativeFile + " (" + JFormat.bytes(size) + ") in " + JFormat.seconds(Duration.between(fileStarted, Instant.now()).toMillis()), JLogPanel.INFO);
								}
								else
								{
									failedCount++;
									TransferStats.putFailed();
									jcmd.writeToLog("Upload failed, local file retained " + sourceFile.getPath(), JLogPanel.ERROR);
									Notify.error(logDestination, "Upload failed", "File : " + sourceFile.getPath(), "Remote folder : " + remoteFolder, "Size : " + JFormat.bytes(size), "Reason : " + (lastSendError.isEmpty() ? jcmd.getLastError() : lastSendError));
								}

							}
							catch (Exception e)
							{
								failedCount++;
								TransferStats.putFailed();
								jcmd.writeToLog("Upload failed, local file retained " + sourceFile.getPath() + " - " + Notify.reason(e), JLogPanel.ERROR);
								Notify.error(logDestination, "Upload failed", "File : " + sourceFile.getPath(), "Remote folder : " + remoteFolder, "Size : " + JFormat.bytes(size), "Reason : " + Notify.reason(e));
							}
							finally
							{

							}

						}

					}

					if ((syncDeletions.size() + syncFolderDeletions.size()) > 0)
					{
						if (getRunMode() == Mode_SHUTDOWN)
						{
							return;
						}

						if (jcmd.isConnected() == false)
						{
							logPassSummary(passStarted);
							jcmd.writeToLog("Sync deletions abandoned, not connected - retried on the next poll", JLogPanel.ERROR);
							return;
						}

						// The safe file has shown the local folder to be the real one, so an empty local folder empties the remote folder too.
						announceFolder(localFolder);

						for (String remoteFile : syncDeletions)
						{
							jcmd.writeToLog("Sync delete = " + remoteFile, JLogPanel.INFO);
							jcmd.rm(remoteFile);
						}

						for (String remoteSubFolder : syncFolderDeletions)
						{
							removeRemoteFolder(remoteSubFolder);
						}
					}

				}

				logPassSummary(passStarted);

			}
			//jcmd.disconnect();

		}

	}

	/**
	 * One blue heading per folder per pass, written just before the first
	 * line about something happening in it - a poll which finds nothing to
	 * do in a folder stays silent, as before.
	 */
	private void announceFolder(File localFolder)
	{
		String path = localFolder.getPath();

		if (path.equals(announcedFolder) == false)
		{
			announcedFolder = path;
			jcmd.writeToLog("Folder " + path, JLogPanel.NORMAL);
		}
	}

	/**
	 * One line at the end of a pass in which at least one file was sent,
	 * failed or skipped - a pass with nothing to do stays silent. A pass
	 * which sent something also goes to the Transfer list, with the files
	 * named; failures are the Error list's business.
	 */
	private void logPassSummary(Instant passStarted)
	{
		if ((sentCount + failedCount + skippedCount) > 0)
		{
			String duration = JFormat.seconds(Duration.between(passStarted, Instant.now()).toMillis());

			jcmd.writeToLog("Transfer finished - " + sentCount + " sent, " + failedCount + " failed, " + skippedCount + " skipped in " + duration, JLogPanel.INFO);

			if (sentCount > 0)
			{
				ArrayList<String> detail = new ArrayList<String>();
				detail.add("Local folder  : " + settingsPut.localDir.data);
				detail.add("Remote folder : " + settingsPut.remoteDir.data);
				detail.add("Sent     : " + sentCount);
				detail.add("Failed   : " + failedCount);
				detail.add("Skipped  : " + skippedCount);
				detail.add("Duration : " + duration);
				Notify.appendFiles(detail, sentFiles);

				Notify.transfer(logDestination, "Files sent", detail.toArray(new String[0]));
			}
		}
	}

	/**
	 * Stability check, part one: the files were sized and timed as they were
	 * collected; this waits until that was the configured number of seconds
	 * ago. Done once per pass, just before the first transfer, so a pass
	 * with nothing to send never waits. Returns false if a shutdown arrived
	 * during the wait.
	 */
	private boolean awaitStability()
	{
		if (stabilityWaited == false)
		{
			stabilityWaited = true;

			long waitMillis = stabilitySeconds() * 1000L - Duration.between(collectedAt, Instant.now()).toMillis();

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
		}

		return true;
	}

	/**
	 * Stability check, part two: a file whose size or modified time differs
	 * from when it was collected (or which has gone) is still being written,
	 * so it is left for the next poll - no backup, no upload, no delete.
	 */
	private boolean isStable(File sourceFile, String relativeFile)
	{
		long[] before = stabilitySnapshot.get(sourceFile);

		if (sourceFile.isFile() == false)
		{
			jcmd.writeToLog("Skipped, no longer there : " + relativeFile, JLogPanel.WARN);
			return false;
		}

		long size = sourceFile.length();
		long modified = sourceFile.lastModified();

		if ((before == null) || (before[0] != size) || (before[1] != modified))
		{
			String sizes = (before == null) ? JFormat.bytes(size) : JFormat.bytes(before[0]) + " -> " + JFormat.bytes(size);
			jcmd.writeToLog("Skipped, still changing : " + relativeFile + " (" + sizes + ") - retried on the next poll", JLogPanel.WARN);
			return false;
		}

		return true;
	}

	private int stabilitySeconds()
	{
		try
		{
			return Math.max(1, Integer.parseInt(settingsPut.stabilitySeconds.data.trim()));
		}
		catch (Exception e)
		{
			return 5;
		}
	}

	/**
	 * Only files the transfer could have put there are ever removed - the
	 * name has to match the file mask and must not be a part-finished upload.
	 */
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

		return JFileMask.matches(filename, settingsPut.localFileMask.data, IOCase.INSENSITIVE);
	}

	/**
	 * Clears out a remote folder which no longer exists locally. Only files
	 * the transfer could have put there are removed, and a folder is only
	 * removed once nothing is left in it. Returns true if the folder has gone.
	 */
	private boolean removeRemoteFolder(String remoteFolder)
	{
		boolean removed = false;

		HashMap<String, SftpATTRS> remoteEntries = jcmd.lsEntries(remoteFolder);

		if (remoteEntries != null)
		{
			boolean empty = true;

			for (Map.Entry<String, SftpATTRS> remoteEntry : remoteEntries.entrySet())
			{
				String remoteName = remotePath(remoteFolder, remoteEntry.getKey());

				if (remoteEntry.getValue().isDir())
				{
					if (removeRemoteFolder(remoteName) == false)
					{
						empty = false;
					}
				}
				else
				{
					if (isSyncDeleteCandidate(remoteEntry.getKey()))
					{
						jcmd.writeToLog("Sync delete = " + remoteName, JLogPanel.INFO);

						if (jcmd.rm(remoteName) == false)
						{
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
				jcmd.writeToLog("Sync delete folder = " + remoteFolder, JLogPanel.INFO);
				removed = jcmd.rmdir(remoteFolder);
			}
		}

		return removed;
	}

	private void collectLocalFiles(File folder, String relativeFolder, FileFilter fileFilter, boolean includeSubFolders, LinkedHashMap<String, ArrayList<File>> folders)
	{
		ArrayList<File> files = new ArrayList<File>();

		File[] entries = folder.listFiles();

		// A folder which cannot be read is left out altogether, so it is never mistaken for an empty one.
		if (entries != null)
		{
			folders.put(relativeFolder, files);

			Arrays.sort(entries);

			for (File entry : entries)
			{
				// The safe files are never sent, whatever the file mask, and nor is a file still being written under the temporary extension.
				if (entry.isFile() && fileFilter.accept(entry) && (JSafeFile.isSafeFile(entry.getName()) == false) && (isTempFile(entry.getName()) == false))
				{
					files.add(entry);
					stabilitySnapshot.put(entry, new long[] { entry.length(), entry.lastModified() });
				}
			}

			if (includeSubFolders)
			{
				for (File entry : entries)
				{
					// Links are not followed and the backup folder is never sent, even if it lives beneath the local folder.
					if (entry.isDirectory() && (Files.isSymbolicLink(entry.toPath()) == false) && (isBackupFolder(entry) == false))
					{
						collectLocalFiles(entry, remotePath(relativeFolder, entry.getName()), fileFilter, includeSubFolders, folders);
					}
				}
			}
		}
	}

	/**
	 * A file carrying the temporary extension is one somebody is still
	 * writing, so it is left alone whatever the file mask says.
	 */
	private boolean isTempFile(String filename)
	{
		String tempFileExtension = settingsPut.tempFileExtension.data;

		return (tempFileExtension.equals("") == false) && filename.endsWith(tempFileExtension);
	}

	private boolean isBackupFolder(File folder)
	{
		boolean result = false;

		if (settingsPut.backupDir.data.equals("") == false)
		{
			try
			{
				result = folder.getCanonicalFile().equals(new File(settingsPut.backupDir.data).getCanonicalFile());
			}
			catch (IOException e)
			{
				result = false;
			}
		}

		return result;
	}

	// Sync only: what a local file means for its remote copy.
	private static final int COPY_TRANSFER = 0;
	private static final int COPY_IDENTICAL = 1;
	private static final int COPY_NEWER = 2;

	/**
	 * Identical (same size and the same second) is left alone; a remote copy
	 * which is newer is only replaced when Overwrite Newer is ticked; anything
	 * else - missing, older, or the same second at a different size - is
	 * sent. SFTP holds modified times in whole seconds.
	 */
	private int remoteCopyState(File localFile, SftpATTRS remoteFile)
	{
		if (remoteFile == null)
		{
			return COPY_TRANSFER;
		}

		long localSeconds = localFile.lastModified() / 1000;
		long remoteSeconds = remoteFile.getMTime();

		if ((remoteFile.getSize() == localFile.length()) && (localSeconds == remoteSeconds))
		{
			return COPY_IDENTICAL;
		}

		if (remoteSeconds > localSeconds)
		{
			return COPY_NEWER;
		}

		return COPY_TRANSFER;
	}

	private boolean sendFile(File sourceFile, String remoteFolder)
	{
		String localFile = sourceFile.getPath();
		String remoteFile = remotePath(remoteFolder, sourceFile.getName());
		String remoteTempFile = remoteFile + settingsPut.tempFileExtension.data;

		boolean sent = false;

		lastSendError = "";

		if (remoteTempFile.equals(remoteFile))
		{
			// No temporary extension configured - upload straight to the final name.
			sent = jcmd.put(localFile, remoteFile);
		}
		else
		{
			// Upload under the temporary name first so an existing remote copy survives a failed upload.
			if (jcmd.put(localFile, remoteTempFile))
			{
				jcmd.rm(remoteFile);

				sent = jcmd.rename(remoteTempFile, remoteFile);

				if ((sent == false) && jcmd.getLastError().isEmpty())
				{
					// rename() does nothing when the temporary name is not a file - a remote folder of that name swallows the upload.
					lastSendError = "temporary file " + remoteTempFile + " not found on the server after the upload (a folder of that name ?)";
				}
			}
		}

		if ((sent == false) && lastSendError.isEmpty() && jcmd.getLastError().isEmpty())
		{
			lastSendError = "not connected";
		}

		return sent;
	}

	private String remotePath(String folder, String name)
	{
		if (folder.equals(""))
		{
			return name;
		}

		if (name.equals(""))
		{
			return folder;
		}

		return folder.endsWith("/") ? folder + name : folder + "/" + name;
	}

}
