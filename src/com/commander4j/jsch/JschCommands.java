package com.commander4j.jsch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.apache.commons.io.IOCase;
import org.apache.logging.log4j.Logger;

import com.commander4j.gui.frame.JFrameSFTPTransfer;
import com.commander4j.gui.jdialog.RemoteFolderChooser;
import com.commander4j.email.Notify;
import com.commander4j.log.JLogPanel;

import com.commander4j.settings.SettingsCommon;
import com.commander4j.sftp.Start;
import com.commander4j.util.JFileMask;
import com.commander4j.web.LogHub;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchUnknownHostKeyException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;
import com.jcraft.jsch.UserInfo;

public class JschCommands
{

	private JSch jsch = new JSch();
	private ChannelSftp channel;
	private SettingsCommon settingsCommon = new SettingsCommon();

	Logger logger = org.apache.logging.log4j.LogManager.getLogger((JschCommands.class));
	
	private Properties cfg = new Properties();

	private Session jschSession;

	public static final int LogDestination_NoGUI = 0;
	public static final int LogDestination_PUT = 1;
	public static final int LogDestination_GET = 2;
	public static final int LogDestination_SYS = 3;

	int defaultLogDestination = 0;

	private static final String[] logTag = new String[] { "[GUI] ", "[PUT] ", "[GET] ", "[SYS] " };

	// True from a successful connect until this side closes the session - a
	// session found closed while this is set was dropped by the server or the network.
	private boolean sessionOpen = false;

	// The reason of the last failed command, for the transfer threads' error emails.
	private volatile String lastError = "";

	// One "Connection failed" email per outage, whatever the list's frequency limit.
	private boolean connectFailedReported = false;

	public String getLastError()
	{
		return lastError;
	}

	private static String reason(Exception e)
	{
		String msg = e.getMessage();
		return (msg == null) ? e.getClass().getSimpleName() : msg;
	}

	/**
	 * Raises an Error-list email for this connection's direction. Only the Put
	 * and Get threads' connections email; the window's own commands (browse,
	 * safe-file buttons) and probes stay quiet.
	 */
	private void notifyError(String event, String... detail)
	{
		if ((defaultLogDestination == LogDestination_PUT) || (defaultLogDestination == LogDestination_GET))
		{
			Notify.error(defaultLogDestination, event, detail);
		}
	}

	private void connectFailed(Exception e)
	{
		lastError = reason(e);

		writeToLog("Connection to " + hostLabel() + " failed - " + lastError, JLogPanel.ERROR);

		if (connectFailedReported == false)
		{
			connectFailedReported = true;
			notifyError("Connection failed", "Host : " + hostLabel(), "User : " + settingsCommon.username.data, "Reason : " + lastError);
		}
	}

	/**
	 * Opens the remote folder chooser. Returns the folder chosen (the default
	 * if the dialog was cancelled), or null when the server could not be
	 * connected to - the reason is on the log, and the caller should say so
	 * on the status line, since nothing else shows. The connection is made
	 * the same way as a transfer: with the jsch properties applied, and a
	 * second try for a host which has just been added to the known hosts file.
	 */
	public String viewTree(SettingsCommon sc, HashMap<String, JschRecord> jschConfig, JFrameSFTPTransfer frame, String rootNode, String defaultNode)
	{
		if (connect(sc, jschConfig) == false)
		{
			return null;
		}

		return RemoteFolderChooser.chooseRemoteFolder(frame, channel, rootNode, defaultNode);
	}

	public JschCommands(int logType)
	{
		setLogDestination(logType);

	}

	private void setLogDestination(int destination)
	{
		this.defaultLogDestination = destination;
	}

	public synchronized void writeToLog(String data, int logmode)
	{
		writeToFile(defaultLogDestination, data, logmode);

		if (Start.gui != null)
		{
			Start.gui.writeToLog(defaultLogDestination, data, logmode);
		}
	}

