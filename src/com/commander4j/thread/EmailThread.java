package com.commander4j.thread;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.Logger;

import com.commander4j.email.DistributionRecord;
import com.commander4j.email.EmailRecord;
import com.commander4j.email.Notify;
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

/**
 * Sends the queued emails, one at a time, on its own thread. Callers only
 * ever queue (addToQueue), which never blocks on the mail server: the queue
 * holds at most QUEUE_CAPACITY emails and anything beyond that is dropped
 * with a warning. The thread checks the distribution list and its frequency
 * limit (keyed on list + subject, kept in memory) just before each send.
 */
public class EmailThread extends Thread
{
	public static final int QUEUE_CAPACITY = 10;

	private Properties smtpProperties = new Properties();
	private volatile HashMap<String, DistributionRecord> distList = new HashMap<String, DistributionRecord>();
	private HashMap<String, Calendar> emailLog = new HashMap<String, Calendar>();
	private final LinkedBlockingQueue<Email> queue = new LinkedBlockingQueue<Email>(QUEUE_CAPACITY);
	private SettingsCommon settingsCommon = new SettingsCommon();

	private JUtility util = new JUtility();
	private SettingUtil settingUtil = new SettingUtil();
	private JWait wait = new JWait();
	private volatile boolean enabled = true;

	private JschCommands jcmd;

	private volatile boolean run = true;

	// Settings handed over from the screen, taken up once the queue is empty.
	private AtomicReference<PendingSettings> pendingSettings = new AtomicReference<PendingSettings>();

	private static class PendingSettings
	{
		SettingsCommon settingsCommon;
		HashMap<String, EmailRecord> emailProps;
		HashMap<String, DistributionRecord> distributionLists;
	}

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

		// Anything still queued at shutdown (the Stopped email) goes before the thread ends.
		while (run || (queue.isEmpty() == false))
		{
			processQueue();

			applyPendingSettings();

			if (queue.isEmpty())
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

	public void addToQueue(String distributionID, String subject, String messageText, String filename)
	{
		addToQueue(new Email(distributionID, subject, messageText, filename));
	}

	/**
	 * Queues an email, subject to the Email Notifications switch and the
	 * queue capacity. Never waits on the mail server. DEBUG, not INFO, when
	 * the switch is off - a repeating error would otherwise write a line per
	 * poll to the System log.
	 */
	public void addToQueue(Email email)
	{
		if (enabled == false)
		{
			jcmd.writeToSystemLog("Email notifications are off (emailEnabled in sftp_common.xml) - not queued : " + email.subject, JLogPanel.DEBUG);
			return;
		}

		if (queue.offer(email) == false)
		{
			jcmd.writeToSystemLog("Email queue full (" + QUEUE_CAPACITY + ") - dropped : " + email.subject, JLogPanel.WARN);
		}
	}

	private void processQueue()
	{
		Email email;

		while ((email = queue.poll()) != null)
		{
			Send(email.distributionID, email.subject, email.messageText, email.filename);
		}
	}

	/**
	 * Sends one email on the calling thread (normally this one). Returns false
	 * when it was not sent, for whatever reason - that is also logged.
	 */
	/** True when the list is on the current settings and enabled - the only case Notify.transfer queues on. */
	public boolean isListActive(String distributionID)
	{
		DistributionRecord list = distList.get(distributionID);

		return (list != null) && list.enabled.equals("true");
	}

	public boolean Send(String distributionID, String subject, String messageText, String filename)
	{
		boolean result = false;

		if (enabled == false)
		{
			jcmd.writeToSystemLog("Email notifications are off (emailEnabled in sftp_common.xml) - not sent : " + subject, JLogPanel.WARN);
			return result;
		}

		DistributionRecord list = distList.get(distributionID);

		if (list == null)
		{
			jcmd.writeToSystemLog("Disabled or empty email distribution list [" + distributionID + "]. No email sent.", JLogPanel.ERROR);
			return result;
		}

		if (list.enabled.equals("true") == false)
		{
			jcmd.writeToSystemLog("Email Distribution list [" + distributionID + "] is disabled.", JLogPanel.WARN);
			return result;
		}

		// The frequency limit is per list and subject; the time is recorded before the attempt, so a failing server is not hammered.
		String emailKey = "[" + distributionID + "] - [" + subject + "]";
		logger.debug(emailKey);

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

		if (ageInMins < list.maxFrequencyMins)
		{
			jcmd.writeToSystemLog("Email suppressed - too frequent (" + list.maxFrequencyMins + " min limit on [" + distributionID + "]) : " + subject, JLogPanel.WARN);
			return result;
		}

		emailLog.put(emailKey, now);

		try
		{
			jcmd.writeToSystemLog("Sending email to distribution list [" + distributionID + "] : " + subject, JLogPanel.INFO);

			transmit(smtpProperties, list.addressList, subject, messageText, filename);

			jcmd.writeToSystemLog("Email sent to [" + distributionID + "] : " + subject, JLogPanel.INFO);

			if (distributionID.equals(Notify.LIST_ERROR))
			{
				TransferStats.errorsEmailed.incrementAndGet();
			}

			result = true;
		}
		catch (Exception ex)
		{
			jcmd.writeToSystemLog("Error encountered sending email [" + reason(ex) + "] : " + subject, JLogPanel.ERROR);
		}

		return result;
	}

	/**
	 * The test button: sends at once, on the caller's thread, with the given
	 * properties and list, ignoring the Email Notifications switch, the queue
	 * and the frequency log. Returns null when sent, otherwise the reason.
	 */
	public String sendNow(HashMap<String, EmailRecord> props, DistributionRecord list, String subject, String body)
	{
		try
		{
			transmit(smtpPropertiesFrom(props), list.addressList, subject, body, "");
			return null;
		}
		catch (Exception ex)
		{
			return reason(ex);
		}
	}

	private static String reason(Exception ex)
	{
		String msg = ex.getMessage();
		return ex.getClass().getSimpleName() + ((msg == null) ? "" : " - " + msg.trim());
	}

	/**
	 * Builds and sends one message. Authenticated with mail.smtp.user /
	 * mail.smtp.password when mail.smtp.auth is true, otherwise anonymous.
	 */
	private void transmit(Properties smtp, String addresses, String subject, String body, String filename) throws Exception
	{
		Properties props = new Properties();
		props.putAll(smtp);

		Session session;

		if (String.valueOf(smtp.get("mail.smtp.auth")).toLowerCase().equals("true"))
		{
			logger.debug("Email authentication required");

			final String user = String.valueOf(smtp.getProperty("mail.smtp.user", ""));
			final String password = String.valueOf(smtp.getProperty("mail.smtp.password", ""));

			session = Session.getInstance(props, new Authenticator()
			{
				@Override
				protected PasswordAuthentication getPasswordAuthentication()
				{
					return new PasswordAuthentication(user, password);
				}
			});
		}
		else
		{
			logger.debug("Email no authentication required");

			props.put("mail.smtp.user", "");
			props.put("mail.smtp.password", "");

			session = Session.getInstance(props, null);
		}

		MimeMessage message = new MimeMessage(session);

		message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(addresses));

		message.setFrom(new InternetAddress(String.valueOf(smtp.getProperty("mail.smtp.from", ""))));

		message.setSubject(subject);

		MimeBodyPart mimeBodyPart = new MimeBodyPart();

		mimeBodyPart.setText(body, "utf-8");

		Multipart multipart = new MimeMultipart();
		multipart.addBodyPart(mimeBodyPart);

		if ((filename != null) && (filename.equals("") == false))
		{
			logger.debug("Email add attachment [" + util.getFilenameFromPath(filename) + "]");

			MimeBodyPart attachmentBodyPart = new MimeBodyPart();
			attachmentBodyPart.attachFile(new File(filename));
			attachmentBodyPart.setDescription(filename);

			multipart.addBodyPart(attachmentBodyPart);
		}

		message.setContent(multipart);

		Transport.send(message);
	}

