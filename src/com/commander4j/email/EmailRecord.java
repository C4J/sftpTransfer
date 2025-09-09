package com.commander4j.email;

import com.commander4j.util.JUtility;

public class EmailRecord
{
	public String property;
	public String encrypted;
	public String value;
	public String enabled;
	private JUtility util = new JUtility();
	
	public String toString()
	{
		return "EmailData property="+property+" value="+value+" encrypted="+encrypted+" enabled="+enabled;
	}
	
	public EmailRecord()
	{
		
	}
	
	public EmailRecord(String property,String value,String encrypted,String enabled)
	{
		this.property = property;
		this.value = value;
		this.encrypted = util.setBooleanFlag(encrypted);
		this.enabled =  util.setBooleanFlag(enabled);
	}
	
	public EmailRecord(String property,String value,boolean encrypted,boolean enabled)
	{
		this.property = property;
		this.value = value;
		this.encrypted = String.valueOf(encrypted);
		this.enabled = String.valueOf(enabled);
	}
}


