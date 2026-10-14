package com.commander4j.settings;

public class SettingsGet
{
	public SettingData enabled = new SettingData("true","false");
	
	public SettingData guid = new SettingData("","false");
	
	public SettingData title = new SettingData("","false");
	
	// Source
	public SettingData remoteDir =new SettingData("","false");
	public SettingData remoteFileMask =new SettingData("","false");
	public SettingData includeSubFolders =new SettingData("false","false");
	public SettingData syncEnabled =new SettingData("false","false");
	public SettingData syncDeleteEnabled =new SettingData("false","false");
	public SettingData syncDeleteFoldersEnabled =new SettingData("false","false");
	// Sync only: whether an older source file replaces a newer copy at the destination. True keeps the mirror as it always was.
	public SettingData overwriteNewer =new SettingData("true","false");
	// Wait, then re-list the folder, before fetching a file - a file still being written is left for the next poll.
	public SettingData stabilityCheckEnabled =new SettingData("false","false");
	public SettingData stabilitySeconds =new SettingData("5","false");

	public SettingData pollFrequencySeconds =new SettingData("","false");
	
	// Destination
	public SettingData tempFileExtension =new SettingData("","false");
	public SettingData localDir = new SettingData("","false");


}