	/**
	 * The property rows as Jakarta Mail properties. A row whose Enable box is
	 * unticked is left out; a row with no flag at all (older file) is kept.
	 */
	static Properties smtpPropertiesFrom(HashMap<String, EmailRecord> props)
	{
		Properties result = new Properties();

		for (HashMap.Entry<String, EmailRecord> entry : props.entrySet())
		{
			EmailRecord rec = entry.getValue();

			if ((rec.enabled != null) && rec.enabled.trim().equalsIgnoreCase("false"))
			{
				continue;
			}

			result.setProperty(entry.getKey(), rec.value == null ? "" : rec.value);
		}

		return result;
	}

	/**
	 * Hands over settings from the screen. They are taken up by the email
	 * thread itself, after any queued emails have gone with the old settings.
	 */
	public void requestSettings(SettingsCommon newCommon, HashMap<String, EmailRecord> newEmailProps, HashMap<String, DistributionRecord> newDistributionLists)
	{
		PendingSettings pending = new PendingSettings();
		pending.settingsCommon = newCommon;
		pending.emailProps = newEmailProps;
		pending.distributionLists = newDistributionLists;

		pendingSettings.set(pending);
	}

	private void applyPendingSettings()
	{
		if (queue.isEmpty())
		{
			PendingSettings pending = pendingSettings.getAndSet(null);

			if (pending != null)
			{
				jcmd.writeToSystemLog("Email Thread applying new settings.", JLogPanel.INFO);

				settingsCommon = pending.settingsCommon;

				setEnabled(Boolean.valueOf(settingsCommon.emailEnabled.data));

				smtpProperties = smtpPropertiesFrom(pending.emailProps);

				// Replaced rather than added to, so a list removed on screen is gone here too.
				distList = new HashMap<String, DistributionRecord>(pending.distributionLists);
			}
		}
	}

	public void loadSmtpPropertie()
	{
		int maxtries = 10;
		int trycount = 1;

		while (queue.isEmpty() == false)
		{
			wait.oneSec();
			trycount++;

			if (trycount == maxtries)
			{
				break;
			}
		}

		if (queue.isEmpty())
		{
			jcmd.writeToSystemLog("Email Thread loading settings", JLogPanel.INFO);

			settingsCommon = settingUtil.readSFTPCommonFromXml();

			setEnabled(Boolean.valueOf(settingsCommon.emailEnabled.data));

			smtpProperties = smtpPropertiesFrom(settingUtil.readEmailPropertiesFromXml());

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
