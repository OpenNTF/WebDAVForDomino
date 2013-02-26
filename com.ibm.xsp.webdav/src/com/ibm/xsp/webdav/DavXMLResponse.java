/** ========================================================================= *
 * Copyright (C) 2012 IBM Corporation                                         *
 *           based on work of                                                 *
 * Copyright (C) 2006, 2007 TAO Consulting Pte <http://www.taoconsulting.sg/> *
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

//import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Stack;
import java.util.TimeZone;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.ibm.xsp.webdav.interfaces.IDAVXMLResponse;

/**
 * The DavXMLResponse renders XML that is returned from PROPFIND or LOCK into
 * the respective XML structure into the DAV: namespace. Not really relevant for
 * web access but critical when accessing the stuff through a webDAV client like
 * webfolders in Windows Explorer
 * 
 * @author Stephan H. Wissel
 * 
 */
public class DavXMLResponse implements IDAVXMLResponse {

	/**
	 * the prefix for every element (needs to be checked if that works)
	 */
	private static final String DAV_PREFIX = "D:";
	private Locale DAV_LOCALE = Locale.getDefault();

	/**
	 * The namespace definition
	 */
	private static final String DAV_NAMESPACE = "DAV:";

	/**
	 * The namespace definition for auxiliary elements "DaveXTensions"
	 */
	private static final String AUX_NAMESPACE = "DXT:";

	/**
	 * the prefix for every element (needs to be checked if that works)
	 */
	private static final String AUX_PREFIX = "dxt:";

	/**
	 * Logger for Errors
	 */
	private static final Log LOGGER = LogFactory.getLog(DavXMLResponse.class);

	/**
	 * The root element needs the namespace, so we track it here
	 */
	private boolean rootElementWritten = false;

	/**
	 * Will we write back more than DAV:
	 */
	private boolean extendedResult = false;

	/**
	 * What stylesheet to use as processing instruction
	 */
	private String xmlStyleSheet = null;

	/**
	 * The writer we write out to
	 */
	// private ByteArrayOutputStream out = null;
	private StringWriter out = null;

	/**
	 * The stream result for the XML document
	 */
	private StreamResult streamResult = null;

	/**
	 * Keeping track of all open / closed XML tags
	 */
	private Stack<String> xmlTagStack = new Stack<String>();

	/**
	 * We can't write comments before we have the first element
	 */
	private ArrayList<String> deferedComments;

	/**
	 * Where we write out all the XML content
	 */
	TransformerHandler body = null;

	/**
	 * creates as new XML reply that gets written out to the response writer
	 * 
	 * @param curResp
	 */
	public DavXMLResponse() {
	}

