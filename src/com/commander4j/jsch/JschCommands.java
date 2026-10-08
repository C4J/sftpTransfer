package com.commander4j.jsch;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;

import org.apache.logging.log4j.Logger;

import com.commander4j.gui.frame.JFrameSFTPTransfer;
import com.commander4j.gui.jdialog.RemoteFolderChooser;
import com.commander4j.log.JLogPanel;

import com.commander4j.settings.SettingsCommon;
import com.commander4j.sftp.Start;
import com.commander4j.web.LogHub;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchUnknownHostKeyException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
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

	public String viewTree(SettingsCommon sc, JFrameSFTPTransfer frame, String rootNode, String defaultNode)
	{
		String chosen = defaultNode;

		// A session left open by an earlier look-up would still be showing the old host.
		resetConnection();

		assignCommonSettings(sc);
		connect();

		if (isConnected())
		{

			chosen = RemoteFolderChooser.chooseRemoteFolder(frame, channel, rootNode, defaultNode);
		};

		return chosen;
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
	 * Directory listing entries are kept at debug. The same line is kept in
	 * memory for the web log viewer.
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
					writeToLog("Connection to " + hostLabel() + " failed - " + e.getMessage(), JLogPanel.ERROR);
				}
			}
			catch (Exception e)
			{
				writeToLog("Connection to " + hostLabel() + " failed - " + e.getMessage(), JLogPanel.ERROR);
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
				writeToLog("rm " + filename + " failed - " + e.getMessage(), JLogPanel.ERROR);
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
				writeToLog("rmdir " + folder + " failed - " + e.getMessage(), JLogPanel.ERROR);
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
					writeToLog("rename " + from + "," + to, JLogPanel.NORMAL);
					channel.rename(from, to);
					result = true;
				}
			}
			catch (Exception e)
			{
				writeToLog("rename " + from + "," + to + " failed - " + e.getMessage(), JLogPanel.ERROR);
			}
		}
		return result;
	}

	public boolean put(String from, String to)
	{
		boolean result = false;

		if (isConnected())
		{
			try
			{
				writeToLog("put " + from + "," + to, JLogPanel.NORMAL);
				channel.put(from, to);
				result = true;
			}
			catch (Exception e)
			{
				writeToLog("put " + from + "," + to + " failed - " + e.getMessage(), JLogPanel.ERROR);
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

		if (isConnected())
		{
			try (OutputStream os = new FileOutputStream(to))
			{
				writeToLog("get " + from + "," + to, JLogPanel.NORMAL);

				channel.get(from, os);

				result = true;
			}
			catch (Exception e)
			{
				writeToLog("get " + from + "," + to + " failed - " + e.getMessage(), JLogPanel.ERROR);
			}
		}
		return result;
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
				writeToLog("stat " + remoteFilename, JLogPanel.NORMAL);
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
	 * The command is always logged; the entries it returns only when
	 * logListing is set, since a sync lists the same files on every poll.
	 */
	public Vector<ChannelSftp.LsEntry> ls(String path, String mask, boolean logListing)
	{
		Vector<ChannelSftp.LsEntry> filelist = new Vector<LsEntry>();

		if (isConnected())
		{
			try
			{
				writeToLog("ls " + path + "/" + mask, JLogPanel.NORMAL);

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
				writeToLog("ls " + path + "/" + mask + " failed - " + e.getMessage(), JLogPanel.ERROR);
			}
		}

		return filelist;

	}

	/**
	 * Names of the folders directly beneath a remote folder. Symbolic links
	 * are not followed.
	 */
	public Vector<String> lsFolders(String path)
	{
		Vector<String> result = new Vector<String>();

		if (isConnected())
		{
			try
			{
				writeToLog("ls " + path, JLogPanel.NORMAL);

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
				writeToLog("ls " + path + " failed - " + e.getMessage(), JLogPanel.ERROR);
			}
		}

		return result;
	}

	/**
	 * Attributes (size, modified time) of the files in a remote folder, keyed
	 * by filename. Returns null if the folder cannot be listed, typically
	 * because it does not exist.
	 */
	public HashMap<String, SftpATTRS> lsFiles(String path)
	{
		HashMap<String, SftpATTRS> result = null;

		if (isConnected())
		{
			try
			{
				writeToLog("ls " + path, JLogPanel.NORMAL);

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
	 * Attributes of everything in a remote folder (files and folders), keyed
	 * by name. Returns null if the folder cannot be listed, so a failed
	 * listing is never mistaken for an empty folder.
	 */
	public HashMap<String, SftpATTRS> lsEntries(String path)
	{
		HashMap<String, SftpATTRS> result = null;

		if (isConnected())
		{
			try
			{
				writeToLog("ls " + path, JLogPanel.NORMAL);

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
				writeToLog("mkdir " + baseFolder + "/" + relativeFolder + " failed - " + e.getMessage(), JLogPanel.ERROR);
			}
		}

		return result;
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
				writeToLog("setstat " + filename + " mtime=" + mtime, JLogPanel.NORMAL);
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
