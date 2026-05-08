package com.commander4j.xml;

import java.io.File;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class JXMLDocument
{
	private DocumentBuilder builder;
	private Document document;
	private XPath xpath = XPathFactory.newInstance().newXPath();

	private final Logger logger = org.apache.logging.log4j.LogManager.getLogger(JXMLDocument.class);

	public Document getDocument()
	{
		return document;
	}

	public JXMLDocument()
	{
		try
		{
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		}
		catch (Exception ex)
		{
			logger.error("JXMLDocument constructor " + ex.getMessage());
		}
	}

	public JXMLDocument(String filename)
	{
		this();
		setDocument(filename);
	}

	public String findXPath(String path)
	{
		String result = "";

		try
		{
			Node widgetNode = (Node) xpath.evaluate(path, document, XPathConstants.NODE);
			result = widgetNode.getFirstChild().getNodeValue().toString();
		}
		catch (Exception ex)
		{
			result = "";
		}

		return result;
	}

	public Boolean setDocument(String filename)
	{
		Boolean result = false;
		logger.debug("setDocument :" + filename);
		try
		{
			document = builder.parse(new File(filename));
			result = true;
		}
		catch (Exception ex)
		{
			logger.error("JXMLDocument.setDocument " + ex.getMessage());
		}
		return result;
	}

	public void setDocument(Document doc)
	{
		document = doc;
	}

	public void setDocumentText(String text)
	{
		logger.debug("setDocumentText :" + text);
		try
		{
			document = builder.parse(new InputSource(new StringReader(text)));
		}
		catch (Exception ex)
		{
			logger.error("JXMLDocument.setDocumentText " + ex.getMessage());
		}
	}
}