	public synchronized void writeToSystemLog(String data, int logmode)
	{
		writeToFile(LogDestination_SYS, data, logmode);

		if (Start.gui != null)
		{
			Start.gui.writeToLog(LogDestination_SYS, data, logmode);
		}
	}

	/**
	 * Every message also goes to the log file, whether or not there is a
	 * window, tagged with the window it belongs to and at a matching level.
	 * Directory listing entries are kept at debug; polling detail (DEBUG rows)
	 * at trace, so the file is as quiet as the Normal view unless log4j2.xml
	 * asks for trace. The same line is kept in memory for the web log viewer.
	 */
	private void writeToFile(int destination, String data, int logmode)
	{
		LogHub.add(destination, logmode, data);

		String line = logTag[destination] + data;

		switch (logmode)
		{
		case JLogPanel.ERROR:
			logger.error(line);
			break;
		case JLogPanel.WARN:
			logger.warn(line);
			break;
		case JLogPanel.DIRECTORY:
			logger.debug(line);
			break;
		case JLogPanel.DEBUG:
			logger.trace(line);
			break;
		default:
			logger.info(line);
			break;
		}
	}

	private String hostLabel()
	{
		return settingsCommon.remoteHost.data + ":" + settingsCommon.remotePort.data;
	}

	public void assignJschConfig(HashMap<String, JschRecord> jschConfig)
	{

		cfg.clear();

		for (HashMap.Entry<String, JschRecord> entry : jschConfig.entrySet())
		{

			String key = entry.getKey();

			String value = entry.getValue().value;

			String enabled = entry.getValue().enabled;

			if (enabled.equals("true"))
			{
				cfg.put(key, value);
			}
		}

	}

	public void assignCommonSettings(SettingsCommon settingsCommon)
	{
		this.settingsCommon = settingsCommon;
	}

	/**
	 * Opens a fresh session for a one-off job from the screen. A session left
	 * open by an earlier job would still be using the old host. A host which
	 * has just been added to the known hosts file connects at the second try.
	 */
	public boolean connect(SettingsCommon sc, HashMap<String, JschRecord> jschConfig)
	{
		resetConnection();

		assignCommonSettings(sc);
		assignJschConfig(jschConfig);

		return connect() || connect();
	}

	public boolean connect()
	{
		boolean result = false;

		if (isConnected() == false)
		{
			// A session whose channel alone has died would otherwise be left open behind the new one.
			closeSession();

			try
			{
				if (Boolean.valueOf(settingsCommon.checkKnownHosts.data))
				{
					jsch.setKnownHosts(settingsCommon.knownHostsFile.data);
				}

				if (Boolean.valueOf(settingsCommon.checkPrivateKeyFile.data))
				{
					if (settingsCommon.privateKeyFile.data.equals("") == false)
					{
						if (Boolean.valueOf(settingsCommon.privateKeyPasswordProtected.data) == true)
						{
							jsch.addIdentity(settingsCommon.privateKeyFile.data, settingsCommon.privateKeyPassword.data);
						}
						else
						{
							jsch.addIdentity(settingsCommon.privateKeyFile.data);
						}
					}
				}

				jschSession = jsch.getSession(settingsCommon.username.data, settingsCommon.remoteHost.data, Integer.valueOf(settingsCommon.remotePort.data));

				jschSession.setConfig(cfg);

				jschSession.setPassword(settingsCommon.password.data);

				writeToLog("Connecting to " + hostLabel() + " as " + settingsCommon.username.data, JLogPanel.NORMAL);

				jschSession.connect(15_000);

				channel = (ChannelSftp) jschSession.openChannel("sftp");

				channel.connect();

				sessionOpen = true;

				writeToLog("Connected to " + hostLabel() + " (server " + jschSession.getServerVersion() + ")", JLogPanel.INFO);

				connectFailedReported = false;

				result = true;
			}
			catch (JSchUnknownHostKeyException e)
			{
				
				if (Boolean.valueOf(settingsCommon.autoAddtoKnownHostsFile.data))
				{
					writeToLog("Adding new host to known hosts file " + jschSession.getHostKey().getHost(), JLogPanel.WARN);
					jsch.getHostKeyRepository().add(jschSession.getHostKey(), getUserInfo());
				}
				else
				{
					connectFailed(e);
				}
			}
			catch (Exception e)
			{
				connectFailed(e);
			}

			if (result == false)
			{
				// A session which authenticated but could not open its channel would otherwise be left open.
				closeSession();
			}
		}
		else
		{
			result = true;
		}
		return result;
	}

