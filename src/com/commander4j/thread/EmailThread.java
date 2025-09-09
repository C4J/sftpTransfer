package com.commander4j.thread;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import com.commander4j.email.DistributionRecord;
import com.commander4j.email.EmailRecord;
import com.commander4j.email.core.Email;
import com.commander4j.jsch.JschCommands;
import com.commander4j.log.JLogPanel;
import com.commander4j.settings.SettingUtil;
import com.commander4j.settings.SettingsCommon;
import com.commander4j.util.JUtility;
import com.commander4j.util.JWait;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

public class EmailThread extends Thread
{

	private Properties smtpProperties =new Properties();
	private HashMap<String, DistributionRecord> distList = new HashMap<String, DistributionRecord>();
	private HashMap<String, Calendar> emailLog = new HashMap<String, Calendar>();
	private LinkedList<Email> queue = new LinkedList<Email>();
	private SettingsCommon settingsCommon = new SettingsCommon();

	private JUtility util = new JUtility();
	private SettingUtil settingUtil = new SettingUtil();
	private JWait wait = new JWait();
	private boolean enabled = true;
	
	private JschCommands jcmd;

	private boolean run = true;

	private Logger logger = org.apache.logging.log4j.LogManager.getLogger((EmailThread.class));
	
	int logDestination = 0;

	public EmailThread(int destination)
	{
		super();
		
		this.logDestination = destination;
		
		jcmd = new JschCommands(logDestination);

		loadSmtpPropertie();
	}

	public void shutdown()
	{
		run = false;
	}

	public void run()
	{
		jcmd.writeToSystemLog("Email Thread started.", JLogPanel.INFO);
		
		while ((run) || (queue.size()>0))
		{
			processQueue();
			
			if (queue.size()==0)
			{
				wait.oneSec();
			}
		}

		jcmd.writeToSystemLog("Email Thread stopped.", JLogPanel.INFO);
	}

	public void setEnabled(boolean value)
	{
		enabled = value;
	}

	public boolean getlEnabled()
	{
		return enabled;
	}

	public int getQueueSize()
	{
		return queue.size();
	}

	public synchronized void addToQueue(String distributionID, String subject, String messageText, String filename)
	{
		Email email = new Email(distributionID, subject, messageText, filename);
		queue.addLast(email);
	}

	public synchronized void addToQueue(Email email)
	{
		if (Boolean.valueOf(settingsCommon.emailEnabled.data)==true)
		{
			if (queue.size() < 10)
			{
				queue.addLast(email);
			}
		}
		else
		{
			jcmd.writeToSystemLog("Email is disabled.", JLogPanel.INFO);
		}
	}

	public synchronized Email getFromQueue()
	{
		Email result = queue.getFirst();
		queue.removeFirst();
		return result;
	}

	public synchronized void processQueue()
	{
		while (queue.size() > 0)
		{
			Email email = getFromQueue();
			Send(email.distributionID, email.subject, email.messageText, email.filename);
		}
	}

