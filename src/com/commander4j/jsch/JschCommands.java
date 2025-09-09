package com.commander4j.jsch;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;

import org.apache.logging.log4j.Logger;

import com.commander4j.gui.frame.JFrameSFTPTransfer;
import com.commander4j.gui.jdialog.RemoteFolderChooser;
import com.commander4j.log.JLogPanel;

import com.commander4j.settings.SettingsCommon;
import com.commander4j.sftp.Start;
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

	public String viewTree(SettingsCommon sc, JFrameSFTPTransfer frame, String rootNode, String defaultNode)
	{
		String chosen = defaultNode;

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
		if (Start.gui != null)
		{
			Start.gui.writeToLog(defaultLogDestination, data, logmode);
		}
		else
		{
			logger.debug(data);		
		}
	}

	public synchronized void writeToSystemLog(String data, int logmode)
	{

		if (Start.gui != null)
		{
			Start.gui.writeToLog(LogDestination_SYS, data, logmode);
		}
		else
		{
			logger.debug(data);		
		}

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

	public boolean connect()
	{
		boolean result = false;

		if (isConnected() == false)
		{
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

				writeToLog("connect to " + settingsCommon.remoteHost.data + ":" + settingsCommon.remotePort.data, JLogPanel.NORMAL);

				jschSession.connect(15_000);

				channel = (ChannelSftp) jschSession.openChannel("sftp");

				channel.connect();

				result = true;
			}
			catch (JSchUnknownHostKeyException e)
			{
				
				if (Boolean.valueOf(settingsCommon.autoAddtoKnownHostsFile.data))
				{
					writeToLog("WARNING - adding new host to known hosts file " + jschSession.getHostKey().getHost(), JLogPanel.ERROR);
					jsch.getHostKeyRepository().add(jschSession.getHostKey(), getUserInfo());
				}
				else
				{
					writeToLog("error " + e.getMessage(), JLogPanel.ERROR);
				}
			}
			catch (Exception e)
			{
				writeToLog("error " + e.getMessage(), JLogPanel.ERROR);
			}
		}
		else
		{
			result = true;
		}
		return result;
	}

	public boolean disconnect()
	{
		boolean result = false;
		try
		{
			channel.disconnect();

			jschSession.disconnect();

			writeToLog("disconnect", JLogPanel.NORMAL);

			result = true;
		}
		catch (Exception e)
		{
			writeToLog("error " + e.getMessage(), JLogPanel.ERROR);
		}
		return result;
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
				}
			}
			catch (Exception e)
			{
				writeToLog(e.getMessage(), JLogPanel.ERROR);
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
				}
			}
			catch (Exception e)
			{
				writeToLog(e.getMessage(), JLogPanel.ERROR);
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
				}
				else
				{
					writeToLog("mkdir " + folder + " folder already exists", JLogPanel.ERROR);
				}
			}
			catch (Exception e)
			{
				writeToLog(e.getMessage(), JLogPanel.ERROR);
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
				}
				else
				{
					writeToLog("rmdir " + folder + " folder does not exist", JLogPanel.ERROR);
				}
			}
			catch (Exception e)
			{
				writeToLog(e.getMessage(), JLogPanel.ERROR);
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

			}
			catch (Exception e)
			{
				writeToLog(e.getMessage(), JLogPanel.ERROR);
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

				}
			}
			catch (Exception e)
			{
				writeToLog(e.getMessage(), JLogPanel.ERROR);
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
			}
			catch (Exception e)
			{
				writeToLog(e.getMessage(), JLogPanel.ERROR);
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
				writeToLog(e.getMessage(), JLogPanel.ERROR);
			}
		}
		return result;
	}

	public boolean get(String from, String to)
	{
		boolean result = false;

		if (isConnected())
		{
			try
			{
				OutputStream os = new FileOutputStream(to);
				writeToLog("get " + from + "," + to, JLogPanel.NORMAL);

				channel.get(from, os);

				os.close();
				os = null;
			}
			catch (Exception e)
			{
				writeToLog("Exception in GET " + e.getMessage(), JLogPanel.ERROR);
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

	private boolean isRemoteFolderPresent(String remoteDirectory)
	{
		boolean result = false;

		if (isConnected())
		{
			try
			{
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
		Vector<ChannelSftp.LsEntry> filelist = new Vector<LsEntry>();
		writeToLog("ls " + path + "/" + mask, JLogPanel.NORMAL);

		if (isConnected())
		{
			try
			{
				filelist = channel.ls(path + "/" + mask);

				for (ChannelSftp.LsEntry entry : filelist)
				{
					writeToLog(entry.toString(), JLogPanel.DIRECTORY);
				}

			}
			catch (SftpException e)
			{
				writeToLog(e.getMessage(), JLogPanel.ERROR);
			}
		}

		return filelist;

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