	/**
	 * Closes the session, if there is one, and says so. This side closing is
	 * the only way a "Disconnected" line is written - a session found closed
	 * by anything else is reported as lost.
	 */
	public boolean disconnect()
	{
		boolean wasConnected = isConnected();

		boolean result = closeSession();

		if (wasConnected)
		{
			writeToLog("Disconnected from " + hostLabel(), JLogPanel.INFO);
		}

		return result;
	}

	private boolean closeSession()
	{
		boolean result = true;

		sessionOpen = false;

		try
		{
			if (channel != null)
			{
				channel.disconnect();
			}

			if (jschSession != null)
			{
				jschSession.disconnect();
			}
		}
		catch (Exception e)
		{
			writeToLog("Disconnect from " + hostLabel() + " failed - " + e.getMessage(), JLogPanel.ERROR);
			result = false;
		}

		channel = null;
		jschSession = null;

		return result;
	}

	/**
	 * Drops the session so that the next connect uses the settings and jsch
	 * properties assigned since. A new JSch forgets the identities and known
	 * hosts loaded for the old settings.
	 */
	public void resetConnection()
	{
		disconnect();

		jsch = new JSch();
	}

	public boolean isConnected()
	{
		boolean result = true;

		if (jschSession == null)
		{
			result = false;
		}
		else
		{
			if (jschSession.isConnected() == false)
			{
				result = false;
			}
			else
			{
				if (channel == null)
				{
					result = false;
				}
				else
				{
					if (channel.isConnected() == false)
					{
						result = false;
					}
				}
			}
		}

		if ((result == false) && sessionOpen)
		{
			// Closed by the server or the network, not by this side.
			sessionOpen = false;
			writeToLog("Connection to " + hostLabel() + " lost", JLogPanel.WARN);
			notifyError("Connection lost", "Host : " + hostLabel());
		}

		return result;
	}

	public Vector<ChannelSftp.LsEntry> dir(String path, String mask)
	{
		Vector<ChannelSftp.LsEntry> result = ls(path, mask);
		return result;
	}

	public boolean rm(String filename)
	{
		boolean result = false;

		if (isConnected())
		{
			try
			{
				if (isRemoteFilePresent(filename))
				{
					writeToLog("rm " + filename, JLogPanel.NORMAL);
					channel.rm(filename);
					result = true;
				}
			}
			catch (Exception e)
			{
				lastError = reason(e);
				writeToLog("rm " + filename + " failed - " + lastError, JLogPanel.ERROR);
				notifyError("Remote delete failed", "Path : " + filename, "Reason : " + lastError);
			}
		}
		return result;
	}

	public boolean cd(String folder)
	{
		boolean result = false;

		if (isConnected())
		{
			try
			{
				if (isRemoteFolderPresent(folder))
				{
					writeToLog("cd " + folder, JLogPanel.NORMAL);
					channel.cd(folder);
					result = true;
				}
			}
			catch (Exception e)
			{
				writeToLog("cd " + folder + " failed - " + e.getMessage(), JLogPanel.ERROR);
			}
		}
		return result;
	}