	public synchronized boolean Send(String distributionID, String subject, String messageText, String filename)
	{
		boolean result = true;
		
		if (getlEnabled() == false)
		{
			jcmd.writeToSystemLog("Email disabled in sftpTransfer.xml", JLogPanel.WARN);
			return result;
		}

		if (distList.containsKey(distributionID) == true)
		{

			if (distList.get(distributionID).enabled.equals("true"))
			{

				String emailKey = "[" + distributionID + "] - [" + subject + "]";
				logger.info(emailKey);

				Calendar lastSent = Calendar.getInstance();
				Calendar now = Calendar.getInstance();

				if (emailLog.containsKey(emailKey))
				{
					lastSent = emailLog.get(emailKey);
				}
				else
				{
					lastSent.add(Calendar.DATE, -1);
					emailLog.put(emailKey, lastSent);
				}

				long seconds = (now.getTimeInMillis() - lastSent.getTimeInMillis()) / 1000;

				long ageInMins = seconds / 60;

				if (ageInMins >= distList.get(distributionID).maxFrequencyMins)
				{

					emailLog.put(emailKey, now);
					jcmd.writeToSystemLog("Email sending frequency permitted", JLogPanel.INFO);

					try
					{

						Properties propAuth = new Properties();
						Properties propNoAuth = new Properties();

						propAuth.putAll(smtpProperties);
						propNoAuth.putAll(smtpProperties);

						Session authenticatedSession = Session.getInstance(propAuth, new Authenticator()
						{
							@Override
							protected PasswordAuthentication getPasswordAuthentication()
							{
								return new PasswordAuthentication(smtpProperties.get("mail.smtp.user").toString(), smtpProperties.get("mail.smtp.password").toString());
							}
						});

						propNoAuth.put("mail.smtp.user", "");
						propNoAuth.put("mail.smtp.password", "");

						Session unauthenticatedSession = Session.getInstance(propAuth, null);

						MimeMessage message;

						if (smtpProperties.get("mail.smtp.auth").toString().toLowerCase().equals("true"))
						{
							logger.debug("Email authentication required");
							message = new MimeMessage(authenticatedSession);
						}
						else
						{
							logger.debug("Email no authentication required");
							message = new MimeMessage(unauthenticatedSession);
						}

						String emails = distList.get(distributionID).addressList;

						jcmd.writeToSystemLog("Email to distribution list ["+distributionID+"]", JLogPanel.INFO);

						message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(emails));

						message.setFrom(new InternetAddress(smtpProperties.get("mail.smtp.from").toString()));

						message.setSubject(subject);

						MimeBodyPart mimeBodyPart = new MimeBodyPart();

						mimeBodyPart.setText(messageText, "utf-8");

						Multipart multipart = new MimeMultipart();
						multipart.addBodyPart(mimeBodyPart);

						if (filename.equals("") == false)
						{
							logger.debug("Email add attachment [" + util.getFilenameFromPath(filename) + "]");

							MimeBodyPart attachmentBodyPart = new MimeBodyPart();
							attachmentBodyPart.attachFile(new File(filename));
							attachmentBodyPart.setDescription(filename);

							multipart.addBodyPart(attachmentBodyPart);

						}
						message.setContent(multipart);

						jcmd.writeToSystemLog("Sending Email", JLogPanel.INFO);
						Transport.send(message);
						jcmd.writeToSystemLog("Email Sent", JLogPanel.INFO);

						message = null;
					}
					catch (Exception ex)
					{
						jcmd.writeToSystemLog("Error encountered sending email [" + ex.getMessage() + "]", JLogPanel.ERROR);
					}

				}
				else
				{
					jcmd.writeToSystemLog("Email suppressed - too frequent", JLogPanel.WARN);
				}

			}
			else
			{
				jcmd.writeToSystemLog("Email Distribution list [" + distributionID + "] is disabled.", JLogPanel.WARN);

			}
		}
		else
		{
			jcmd.writeToSystemLog("Disabled or empty email distribution list [" + distributionID + "]. No email sent.", JLogPanel.ERROR);
		}

		return result;
	}

	public void loadSmtpPropertie()
	{
		int maxtries = 10;
		int trycount = 1;
		
		while (queue.size()>0)
		{
			wait.oneSec();
			trycount ++;
			
			if (trycount == maxtries)
			{
				break;
			}
		}

		if (queue.size()==0)
		{
			
			jcmd.writeToSystemLog("Email Thread loading settings", JLogPanel.INFO);
			
			settingsCommon = settingUtil.readSFTPCommonFromXml();
			
			setEnabled(Boolean.valueOf(settingsCommon.emailEnabled.data));
			
			HashMap<String, EmailRecord> emailProps = settingUtil.readEmailPropertiesFromXml();
	
			if (smtpProperties != null)
			{
				smtpProperties.clear();
			}
	
			for (HashMap.Entry<String, EmailRecord> entry : emailProps.entrySet())
			{
				smtpProperties.setProperty(entry.getKey(), entry.getValue().value);
			}
			
			loadDistributionLists();
		}

	}

	public void loadDistributionLists()
	{

		jcmd.writeToSystemLog("Email Thread Loading Distribution Lists", JLogPanel.INFO);
		
		HashMap<String, DistributionRecord> distrec = settingUtil.readDistributionListFromXml();

		for (HashMap.Entry<String, DistributionRecord> entry : distrec.entrySet())
		{
			distList.put(entry.getKey(), entry.getValue());
		}

	}

}