	/**
	 * creates as new XML reply that gets written out to the response writer
	 * adds an XML stylesheet processing instruction at the top to ensure nice
	 * display in browsers
	 * 
	 * @param curResp
	 * @param stylesheet
	 */
	public DavXMLResponse(String stylesheet, boolean extendedResult) {
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
			try {
				body.comment(comment.toCharArray(), 0, comment.length());
			} catch (SAXException e) {
				LOGGER.error(e);
			}
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
		// results
		if (!this.rootElementWritten || !this.extendedResult) {
			return;
		}
		try {
			body.startElement("", tagName, AUX_PREFIX + tagName, null);
			body.characters(tagValue.toCharArray(), 0, tagValue.length());
			body.endElement("", tagName, AUX_PREFIX + tagName);
		} catch (SAXException e) {
			LOGGER.error(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.xsp.webdav.IDAVXMLResponse#cdataTag(java.lang.String,
	 * java.lang.String)
	 */
	public void cdataTag(String tagName, String tagValue) {
		this.openTag(tagName);
		try {
			body.startCDATA();
			body.characters(tagValue.toCharArray(), 0, tagValue.length());
			body.endCDATA();
		} catch (SAXException e) {
			LOGGER.error(e);
		}
		this.closeTag(1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.xsp.webdav.IDAVXMLResponse#closeDocument()
	 */
	public void closeDocument() {
		this.closeTag(-1); // Make sure all tages are closes
		try {
			this.body.endDocument();
			this.out.flush();
			this.out.close();
		} catch (SAXException e) {
			LOGGER.error(e);
		} catch (IOException e) {
			LOGGER.error(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.xsp.webdav.IDAVXMLResponse#closeTag(int)
	 */
	public void closeTag(int howMany) {

		if (howMany < 0) {
			while (!this.xmlTagStack.empty()) {
				try {
					String closeTag = this.xmlTagStack.pop();
					body.endElement("", closeTag, DAV_PREFIX + closeTag);
				} catch (SAXException e) {
					LOGGER.error(e);
					break;
				}
			}
		} else {
			for (int i = 0; i < howMany; i++) {
				if (!this.xmlTagStack.empty()) {
					try {
						String closeTag = this.xmlTagStack.pop();
						body.endElement("", closeTag, DAV_PREFIX + closeTag);
					} catch (SAXException e) {
						LOGGER.error(e);
						break;
					}
				} else {
					break; // No point looping
				}
			}
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
			try {
				String closeTag = this.xmlTagStack.pop();
				body.endElement("", closeTag, DAV_PREFIX + closeTag);
				if (closeTag.equals(lastTagToClose)) {
					result = true;
					break;
				}
			} catch (SAXException e) {
				LOGGER.error(e);
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
		this.openTag(tagName);
		this.closeTag(1);
	}

	/**
	 * Gets the transformer handler object where we write everything to
	 * 
	 * @param streamResult
	 *            the place where we write the result to
	 * @return the body object to append XML tags to
	 */
	private TransformerHandler getSAXOutputObject(StreamResult streamResult) {

		// Factory pattern at work
		SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory
				.newInstance();
		// SAX2.0 ContentHandler that provides the append point and access to
		// serializing options
		TransformerHandler hd;
		try {
			hd = tf.newTransformerHandler();
			Transformer serializer = hd.getTransformer();
			serializer.setOutputProperty(OutputKeys.ENCODING, "utf-8");// Suitable
			// for
			// all
			// languages
			serializer.setOutputProperty(OutputKeys.METHOD, "xml");
			if (this.extendedResult || true) {
				serializer.setOutputProperty(OutputKeys.INDENT, "yes");
			}
			hd.setResult(streamResult);

			return hd;

		} catch (TransformerConfigurationException e) {
			LOGGER.error(e);
		}

		return null;

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

			try {
				body.startElement("", tagName, DAV_PREFIX + tagName, null);
				this.xmlTagStack.push(tagName);
			} catch (SAXException e) {
				LOGGER.error(e);
			}
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
		try {
			body.characters(tagValue.toCharArray(), 0, tagValue.length());
		} catch (SAXException e) {
			LOGGER.error(e);
		} catch (NullPointerException e) {
			LOGGER.error(e);
		}
		this.closeTag(1);
	}

	/**
	 * Starts a new XML document and inserts the correct starting tag with name
	 * spaces
	 * 
	 * @param tagName
	 */
	private void startNewResponseBody(String tagName) {

		// this.out = new ByteArrayOutputStream();
		this.out = new StringWriter();

		if (this.out != null) {
			this.streamResult = new StreamResult(out);
			this.body = this.getSAXOutputObject(streamResult);

			try {
				// Start the document
				this.body.startDocument();

				// Stylesheet if we have one and we are on extended results
				if (this.xmlStyleSheet != null && this.extendedResult) {
					this.body.processingInstruction("xml-stylesheet",
							"type=\"text/xsl\" href=\"" + this.xmlStyleSheet
									+ "\"");
				}

				// First tag with correct namespaces
				AttributesImpl attr = new AttributesImpl();
				String nsShort = DAV_PREFIX.substring(0,
						(DAV_PREFIX.length() - 1));
				String auxShort = AUX_PREFIX.substring(0,
						(AUX_PREFIX.length() - 1));
				attr.addAttribute("", nsShort, "xmlns:" + nsShort, "String",
						DAV_NAMESPACE);
				if (this.extendedResult) {
					attr.addAttribute("", auxShort, "xmlns:" + auxShort,
							"String", AUX_NAMESPACE);
				}
				this.body.startElement("", tagName, DAV_PREFIX + tagName, attr);
				this.xmlTagStack.push(tagName);
				this.rootElementWritten = true; // We memorize that

				// If there are defered commends process them
				if (this.deferedComments != null) {
					for (String curComment : this.deferedComments) {
						this.addComment(curComment);
					}
				}

			} catch (SAXException e) {
				LOGGER.error(e);
			}

		} else {
			LOGGER.error("Can't start an XML body for the response");
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
			// return;
			date = new Date();

		}

		// "2005-08-10T10:19:24Z";
		String creatFormat = "yyyy'-'MM'-'d'T0'H':'m':'s'Z'";
		SimpleDateFormat fmt = new SimpleDateFormat(creatFormat, DAV_LOCALE);
		fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
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
			date = new Date();
			// return;
		}
		// "Sat, 26 Mar 2005 11:22:20 GMT";
		String lastmodFormat = "EE', 'd' 'MMM' 'yyyy' 'H':'m':'s' 'z";
		SimpleDateFormat fmt = new SimpleDateFormat(lastmodFormat, DAV_LOCALE);
		fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		String datestring = fmt.format(date);
		this.simpleTag(TagName, datestring);
	}

	@Override
	public String toString() {
		if (this.out != null) {
			return this.out.toString();
		}
		return super.toString();
	}

	public void setLocale(Locale l) {
		this.DAV_LOCALE = l;
	}
}
