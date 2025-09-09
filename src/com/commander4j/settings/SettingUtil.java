package com.commander4j.settings;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.commander4j.email.DistributionRecord;
import com.commander4j.email.EmailRecord;
import com.commander4j.jsch.JschRecord;
import com.commander4j.util.EncryptData;
import com.commander4j.util.JCipher;
import com.commander4j.util.JUtility;

public class SettingUtil
{

	UUID uuid;
	
	JCipher cipher = new JCipher(EncryptData.key);

	private JUtility util = new JUtility();

	public SettingsCommon readSFTPCommonFromXml()
	{
		SettingsCommon result = new SettingsCommon();

		try
		{

			File xmlFile = new File("./xml/config/sftp_common.xml");

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();

			// Parse the XML
			Document document = builder.parse(xmlFile);
			document.getDocumentElement().normalize();

			// Get the <settings> element
			NodeList settingsList = document.getElementsByTagName("settings");

			if (settingsList.getLength() > 0)
			{
				Element settings = (Element) settingsList.item(0);

				// Loop over child nodes
				NodeList children = settings.getChildNodes();
				for (int i2 = 0; i2 < children.getLength(); i2++)
				{
					Node child = children.item(i2);

					if (child.getNodeType() == Node.ELEMENT_NODE)
					{

						String name = child.getNodeName();
						NamedNodeMap node = child.getAttributes();
						Node nn = node.getNamedItem("encrypted");
						String encrypted = util.setBooleanFlag(nn.getNodeValue().toLowerCase());

						switch (name)
						{
						case "title":
							result.title.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.title.encrypted = encrypted;
							break;
						case "emailEnabled":
							result.emailEnabled.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.emailEnabled.encrypted = encrypted;
							break;
						case "remoteHost":
							result.remoteHost.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.remoteHost.encrypted = encrypted;
							break;
						case "remotePort":
							result.remotePort.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.remotePort.encrypted = encrypted;
							break;
						case "checkKnownHosts":
							result.checkKnownHosts.data = util.setBooleanFlag(cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted));
							result.checkKnownHosts.encrypted = encrypted;
							break;
						case "knownHostsFile":
							result.knownHostsFile.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.knownHostsFile.encrypted = encrypted;
							if (result.knownHostsFile.data.equals("")) result.knownHostsFile.data = "."+File.separator+"ssh"+File.separator+"known_hosts";
							break;
						case "autoAddtoKnownHostsFile":
							result.autoAddtoKnownHostsFile.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.autoAddtoKnownHostsFile.encrypted = encrypted;
							break;
						case "authType":
							result.authType.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.authType.encrypted = encrypted;
							break;
						case "username":
							result.username.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.username.encrypted = encrypted;
							break;
						case "password":
							result.password.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.password.encrypted = encrypted;
							break;
						case "checkPrivateKeyFile":
							result.checkPrivateKeyFile.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.checkPrivateKeyFile.encrypted = encrypted;
							break;
						case "privateKeyFile":
							result.privateKeyFile.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.privateKeyFile.encrypted = encrypted;
							if (result.privateKeyFile.data.equals("")) 
								{
									result.privateKeyFile.data = "."+File.separator+"ssh"+File.separator+"sftpTransfer.pem";
									result.checkPrivateKeyFile.data = "false";
								}
							break;
						case "privateKeyComment":
							result.privateKeyComment.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.privateKeyComment.encrypted = encrypted;
							break;
						case "publicKeyFile":
							result.publicKeyFile.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.publicKeyFile.encrypted = encrypted;
							if (result.publicKeyFile.data.equals("")) result.publicKeyFile.data = "."+File.separator+"ssh"+File.separator+"sftpTransfer.pub";
							break;
						case "privateKeyPasswordProtected":
							result.privateKeyPasswordProtected.data = util.setBooleanFlag(cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted));
							result.privateKeyPasswordProtected.encrypted = encrypted;
							break;
						case "privateKeyPassword":
							result.privateKeyPassword.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.privateKeyPassword.encrypted = encrypted;
							break;
						case "applicationPassword":
							result.applicationPassword.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.applicationPassword.encrypted = encrypted;
							break;
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return result;
	}

	public SettingsPut readSFTPPutFromXml()
	{
		SettingsPut result = new SettingsPut();

		try
		{

			File xmlFile = new File("./xml/config/sftp_put.xml");

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();

			// Parse the XML
			Document document = builder.parse(xmlFile);
			document.getDocumentElement().normalize();

			// Get the <settings> element
			NodeList settingsList = document.getElementsByTagName("put");

			if (settingsList.getLength() > 0)
			{
				Element settings = (Element) settingsList.item(0);

				// Loop over child nodes
				NodeList children = settings.getChildNodes();
				for (int i2 = 0; i2 < children.getLength(); i2++)
				{
					Node child = children.item(i2);

					if (child.getNodeType() == Node.ELEMENT_NODE)
					{

						String name = child.getNodeName();
						NamedNodeMap node = child.getAttributes();
						Node nn = node.getNamedItem("encrypted");
						String encrypted = util.setBooleanFlag(nn.getNodeValue().toLowerCase());

						switch (name)
						{
						case "enabled":
							result.enabled.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.enabled.encrypted = encrypted;
							break;
						case "guid":
							result.guid.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.guid.encrypted = encrypted;
							if (result.guid.data.equals(""))
							{
								uuid = UUID.randomUUID();
								result.guid.data = uuid.toString();
							}
							break;
						case "title":
							result.title.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.title.encrypted = encrypted;
							break;
						case "localDir":
							result.localDir.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.localDir.encrypted = encrypted;
							if (result.localDir.data.equals("")) result.localDir.data = "."+File.separator+"send";
							break;
						case "localFileMask":
							result.localFileMask.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.localFileMask.encrypted = encrypted;
							break;
						case "backupEnabled":
							result.backupEnabled.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.backupEnabled.encrypted = encrypted;
							break;
						case "backupDir":
							result.backupDir.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.backupDir.encrypted = encrypted;
							if (result.backupDir.data.equals(""))
							{
								result.backupDir.data = "."+File.separator+"backups";
							}
							break;
						case "backupRetention":
							result.backupRetention.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							if (result.backupRetention.data.equals(""))
							{
								result.backupRetention.data = "7";
							}
							result.backupRetention.encrypted = encrypted;
							break;
						case "pollFrequencySeconds":
							result.pollFrequencySeconds.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.pollFrequencySeconds.encrypted = encrypted;
							break;
						case "remoteDir":
							result.remoteDir.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.remoteDir.encrypted = encrypted;
							if (result.remoteDir.data.equals("")) result.remoteDir.data = "/";
							break;
						case "tempFileExtension":
							result.tempFileExtension.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.tempFileExtension.encrypted = encrypted;
							if (result.tempFileExtension.data.equals("")) result.tempFileExtension.data = ".tmp";
							break;
						}
					}
				}

				if (result.backupRetention.data.equals(""))
				{
					result.backupRetention.data = "7";
				}

				if (result.pollFrequencySeconds.data.equals(""))
				{
					result.pollFrequencySeconds.data = "10";
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return result;
	}

	public SettingsGet readSFTPGetFromXml()
	{
		SettingsGet result = new SettingsGet();

		try
		{

			File xmlFile = new File("./xml/config/sftp_get.xml");

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();

			// Parse the XML
			Document document = builder.parse(xmlFile);
			document.getDocumentElement().normalize();

			// Get the <settings> element
			NodeList settingsList = document.getElementsByTagName("get");

			if (settingsList.getLength() > 0)
			{
				Element settings = (Element) settingsList.item(0);

				// Loop over child nodes
				NodeList children = settings.getChildNodes();
				for (int i2 = 0; i2 < children.getLength(); i2++)
				{
					Node child = children.item(i2);

					if (child.getNodeType() == Node.ELEMENT_NODE)
					{

						String name = child.getNodeName();
						NamedNodeMap node = child.getAttributes();
						Node nn = node.getNamedItem("encrypted");
						String encrypted = util.setBooleanFlag(nn.getNodeValue().toLowerCase());

						switch (name)
						{
						case "enabled":
							result.enabled.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.enabled.encrypted = encrypted;
							break;
						case "guid":
							result.guid.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.guid.encrypted = encrypted;
							if (result.guid.data.equals(""))
							{
								uuid = UUID.randomUUID();
								result.guid.data = uuid.toString();
							}
							break;
						case "title":
							result.title.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.title.encrypted = encrypted;
							break;
						case "localDir":
							result.localDir.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.localDir.encrypted = encrypted;
							if (result.localDir.data.equals("")) result.localDir.data = "."+File.separator+"receive";
							break;
						case "remoteFileMask":
							result.remoteFileMask.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.remoteFileMask.encrypted = encrypted;
							break;
						case "pollFrequencySeconds":
							result.pollFrequencySeconds.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.pollFrequencySeconds.encrypted = encrypted;
							break;
						case "remoteDir":
							result.remoteDir.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.remoteDir.encrypted = encrypted;
							if (result.remoteDir.data.equals("")) result.remoteDir.data = "/";
							break;
						case "tempFileExtension":
							result.tempFileExtension.data = cipher.conditionalDecrypt(child.getTextContent().trim(), encrypted);
							result.tempFileExtension.encrypted = encrypted;
							if (result.tempFileExtension.data.equals("")) result.tempFileExtension.data = ".tmp";
							break;
						}
					}
				}
				if (result.pollFrequencySeconds.data.equals(""))
				{
					result.pollFrequencySeconds.data = "10";
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return result;
	}

	public boolean saveSFTPCommonToXml(SettingsCommon settings)
	{

		boolean result = false;

		try
		{

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			// Root element
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("config");
			Element settingsElement = doc.createElement("settings");

			rootElement.appendChild(settingsElement);

			doc.appendChild(rootElement);

			Element title = (Element) doc.createElement("title");
			title.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.title.data, settings.title.encrypted)));
			title.setAttribute("encrypted", settings.title.encrypted);
			settingsElement.appendChild(title);

			Element emailEnabled = (Element) doc.createElement("emailEnabled");
			emailEnabled.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.emailEnabled.data, settings.emailEnabled.encrypted)));
			emailEnabled.setAttribute("encrypted", settings.emailEnabled.encrypted);
			settingsElement.appendChild(emailEnabled);

			Element remoteHost = (Element) doc.createElement("remoteHost");
			remoteHost.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.remoteHost.data, settings.remoteHost.encrypted)));
			remoteHost.setAttribute("encrypted", settings.remoteHost.encrypted);
			settingsElement.appendChild(remoteHost);

			Element remotePort = (Element) doc.createElement("remotePort");
			remotePort.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.remotePort.data, settings.remotePort.encrypted)));
			remotePort.setAttribute("encrypted", settings.remotePort.encrypted);
			settingsElement.appendChild(remotePort);

			Element checkKnownHosts = (Element) doc.createElement("checkKnownHosts");
			checkKnownHosts.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.checkKnownHosts.data, settings.checkKnownHosts.encrypted)));
			checkKnownHosts.setAttribute("encrypted", settings.checkKnownHosts.encrypted);
			settingsElement.appendChild(checkKnownHosts);

			Element knownHostsFile = (Element) doc.createElement("knownHostsFile");
			knownHostsFile.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.knownHostsFile.data, settings.knownHostsFile.encrypted)));
			knownHostsFile.setAttribute("encrypted", settings.knownHostsFile.encrypted);
			settingsElement.appendChild(knownHostsFile);
			
			Element autoAddtoKnownHostsFile = (Element) doc.createElement("autoAddtoKnownHostsFile");
			autoAddtoKnownHostsFile.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.autoAddtoKnownHostsFile.data, settings.autoAddtoKnownHostsFile.encrypted)));
			autoAddtoKnownHostsFile.setAttribute("encrypted", settings.autoAddtoKnownHostsFile.encrypted);
			settingsElement.appendChild(autoAddtoKnownHostsFile);

			Element authType = (Element) doc.createElement("authType");
			authType.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.authType.data, settings.authType.encrypted)));
			authType.setAttribute("encrypted", settings.authType.encrypted);
			settingsElement.appendChild(authType);

			Element username = (Element) doc.createElement("username");
			username.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.username.data, settings.username.encrypted)));
			username.setAttribute("encrypted", settings.username.encrypted);
			settingsElement.appendChild(username);

			Element password = (Element) doc.createElement("password");
			password.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.password.data, settings.password.encrypted)));
			password.setAttribute("encrypted", settings.password.encrypted);
			settingsElement.appendChild(password);

			Element checkPrivateKeyFile = (Element) doc.createElement("checkPrivateKeyFile");
			checkPrivateKeyFile.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.checkPrivateKeyFile.data, settings.privateKeyFile.encrypted)));
			checkPrivateKeyFile.setAttribute("encrypted", settings.privateKeyFile.encrypted);
			settingsElement.appendChild(checkPrivateKeyFile);

			Element privateKeyFile = (Element) doc.createElement("privateKeyFile");
			privateKeyFile.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.privateKeyFile.data, settings.privateKeyFile.encrypted)));
			privateKeyFile.setAttribute("encrypted", settings.privateKeyFile.encrypted);
			settingsElement.appendChild(privateKeyFile);
			
			Element privateKeyComment = (Element) doc.createElement("privateKeyComment");
			privateKeyComment.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.privateKeyComment.data, settings.privateKeyComment.encrypted)));
			privateKeyComment.setAttribute("encrypted", settings.privateKeyComment.encrypted);
			settingsElement.appendChild(privateKeyComment);
			
			Element publicKeyFile = (Element) doc.createElement("publicKeyFile");
			publicKeyFile.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.publicKeyFile.data, settings.publicKeyFile.encrypted)));
			publicKeyFile.setAttribute("encrypted", settings.privateKeyFile.encrypted);
			settingsElement.appendChild(publicKeyFile);

			Element privateKeyPasswordProtected = (Element) doc.createElement("privateKeyPasswordProtected");
			privateKeyPasswordProtected.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.privateKeyPasswordProtected.data, settings.privateKeyPasswordProtected.encrypted)));
			privateKeyPasswordProtected.setAttribute("encrypted", settings.privateKeyPasswordProtected.encrypted);
			settingsElement.appendChild(privateKeyPasswordProtected);

			Element privateKeyPassword = (Element) doc.createElement("privateKeyPassword");
			privateKeyPassword.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.privateKeyPassword.data, settings.privateKeyPassword.encrypted)));
			privateKeyPassword.setAttribute("encrypted", settings.privateKeyPassword.encrypted);
			settingsElement.appendChild(privateKeyPassword);
			
			Element applicationPassword = (Element) doc.createElement("applicationPassword");
			applicationPassword.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.applicationPassword.data, settings.applicationPassword.encrypted)));
			applicationPassword.setAttribute("encrypted", settings.applicationPassword.encrypted);
			settingsElement.appendChild(applicationPassword);

			// Write the content into XML file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();

			// Pretty print
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

			DOMSource source = new DOMSource(doc);
			StreamResult streamResult = new StreamResult(new File("./xml/config/sftp_common.xml"));

			transformer.transform(source, streamResult);

			result = true;
		}
		catch (Exception ex)
		{

		}

		return result;

	}

	public boolean saveSFTPPutToXml(SettingsPut settings)
	{

		boolean result = false;

		try
		{

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			// Root element
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("config");
			Element settingsElement = doc.createElement("put");

			rootElement.appendChild(settingsElement);

			doc.appendChild(rootElement);

			Element enabled = (Element) doc.createElement("enabled");
			enabled.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.enabled.data, settings.enabled.encrypted)));
			enabled.setAttribute("encrypted", settings.enabled.encrypted);
			settingsElement.appendChild(enabled);

			Element guid = (Element) doc.createElement("gui");
			if (settings.guid.data.equals(""))
			{
				uuid = UUID.randomUUID();
				settings.guid.data = uuid.toString();
			}
			guid.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.guid.data, settings.guid.encrypted)));
			guid.setAttribute("encrypted", settings.guid.encrypted);
			settingsElement.appendChild(guid);
			
			Element title = (Element) doc.createElement("title");
			title.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.title.data, settings.title.encrypted)));
			title.setAttribute("encrypted", settings.title.encrypted);
			settingsElement.appendChild(title);
			
			Element localDir = (Element) doc.createElement("localDir");
			localDir.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.localDir.data, settings.localDir.encrypted)));
			localDir.setAttribute("encrypted", settings.localDir.encrypted);
			settingsElement.appendChild(localDir);

			Element localFileMask = (Element) doc.createElement("localFileMask");
			localFileMask.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.localFileMask.data, settings.localFileMask.encrypted)));
			localFileMask.setAttribute("encrypted", settings.localFileMask.encrypted);
			settingsElement.appendChild(localFileMask);

			Element backupEnabled = (Element) doc.createElement("backupEnabled");
			backupEnabled.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.backupEnabled.data, settings.backupEnabled.encrypted)));
			backupEnabled.setAttribute("encrypted", settings.backupEnabled.encrypted);
			settingsElement.appendChild(backupEnabled);

			Element backupDir = (Element) doc.createElement("backupDir");
			backupDir.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.backupDir.data, settings.backupDir.encrypted)));
			backupDir.setAttribute("encrypted", settings.backupDir.encrypted);
			settingsElement.appendChild(backupDir);

			Element backupRetention = (Element) doc.createElement("backupRetention");
			backupRetention.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.backupRetention.data, settings.backupRetention.encrypted)));
			backupRetention.setAttribute("encrypted", settings.backupRetention.encrypted);
			settingsElement.appendChild(backupRetention);

			Element pollFrequencySeconds = (Element) doc.createElement("pollFrequencySeconds");
			pollFrequencySeconds.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.pollFrequencySeconds.data, settings.pollFrequencySeconds.encrypted)));
			pollFrequencySeconds.setAttribute("encrypted", settings.pollFrequencySeconds.encrypted);
			settingsElement.appendChild(pollFrequencySeconds);

			Element remoteDir = (Element) doc.createElement("remoteDir");
			remoteDir.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.remoteDir.data, settings.remoteDir.encrypted)));
			remoteDir.setAttribute("encrypted", settings.remoteDir.encrypted);
			settingsElement.appendChild(remoteDir);

			Element tempFileExtension = (Element) doc.createElement("tempFileExtension");
			tempFileExtension.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.tempFileExtension.data, settings.tempFileExtension.encrypted)));
			tempFileExtension.setAttribute("encrypted", settings.tempFileExtension.encrypted);
			settingsElement.appendChild(tempFileExtension);

			// Write the content into XML file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();

			// Pretty print
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

			DOMSource source = new DOMSource(doc);
			StreamResult streamResult = new StreamResult(new File("./xml/config/sftp_put.xml"));

			transformer.transform(source, streamResult);

			result = true;
		}
		catch (Exception ex)
		{

		}

		return result;

	}

	public boolean saveSFTPGetToXml(SettingsGet settings)
	{

		boolean result = false;

		try
		{

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			// Root element
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("config");
			Element settingsElement = doc.createElement("get");

			rootElement.appendChild(settingsElement);

			doc.appendChild(rootElement);

			Element enabled = (Element) doc.createElement("enabled");
			enabled.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.enabled.data, settings.enabled.encrypted)));
			enabled.setAttribute("encrypted", settings.enabled.encrypted);
			settingsElement.appendChild(enabled);
			
			Element guid = (Element) doc.createElement("gui");
			if (settings.guid.data.equals(""))
			{
				uuid = UUID.randomUUID();
				settings.guid.data = uuid.toString();
			}
			guid.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.guid.data, settings.guid.encrypted)));
			guid.setAttribute("encrypted", settings.guid.encrypted);
			settingsElement.appendChild(guid);
			
			Element title = (Element) doc.createElement("title");
			title.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.title.data, settings.title.encrypted)));
			title.setAttribute("encrypted", settings.title.encrypted);
			settingsElement.appendChild(title);

			Element remoteDir = (Element) doc.createElement("remoteDir");
			remoteDir.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.remoteDir.data, settings.remoteDir.encrypted)));
			remoteDir.setAttribute("encrypted", settings.remoteDir.encrypted);
			settingsElement.appendChild(remoteDir);

			Element localFileMask = (Element) doc.createElement("remoteFileMask");
			localFileMask.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.remoteFileMask.data, settings.remoteFileMask.encrypted)));
			localFileMask.setAttribute("encrypted", settings.remoteFileMask.encrypted);
			settingsElement.appendChild(localFileMask);

			Element pollFrequencySeconds = (Element) doc.createElement("pollFrequencySeconds");
			pollFrequencySeconds.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.pollFrequencySeconds.data, settings.pollFrequencySeconds.encrypted)));
			pollFrequencySeconds.setAttribute("encrypted", settings.pollFrequencySeconds.encrypted);
			settingsElement.appendChild(pollFrequencySeconds);

			Element localDir = (Element) doc.createElement("localDir");
			localDir.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.localDir.data, settings.localDir.encrypted)));
			localDir.setAttribute("encrypted", settings.localDir.encrypted);
			settingsElement.appendChild(localDir);

			Element tempFileExtension = (Element) doc.createElement("tempFileExtension");
			tempFileExtension.appendChild(doc.createTextNode(cipher.conditionalEncrypt(settings.tempFileExtension.data, settings.tempFileExtension.encrypted)));
			tempFileExtension.setAttribute("encrypted", settings.tempFileExtension.encrypted);
			settingsElement.appendChild(tempFileExtension);

			// Write the content into XML file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();

			// Pretty print
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

			DOMSource source = new DOMSource(doc);
			StreamResult streamResult = new StreamResult(new File("./xml/config/sftp_get.xml"));

			transformer.transform(source, streamResult);

			result = true;
		}
		catch (Exception ex)
		{

		}

		return result;

	}

	public HashMap<String, EmailRecord> readEmailPropertiesFromXml()
	{
		HashMap<String, EmailRecord> result = new HashMap<String, EmailRecord>();

		try
		{

			File xmlFile = new File("./xml/config/email_properties.xml");

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();

			// Parse the XML
			Document document = builder.parse(xmlFile);
			document.getDocumentElement().normalize();

			// Get the <settings> element
			NodeList settingsList = document.getElementsByTagName("configuration");

			if (settingsList.getLength() > 0)
			{
				Element settings = (Element) settingsList.item(0);

				// Loop over child nodes
				NodeList children = settings.getChildNodes();
				for (int i2 = 0; i2 < children.getLength(); i2++)
				{
					Node child = children.item(i2);

					if (child.getNodeType() == Node.ELEMENT_NODE)
					{

						NamedNodeMap node = child.getAttributes();

						if (node != null)
						{
							EmailRecord ed = new EmailRecord();

							if (node.getNamedItem("name") != null)
							{
								ed.property = node.getNamedItem("name").getNodeValue();
							}
							else
							{
								ed.property = "error";
							}

							if (node.getNamedItem("encrypted") != null)
							{
								ed.encrypted = util.setBooleanFlag(node.getNamedItem("encrypted").getNodeValue());
							}
							else
							{
								ed.encrypted = "false";
							}

							if (node.getNamedItem("value") != null)
							{
								ed.value = cipher.conditionalDecrypt(node.getNamedItem("value").getNodeValue(), ed.encrypted);
							}
							else
							{
								ed.value = "";
							}

							if (node.getNamedItem("enabled") != null)
							{
								ed.enabled = util.setBooleanFlag(node.getNamedItem("enabled").getNodeValue());

							}
							else
							{
								ed.enabled = "false";
							}

							if (ed.property.equals("error") == false)
							{
								result.put(ed.property, ed);
							}

						}
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return result;
	}

	public HashMap<String, DistributionRecord> readDistributionListFromXml()
	{
		HashMap<String, DistributionRecord> result = new HashMap<>();

		try
		{

			var factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setIgnoringComments(true);
			factory.setIgnoringElementContentWhitespace(true);

			File xmlFile = new File("./xml/config/email_distribution_properties.xml");
			var builder = factory.newDocumentBuilder();
			Document doc = builder.parse(xmlFile);
			doc.getDocumentElement().normalize();

			NodeList nodes = doc.getElementsByTagName("distributionList");
			for (int i = 0; i < nodes.getLength(); i++)
			{
				Node n = nodes.item(i);
				if (n.getNodeType() != Node.ELEMENT_NODE)
					continue;
				Element e = (Element) n;

				String id = e.getAttribute("id");
				if (id == null || id.isBlank())
				{
					// skip entries without an id
					continue;
				}

				DistributionRecord ddata = new DistributionRecord();
				ddata.listId = id;

				ddata.enabled = util.setBooleanFlag(attrOrDefault(e, "enabled", "true").toLowerCase());

				ddata.maxFrequencyMins = parseLongAttr(e, "maxFrequencyMins", 0L);
				String address = e.getAttribute("value");
				if (address.equals(""))
					address = "";
				ddata.addressList = address;

				result.put(id, ddata);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return result;
	}

	public DistributionRecord readDistributionListFromXml(String distid)
	{
		DistributionRecord result = new DistributionRecord();

		try
		{

			var factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			factory.setIgnoringComments(true);
			factory.setIgnoringElementContentWhitespace(true);

			File xmlFile = new File("./xml/config/email_distribution_properties.xml");
			var builder = factory.newDocumentBuilder();
			Document doc = builder.parse(xmlFile);
			doc.getDocumentElement().normalize();

			NodeList nodes = doc.getElementsByTagName("distributionList");
			for (int i = 0; i < nodes.getLength(); i++)
			{
				Node n = nodes.item(i);
				if (n.getNodeType() != Node.ELEMENT_NODE)
					continue;
				Element e = (Element) n;

				String id = e.getAttribute("id");
				if (id == null || id.isBlank())
				{
					// skip entries without an id
					continue;
				}

				if (id.equals(distid))
				{

					result.listId = id;

					result.enabled = util.setBooleanFlag(attrOrDefault(e, "enabled", "true").toLowerCase());

					result.maxFrequencyMins = parseLongAttr(e, "maxFrequencyMins", 0L);

					String address = e.getAttribute("value");
					if (address.equals(""))
						address = "";

					result.addressList = address;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return result;
	}

	// --- helpers ---

	private static String attrOrDefault(Element e, String attr, String defVal)
	{
		String v = e.getAttribute(attr);
		return (v == null || v.isBlank()) ? defVal : v.trim();
	}

	private static long parseLongAttr(Element e, String attr, long defVal)
	{
		String v = e.getAttribute(attr);
		if (v == null || v.isBlank())
			return defVal;
		try
		{
			return Long.parseLong(v.trim());
		}
		catch (NumberFormatException ex)
		{
			return defVal;
		}
	}

	public boolean saveEmailPropertiesToXml(HashMap<String, EmailRecord> emailConfig)
	{

		boolean result = false;

		try
		{

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			// Root element
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("emailSettings");
			Element configElement = doc.createElement("configuration");

			rootElement.appendChild(configElement);

			doc.appendChild(rootElement);

			for (HashMap.Entry<String, EmailRecord> entry : emailConfig.entrySet())
			{

				Element propertyElement = doc.createElement("property");

				propertyElement.setAttribute("name", entry.getKey());
				propertyElement.setAttribute("encrypted", entry.getValue().encrypted);
				propertyElement.setAttribute("enabled", entry.getValue().enabled);
				propertyElement.setAttribute("value", cipher.conditionalEncrypt(entry.getValue().value, entry.getValue().encrypted));

				configElement.appendChild(propertyElement);

			}

			// Write the content into XML file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();

			// Pretty print
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

			DOMSource source = new DOMSource(doc);
			StreamResult streamResult = new StreamResult(new File("./xml/config/email_properties.xml"));

			transformer.transform(source, streamResult);

			result = true;
		}
		catch (Exception ex)
		{

		}

		return result;

	}

	public boolean saveEmailDistributionListToXml(HashMap<String, DistributionRecord> distConfig)
	{

		boolean result = false;

		try
		{

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			// Root element
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("emailSettings");

			doc.appendChild(rootElement);

			for (HashMap.Entry<String, DistributionRecord> entry : distConfig.entrySet())
			{
				Element distributionElement = doc.createElement("distributionList");

				distributionElement.setAttribute("id", entry.getKey());
				distributionElement.setAttribute("enabled", entry.getValue().enabled);
				distributionElement.setAttribute("value", entry.getValue().addressList);
				distributionElement.setAttribute("maxFrequencyMins", entry.getValue().maxFrequencyMins.toString());

				rootElement.appendChild(distributionElement);
			}

			// Write the content into XML file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();

			// Pretty print
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

			DOMSource source = new DOMSource(doc);
			StreamResult streamResult = new StreamResult(new File("./xml/config/email_distribution_properties.xml"));

			transformer.transform(source, streamResult);

			result = true;
		}
		catch (Exception ex)
		{

		}

		return result;

	}

	public HashMap<String, JschRecord> readJschPropertiesFromXml()
	{
		HashMap<String, JschRecord> result = new HashMap<String, JschRecord>();

		try
		{

			File xmlFile = new File("./xml/config/jsch_properties.xml");

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();

			// Parse the XML
			Document document = builder.parse(xmlFile);
			document.getDocumentElement().normalize();

			// Get the <settings> element
			NodeList settingsList = document.getElementsByTagName("properties");

			if (settingsList.getLength() > 0)
			{
				Element settings = (Element) settingsList.item(0);

				// Loop over child nodes
				NodeList children = settings.getChildNodes();
				for (int i2 = 0; i2 < children.getLength(); i2++)
				{
					Node child = children.item(i2);

					if (child.getNodeType() == Node.ELEMENT_NODE)
					{

						NamedNodeMap node = child.getAttributes();

						if (node != null)
						{
							JschRecord ed = new JschRecord();

							if (node.getNamedItem("id") != null)
							{
								ed.id = node.getNamedItem("id").getNodeValue();
							}
							else
							{
								ed.id = "error";
							}

							if (node.getNamedItem("encrypted") != null)
							{
								ed.encrypted = util.setBooleanFlag(node.getNamedItem("encrypted").getNodeValue());
							}
							else
							{
								ed.encrypted = "false";
							}

							if (node.getNamedItem("value") != null)
							{
								ed.value = cipher.conditionalDecrypt(node.getNamedItem("value").getNodeValue(), ed.encrypted);
							}
							else
							{
								ed.value = "";
							}

							if (node.getNamedItem("enabled") != null)
							{
								ed.enabled = util.setBooleanFlag(node.getNamedItem("enabled").getNodeValue());

							}
							else
							{
								ed.enabled = "false";
							}

							if (ed.id.equals("error") == false)
							{
								result.put(ed.id, ed);
							}

						}
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return result;
	}

	public boolean saveJschPropertiesToXml(HashMap<String, JschRecord> jshConfig)
	{

		boolean result = false;

		try
		{

			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

			// Root element
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("jsch");
			Element configElement = doc.createElement("properties");

			rootElement.appendChild(configElement);

			doc.appendChild(rootElement);

			for (HashMap.Entry<String, JschRecord> entry : jshConfig.entrySet())
			{

				Element propertyElement = doc.createElement("property");

				propertyElement.setAttribute("id", entry.getKey());
				propertyElement.setAttribute("encrypted", entry.getValue().encrypted);
				propertyElement.setAttribute("enabled", entry.getValue().enabled);
				propertyElement.setAttribute("value", cipher.conditionalEncrypt(entry.getValue().value, entry.getValue().encrypted));

				configElement.appendChild(propertyElement);

			}

			// Write the content into XML file
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();

			// Pretty print
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

			DOMSource source = new DOMSource(doc);
			StreamResult streamResult = new StreamResult(new File("./xml/config/jsch_properties.xml"));

			transformer.transform(source, streamResult);

			result = true;
		}
		catch (Exception ex)
		{

		}

		return result;

	}

}
