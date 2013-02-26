/** ========================================================================= *
 * Copyright (C) 2011, 2012 IBM Corporation                                   *
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
package com.ibm.xsp.webdav.repository;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import biz.taoconsulting.dominodav.exceptions.DAVNotFoundException;
import biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation;
import biz.taoconsulting.dominodav.interfaces.IDAVRepository;
import biz.taoconsulting.dominodav.interfaces.IDAVResource; //import biz.taoconsulting.dominodav.resource.DAVAbstractResource;

import com.ibm.xsp.webdav.resource.DAVResourceDominoAttachments;

/**
 * 
 * Repository to access Attachments in documents in a given Domino view
 * attachments
 * 
 * @author Stephan H. Wissel
 * 
 */
public class DAVRepositoryDominoAttachments extends DAVRepositoryDomino
		implements IDAVRepository {

	/**
	 * Logger for Errors
	 */
	private static final Log LOGGER = LogFactory
			.getLog(DAVRepositoryDominoAttachments.class);

	/**
	 * Default constructor for use with reflection
	 * 
	 */
	public DAVRepositoryDominoAttachments() {
		LOGGER.debug("Empty Domino Attachment Repository created");

	}

	/**
	 * @see biz.taoconsulting.dominodav.repository.AbstractStreamBasedRepository#getResource(java.lang.String)
	 */
	public IDAVResource getResource(String requestURI)
			throws DAVNotFoundException {

		// Intercepting requests for desktop.ini to please Windows7 explorer

		if (requestURI.endsWith(DAVRepositoryMETA.FOLDER_INFOFILE)) {
			return this.getDesktopIni();
		}

		DAVResourceDominoAttachments result = new DAVResourceDominoAttachments(
				this, requestURI);

		// if (result == null) {
		// throw new DAVNotFoundException();
		// }

		if ("NotesDocument".equals(result.getResourceType())) {
			// A Notes document is always a collection in the
			// AttachmentRepository!
			result.setCollection(true);
		}

		return (IDAVResource) result;
	}

	/**
	 * @see biz.taoconsulting.dominodav.repository.AbstractStreamBasedRepository#getResource(java.lang.String,
	 *      boolean)
	 */
	public IDAVResource getResource(String requestURI, boolean b)
			throws DAVNotFoundException {

		// Intercepting requests for desktop.ini to please Windows7 explorer
		if (requestURI.endsWith(DAVRepositoryMETA.FOLDER_INFOFILE)) {
			return this.getDesktopIni();
		}

		DAVResourceDominoAttachments result = new DAVResourceDominoAttachments(
				this, requestURI, b);

		if ("NotesDocument".equals(result.getResourceType())) {
			// A Notes document is always a collection in the
			// AttachmentRepository!
			result.setCollection(true);
		}
		return (IDAVResource) result;
	}

	/**
	 * Shortcut to a valid desktop.ini file
	 * 
	 * @return
	 * @throws DAVNotFoundException
	 */
	private IDAVResource getDesktopIni() throws DAVNotFoundException {
		WebDAVInternalRepository internal = new WebDAVInternalRepository();
		return internal.getInternalResource(DAVRepositoryMETA.FOLDER_INFOFILE);
	}

	/**
	 * @see biz.taoconsulting.dominodav.repository.AbstractStreamBasedRepository#writeResource(biz.taoconsulting.dominodav.resource.DAVAbstractResource)
	 */
	public void writeResource(IDAVResource resc) {
		// TODO Implement

	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#createNewResource(java.lang.String)
	 */
	public IDAVResource createNewResource(String requestURI) {
		// TODO Implement
		return null;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#createNewCollection(java.lang.String)
	 */
	public IDAVResource createNewCollection(String requestURI) {
		// TODO implement
		return null;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#moveResource(java.lang.String,
	 *      java.lang.String)
	 */
	public int moveResource(String from, String to) {
		// TODO Implement
		return 0;
	}

	public String getInternalAddressFromExternalUrl(String externalURLraw,
			String callee) {
		// The repository address to "translate from external to internal
		// address

		String externalURL = null;
		if (externalURLraw != null) {
			try {
				externalURL = URLDecoder.decode(externalURLraw, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				LOGGER.error(e);
				externalURL = externalURLraw; // We take it unencoded then
			}
		}

		IDAVAddressInformation repAdr = (IDAVAddressInformation) this;

		// Get the part that is not repository
		String relativeUrl = this.getRelativeURL(repAdr, externalURL);

		// Now form the internal URL out of it --- we
		// don't have the UNID so we need to improvise a bit

		String finalUrl;
		// TODO: Check the internal address
		if (relativeUrl.equals("")) {
			finalUrl = this.getInternalAddress();

			// Now we need to improvise a litte. DAV will try to load the
			// attachment directly
			// from the document URL without the $File inbetween, so if we have
			// a slash but no $File we build it inside
			// Special case: if it ends with / -- needs to be handled by the
			// last case
		} else if ((relativeUrl.indexOf("$File") < 0)
				&& (relativeUrl.indexOf("/") > 0)
				&& !(relativeUrl.endsWith("/"))) {

			String[] pieces = relativeUrl.split("/");
			finalUrl = this.getInternalAddress() + "/" + pieces[0] + "/$File/"
					+ pieces[1];

		} else {
			finalUrl = this.getInternalAddress() + "/" + relativeUrl;
		}

		LOGGER.debug("External (" + callee + "): " + externalURL);
		LOGGER.debug("Internal (" + callee + "): " + finalUrl);

		return finalUrl;
	}

	public String getFilter() {
		return "";
	}

}
