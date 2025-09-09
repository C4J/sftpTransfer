package com.commander4j.settings;

public class SettingsCommon
{

	// General
	public SettingData title = new SettingData("","false");
	public SettingData emailEnabled = new SettingData("","false");

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
