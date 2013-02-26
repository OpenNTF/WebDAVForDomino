/** ========================================================================= *
 * Copyright (C) 2012 IBM Corporation                                         *
 *                            All rights reserved.                            *
 * ========================================================================== *
 *                                                                            *
 * Licensed under the  Apache License, Version 2.0  (the "License").  You may *
 * not use this file except in compliance with the License.  You may obtain a *
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.       *
 *                                                                            *
 * Unless  required  by applicable  law or  agreed  to  in writing,  software *
 * distributed under the License is distributed on an  "AS IS" BASIS, WITHOUT *
 * WARRANTIES OR  CONDITIONS OF ANY KIND, either express or implied.  See the *
 * License for the  specific language  governing permissions  and limitations *
 * under the License.                                                         *
 *                                                                            *
 * ========================================================================== **/
package com.ibm.xsp.webdav;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Stack;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ibm.xsp.webdav.interfaces.IDAVXMLResponse;

/**
 * This class should not exist. It is manual XML, but Windows folder might need
 * it
 * 
 * @author Stephan H. Wissel
 * 
 */
public class DavXMLResponsePlain implements IDAVXMLResponse {

	/**
	 * the prefix for every element (needs to be checked if that works)
	 */
	private static final String DAV_PREFIX = "D:";

	/**
	 * The namespace definition
	 */
	private static final String DAV_NAMESPACE = "DAV:";

	/**
	 * The namespace definition for auxiliary elements "DaveXTensions"
	 */
	private static final String[] AUX_NAMESPACE = {
			"urn:schemas-microsoft-com:office:office",
			"http://schemas.microsoft.com/" };

	/**
	 * the prefix for every element (needs to be checked if that works)
	 */
	private static final String[] AUX_PREFIX = { "o:", "Repl" };

	/**
	 * Logger for Errors
	 */
	private static final Log LOGGER = LogFactory
			.getLog(DavXMLResponsePlain.class);

	// To avoid empty lines
	private boolean newLineSend = false;

	/**
	 * The root element needs the namespace, so we track it here
	 */
	private boolean rootElementWritten = false;

	/**
	 * Will we write back more than DAV:
	 */
	private boolean extendedResult = true;

	/**
	 * What stylesheet to use as processing instruction
	 */
	private String xmlStyleSheet = null;

	private ArrayList<String> tagsOnNewLine = null;

	/**
	 * The writer we write out to
	 */
	private StringBuilder out = null;

	/**
	 * Keeping track of all open / closed XML tags
	 */
	private Stack<String> xmlTagStack = new Stack<String>();

	/**
	 * We can't write comments before we have the first element
	 */
	private ArrayList<String> deferedComments;

	/**
	 * creates as new XML reply that gets written out to the response writer
	 * 
	 * @param curResp
	 */
	public DavXMLResponsePlain() {
	}

