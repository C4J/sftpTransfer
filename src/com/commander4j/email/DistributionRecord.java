package com.commander4j.email;

import com.commander4j.util.JUtility;

public class DistributionRecord
{
	public String listId= "";
	public String addressList = "";
	public Long maxFrequencyMins = (long) 0;
	public String enabled = "";
	private JUtility util = new JUtility();

	public DistributionRecord()
	{
		
	}
	
	public DistributionRecord(String listId,String addressList,String maxFrequencyMins,String enabled)
	{
		this.listId = listId;
		this.addressList = addressList;
		this.maxFrequencyMins = Long.valueOf(maxFrequencyMins);
		this.enabled = util.setBooleanFlag(enabled);
	}
	
	public DistributionRecord(String listId,String addressList,Long maxFrequencyMins,boolean enabled)
	{
		this.listId = listId;
		this.addressList = addressList;
		this.maxFrequencyMins = maxFrequencyMins;
		this.enabled = String.valueOf(enabled);
	}
	
}
