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
package biz.taoconsulting.dominodav.repository;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import biz.taoconsulting.dominodav.exceptions.DAVNotFoundException;
import biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation;
import biz.taoconsulting.dominodav.interfaces.IDAVRepository;
import biz.taoconsulting.dominodav.interfaces.IDAVResource;
import biz.taoconsulting.dominodav.resource.DAVAbstractResource;
import biz.taoconsulting.dominodav.resource.DAVResourceFile;

/**
 * @author Stephan H. Wissel
 * 
 */
public class DAVRepositoryFiles extends AbstractRepositoryImplementation
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
			.getLog(DAVRepositoryFiles.class);

	/**
	 * <p>
	 * The {@link File} identifying the root of this repository.
	 * </p>
	 */
	private File root = null;

	/**
	 * Default constructor, needed for reflection access
	 */
	public DAVRepositoryFiles() {
		LOGGER.debug("Empty file repository created");
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#createNewCollection(java.lang.String)
	 */
	public DAVAbstractResource createNewCollection(String uri) {
		LOGGER.debug("createNewCollection");

		// String path = this.getPathFromUri(uri);

		// Get the file-path and a new file
		String fpath = this.getInternalAddressFromExternalUrl(uri,
				"createNewCollection");

		File f = new File(fpath);
		// This is Collection creation specific

		f.mkdir();
		try {
			return this.getResource(uri);
		} catch (DAVNotFoundException e) {
			return null;
		}

	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#createNewResource(java.lang.String,
	 *      java.lang.String)
	 */
	public DAVAbstractResource createNewResource(String uri) {
		LOGGER.debug("createNewResource on URI:" + uri);

		// Get the file-path and a new file
		String fpath = this.getInternalAddressFromExternalUrl(uri,
				"createNewResource");

		File f = new File(fpath);

		try {
			f.createNewFile();
		} catch (IOException ex) {
			LOGGER.error(ex);
			return null;
		}

		try {
			DAVAbstractResource res = this.getResource(uri);
			return res;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.repository.AbstractRepositoryImplementation#getResource(java.lang.String,
	 *      java.lang.String)
	 */
	public DAVAbstractResource getResource(String requestURI)
			throws DAVNotFoundException {
		LOGGER.debug("getResource");
		DAVAbstractResource resc = (DAVAbstractResource) (new DAVResourceFile(
				this, requestURI));
		return resc;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.repository.AbstractRepositoryImplementation#getResource(java.lang.String,
	 *      java.lang.String, boolean)
	 */
	public DAVAbstractResource getResource(String requestURI,
			boolean isChildResource) throws DAVNotFoundException {

		LOGGER.debug("getResource (File):" + requestURI);
		DAVAbstractResource resc = (DAVAbstractResource) (new DAVResourceFile(
				this, requestURI, isChildResource));
		return resc;
	}

	/**
	 * @return The repository root collection as a file object
	 */
	public File getRootAsFile() {
		LOGGER.debug("getRootAsFile");
		return this.root;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#moveResource(java.lang.String,
	 *      java.lang.String)
	 */
	public int moveResource(String from, String to) {
		LOGGER.debug("moveResource");
		// TODO Needs fixing
		try {
			DAVResourceFile res = (DAVResourceFile) this.getResource(from);
			File des = new File(this.getInternalAddress() + to);
			if (res.getFile().renameTo(des)) {
				return 201;
			} else {
				return 409;
			}
		} catch (DAVNotFoundException e) {
			return 404;
		}
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.repository.AbstractRepositoryImplementation#setInternalAddress(java.lang.String)
	 */
	public boolean setInternalAddress(String location)
			throws DAVNotFoundException {
		LOGGER.debug("setRepositoryLocation to:" + location);
		super.setInternalAddress(location);
		String rootLoction = this.getInternalAddress();
		this.root = new File(rootLoction);
		if (!this.root.isDirectory()) {
			throw new DAVNotFoundException("File root resouce not found:"
					+ rootLoction);
		}
		return true;
	}

	/**
	 * @param root
	 *            Root (File) of the repository
	 */
	public void setRoot(File root) {
		LOGGER.debug("setRoot");
		// ToDo: change other attributes as well...
		this.root = root;
	}

	/**
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#writeResource(biz.taoconsulting.dominodav.resource.DAVAbstractResource)
	 */
	public void writeResource(IDAVResource resc) {
		LOGGER.debug("writeResource");
		// TODO Auto-generated method stub

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

		// Get the file-path and a new file
		String fpath = repAdr.getInternalAddress() + File.separator
				+ relativeUrl.replaceAll("/", SEPARATOR_REGEX);

		LOGGER.debug("External (" + callee + "): " + externalURL);
		LOGGER.debug("Internal (" + callee + "): " + fpath);

		return fpath;
	}

}