	public boolean mkdir(String folder)
	{
		boolean result = false;

		if (isConnected())
		{
			try
			{
				if (isRemoteFolderPresent(folder) == false)
				{
					writeToLog("mkdir " + folder, JLogPanel.NORMAL);
					channel.mkdir(folder);
					result = true;
				}
				else
				{
					writeToLog("mkdir " + folder + " folder already exists", JLogPanel.ERROR);
				}
			}
			catch (Exception e)
			{
				writeToLog("mkdir " + folder + " failed - " + e.getMessage(), JLogPanel.ERROR);
			}
		}
		return result;
	}

	public boolean rmdir(String folder)
	{
		boolean result = false;

		if (isConnected())
		{
			try
			{
				if (isRemoteFolderPresent(folder))
				{
					writeToLog("rmdir " + folder, JLogPanel.NORMAL);
					channel.rmdir(folder);
					result = true;
				}
				else
				{
					writeToLog("rmdir " + folder + " folder does not exist", JLogPanel.ERROR);
				}
			}
			catch (Exception e)
			{
				lastError = reason(e);
				writeToLog("rmdir " + folder + " failed - " + lastError, JLogPanel.ERROR);
				notifyError("Remote delete failed", "Folder : " + folder, "Reason : " + lastError);
			}
		}
		return result;
	}

	public boolean cdup()
	{
		boolean result = false;

		if (isConnected())
		{
			try
			{
				writeToLog("cdup", JLogPanel.NORMAL);
				channel.cd("..");
				result = true;
			}
			catch (Exception e)
			{
				writeToLog("cdup failed - " + e.getMessage(), JLogPanel.ERROR);
			}
		}
		return result;
	}

	public boolean rename(String from, String to)
	{
		boolean result = false;

		if (isConnected())
		{

			try
			{
				if (isRemoteFilePresent(from))
				{
					// Names only - both live in the folder the heading above named; the failure line keeps the full paths.
					writeToLog("rename " + name(from) + "," + name(to), JLogPanel.NORMAL);
					channel.rename(from, to);
					result = true;
				}
			}
			catch (Exception e)
			{
				lastError = reason(e);
				writeToLog("rename " + from + "," + to + " failed - " + lastError, JLogPanel.ERROR);
			}
		}
		return result;
	}

	public boolean put(String from, String to)
	{
		boolean result = false;
		TransferProgress progress = null;

		if (isConnected())
		{
			lastError = "";

			try
			{
				// Name only - the Folder heading above it already carries the path;
				// the failure line below keeps the full from,to.
				String name = new File(from).getName();
				writeToLog("put " + name, JLogPanel.NORMAL);
				progress = progressMonitor(name);
				channel.put(from, to, progress);
				result = true;
			}
			catch (Exception e)
			{
				lastError = reason(e);
				writeToLog("put " + from + "," + to + " failed - " + lastError, JLogPanel.ERROR);
				progressFailed(progress);
			}
		}
		return result;
	}

	public String pwd()
	{
		String result = "";

		if (isConnected())
		{
			try
			{
				writeToLog("pwd", JLogPanel.NORMAL);
				result = channel.pwd();
				writeToLog(result, JLogPanel.NORMAL);
			}
			catch (Exception e)
			{
				writeToLog("pwd failed - " + e.getMessage(), JLogPanel.ERROR);
			}
		}
		return result;
	}

	public boolean get(String from, String to)
	{
		boolean result = false;
		TransferProgress progress = null;

		if (isConnected())
		{
			lastError = "";

			try (OutputStream os = new FileOutputStream(to))
			{
				// Name only - the Folder heading above it already carries the path;
				// the failure line below keeps the full from,to.
				String name = name(from);
				writeToLog("get " + name, JLogPanel.NORMAL);
				progress = progressMonitor(name);
				channel.get(from, os, progress);

				result = true;
			}
			catch (Exception e)
			{
				lastError = reason(e);
				writeToLog("get " + from + "," + to + " failed - " + lastError, JLogPanel.ERROR);
				progressFailed(progress);
			}
		}
		return result;
	}

