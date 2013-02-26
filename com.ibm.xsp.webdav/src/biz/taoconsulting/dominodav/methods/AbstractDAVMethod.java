/*
 * ========================================================================== *
 * Copyright (C) 2006, 2007 TAO Consulting Pte <http://www.taoconsulting.sg/> *
 * based on work of * Copyright (C) 2004-2005 Pier Fumagalli
 * <http://www.betaversion.org/~pier/> * All rights reserved. *
 * ========================================================================== *
 * * Licensed under the Apache License, Version 2.0 (the "License"). You may *
 * not use this file except in compliance with the License. You may obtain a *
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>. * *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT *
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the *
 * License for the specific language governing permissions and limitations *
 * under the License. * *
 * ==========================================================================
 */
package biz.taoconsulting.dominodav.methods;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Stack;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import biz.taoconsulting.dominodav.LockManager;
import biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation;
import biz.taoconsulting.dominodav.interfaces.IDAVProcessable;
import biz.taoconsulting.dominodav.interfaces.IDAVRepository;
import biz.taoconsulting.dominodav.interfaces.IDAVResource;

/**
 * @author Bastian Buch (TAO Consulting), Stephan H. Wissel
 */
public abstract class AbstractDAVMethod implements IDAVProcessable {

	/**
	 * The logger object for event logging
	 */
	private static final Log LOGGER = LogFactory
			.getLog(AbstractDAVMethod.class);

	/**
	 *
	 */
	private LockManager lockManager;

	/**
	 *
	 */
	private IDAVRepository repository;

	/**
	 *
	 */
	private HttpServletRequest req;

	/**
	 *
	 */
	private HttpServletResponse resp;

	/**
	 * The servlet context, only used for the MIME Type
	 */
	private ServletContext context = null;

	/**
	 * What was the last httpStatus set?
	 */
	private Stack<String> lastHttpStatus = new Stack<String>();

	/**
	 *
	 */
	private HashMap<String, String> headerValues;

	/**
	 * Parameters we pass from one to the other methos
	 */
	private HashMap<String, String> headerAttributes;

	/**
	 * Has the HTTPServletResponse.OutputStream been used
	 */
	private boolean streamUsedFlag = false;

	/**
	 * This the DAV Method succeed or throw an error
	 */
	private boolean methodSuccess = true; // Innocent until proven guilty

	/**
	 * The last error message to be retrieved by the servlet
	 */
	private String lastErrorMessage = null;

	/**
	 * The original resource
	 */
	protected IDAVResource resource;

	/**
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVProcessable#process(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse,
	 *      biz.taoconsulting.dominodav.interfaces.IDAVRepository,
	 *      biz.taoconsulting.dominodav.LockManager)
	 */
	public void process(HttpServletRequest req, HttpServletResponse resp,
			IDAVRepository repository, LockManager lockmanager)
			throws IOException {
		this.repository = repository;
		this.req = req;
		this.resp = resp;
		this.lockManager = lockmanager;
		// extracts values from the requestheader
		this.interpretRequestHeader();
		try {
			this.writeInitialHeader();
		} catch (Exception e) {
			LOGGER.error("WriteInitialHeader failed", e);
		}
		try {
			// LOGGER.info("Start action for method "+this.getClass());
			this.action();
		} catch (Exception e) {
			LOGGER.error("Executing action failed:" + e.getMessage(), e);
			// Status auf 500 setzne..
			this.resp.setStatus(500);
			PrintWriter pw = this.getOutputWriter();
			pw.write("<h3>Error:" + e.getMessage() + "</h3>");
		}
	}

