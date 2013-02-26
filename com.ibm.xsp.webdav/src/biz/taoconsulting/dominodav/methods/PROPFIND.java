/** ========================================================================= *
 * Copyright (C) 2006, 2007 TAO Consulting Pte <http://www.taoconsulting.sg/> *
 *           based on work of                                                 *
 * Copyright (C) 2004-2005 Pier Fumagalli <http://www.betaversion.org/~pier/> *
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

import java.io.IOException;
import java.io.PrintWriter;

//import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import biz.taoconsulting.dominodav.DAVProperties;
import biz.taoconsulting.dominodav.exceptions.DAVNotFoundException;
import biz.taoconsulting.dominodav.interfaces.IDAVRepository;
import biz.taoconsulting.dominodav.interfaces.IDAVResource;

import com.ibm.xsp.webdav.DavXMLResponsefactory;
import com.ibm.xsp.webdav.WebDavManager;
import com.ibm.xsp.webdav.interfaces.IDAVXMLResponse;
import com.ibm.xsp.webdav.repository.DAVRepositoryMETA;

/**
 * @author Bastian Buch (TAO Consulting) / NotesSensei
 * 
 */
public class PROPFIND extends AbstractDAVMethod {

	/**
	 * The logger object for event logging
	 */
	private static final Log LOGGER = LogFactory.getLog(PROPFIND.class);

	/**
	 * If true additional information is rendered back like stylesheets and
	 * comments
	 */
	private boolean redirectedFromGet = false;

