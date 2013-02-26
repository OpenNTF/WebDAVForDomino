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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ibm.xsp.webdav.WebDavManager;
import com.ibm.xsp.webdav.resource.DAVResourceInternal;

import biz.taoconsulting.dominodav.exceptions.DAVNotFoundException;
import biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation;
import biz.taoconsulting.dominodav.interfaces.IDAVRepository;
import biz.taoconsulting.dominodav.interfaces.IDAVResource;
import biz.taoconsulting.dominodav.repository.AbstractRepositoryImplementation;
import biz.taoconsulting.dominodav.resource.DAVAbstractResource;

/**
 * Pseudo repository that only returns the content of a Desktop.ini file
 * regardless of the request
 * 
 * @author Stephan H. Wissel
 * 
 */
public class WebDAVInternalRepository extends AbstractRepositoryImplementation
		implements IDAVRepository {

	/*
	 * Now determine the Physical path on the server, here we need to take the
	 * platform delimiters into account. On Windows it is \ on Linux / on Mac
	 * (?) : We get the value from File object We need to give special
	 * consideration to the \ since it is Java's regex escape character So if \
	 * is the delimiter we need to write \\\\ all others are OK
	 */
	public static String SEPARATOR_REGEX = (File.separator.equals("\\")) ? "\\\\"
			: File.separator;

	/**
	 * Logger for log4J
	 */
	private static final Log LOGGER = LogFactory
			.getLog(WebDAVInternalRepository.class);

	/**
	 * The variables from desktop.ini
	 */
	public static final String ICON_FILE = "Folder.ico";
	public static final String LOGO_FILE = "logo.jpg";
	public static final String WIDE_LOGO = "widelogo.jpg";
	public static final String ICON_UP = "actn022.gif";
	/**
	 * The Stylesheets
	 */
	public static final String PROPFIND_XSLT = "propfind.xslt";
	public static final String REPOSITORY_XSLT = "repositories.xslt";
	public static final String FILES_FILE = "files.gif";
	public static final String FILES_ICON = "Files.ico";
	public static final String FOLDER_FILE = "folder.gif";
	/**
	 * The installer
	 */
	public static final String INSTALLER_FILE = "WebDocOpenSetup.exe";
	public static final String INSTALLER_SOURCE = "WebDocOpen_src.zip";

	/**
	 * We keep a list of permitted files
	 */
	private ArrayList<String> permittedInternalFiles = null;

	/**
	 * Default constructor, needed for reflection access
	 */
	public WebDAVInternalRepository() {
		LOGGER.debug("Desktop.ini Repository created");
		this.setInternalAddress(DAVRepositoryMETA.INTERNAL_REPOSITORY_NAME);
		this.setPublicHref(WebDavManager.getManager(null).getServletPath()
				+ "/");
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#createNewCollection(java.lang.String)
	 */
	public DAVAbstractResource createNewCollection(String uri) {
		// No collections here
		return null;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#createNewResource(java.lang.String,
	 *      java.lang.String)
	 */
	public DAVAbstractResource createNewResource(String uri) {
		// We don't create new resources here
		return null;
	}

	/**
	 * Creates a pseudo resource with a Desktop.ini file as return
	 * 
	 * @return
	 */
	private DAVAbstractResource getDesktopIniResource(String whatResource) {
		if (!this.isPermittedFile(whatResource)) {
			return null;
		}
		DAVAbstractResource result = new DAVResourceInternal(whatResource, this);
		return result;
	}

	/**
	 * Match external to internal name
	 */
	public String getInternalAddressFromExternalUrl(String externalURLraw,
			String callee) {
		String externalURL = null;
		if (externalURLraw != null) {
			try {
				externalURL = URLDecoder.decode(externalURLraw, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				LOGGER.error(e);
				externalURL = externalURLraw; // We take it unencoded then
			}
		}

		// The repository address to "translate from external to internal
		// address
		IDAVAddressInformation repAdr = (IDAVAddressInformation) this;

		// Get the part that is not repository
		String relativeUrl = this.getRelativeURL(repAdr, externalURL);

		// Special case: if the repository is empty we actually need the
		// desktop.ini
		if (relativeUrl.equals("")) {
			relativeUrl = "/";
		}

		if (!this.isPermittedFile(relativeUrl)) {
			return null;
		}

		LOGGER.debug("External (" + callee + "): " + externalURL);
		LOGGER.debug("Internal (" + callee + "): " + relativeUrl);

		return relativeUrl;

	}

	@Override
	public String getName() {
		return DAVRepositoryMETA.INTERNAL_REPOSITORY_NAME;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.repository.AbstractRepositoryImplementation#getResource(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public DAVAbstractResource getResource(String requestURI)
			throws DAVNotFoundException {
		String internalName = this.getInternalAddressFromExternalUrl(
				requestURI, "InternalRepository");
		if (internalName == null) {
			return null;
		}
		return this.getDesktopIniResource(internalName);
	}

	public DAVAbstractResource getInternalResource(String internalName)
			throws DAVNotFoundException {
		return this.getDesktopIniResource(internalName);
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.repository.AbstractRepositoryImplementation#getResource(java.lang.String,
	 *      java.lang.String, boolean)
	 */
	@Override
	public DAVAbstractResource getResource(String requestURI,
			boolean isChildResource) throws DAVNotFoundException {

		return this.getResource(requestURI);
	}

	@Override
	public boolean isSupportedMethod(String method) {
		return true;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#moveResource(java.lang.String,
	 *      java.lang.String)
	 */
	public int moveResource(String from, String to) {
		// We don't move stuff here
		return 404;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.repository.AbstractRepositoryImplementation#setInternalAddress(java.lang.String)
	 */
	@Override
	public boolean setInternalAddress(String location) {
		// We don't need to set anything for this
		return true;
	}

	/**
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#writeResource(biz.taoconsulting.dominodav.resource.DAVAbstractResource)
	 */
	@Override
	public void writeResource(IDAVResource resc) {
		LOGGER.debug("writeResource on Desktop.ini called");
	}

	/**
	 * Is it one of the files we do accept for internal retrieval?
	 * 
	 * @param fileName
	 * @return
	 */
	private boolean isPermittedFile(String fileName) {
		boolean result = false;

		if (this.permittedInternalFiles == null) {
			this.loadPermittedFiles();
		}

		result = this.permittedInternalFiles.contains(fileName);

		return result;
	}

	private void loadPermittedFiles() {
		this.permittedInternalFiles = new ArrayList<String>();
		this.permittedInternalFiles.add(ICON_FILE);
		this.permittedInternalFiles.add(ICON_UP);
		this.permittedInternalFiles.add(LOGO_FILE);
		this.permittedInternalFiles.add(WIDE_LOGO);
		this.permittedInternalFiles.add(PROPFIND_XSLT);
		this.permittedInternalFiles.add(REPOSITORY_XSLT);
		this.permittedInternalFiles.add(FILES_FILE);
		this.permittedInternalFiles.add(FOLDER_FILE);
		this.permittedInternalFiles.add(FILES_ICON);
		this.permittedInternalFiles.add(INSTALLER_FILE);
		this.permittedInternalFiles.add(INSTALLER_SOURCE);
		this.permittedInternalFiles.add("/");
		this.permittedInternalFiles.add(DAVRepositoryMETA.FOLDER_INFOFILE);
	}

}
