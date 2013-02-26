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

import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import biz.taoconsulting.dominodav.LockInfo;
import biz.taoconsulting.dominodav.LockManager;
import biz.taoconsulting.dominodav.exceptions.DAVNotFoundException;
import biz.taoconsulting.dominodav.interfaces.IDAVRepository;
import biz.taoconsulting.dominodav.interfaces.IDAVResource;

/**
 * Writes a new / edit an existing file. Usually a HEAD and a PROPFIND request
 * open the PUT request to make sure if the file exists and to tell the server
 * about file properties of the following stream.
 * 
 * @author Bastian Buch (TAO Consulting)
 * 
 */
public class PUT extends AbstractDAVMethod {
	/**
	 * The logger object for event logging
	 */
	private static final Log LOGGER = LogFactory.getLog(PUT.class);

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.methods.AbstractDAVMethod#action()
	 */
	public void action() throws Exception {
		IDAVRepository rep = this.getRepository();

		// uri is the unique identifier on the host includes servlet and
		// repository but not server
		String curURI = (String) this.getHeaderValues().get("uri");
		IDAVResource resource = null;
		InputStream instream = null;
		OutputStream out = null;
		boolean success = true;
		String relockToken = this.getRelockToken(this.getReq());
		LockManager lm = this.getLockManager();
		Long TimeOutValue = this.getTimeOutValue(this.getReq());
		// String lockrequestorName =
		// this.getOwnerFromXMLLockRequest(this.getReq());
		int status = HttpServletResponse.SC_OK; // We presume success
		LockInfo li = null;
		try {
			curURI = java.net.URLDecoder.decode(curURI, "UTF-8");
		} catch (Exception e) {
		}

		try {
			// LOGGER.info("getResource");
			resource = rep.getResource(curURI, true);

		} catch (DAVNotFoundException e) {
			// This exception isn't a problem since we just can create the new
			// URL
			// LOGGER.info("Exception not found resource");

		}
		if (resource == null) {
			// LOGGER.info("Resource Null start create new resource");
			resource = rep.createNewResource(curURI);
			// isNew=true;
		}
		if (resource == null) {
			// LOGGER.info("Error, resource is null");
			// Set the return error
			// Unprocessable Entity (see
			// http://www.webdav.org/specs/rfc2518.html#status.code.extensions.to.http11)
			this.setHTTPStatus(422);
		} else {
			if (relockToken != null) {
				li = lm.relock(resource, relockToken, TimeOutValue);
				if (li == null) {
					String eString = "Relock failed for " + relockToken;
					LOGGER.debug(eString);
					this.setErrorMessage(eString, 412); // Precondition failed
					status = 412;
				} else {
					LOGGER.debug("successful relock for " + relockToken
							+ ", new Token:" + li.getToken());
					status = HttpServletResponse.SC_OK;
				}
			}
			if (status >= 400) {
				this.setHTTPStatus(status);
				HttpServletResponse resp = this.getResp();
				resp.setStatus(status);
				return;
			}
			try {
				instream = this.getReq().getInputStream();
				out = resource.getOutputStream();
			} catch (Exception e) {
				LOGGER.error("Input/Output stream creation failed", e);
				success = false;
				this.setErrorMessage(
						"Input/Output stream creation failed in PUT for "
								+ curURI, 501);
			}
			if (success) {
				try {
					int read = 0;
					byte[] bytes = new byte[2 * 2048]; // TODO: are 2 KB blocks
														// the right size?
					while ((read = instream.read(bytes)) != -1) {
						// LOGGER.info("Read total"+ new
						// Integer(read).toString() +" bytes");
						out.write(bytes, 0, read);
						// LOGGER.info("Write  total"+ new
						// Integer(read).toString() +" bytes");
					}

				} catch (Exception ex) {
					LOGGER.error(ex);
				} finally {
					try {
						if (instream != null) {
							instream.close();
							// LOGGER.info("istream successfully closed");
						}
					} catch (Exception finalE) {
						// Not a fatal error
						LOGGER.error("Put stream closing failed", finalE);
					}

					try {
						if (out != null) {
							// LOGGER.info("out closed");
							out.flush();
							// LOGGER.info("Output stream flushed");
							out.close();
							// LOGGER.info("Output stream closed");
						}
					} catch (Exception outE) {
						// Success is false!
						LOGGER.error(
								"closing of output stream (and saving) failed",
								outE);
						this.setErrorMessage(
								"closing of output stream (and saving) failed for"
										+ curURI, 501);
						success = false;
					}
				}
			}
		}
		if (this.getReq().getContentLength() == 0) {
			this.setHTTPStatus(HttpServletResponse.SC_CREATED);
			HttpServletResponse resp = this.getResp();
			resp.setStatus(HttpServletResponse.SC_CREATED);
			return;
		}
		this.setHTTPStatus(HttpServletResponse.SC_OK);
		HttpServletResponse resp = this.getResp();
		resp.setStatus(HttpServletResponse.SC_OK);
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