	/**
	 * Progress of one put or get for the bar above the log window. Only built
	 * when there is a window - the service passes null and jsch skips the
	 * callbacks. jsch calls count() for every block (about 32K), far too often
	 * for the Swing queue, so only a change in the whole-number percentage is
	 * passed on (every megabyte when the size is unknown). The local name is
	 * shown for a put and the remote name for a get - the temporary extension
	 * is on the other side and would otherwise appear in the bar.
	 */
	private TransferProgress progressMonitor(String fileName)
	{
		if (Start.gui == null)
		{
			return null;
		}

		return new TransferProgress(fileName);
	}

	private void progressFailed(TransferProgress progress)
	{
		if (progress != null)
		{
			progress.failed();
		}
	}

	private class TransferProgress implements SftpProgressMonitor
	{
		private final String fileName;
		private long total = SftpProgressMonitor.UNKNOWN_SIZE;
		private long done = 0;
		private long lastStep = -1;

		private TransferProgress(String fileName)
		{
			this.fileName = fileName;
		}

		public void init(int op, String src, String dest, long max)
		{
			total = max;
			done = 0;
			lastStep = -1;
			Start.gui.transferProgress(defaultLogDestination, fileName, done, total, JFrameSFTPTransfer.PROGRESS_START);
		}

		public boolean count(long bytes)
		{
			done += bytes;

			long step = (total > 0) ? (done * 100) / total : done / (1024 * 1024);

			if (step != lastStep)
			{
				lastStep = step;
				Start.gui.transferProgress(defaultLogDestination, fileName, done, total, JFrameSFTPTransfer.PROGRESS_UPDATE);
			}

			// false would abort the transfer - the hook for a Stop button, not used yet.
			return true;
		}

		public void end()
		{
			Start.gui.transferProgress(defaultLogDestination, fileName, done, total, JFrameSFTPTransfer.PROGRESS_END);
		}

		private void failed()
		{
			Start.gui.transferProgress(defaultLogDestination, fileName, done, total, JFrameSFTPTransfer.PROGRESS_FAILED);
		}
	}

	public boolean delete(String file)
	{
		boolean result = rm(file);
		return result;
	}

	private boolean isRemoteFilePresent(String remoteFilename)
	{
		boolean result = false;

		if (isConnected())
		{
			try
			{
				writeToLog("stat " + name(remoteFilename), JLogPanel.NORMAL);
				SftpATTRS attrs = channel.stat(remoteFilename);
				if (attrs.isDir() == false)
				{
					// Not a directory
					result = true;
				}
			}
			catch (SftpException e)
			{
				result = false;
			}
		}
		return result;
	}

	public boolean isRemoteFolderPresent(String remoteDirectory)
	{
		boolean result = false;

		if (isConnected())
		{
			try
			{
				writeToLog("stat " + remoteDirectory, JLogPanel.NORMAL);
				SftpATTRS attrs = channel.stat(remoteDirectory);
				if (attrs.isDir() == true)
				{
					// Not a directory
					result = true;
				}
			}
			catch (SftpException e)
			{
				result = false;
			}
		}
		return result;
	}

	public Vector<ChannelSftp.LsEntry> ls(String path, String mask)
	{
		return ls(path, mask, true);
	}

	/**
	 * A listing is a poll, so the command is a DEBUG row (shown only at the
	 * Debug display level); a failure is an error. The entries it returns are
	 * logged when logListing is set.
	 */
	public Vector<ChannelSftp.LsEntry> ls(String path, String mask, boolean logListing)
	{
		Vector<ChannelSftp.LsEntry> filelist = new Vector<LsEntry>();

		if (isConnected())
		{
			try
			{
				writeToLog("ls " + path + "/" + mask, JLogPanel.DEBUG);

				filelist = channel.ls(path + "/" + mask);

				if (logListing)
				{
					for (ChannelSftp.LsEntry entry : filelist)
					{
						writeToLog(entry.toString(), JLogPanel.DIRECTORY);
					}
				}

			}
			catch (SftpException e)
			{
				lastError = reason(e);
				writeToLog("ls " + path + "/" + mask + " failed - " + lastError, JLogPanel.ERROR);
				notifyError("Remote folder could not be listed", "Folder : " + path, "Reason : " + lastError);
			}
		}

		return filelist;

	}

