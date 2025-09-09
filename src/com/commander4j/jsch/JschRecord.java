package com.commander4j.jsch;

import com.commander4j.util.JUtility;

public class JschRecord
{
	public String id;
	public String encrypted;
	public String value;
	public String enabled;
	private JUtility util = new JUtility();
	
	public String toString()
	{
		return "JschRecord id="+id+" value="+value+" encrypted="+encrypted+" enabled="+enabled;
	}
	
	public JschRecord()
	{
		
	}
	
	public JschRecord(String id,String value,String encrypted,String enabled)
	{
		this.id = id;
		this.value = value;
		this.encrypted = util.setBooleanFlag(encrypted);
		this.enabled =  util.setBooleanFlag(enabled);
	}
	
	public JschRecord(String id,String value,boolean encrypted,boolean enabled)
	{
		this.id = id;
		this.value = value;
		this.encrypted = String.valueOf(encrypted);
		this.enabled = String.valueOf(enabled);
	}
}