	/**
	 * creates as new XML reply that gets written out to the response writer
	 * adds an XML stylesheet processing instruction at the top to ensure nice
	 * display in browsers
	 * 
	 * @param curResp
	 * @param stylesheet
	 */
	public DavXMLResponsePlain(String stylesheet, boolean extendedResult) {
		this.extendedResult = extendedResult;
		this.xmlStyleSheet = stylesheet;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.xsp.webdav.IDAVXMLResponse#addComment(java.lang.String)
	 */
	public void addComment(String comment) {
		// Comment only gets written on extended results
		if (!this.extendedResult) {
			return;
		}
		if (!this.rootElementWritten) {
			if (this.deferedComments == null) {
				this.deferedComments = new ArrayList<String>();
			}
			this.deferedComments.add(comment);
		} else {
			out.append("\n<!-- ");
			out.append(comment);
			out.append(" -->");
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.xsp.webdav.IDAVXMLResponse#auxTag(java.lang.String,
	 * java.lang.String)
	 */
	public void auxTag(String tagName, String tagValue) {
		// Auxliliary Tag can be the first and only gets written on extended
		// results and always start on a new line
		if (!this.rootElementWritten || !this.extendedResult) {
			return;
		}

		if (!newLineSend) {
			out.append("\n");
		}
		out.append("<" + AUX_PREFIX[0] + tagName + ">");
		out.append(tagValue);
		out.append("</" + AUX_PREFIX[0] + tagName + ">\n");
		newLineSend = true;
	}

	public void auxPrefixedTag(String prefixName, String tagName,
			String tagValue) {
		// Auxliliary Tag can be the first and only gets written on extended
		// results and always start on a new line
		if (!this.rootElementWritten || !this.extendedResult) {
			return;
		}

		if (!newLineSend) {
			out.append("\n");
		}
		out.append("<" + prefixName + tagName + ">");
		out.append(tagValue);
		out.append("</" + prefixName + tagName + ">\n");
		newLineSend = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.xsp.webdav.IDAVXMLResponse#cdataTag(java.lang.String,
	 * java.lang.String)
	 */
	public void cdataTag(String tagName, String tagValue) {
		this.openTag(tagName);
		out.append("<![CDATA[");
		out.append(tagValue);
		out.append("]]>");
		this.closeTag(1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.xsp.webdav.IDAVXMLResponse#closeDocument()
	 */
	public void closeDocument() {
		this.closeTag(-1); // Make sure all tages are closes
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.xsp.webdav.IDAVXMLResponse#closeTag(int)
	 */
	public void closeTag(int howMany) {

		if (howMany < 0) {
			while (!this.xmlTagStack.empty()) {
				String closeTag = this.xmlTagStack.pop();
				out.append("</" + DAV_PREFIX + closeTag + ">");
				doNewLineIfNeeded(closeTag);
			}
		} else {
			for (int i = 0; i < howMany; i++) {
				if (!this.xmlTagStack.empty()) {
					String closeTag = this.xmlTagStack.pop();
					out.append("</" + DAV_PREFIX + closeTag + ">");
					doNewLineIfNeeded(closeTag);
				} else {
					break; // No point looping
				}
			}
		}
	}

	private void doNewLineIfNeeded(String closeTag) {
		if (getNewLineForTag(closeTag)) {
			out.append("\n");
			newLineSend = true;
		} else {
			newLineSend = false;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.xsp.webdav.IDAVXMLResponse#closeTag(java.lang.String)
	 */
	public boolean closeTag(String lastTagToClose) {

		boolean result = false;

		while (!this.xmlTagStack.empty()) {

			String closeTag = this.xmlTagStack.pop();
			out.append("</" + DAV_PREFIX + closeTag + ">");
			doNewLineIfNeeded(closeTag);
			if (closeTag.equals(lastTagToClose)) {
				result = true;
				break;
			}
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.xsp.webdav.IDAVXMLResponse#emptyTag(java.lang.String)
	 */
	public void emptyTag(String tagName) {

		if ("source".equals(tagName)) {
			this.openTag(tagName);
			this.closeTag(1);
		} else {
			boolean doNewLine = getNewLineForTag(tagName);
			if (doNewLine && !newLineSend) {
				out.append("\n");
			}
			out.append("<" + DAV_PREFIX + tagName + "/>");
			if (doNewLine) {
				out.append("\n");
			}
			newLineSend = doNewLine;
		}

	}

	/**
	 * Returns the XML for the current document. Nice to be written into the
	 * response
	 * 
	 * @return
	 */
	// public byte[] getXMLBytes() {
	// return this.out.toByteArray();
	// }
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.xsp.webdav.IDAVXMLResponse#openTag(java.lang.String)
	 */
	public void openTag(String tagName) {

		if (this.rootElementWritten) {
			if (getNewLineForTag(tagName) && !newLineSend) {
				out.append("\n");
			}
			newLineSend = false;
			out.append("<" + DAV_PREFIX + tagName + ">");
			this.xmlTagStack.push(tagName);
		} else {
			this.startNewResponseBody(tagName);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.xsp.webdav.IDAVXMLResponse#simpleTag(java.lang.String,
	 * java.lang.String)
	 */
	public void simpleTag(String tagName, String tagValue) {
		this.openTag(tagName);
		out.append(tagValue);
		this.closeTag(1);
	}

	/**
	 * Starts a new XML document and inserts the correct starting tag with name
	 * spaces
	 * 
	 * @param tagName
	 */
	private void startNewResponseBody(String tagName) {

		LOGGER.debug("New simple XML started");
		this.out = new StringBuilder();
		out.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");

		if (this.xmlStyleSheet != null && this.extendedResult) {
			// Stylesheet if we have one and we are on extended results
			out.append("<?xml-stylesheet type=\"text/xsl\" href=\""
					+ this.xmlStyleSheet + "\"?>\n");
		}

		String nsShort = DAV_PREFIX.substring(0, (DAV_PREFIX.length() - 1));
		out.append("<" + DAV_PREFIX + tagName + " xmlns:" + nsShort + "=\""
				+ DAV_NAMESPACE + "\"");
		if (this.extendedResult) {
			for (int i = 0; i < AUX_PREFIX.length; i++) {
				String auxShort = AUX_PREFIX[i].substring(0,
						(AUX_PREFIX[i].length() - 1));
				out.append(" xmlns:" + auxShort + "=\"" + AUX_NAMESPACE[i]
						+ "\"");
			}
		}
		out.append(">");

		this.xmlTagStack.push(tagName);
		this.rootElementWritten = true; // We memorize that

		// If there are defered commends process them
		if (this.deferedComments != null) {
			for (String curComment : this.deferedComments) {
				this.addComment(curComment);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.xsp.webdav.IDAVXMLResponse#dateTagForCreateDate(java.lang.String,
	 * java.util.Date)
	 */
	public void dateTagForCreateDate(String TagName, Date date) {
		if (date == null) {
			return;
		}

		// "2005-08-10T10:19:24Z";
		/*
		 * String[] dateParsed=date.toString().split(" "); String
		 * timeZone=dateParsed[dateParsed.length-2];
		 * if(timeZone.equals("EET")||timeZone.equals("EEST")){ timeZone="GMT";
		 * }
		 */
		String creatFormat = "yyyy'-'MM'-'dd'T'HH':'mm':'ss'Z'";
		SimpleDateFormat fmt = new SimpleDateFormat(creatFormat, Locale.UK);
		String datestring = fmt.format(date);
		this.simpleTag(TagName, datestring);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.xsp.webdav.IDAVXMLResponse#dateTag(java.lang.String,
	 * java.util.Date)
	 */
	public void dateTag(String TagName, Date date) {
		if (date == null) {
			return;
		}
		// "Sat, 26 Mar 2005 11:22:20 GMT";
		String[] dateParsed = date.toString().split(" ");
		String timeZone = dateParsed[dateParsed.length - 2];
		if (timeZone.equals("EEST")) {
			timeZone = "EET";
		}
		if (timeZone.equals("EET") || timeZone.equals("ZE2")) {

			// if(TimeZone.getTimeZone(timeZone).getRawOffset()>0){
			// LOGGER.info("Time zone="+timeZone);
			// LOGGER.info("Time offset="+ new Integer
			// (TimeZone.getTimeZone(timeZone).getRawOffset()).toString());
			date = new Date(date.getTime()
					- TimeZone.getTimeZone(timeZone).getRawOffset());
			timeZone = "GMT";
		}
		String lastmodFormat = "EE', 'd' 'MMM' 'yyyy' 'HH':'mm':'ss";
		//Locale.UK is the secret to make it work in Win7!
		SimpleDateFormat fmt = new SimpleDateFormat(lastmodFormat, Locale.UK);
		String datestring = fmt.format(date);
		this.simpleTag(TagName, datestring + " " + timeZone);

	}

	@Override
	public String toString() {
		if (this.out != null) {
			return this.out.toString();
		}
		return super.toString();
	}

	/**
	 * Not all Tags start on a new line
	 * 
	 * @param tagName
	 * @return
	 */
	private boolean getNewLineForTag(String tagName) {
		if (this.tagsOnNewLine == null) {
			this.loadTagsOnNewLine();
		}
		return this.tagsOnNewLine.contains(tagName);
	}

	private void loadTagsOnNewLine() {
		tagsOnNewLine = new ArrayList<String>();
		tagsOnNewLine.add("multistatus");
		tagsOnNewLine.add("response");
		tagsOnNewLine.add("propstat");
		tagsOnNewLine.add("displayname");
		tagsOnNewLine.add("resoucetype");
		tagsOnNewLine.add("source");
		tagsOnNewLine.add("supportedlock");
		tagsOnNewLine.add("lockdiscovery");
		tagsOnNewLine.add("status");
		tagsOnNewLine.add("getlastmodified");
		tagsOnNewLine.add("getcontentlength");
		tagsOnNewLine.add("getetag");
		tagsOnNewLine.add("getcontenttype");
	}
}
