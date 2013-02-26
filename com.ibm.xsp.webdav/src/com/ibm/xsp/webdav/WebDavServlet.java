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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ibm.xsp.webdav.methods.WebdavMethodFactory;

import com.ibm.xsp.webdav.DAVCredentials;
import biz.taoconsulting.dominodav.LockManager;
import biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation;
import biz.taoconsulting.dominodav.interfaces.IDAVProcessable;
import biz.taoconsulting.dominodav.interfaces.IDAVRepository;
import biz.taoconsulting.dominodav.methods.Unimplemented;

import com.ibm.xsp.webdav.repository.DAVRepositoryMETA;

/**
 * webDAV files servlet
 * 
 * @author Stephan H. Wissel
 */
public class WebDavServlet extends HttpServlet {

	private static final long serialVersionUID = 4L;

	private WebDavManager manager;

	private String servletPath;

	/**
	 * The logger object for event logging
	 */
	private static final Log LOGGER = LogFactory.getLog(WebDavServlet.class);

	/**
	 * @param servletPath
	 *            = The calling path of the servlet
	 * @return Returns the lockManager - wrapper around webDavManager for
	 *         backwards compatibility
	 */
	public LockManager getLockManager() {
		return this.getManager().getLockManager();
	}

	/**
	 * Gets the webDavManager
	 * 
	 * @return The webDAVManager
	 */
	private WebDavManager getManager() {
		if (this.manager == null) {
			this.manager = WebDavManager.getManager(servletPath);
		}
		return this.manager;
	}

	/**
	 * Retrieves a method instance if one is configured and the current
	 * repository does support it
	 * 
	 * @param req
	 *            servlet request
	 * @param resp
	 *            servlet response
	 * @param repository
	 *            the repository to retrieve
	 * @return the method instance
	 */
	private IDAVProcessable getMethod(HttpServletRequest req,
			HttpServletResponse resp, IDAVRepository repository) {

		// If we don't have a repository, we don't need a method
		if (repository == null) {
			return null;
		}

		// The final method instance
		IDAVProcessable meth = null;

		// Extract the Method name
		String curMethod = req.getMethod();
		String repName = ((IDAVAddressInformation) repository).getName();
		// LOGGER.info("Try to load HTTP Service: " + curMethod + " for " +
		// repName);

		if (repository.isSupportedMethod(curMethod)) {
			// Get the class and then the object
			String classForMethod = this.getManager().getClassForMethod(
					curMethod);
			if (classForMethod != null) {
				meth = WebdavMethodFactory.newInstance(curMethod,
						this.getManager());
				if (meth != null) {
					// Store the context for mime retrieval
					meth.setContext(this.getServletContext());
				}
			}
		}
		LOGGER.debug((meth == null) ? (curMethod + " could not be loaded for " + repName)
				: (curMethod + " sucessfully loaded for " + repName));

		return meth;
	}

	/**
	 * @param req
	 *            The HTTP Request
	 * @param repositoryName
	 *            the repository to load
	 * @return
	 */
	private IDAVRepository getRepository(HttpServletRequest req,
			String curPathFromServlet, String servletPath) {

		// The session where the Repository might be stored
		IDAVRepository result = null;
		DAVRepositoryMETA meta = null;
		String curRepositoryName = null;

		if (curPathFromServlet == null || curPathFromServlet.equals("/")) {
			curRepositoryName = "/"; // We make sure we have a legitimate value
		} else {
			// Find the name of the repository. First in the chain
			// [1] = second part since the path starts with / so [0] = ""
			// [1] = our value
			curRepositoryName = curPathFromServlet.split("/")[1];
		}

		// The HTTP Session to cache repositories
		@SuppressWarnings("unused")
		HttpSession hs = req.getSession();

		meta = this.getManager().getRepositoryMeta();

		if (curRepositoryName.equals("/")) {
			result = meta;
		} else {

			// TODO: Is that a good idea to save the session,
			// hs.get/setAttribute commented out for now
			HashMap<String, IDAVRepository> sessionRepositories = null;
			// Object rlObject = null;

			// Object rlObject = hs.getAttribute("repositoryList");

			// if (rlObject != null) {
			// sessionRepositories = (HashMap<String, IDAVRepository>) rlObject;
			// } else {
			sessionRepositories = new HashMap<String, IDAVRepository>();
			// hs.setAttribute("repositoryList", sessionRepositories);
			// }

			if (sessionRepositories.containsKey(curRepositoryName)) {
				result = sessionRepositories.get(curRepositoryName);
			} else {
				result = meta.loadRepository(curRepositoryName);
				// sessionRepositories.put(curRepositoryName, result);
			}

		}
		return result;

	}

