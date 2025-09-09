package com.commander4j.settings;

public class SettingsPut
{
	
	public SettingData enabled = new SettingData("true","false");
	
	public SettingData guid = new SettingData("","false");
	
	public SettingData title = new SettingData("","false");
	
	// Source
	public SettingData localDir = new SettingData("","false");
	public SettingData localFileMask =new SettingData("","false");
	
	public SettingData backupEnabled =new SettingData("","false");
	public SettingData backupDir =new SettingData("","false");
	public SettingData backupRetention =new SettingData("","false");
	
	public SettingData pollFrequencySeconds =new SettingData("","false");

	// Destination
	public SettingData remoteDir =new SettingData("","false");
	public SettingData tempFileExtension =new SettingData("","false");
}
