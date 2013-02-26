/* ========================================================================== *
 * Copyright (C) 2006, 2007 TAO Consulting Pte <http://www.taoconsulting.sg/> *
 *           based on work of                                                 *
 *           C) 2004-2005 Pier Fumagalli <http://www.betaversion.org/~pier/>  *
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
public class LOCK extends AbstractDAVMethod {

	/**
	 * The logger object for event logging
	 */
	private static final Log LOGGER = LogFactory.getLog(LOCK.class);

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.methods.AbstractDAVMethod#action()
	 */
	protected void action() {
		// uri is the unique identifier on the host includes servlet and
		// repository but not server
		LockInfo li = null;
		int status = HttpServletResponse.SC_OK; // We presume success
		String curURI = (String) this.getHeaderValues().get("uri");
		LockManager lm = this.getLockManager();
		HttpServletRequest req = this.getReq();
		HttpServletResponse res = this.getResp();
		String lockrequestorName = this.getOwnerFromXMLLockRequest(req);

		IDAVResource resource = null;
		Long TimeOutValue = this.getTimeOutValue(req);
		String relockToken = this.getRelockToken(req);

		// Get the resource to lock
		// Populates the resource and set status
		resource = this.getResourceToLock(curURI);
		try {
			curURI = java.net.URLDecoder.decode(curURI, "UTF-8");
		} catch (Exception e) {
		}
		// One can't lock a readonly resource
		if (resource == null || resource.isReadOnly()) {
			status = HttpServletResponse.SC_FORBIDDEN; // Client can't lock this
		} else if (relockToken != null) {
			li = lm.relock(resource, relockToken, TimeOutValue);
			if (li == null) {
				String eString = "Relock failed for " + relockToken + " for "
						+ lockrequestorName;
				LOGGER.debug(eString);
				this.setErrorMessage(eString, 412); // Precondition failed
				status = 412;
			} else {
				LOGGER.debug("successful relock for " + relockToken
						+ ", new Token:" + li.getToken());
				status = HttpServletResponse.SC_OK;
			}
		} else {
			li = lm.lock(resource, lockrequestorName, TimeOutValue);
			if (li == null) {
				String eString = "Lock failed for " + lockrequestorName
						+ " and " + curURI;
				LOGGER.debug(eString); // Locked by someone else
				this.setErrorMessage(eString, 423); // Locked by someone else
				status = 423;
			} else {
				LOGGER.debug("Lock successful:" + curURI + " for "
						+ lockrequestorName);
				status = HttpServletResponse.SC_OK;
			}
		}

		// Status of the request
		this.setHTTPStatus(status);

		if (status < 400) { // 400 and 500 determine failure!
			res.addHeader("Lock-Token", "<" + li.getToken() + ">");
			// Render the XML response
			this.writeLockResponseXML(res, li);
		}
	}

	/**
	 * Extracts the XML Document than contains the lock request
	 * 
	 * @param req
	 */

	/**
	 * Extracts the XML Document than contains the lock request
	 * 
	 * @param req
	 */
	private String getOwnerFromXMLLockRequest(HttpServletRequest req) {
		// We don't treat XML as XML here, but we just need a string
		// Not terrible good code
		String result = null;
		/*
		 * Looks like this <?xml version="1.0" encoding="UTF-8" ?> <lockinfo
		 * xmlns="DAV:"> <locktype> <write/> </locktype> <lockscope>
		 * <exclusive/> </lockscope> <owner>Administrator</owner> </lockinfo>
		 */

		boolean found = false;
		String curLine = null;
		BufferedReader in = null;
		// byte[] data = new byte[req.getContentLength()];
		// int len = 0, totalLen = 0;

		try {
			// LOGGER.info("1.1");
			// in = req.getReader();
			InputStream ins = req.getInputStream();
			// InputStream in= new
			// FileInputStream("d:/forChml/PunchOutSetupRequest.xml");
			StringBuffer xmlStr = new StringBuffer();
			int d;
			while ((d = ins.read()) != -1) {
				xmlStr.append((char) d);
			}
			// LOGGER.info("1.2");
			// do {
			// curLine = in.readLine();
			curLine = xmlStr.toString();
			// LOGGER.info("1.3="+curLine);
			if (curLine != null && curLine.contains("<owner>")) {
				found = true;

			} else {
				// LOGGER.info("1.4");
				if (curLine != null && curLine.contains("<D:owner>")) {
					found = true;
				}
			}
			// } while (!found && curLine != null);

			if (found) {
				// curLine=curLine.substring(startIndex);
				int start = curLine.indexOf(">");
				int stop = curLine.lastIndexOf("<");
				result = curLine.substring(start + 1, stop);
				// LOGGER.info("1.5="+result);
				start = result.indexOf(">");
				stop = result.lastIndexOf("<");

				if (start > 0) {
					result = result.substring(start + 1, stop);
					start = result.indexOf("href>");
					stop = result.lastIndexOf("<");

					if (start > 0) {
						if (stop > 0) {
							result = result.substring(start + 5, stop);
						} else {
							result = result.substring(start + 5);
						}
					}

					// LOGGER.info("1.6="+result);
				}
			}

		} catch (Exception e) {
			LOGGER.error(e);
			result = null;
		} finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {
				LOGGER.error(e);
			}
		}

		return result;

	}

	/**
	 * Gets the relock header if we have one and extracts the opaque token for
	 * the lock activity
	 * 
	 * @param req
	 * @return the relock token or null if it isn't in a suitable format
	 */
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

	/**
	 * Get the resource to lock - migh be a new resource
	 * 
	 * @param resource
	 *            the empty resource
	 * @param curURI
	 *            the URL
	 * @return new resource if it worked
	 */
	private IDAVResource getResourceToLock(String curURI) {
		IDAVRepository rep = this.getRepository();
		IDAVResource resource = null;
		try {
			curURI = java.net.URLDecoder.decode(curURI, "UTF-8");
		} catch (Exception e) {
		}
		try {
			resource = rep.getResource(curURI, true);
		} catch (DAVNotFoundException exc) {
			// This could be a preliminary lock before a write
			resource = rep.createNewResource(curURI);
		}

		return resource;
	}

	/**
	 * Get the Lock duration in seconds, fall back to default if not found in
	 * header or the value is not in the expected format
	 * 
	 * @param req
	 * @return
	 */
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

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.methods.AbstractDAVMethod#writeInitialHeader()
	 */
	protected void writeInitialHeader() {
		// No header to add

	}

	/**
	 * Writes out the XML response to a lock request
	 * 
	 * @param res
	 * @param li
	 */
	private void writeLockResponseXML(HttpServletResponse resp, LockInfo li) {
		IDAVXMLResponse xr = DavXMLResponsefactory.getXMLResponse(null, false);

		/*
		 * Response looks like this: <D:prop xmlns:D="DAV:"> <D:lockdiscovery>
		 * <D:activelock> <D:locktype> <D:write/> </D:locktype> <D:lockscope>
		 * <D:exclusive/> </D:lockscope> <D:depth>0</D:depth>
		 * <D:owner>Administrator</D:owner> <D:timeout>Second-179</D:timeout>
		 * <D:locktoken>
		 * <D:href>opaquelocktoken:45b028e368da5f3e1d9652b2a8f1e1dc</D:href>
		 * </D:locktoken> </D:activelock> </D:lockdiscovery></D:prop>
		 */

		xr.openTag("prop");
		xr.auxTag("username", li.getUsername());
		xr.openTag("lockdiscovery");
		xr.openTag("activelock");

		xr.openTag("locktype");
		xr.emptyTag("write");
		xr.closeTag(1);

		xr.openTag("lockscope");
		xr.emptyTag("exclusive");
		xr.closeTag(1);

		xr.simpleTag("depth", "0");
		xr.simpleTag("owner", li.getLocalUsername());
		xr.simpleTag("timeout", "Second-" + li.getTimeout());
		xr.openTag("locktoken");
		xr.simpleTag("href", li.getToken());

		xr.closeDocument(); // Close all pending tags and the document

		resp.setContentType(DAVProperties.TYPE_XML);
		// resp.setHeader("content-encoding", "utf-8");
		// resp.setContentLength(xr.getXMLBytes().length);

		try {
			String result = xr.toString();
			resp.setContentLength(result.length());
			PrintWriter out = resp.getWriter();
			out.write(result);
			out.close();
			// ServletOutputStream out = this.getOutputStream();
			// out.write(xr.getXMLBytes());
			// out.close();
		} catch (IOException e) {
			LOGGER.error(e);
		}

	}
}