	/**
	 * The entries in a remote folder whose names match any of the masks. The
	 * folder is read once and the masks are applied here - SFTP has no
	 * server-side wildcard, and a file matching two masks is listed once.
	 * Matching is case sensitive, as the server's own listing is. A listing
	 * is a poll, so the command is a DEBUG row (shown only at the Debug
	 * display level) and the entries are not logged here at all - the caller
	 * logs the ones it goes on to process. A failure is an error.
	 */
	public Vector<ChannelSftp.LsEntry> ls(String path, List<String> masks)
	{
		Vector<ChannelSftp.LsEntry> filelist = new Vector<LsEntry>();

		if (isConnected())
		{
			String command = "ls " + path + "/" + JFileMask.describe(masks);

			try
			{
				writeToLog(command, JLogPanel.DEBUG);

				for (ChannelSftp.LsEntry entry : channel.ls(path))
				{
					String name = entry.getFilename();

					if (name.equals(".") || name.equals(".."))
					{
						continue;
					}

					if (JFileMask.matches(name, masks, IOCase.SENSITIVE))
					{
						filelist.add(entry);
					}
				}
			}
			catch (SftpException e)
			{
				lastError = reason(e);
				writeToLog(command + " failed - " + lastError, JLogPanel.ERROR);
				notifyError("Remote folder could not be listed", "Folder : " + path, "Reason : " + lastError);
			}
		}

		return filelist;
	}

	/**
	 * Names of the folders directly beneath a remote folder. Symbolic links
	 * are not followed. A listing is a poll, so the command is a DEBUG row;
	 * a failure is an error.
	 */
	public Vector<String> lsFolders(String path)
	{
		Vector<String> result = new Vector<String>();

		if (isConnected())
		{
			try
			{
				writeToLog("ls " + path, JLogPanel.DEBUG);

				Vector<ChannelSftp.LsEntry> filelist = channel.ls(path);

				for (ChannelSftp.LsEntry entry : filelist)
				{
					String name = entry.getFilename();

					if (entry.getAttrs().isDir() && (name.equals(".") == false) && (name.equals("..") == false))
					{
						result.add(name);
					}
				}

				Collections.sort(result);
			}
			catch (SftpException e)
			{
				lastError = reason(e);
				writeToLog("ls " + path + " failed - " + lastError, JLogPanel.ERROR);
				notifyError("Remote folder could not be listed", "Folder : " + path, "Reason : " + lastError);
			}
		}

		return result;
	}

	/**
	 * Attributes (size, modified time) of the files in a remote folder, keyed
	 * by filename. Returns null if the folder cannot be listed, typically
	 * because it does not exist. A listing is a poll, so the command is a
	 * DEBUG row; a missing folder is expected during a sync and is not logged.
	 */
	public HashMap<String, SftpATTRS> lsFiles(String path)
	{
		HashMap<String, SftpATTRS> result = null;

		if (isConnected())
		{
			try
			{
				writeToLog("ls " + path, JLogPanel.DEBUG);

				Vector<ChannelSftp.LsEntry> filelist = channel.ls(path);

				result = new HashMap<String, SftpATTRS>();

				for (ChannelSftp.LsEntry entry : filelist)
				{
					if (entry.getAttrs().isDir() == false)
					{
						result.put(entry.getFilename(), entry.getAttrs());
					}
				}
			}
			catch (SftpException e)
			{
				result = null;
			}
		}

		return result;
	}

