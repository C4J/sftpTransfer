package com.commander4j.settings;

import com.commander4j.util.JUtility;

public class SettingData
{
	public String data="";
	public String encrypted="";
	private JUtility util = new JUtility();
	
	public SettingData(String d,String e)
	{
		data=d;
		encrypted=util.setBooleanFlag(e);
		
	}
	

}
