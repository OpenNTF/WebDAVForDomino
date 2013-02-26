/* ========================================================================== *
 * Copyright (C) 2006, 2007 TAO Consulting Pte <http://www.taoconsulting.sg/> *
 *           based on work of                                                 *
 *           (C) 2004-2005 Pier Fumagalli <http://www.betaversion.org/~pier/> *
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
 * ========================================================================== */
package biz.taoconsulting.dominodav.methods;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import biz.taoconsulting.dominodav.DAVProperties;
import biz.taoconsulting.dominodav.LockInfo;
import biz.taoconsulting.dominodav.LockManager;
import biz.taoconsulting.dominodav.exceptions.DAVNotFoundException;
import biz.taoconsulting.dominodav.interfaces.IDAVRepository;
import biz.taoconsulting.dominodav.interfaces.IDAVResource;

import com.ibm.xsp.webdav.DavXMLResponsefactory;
import com.ibm.xsp.webdav.interfaces.IDAVXMLResponse;

/**
 * @author Bastian Buch (TAO Consulting)
 * 
 */
public class PROPPATCH extends AbstractDAVMethod {
	private static final Log LOGGER = LogFactory.getLog(PUT.class);

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.methods.AbstractDAVMethod#action()
	 */
	@SuppressWarnings("deprecation")
	protected void action() throws Exception {
		// TODO Implement!
		IDAVRepository rep = this.getRepository();
		String curURI = (String) this.getHeaderValues().get("uri");
		IDAVResource resource = null;
		String relockToken = this.getRelockToken(this.getReq());
		LockManager lm = this.getLockManager();
		// int status = HttpServletResponse.SC_OK; // We presume success
		LockInfo li = null;
		HttpServletResponse resp = this.getResp();
		Long TimeOutValue = this.getTimeOutValue(this.getReq());
		try {
			// LOGGER.info("getResource");
			resource = rep.getResource(curURI, true);

		} catch (DAVNotFoundException e) {
			// This exception isn't a problem since we just can create the new
			// URL
			// LOGGER.info("Exception not found resource");

		}
		if (resource == null) {
			// LOGGER.info("Error, resource is null");
			// Set the return error
			// Unprocessable Entity (see
			// http://www.webdav.org/specs/rfc2518.html#status.code.extensions.to.http11)
			this.setHTTPStatus(403);
			return;
		}
		if (resource.isReadOnly()) {
			this.setHTTPStatus(403);
			return;
		}
		if (relockToken != null) {
			li = lm.relock(resource, relockToken, TimeOutValue);
			if (li == null) {
				String eString = "Relock failed for " + relockToken;
				LOGGER.debug(eString);
				this.setErrorMessage(eString, 423); // Precondition failed
				this.setHTTPStatus(423);
				return;
			}
		}
		String creationDate = null, modifiedDate = null;
		Date dt = new Date();
		Date dtM = new Date();
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		LOGGER.info("DB Factory built OK");
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		LOGGER.info("Document builder OK");
		Document doc = dBuilder.parse(this.getReq().getInputStream());
		LOGGER.info("XML Doc ok read");
		if (doc != null) {
			LOGGER.info("doc is not null");
			NodeList nlCreation = doc
					.getElementsByTagName("Z:Win32CreationTime");
			NodeList nlModified = doc
					.getElementsByTagName("Z:Win32LastModifiedTime");
			if (nlCreation != null) {
				LOGGER.info("nlCreation not null");
				Node nNodeCreation = nlCreation.item(0);
				if (nNodeCreation != null) {
					LOGGER.info("nNodeCreation not null, is "
							+ nNodeCreation.getTextContent());
					creationDate = nNodeCreation.getTextContent();
					LOGGER.info("Creation date=" + creationDate
							+ "  Locale is "
							+ this.getReq().getLocale().toString());
					DateFormat df = new SimpleDateFormat(
							"EEE, dd MMM yyyy HH:mm:ss z", Locale.getDefault());
					LOGGER.info("SimpleDate Format ok created");
					try {
						dt = df.parse(creationDate);
					} catch (Exception e) {
						creationDate += "+00";
						dt = df.parse(creationDate);
					}
					try {
						// dt.setTime(dt.getTime()-3*60*60*1000);
						resource.patchCreationDate(dt);
					} catch (Exception e) {
					}
					LOGGER.info("Date dt parsed with value=" + dt.toString());
				}
			}
			if (nlModified != null) {
				LOGGER.info("nlModified not null");
				Node nNodeModified = nlModified.item(0);
				if (nNodeModified != null) {
					LOGGER.info("nNodeModified not null");
					modifiedDate = nNodeModified.getTextContent();
					LOGGER.info("Modified date=" + modifiedDate);
					Locale defLoc = Locale.getDefault();
					// This is the crap reason why Win7 didn;t work!
					DateFormat df = new SimpleDateFormat(
							"EEE, dd MMM yyyy HH:mm:ss z", defLoc);
					try {
						dtM = df.parse(modifiedDate);
					} catch (Exception e) {
						modifiedDate += "+00";
						dtM = df.parse(modifiedDate);
					}
					try {
						// dtM.setTime(dtM.getTime()-3*60*60*1000);
						resource.patchLastModified(dtM);
					} catch (Exception e) {
					}
					LOGGER.info("Date dtM parsed with value=" + dtM.toString());
				}
			}
		}

		IDAVXMLResponse xr = DavXMLResponsefactory.getXMLResponse(null, false);
		xr.openTag("multistatus");
		resource.addToDavXMLResponsePROPPATCH(xr);
		xr.closeDocument();
		this.setHTTPStatus(DAVProperties.STATUS_MULTIPART);
		// FIXME: this is depricated!
		resp.setStatus(DAVProperties.STATUS_MULTIPART,
				DAVProperties.STATUS_MULTIPART_STRING);
		resp.setContentType(DAVProperties.TYPE_XML);
		String result = xr.toString();
		resp.setContentLength(result.length());
		PrintWriter out = resp.getWriter();
		out.write(result);
		out.close();
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.methods.AbstractDAVMethod#writeInitialHeader()
	 */
	protected void writeInitialHeader() {
		// No action needed

	}

	private String getRelockToken(HttpServletRequest req) {
		/*
		 * The token looks like this: If:
		 * (<opaquelocktoken:fecc6ff70de5a701f6a52bb53fec9083>)
		 */
		String result = null;
		String relockToken = req.getHeader("If");

		if (relockToken != null) {

			int start = relockToken.indexOf("<");
			int end = relockToken.lastIndexOf(">");

			if (start > 0 && end > start + 1) {
				result = relockToken.substring(start + 1, end);
			}
		}
		return result;
	}

	private Long getTimeOutValue(HttpServletRequest req) {

		String TimeOutHeader = req.getHeader("Timeout");
		Long TimeOutValue = LockManager.MAX_LOCK_DURATION_SEC;

		if (TimeOutHeader != null) {
			int whereDoSecondsStart = TimeOutHeader.indexOf('-') + 1;
			if (whereDoSecondsStart > 0) {
				try {
					String sub = TimeOutHeader.substring(whereDoSecondsStart);
					TimeOutValue = new Long(sub).longValue();
					if (TimeOutValue > LockManager.MAX_LOCK_DURATION_SEC) {
						TimeOutValue = LockManager.MAX_LOCK_DURATION_SEC;
					}
				} catch (Exception e) {
					// The header could not get converted
					TimeOutValue = LockManager.MAX_LOCK_DURATION_SEC;
				}
			}
		}
		return TimeOutValue;

	}
}