	/**
	 * One read of a remote folder and the views the transfer threads take of
	 * it: the files with their attributes, everything with its attributes, the
	 * sub folder names, and the entries matching a set of file masks. A pass
	 * reads a folder once and takes every view from that read instead of
	 * listing the folder again for each - the safe file check, the sync
	 * comparison and the sub folder walk used to be separate listings of the
	 * same folder a few milliseconds apart. A listing belongs to the pass that
	 * took it and is never kept for the next poll. "." and ".." are left out.
	 */
	public static final class RemoteListing
	{
		private final Vector<LsEntry> entries = new Vector<LsEntry>();

		RemoteListing(Vector<LsEntry> raw)
		{
			for (LsEntry entry : raw)
			{
				String name = entry.getFilename();

				if ((name.equals(".") == false) && (name.equals("..") == false))
				{
					entries.add(entry);
				}
			}
		}

		/** Attributes of the files (not folders), keyed by filename - the lsFiles view. */
		public HashMap<String, SftpATTRS> files()
		{
			HashMap<String, SftpATTRS> result = new HashMap<String, SftpATTRS>();

			for (LsEntry entry : entries)
			{
				if (entry.getAttrs().isDir() == false)
				{
					result.put(entry.getFilename(), entry.getAttrs());
				}
			}

			return result;
		}

		/** Attributes of everything, keyed by name - the lsEntries view. */
		public HashMap<String, SftpATTRS> entries()
		{
			HashMap<String, SftpATTRS> result = new HashMap<String, SftpATTRS>();

			for (LsEntry entry : entries)
			{
				result.put(entry.getFilename(), entry.getAttrs());
			}

			return result;
		}

		/** Names of the sub folders, sorted - the lsFolders view. Symbolic links are not followed. */
		public Vector<String> folders()
		{
			Vector<String> result = new Vector<String>();

			for (LsEntry entry : entries)
			{
				if (entry.getAttrs().isDir())
				{
					result.add(entry.getFilename());
				}
			}

			Collections.sort(result);

			return result;
		}

		/** The entries whose names match any of the masks, in the server's order - the ls(path, masks) view, same case rule. */
		public Vector<LsEntry> matching(List<String> masks)
		{
			Vector<LsEntry> result = new Vector<LsEntry>();

			for (LsEntry entry : entries)
			{
				if (JFileMask.matches(entry.getFilename(), masks, IOCase.SENSITIVE))
				{
					result.add(entry);
				}
			}

			return result;
		}

		/** Whether a file (not a folder) of that name is in the listing. */
		public boolean hasFile(String name)
		{
			for (LsEntry entry : entries)
			{
				if (entry.getFilename().equals(name) && (entry.getAttrs().isDir() == false))
				{
					return true;
				}
			}

			return false;
		}
	}

	/**
	 * Reads a remote folder once. Returns null if it cannot be listed, so a
	 * failed listing is never mistaken for an empty folder; the failure is
	 * logged as an error when logFailure is set (a folder missing during a
	 * sync is expected and is not). A listing is a poll, so the command is a
	 * DEBUG row.
	 */
	public RemoteListing listFolder(String path, boolean logFailure)
	{
		RemoteListing result = null;

		if (isConnected())
		{
			try
			{
				writeToLog("ls " + path, JLogPanel.DEBUG);

				result = new RemoteListing(channel.ls(path));
			}
			catch (SftpException e)
			{
				lastError = reason(e);

				if (logFailure)
				{
					writeToLog("ls " + path + " failed - " + lastError, JLogPanel.ERROR);
					notifyError("Remote folder could not be listed", "Folder : " + path, "Reason : " + lastError);
				}

				result = null;
			}
		}

		return result;
	}

