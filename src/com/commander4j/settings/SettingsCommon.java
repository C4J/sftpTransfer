package com.commander4j.settings;

public class SettingsCommon
{

	// General
	public SettingData title = new SettingData("","false");
	public SettingData emailEnabled = new SettingData("","false");

	// Web log viewer (read-only page showing the Put, Get and System logs). Off unless enabled;
	// a blank port (config written before the setting existed) means the default port.
	public SettingData webEnabled = new SettingData("false","false");
	public SettingData webPort = new SettingData("","false");

	// Daily statistics email to the Monitor list; the time is HH:mm on this machine's clock.
	// Both absent from a config written before the setting existed = off, 06:00.
	public SettingData statsReportEnabled = new SettingData("false","false");
	public SettingData statsReportTime = new SettingData("06:00","false");

	// Display level of the desktop log tabs, one of JLogPanel.LEVEL_NAMES; blank (or an unknown
	// name) means Normal. Written the moment the Show combo changes, not through Apply/Save.
	public SettingData logLevel = new SettingData("","false");

	// Security
	public SettingData remoteHost = new SettingData("","false");
	public SettingData remotePort =new SettingData("","false");
	public SettingData checkKnownHosts =new SettingData("","false");
	public SettingData knownHostsFile =new SettingData("","false");
	public SettingData autoAddtoKnownHostsFile= new SettingData("","false");
	public SettingData authType=new SettingData("","false");
	public SettingData username =new SettingData("","false");
	public SettingData password = new SettingData("","true");
	public SettingData checkPrivateKeyFile =new SettingData("","false");
	public SettingData privateKeyFile =new SettingData("","false");
	public SettingData privateKeyComment =new SettingData("","false");
	public SettingData publicKeyFile =new SettingData("","false");
	public SettingData privateKeyPasswordProtected =new SettingData("","false");
	public SettingData privateKeyPassword = new SettingData("","true");
	
	public SettingData applicationPassword = new SettingData("","true");

}
