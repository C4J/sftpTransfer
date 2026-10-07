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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.commander4j.jsch.JschCommands;
import com.commander4j.jsch.JschRecord;
import com.commander4j.log.JLogPanel;
import com.commander4j.settings.SettingUtil;
import com.commander4j.settings.SettingsCommon;
import com.commander4j.settings.SettingsPut;
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
			}
		}

		localSafeFileMissing = (present == false);

		return present;
	}

	/**
	 * The safe file is the proof that the remote folder is the one the files
	 * are meant for - and the one files may be deleted from.
	 */
	private boolean isRemoteSafeFilePresent()
	{
		String safeFile = remotePath(settingsPut.remoteDir.data, JSafeFile.remote);

		// The remote folder is listed because a server may answer a question about one file from a cache.
		boolean present = jcmd.isRemoteFileListed(settingsPut.remoteDir.data, JSafeFile.remote);

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
			}
		}

		remoteSafeFileMissing = (present == false);

		return present;
	}

	private void sendLocalFolder()
	{

		boolean includeSubFolders = Boolean.valueOf(settingsPut.includeSubFolders.data);
		boolean sync = Boolean.valueOf(settingsPut.syncEnabled.data);
		boolean syncDelete = sync && Boolean.valueOf(settingsPut.syncDeleteEnabled.data);
		boolean syncDeleteFolders = syncDelete && includeSubFolders && Boolean.valueOf(settingsPut.syncDeleteFoldersEnabled.data);

		// Remote files with no local counterpart, removed at the end of a completed sync.
		ArrayList<String> syncDeletions = new ArrayList<String>();

		// Remote folders with no local counterpart, cleared out at the end of a completed sync.
		ArrayList<String> syncFolderDeletions = new ArrayList<String>();

		File sourceDirectory = new File(settingsPut.localDir.data);
		FileFilter fileFilter = WildcardFileFilter.builder().setWildcards(settingsPut.localFileMask.data).setIoCase(IOCase.INSENSITIVE).get();

		// Relative folder ("" is the local folder itself) and the files in it which match the mask.
		LinkedHashMap<String, ArrayList<File>> folders = new LinkedHashMap<String, ArrayList<File>>();

		if (sourceDirectory.isDirectory())
		{
			collectLocalFiles(sourceDirectory, "", fileFilter, includeSubFolders, folders);
		}
		else
		{
			jcmd.writeToLog("Local folder not found " + settingsPut.localDir.data, JLogPanel.ERROR);
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
			if (jcmd.isConnected() && isRemoteSafeFilePresent())
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

					HashMap<String, SftpATTRS> remoteFiles = null;

					if (sync)
					{
						remoteFiles = jcmd.lsFiles(remoteFolder);

						if (remoteFiles == null)
						{
							if (relativeFolder.equals("") == false)
							{
								jcmd.mkdirs(settingsPut.remoteDir.data, relativeFolder);
							}

							remoteFiles = new HashMap<String, SftpATTRS>();
						}

						if (syncDelete)
						{
							File localFolder = relativeFolder.equals("") ? sourceDirectory : new File(sourceDirectory, relativeFolder.replace("/", File.separator));

							for (String remoteName : remoteFiles.keySet())
							{
								if (isSyncDeleteCandidate(remoteName) && (new File(localFolder, remoteName).exists() == false))
								{
									syncDeletions.add(remotePath(remoteFolder, remoteName));
								}
							}

							if (syncDeleteFolders)
							{
								HashMap<String, SftpATTRS> remoteEntries = jcmd.lsEntries(remoteFolder);

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

						if (sync)
						{
							if (isRemoteCopyCurrent(sourceFile, remoteFiles.get(filename)))
							{
								continue;
							}

							jcmd.writeToLog("Sync = " + relativeFile, JLogPanel.INFO);
						}
						else
						{
							jcmd.writeToLog("Processing = " + String.valueOf(fileNumber) + " of " + fileCount + " " + relativeFile, JLogPanel.INFO);
						}

						if (sourceFile.isFile())
						{

							if (Boolean.valueOf(settingsPut.backupEnabled.data)==true)
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
										jcmd.writeToLog(e.getMessage(), JLogPanel.ERROR);
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
										FileUtils.deleteQuietly(sourceFile);
									}
								}
								else
								{
									jcmd.writeToLog("Upload failed, local file retained " + sourceFile.getPath(), JLogPanel.ERROR);
								}

							}
							catch (Exception e)
							{
								jcmd.writeToLog(e.getMessage(), JLogPanel.ERROR);
							}
							finally
							{

							}

						}

					}

				}

				if (jcmd.isConnected() == false)
				{
					jcmd.writeToLog("Sync deletions abandoned, not connected - retried on the next poll", JLogPanel.ERROR);
					return;
				}

				// The safe file has shown the local folder to be the real one, so an empty local folder empties the remote folder too.
				for (String remoteFile : syncDeletions)
				{
					jcmd.writeToLog("Sync delete = " + remoteFile, JLogPanel.INFO);
					jcmd.rm(remoteFile);
				}

				for (String remoteFolder : syncFolderDeletions)
				{
					removeRemoteFolder(remoteFolder);
				}

			}
			//jcmd.disconnect();

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

		String tempFileExtension = settingsPut.tempFileExtension.data;

		if ((tempFileExtension.equals("") == false) && filename.endsWith(tempFileExtension))
		{
			return false;
		}

		return FilenameUtils.wildcardMatch(filename, settingsPut.localFileMask.data, IOCase.INSENSITIVE);
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
				// The safe files are never sent, whatever the file mask.
				if (entry.isFile() && fileFilter.accept(entry) && (JSafeFile.isSafeFile(entry.getName()) == false))
				{
					files.add(entry);
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

	private boolean isRemoteCopyCurrent(File localFile, SftpATTRS remoteFile)
	{
		boolean result = false;

		if (remoteFile != null)
		{
			// SFTP holds modified times in whole seconds.
			if ((remoteFile.getSize() == localFile.length()) && ((localFile.lastModified() / 1000) <= remoteFile.getMTime()))
			{
				result = true;
			}
		}

		return result;
	}

	private boolean sendFile(File sourceFile, String remoteFolder)
	{
		String localFile = sourceFile.getPath();
		String remoteFile = remotePath(remoteFolder, sourceFile.getName());
		String remoteTempFile = remoteFile + settingsPut.tempFileExtension.data;

		boolean sent = false;

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
			}
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