	/**
	 *
	 */
	private void interpretRequestHeader() {
		if (this.headerValues == null) {
			this.headerValues = new HashMap<String, String>();
		}
		if (this.headerAttributes == null) {
			this.headerAttributes = new HashMap<String, String>();
		}
		@SuppressWarnings("rawtypes")
		Enumeration enumHeaders = this.req.getHeaderNames();
		@SuppressWarnings("rawtypes")
		Enumeration enumAttribute = this.req.getAttributeNames();

		// extract all headers as key/value pairs
		if (enumHeaders != null) {
			while (enumHeaders.hasMoreElements()) {
				String key = (String) (enumHeaders.nextElement());
				String value = (String) this.req.getHeader(key);
				this.headerValues.put(key, value);
				// LOGGER.info("Header [" + key + "]:" + value);
			}
		}

		try {
			// extract additional standard header parameters
			String contextpath = this.req.getContextPath();
			String pathtranslated = this.req.getPathTranslated();
			String contenttype = this.req.getContentType();
			String characterencoding = this.req.getCharacterEncoding();
			String querystring = this.req.getQueryString();
			String uri = this.req.getRequestURI();

			if (contextpath != null) {
				this.headerValues.put("context-path", contextpath);
			}

			if (pathtranslated != null) {
				this.headerValues.put("path-translated", pathtranslated);
			}

			if (contenttype != null) {
				this.headerValues.put("content-type", contenttype);
			}

			if (characterencoding != null) {
				this.headerValues.put("character-encoding", characterencoding);
			}

			if (querystring != null) {
				this.headerValues.put("query-string", querystring);
			}

			if (uri != null) {
				this.headerValues.put("uri", uri);
			}

			// We record the Path-Info (includes the repository) and the
			// resource location which is the path-info minus the repository
			// name
			String pathInfo = this.req.getPathInfo();
			if (pathInfo == null) {
				LOGGER.debug("No Pathinfo found, defaulting to / for repository "
						+ this.getRepositoryName());

				pathInfo = "/";
			} else {
				// We translate the pathinfo from URL Encoded back into the
				// normal form
				try {
					pathInfo = URLDecoder.decode(pathInfo, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					LOGGER.error("Decoding pathInfo failed with UTF-8", e);
					pathInfo = this.req.getPathInfo(); // Reset to the old value
				}
			}
			LOGGER.debug("Path-info: " + pathInfo);
			this.headerValues.put("path-info", pathInfo);

			/*
			 * Now the resource-path, this is a bit more tricky Example: the
			 * servlet is "dav", the repository "rep" Both URLs must yield the
			 * same result: "/dav/rep" and "/dav/rep/"
			 */
			String resourcePath = this.req.getPathInfo();
			String curName = this.getRepositoryName();

			if (resourcePath == null || resourcePath.equals("")) {
				resourcePath = "/";
			} else {
				try {
					resourcePath = URLDecoder.decode(
							resourcePath.replaceFirst("/" + curName, ""),
							"UTF-8");
				} catch (UnsupportedEncodingException e) {
					LOGGER.error("Decoding resourcePath failed with UTF-8", e);
					// Reset to the old value
					resourcePath = this.req.getPathInfo().replaceFirst(
							"/" + curName, "");
				}
			}
			this.headerValues.put("resource-path", resourcePath);
			LOGGER.debug("resource-path: " + resourcePath);

			// List the attributes
			if (enumAttribute != null) {
				while (enumAttribute.hasMoreElements()) {
					String key = (String) (enumAttribute.nextElement());
					String value = (String) this.req.getAttribute(key);
					this.headerAttributes.put(key, value);
					// LOGGER.info("Attribute [" + key + "]:" + value);
				}
			}
		} catch (Error e) {
			LOGGER.error("Interpret request header failed:", e);
		}
	}

	/**
	 * The interprete request header might get called without a repository
	 * 
	 * @return
	 */
	private String getRepositoryName() {
		if (this.repository == null) {
			return "NullRepository";
		}
		return ((IDAVAddressInformation) this.repository).getName();
	}

	/**
	 * @throws Exception
	 *             Thrown if action cannot run
	 */
	protected abstract void action() throws Exception;

	/**
	 *
	 */
	protected abstract void writeInitialHeader();

	/**
	 * @return ...
	 */
	protected HashMap<String, String> getHeaderAttributes() {
		return this.headerAttributes;
	}

	/**
	 * @param headerAttributes
	 *            ...
	 */
	protected void setHeaderAttributes(HashMap<String, String> headerAttributes) {
		this.headerAttributes = headerAttributes;
	}

	/**
	 * @return ...
	 */
	protected HashMap<String, String> getHeaderValues() {
		return this.headerValues;
	}

	/**
	 * @param headerValues
	 *            ...
	 */
	protected void setHeaderValues(HashMap<String, String> headerValues) {
		this.headerValues = headerValues;
	}

	/**
	 * @return the writer
	 */
	protected PrintWriter getOutputWriter() {
		try {
			return this.getResp().getWriter();
		} catch (IOException exc) {
			LOGGER.error("getOutput failed", exc);
			return null;
		}
	}

	/**
	 * @return the stream
	 */
	protected ServletOutputStream getOutputStream() {
		try {
			this.streamUsedFlag = true;
			return this.getResp().getOutputStream();
		} catch (IOException exc) {
			LOGGER.error("getOutput failed", exc);
			return null;
		}
	}

	/**
	 * @return ...
	 */
	protected IDAVRepository getRepository() {
		return this.repository;
	}

	/**
	 * @param repository
	 *            ...
	 */
	protected void setRepository(IDAVRepository repository) {
		this.repository = repository;
	}

	/**
	 * @return ...
	 */
	protected HttpServletRequest getReq() {
		return this.req;
	}

	/**
	 * @param req
	 *            ...
	 */
	protected void setReq(HttpServletRequest req) {
		this.req = req;
	}

	/**
	 * @return ...
	 */
	protected HttpServletResponse getResp() {
		return this.resp;
	}

	/**
	 * @param resp
	 *            ...
	 */
	protected void setResp(HttpServletResponse resp) {
		this.resp = resp;
	}

	/**
	 * @return Reference to the LockManager class
	 */
	public LockManager getLockManager() {
		return this.lockManager;
	}

	/**
	 * @param lockManager
	 *            Instance of the lock Manager
	 */
	public void setLockManager(LockManager lockManager) {
		this.lockManager = lockManager;
	}

	/**
	 * @return ServletContext context : used to retrieve the mime-type
	 */
	// ToDo: Remove dependency to ServletContext
	public ServletContext getContext() {
		return this.context;
	}

	/**
	 * @param con
	 *            ServletContext
	 */
	public void setContext(ServletContext con) {
		this.context = con;
	}

	/**
	 * Last HTTP Status set in the method
	 */
	public String getLastHttpStatus() {
		if (this.lastHttpStatus.isEmpty()) {
			return IDAVProcessable.NO_STATUS_SET;
		}
		return this.lastHttpStatus.peek();
	}

	protected void setHTTPStatus(int newStatusInt) {

		String newStatus = String.valueOf(newStatusInt);
		if (!this.lastHttpStatus.isEmpty()) {
			String oldStatus = this.lastHttpStatus.peek();
			if (!oldStatus.equals(newStatus)) {
				LOGGER.error("HTTP Status change from: " + oldStatus + " to "
						+ newStatus);
			}
		}

		this.resp.setStatus(newStatusInt);
		this.lastHttpStatus.push(newStatus);
	}

	/**
	 * Did the execution of this message succeed
	 */
	public boolean didMethodSucceed() {
		return this.methodSuccess;
	}

	/**
	 * The last error that had occured
	 */
	public String getErrorMessage() {
		return this.lastErrorMessage;
	}

	/**
	 * If we have an error message we know it didn't work
	 * 
	 * @param theMessage
	 * @param ErrorStatus
	 *            - Http Error Code
	 */
	protected void setErrorMessage(String theMessage, int ErrorStatus) {
		if (theMessage != null) {
			this.methodSuccess = false;
			this.lastErrorMessage = theMessage;
			this.setHTTPStatus(ErrorStatus);
		}
	}

	/**
	 * Has the Outputstream been used - prohibits the use of the PrintWriter
	 * then
	 */
	public boolean streamUsed() {
		return this.streamUsedFlag;
	}

	/**
	 * Set the usageflag for the stream!
	 */
	protected void streamHasBeenUsed() {
		this.streamUsedFlag = true;
	}
}