	/**
	 * Attributes of everything in a remote folder (files and folders), keyed
	 * by name. Returns null if the folder cannot be listed, so a failed
	 * listing is never mistaken for an empty folder. A listing is a poll, so
	 * the command is a DEBUG row; a missing folder is not logged.
	 */
	public HashMap<String, SftpATTRS> lsEntries(String path)
	{
		HashMap<String, SftpATTRS> result = null;

		if (isConnected())
		{
			try
			{
				writeToLog("ls " + path, JLogPanel.DEBUG);

				Vector<ChannelSftp.LsEntry> filelist = channel.ls(path);

				result = new HashMap<String, SftpATTRS>();

				for (ChannelSftp.LsEntry entry : filelist)
				{
					String name = entry.getFilename();

					if ((name.equals(".") == false) && (name.equals("..") == false))
					{
						result.put(name, entry.getAttrs());
					}
				}
			}
			catch (SftpException e)
			{
				lastError = reason(e);
				result = null;
			}
		}

		return result;
	}

	/**
	 * Looks for a file by listing its folder rather than asking for the file
	 * itself. A listing is a standard request every SFTP server supports and
	 * it reads the folder as it is now, whereas a server is free to answer a
	 * question about one file from details it remembered earlier in the
	 * session - a file removed by another session can then still appear to
	 * be there.
	 */
	public boolean isRemoteFileListed(String folder, String filename)
	{
		boolean result = false;

		HashMap<String, SftpATTRS> entries = lsEntries(folder);

		if (entries != null)
		{
			SftpATTRS attrs = entries.get(filename);

			result = (attrs != null) && (attrs.isDir() == false);
		}

		return result;
	}

	/**
	 * Creates any missing folders of relativeFolder beneath baseFolder. The
	 * base folder itself is never created.
	 */
	public boolean mkdirs(String baseFolder, String relativeFolder)
	{
		boolean result = false;

		if (isConnected())
		{
			try
			{
				String folder = baseFolder;

				for (String part : relativeFolder.split("/"))
				{
					if (part.equals("") == false)
					{
						folder = folder.endsWith("/") ? folder + part : folder + "/" + part;

						if (isRemoteFolderPresent(folder) == false)
						{
							writeToLog("mkdir " + folder, JLogPanel.NORMAL);
							channel.mkdir(folder);
						}
					}
				}

				result = true;
			}
			catch (Exception e)
			{
				lastError = reason(e);
				writeToLog("mkdir " + baseFolder + "/" + relativeFolder + " failed - " + lastError, JLogPanel.ERROR);
			}
		}

		return result;
	}

	/**
	 * The last segment of a remote path, for the per-file log lines (put, get,
	 * rename, stat, setstat) - the Folder heading above them carries the path.
	 * Failure lines and the folder commands (mkdir, rmdir, folder stat) keep
	 * the full path.
	 */
	private static String name(String remotePath)
	{
		return remotePath.substring(remotePath.lastIndexOf('/') + 1);
	}

	/**
	 * Sets the modified time (seconds since 1970) of a remote file.
	 */
	public boolean setMtime(String filename, int mtime)
	{
		boolean result = false;

		if (isConnected())
		{
			try
			{
				writeToLog("setstat " + name(filename) + " mtime=" + mtime, JLogPanel.NORMAL);
				channel.setMtime(filename, mtime);
				result = true;
			}
			catch (Exception e)
			{
				writeToLog("setstat " + filename + " failed - " + e.getMessage(), JLogPanel.WARN);
			}
		}

		return result;
	}

	private UserInfo getUserInfo()
	{
		UserInfo ui = new UserInfo()
		{

			public String getPassword()
			{
				return settingsCommon.password.data;
			}

			public boolean promptPassword(String message)
			{
				return false;
			}

			public boolean promptYesNo(String message)
			{
				return false;
			}

			public void showMessage(String message)
			{
				System.out.println(message);
			}

			public String getPassphrase()
			{
				return null;
			}

			public boolean promptPassphrase(String message)
			{
				return false;
			}
		};
		return ui;
	}
}