	/**
	 * Resets the Manager, so it gets reloaded
	 */
	private void reset() {
		if (this.manager == null || this.manager.isAllowReset()) {
			this.manager = WebDavManager.getManager(this.servletPath, true);
		}
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) {

		boolean canContinue = true; // Optimistic
		String errorMessage = null;
		IDAVAddressInformation repoAddress = null;
		this.writeDefaultHeaderInfo(req, resp);

		// We need to extract the repository name from the requestURL
		// If that is empty we list the available repositories as directories
		String curPath = req.getPathInfo();
		// LOGGER.info("Curr path="+curPath+"; method="+req.getMethod());
		if (curPath != null) {
			try {
				curPath = URLDecoder.decode(curPath, "UTF-8");
			} catch (UnsupportedEncodingException e1) {
				LOGGER.error(e1);
				curPath = req.getPathInfo(); // We take it unencoded then
			}
		}
		IDAVRepository repository = this.getRepository(req, curPath,
				servletPath);
		// LOGGER.info("CurrentPath="+curPath+";  servletPath="+servletPath+"");
		IDAVProcessable meth = this.getMethod(req, resp, repository);
		if (meth == null) {
			// LOGGER.info("Method is null");
		} else {
			// LOGGER.info("Method is " +meth.getClass());
		}

		// Now we could have everything, we check if we can move ahead

		// if ((repository == null) ||(curPath == null) || curPath.equals("/")){
		if ((repository == null)) {
			// LOGGER.info("Not found");
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			canContinue = false;
			errorMessage = "<HTML><HEAD><TITLE>Unable to Process Request</TITLE></HEAD><BODY><P>Http Status Code: 404</P><P>Reason: Unable to process request, resource not found</P></BODY></HTML>";
			resp.setContentLength(errorMessage.length());
			resp.setContentType("text/html");
			try {
				PrintWriter out = resp.getWriter();
				out.write(errorMessage);
				out.close();
				return;
			} catch (IOException e) {
				LOGGER.error(e);
			}

		} else {
			// LOGGER.info("repository found "+repository.getClass());
			repoAddress = (IDAVAddressInformation) repository;
		}

		// Without a method there's no point to continue
		if (canContinue && meth == null) {
			// LOGGER.info("Can continue and method is null");
			resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
			canContinue = false;
			errorMessage = "<h1>Repository "
					+ repoAddress.getName()
					+ "</h1><h2>Sorry, but this repository doesn't support <span style=\"color : red; font-weight : bold;\">[HTTP "
					+ req.getMethod() + "]</span></h2>";
		}

		// Now update credentials if we have them
		if (repository != null) {
			this.updateCredentials(req, repository);
		}

		if (canContinue) {
			try {
				// LOGGER.info("Can continue....");
				// We check if we can/have to reset the manager with all
				// repositories
				String reset = req.getParameter("reset");
				if (reset != null) {
					this.reset();
				}

				// Finally execute the method

				meth.process(req, resp, repository, this.getLockManager());

				if (meth.getLastHttpStatus().equals(
						IDAVProcessable.NO_STATUS_SET)
						&& meth.didMethodSucceed()) {
					resp.setStatus(HttpServletResponse.SC_OK); // Make sure we
																// have a status
				} else if (!meth.didMethodSucceed()) {
					canContinue = false;
					errorMessage = meth.getErrorMessage();
					return;
				}
			} catch (IOException e) {
				resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				errorMessage = "<h1>Error executing "
						+ meth.getClass().getName() + ": " + e.getMessage()
						+ "</h1>";
				canContinue = false;
				LOGGER.error(e);
			}
		}

		// We might have hit an error just above
		if (!canContinue) {
			// Write out the error
			Unimplemented nothingToDo = new Unimplemented();
			nothingToDo.setUseStream(meth.streamUsed());
			nothingToDo.setErrorMessage(errorMessage);
			nothingToDo.setErrNum(meth.getLastHttpStatus());
			try {
				nothingToDo.process(req, resp, null, null);
			} catch (IOException e) {
				resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				LOGGER.error(e);
			}
		}
	}

	/**
	 * Updates the repository with credentials, so the backend system can use
	 * them (FileSystem / Domino / JDBC)
	 * 
	 * @param req
	 * @param repository
	 */
	private void updateCredentials(HttpServletRequest req,
			IDAVRepository repository) {

		// Update the repository with the credentials, so we can access
		// the backend properly, we retrieve the credentials from the
		// session
		HttpSession hs = req.getSession();

		DAVCredentials cred;
		try {
			cred = (DAVCredentials) hs.getAttribute("credentials");
		} catch (Exception e) {
			// Probably a typecast
			LOGGER.trace("Get credentials from session failed", e);
			cred = null;
		}
		if (cred == null) {
			cred = new DAVCredentials();
		}

		cred.updateCredentials(req);
		repository.setCredentials(cred);

		// Save to the session
		hs.setAttribute("credentials", cred);

	}

	private void writeDefaultHeaderInfo(HttpServletRequest req,
			HttpServletResponse resp) {

		this.servletPath = req.getServletPath();

		// LOGGER.info("req.getPathInfo():" + req.getPathInfo());
		// LOGGER.info("req.getRequestURI():" + req.getRequestURI());
		// LOGGER.info("req.getRequestURL():" + req.getRequestURL());
		// LOGGER.info("req.getServletPath():" + this.servletPath);
	}

}
