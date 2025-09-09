package com.commander4j.settings;

public class SettingsGet
{
	public SettingData enabled = new SettingData("true","false");
	
	public SettingData guid = new SettingData("","false");
	
	public SettingData title = new SettingData("","false");
	
	// Source
	public SettingData remoteDir =new SettingData("","false");
	public SettingData remoteFileMask =new SettingData("","false");

	public SettingData pollFrequencySeconds =new SettingData("","false");
	
	// Destination
	public SettingData tempFileExtension =new SettingData("","false");
	public SettingData localDir = new SettingData("","false");


}