	/**
	 * Flag to indicate to propfind to output additional information that would
	 * be useful to a browser and don't matter to a file client
	 */
	public void calledFromGET() {
		this.redirectedFromGet = true;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.methods.AbstractDAVMethod#action()
	 */
	@SuppressWarnings("deprecation")
	protected void action() throws Exception { // TODO: cleanup action method by
												// refactoring -- too messy
		boolean withoutChildren = true; // Resource only, no children
		IDAVRepository rep = this.getRepository();
		IDAVResource resource;
		HttpServletResponse resp = this.getResp();

		// Resource-Path is stripped by the repository name!
		String curPath = null;
		String curURI = null;

		try {
			curPath = (String) this.getHeaderValues().get("resource-path");
			if (curPath == null || curPath.equals("")) {
				curPath = "/";
			}
			// If Depth is missing it might throw an error, so we assume 0 then
			String depth = this.getHeaderValues().get("Depth");
			if (depth != null && depth.equals("0")) {
				withoutChildren = true;
			} else {
				withoutChildren = false;
			}
			// uri is the unique identifier on the host includes servlet and
			// repository but not server
			curURI = (String) this.getHeaderValues().get("uri");

		} catch (Exception e) {
			LOGGER.error(e);
			withoutChildren = true; // No recursive call to propfind in a error
									// condition
		}

		LOGGER.info("PROPFIND for path:[" + curPath + "] and URI:" + curURI);

		IDAVXMLResponse xr = DavXMLResponsefactory.getXMLResponse(null, false);
		if (this.redirectedFromGet) {
			// Determine the style
			String xsltStyle = null;
			if (rep instanceof DAVRepositoryMETA) {
				xsltStyle = WebDavManager.getManager(null)
						.getRootPropfindStyle();
			} else {
				xsltStyle = WebDavManager.getManager(null).getPropfindStyle();
			}
			xr = DavXMLResponsefactory.getXMLResponse(xsltStyle, true);

		} else {
			xr = DavXMLResponsefactory.getXMLResponse(null, true);
		}
		// LOGGER.info("xr OK");
		// xr = DavXMLResponsefactory.getXMLResponse(null, false);

		try {

			// Here we need to retrieve the path without the repository name!
			resource = rep.getResource(curURI, withoutChildren);

			if (resource == null) {
				// LOGGER.info("Resource is null");
				this.setErrorMessage("<h1>404 - Resource not found</h1>",
						HttpServletResponse.SC_NOT_FOUND);

				String result = this.getErrorMessage();
				resp.setContentLength(result.length());
				PrintWriter out = resp.getWriter();
				out.write(result);
				out.close();

			} else {

				if (!resource.isCollection()) {
					// LOGGER.info("Resource is not a collection");
					this.getResp().setHeader("ETag", resource.getETag());
				}
				// Modified by EC
				// LOGGER.info("Further.....");

				java.util.Date dt = new java.util.Date();
				this.getResp().setHeader("Last-Modified", dt.toGMTString());
				this.getResp().setHeader("Cache-Control", "no-cache");
				dt.setYear(70);
				this.getResp().setHeader("Expires", dt.toGMTString());
				this.getResp().setHeader("Server", "Microsoft-IIS/6.0");
				this.getResp().setHeader("Public-Extension",
						"http://schemas.microsoft.com/repl-2");
				this.getResp().setHeader("MicrosoftSharePointTeamServices",
						"12.0.0.6210");
				this.getResp().setHeader("Set-Cookie",
						"WSS_KeepSessionAuthenticated=80; path=" + curURI);

				// End Mody by EC
				xr.openTag("multistatus");
				resource.addToDavXMLResponse(xr);
				xr.auxTag("username", rep.getCredentials().getUserName());
				xr.addComment("Called method : PROPFIND");
				xr.closeDocument();
				// LOGGER.info("Close doc");
				// Ugly hack: Domino closes connections on status 207
				// and Win7 has a problem with it

				/*
				 * if (this.redirectedFromGet ||
				 * this.cameFromWindows7webDAVredir(this.getReq())) {
				 * this.setHTTPStatus(HttpServletResponse.SC_OK);
				 * resp.setStatus(HttpServletResponse.SC_OK); } else {
				 * this.setHTTPStatus(DAVProperties.STATUS_MULTIPART);
				 * resp.setStatus(DAVProperties.STATUS_MULTIPART,
				 * DAVProperties.STATUS_MULTIPART_STRING); }
				 */

				this.setHTTPStatus(DAVProperties.STATUS_MULTIPART);
				resp.setStatus(DAVProperties.STATUS_MULTIPART,
						DAVProperties.STATUS_MULTIPART_STRING);
				resp.setHeader("Connection", "keep-alive");

				resp.setContentType(DAVProperties.TYPE_XML);
				// resp.setContentType("text/xml");
				// resp.setHeader("content-encoding", "utf-8");
				// resp.setContentLength(xr.getXMLBytes().length);
				// ServletOutputStream out = this.getOutputStream();
				// out.write(xr.getXMLBytes());
				// out.close();
				String result = xr.toString();
				resp.setContentLength(result.length());
				PrintWriter out = resp.getWriter();
				out.write(result);
				out.close();
			}

		} catch (DAVNotFoundException exc) {
			// LOGGER.error(exc);
			this.setErrorMessage("<h1>404 - Resource not found</h1>",
					HttpServletResponse.SC_NOT_FOUND);
			String result = this.getErrorMessage();
			resp.setContentLength(result.length());
			PrintWriter out = resp.getWriter();
			out.write(result);
			out.close();
		} catch (IOException e) {
			// LOGGER.error(e);
			this.setErrorMessage(
					"<h1>500 - We screwed up</h1><h2>" + e.getMessage()
							+ "</h2>",
					HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

	}

	/**
	 * Hack around Windows 7 WebDAV redirector
	 * 
	 * @param req
	 * @return
	 */
	@SuppressWarnings("unused")
	private boolean cameFromWindows7webDAVredir(HttpServletRequest req) {
		String ua = req.getHeader("User-Agent");
		if (ua == null) {
			return false;
		}

		return ua.contains("Microsoft-WebDAV-MiniRedir");

	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.methods.AbstractDAVMethod#writeInitialHeader()
	 */
	protected void writeInitialHeader() {
		// Move to DavXMLResponse for XML
	}

}
