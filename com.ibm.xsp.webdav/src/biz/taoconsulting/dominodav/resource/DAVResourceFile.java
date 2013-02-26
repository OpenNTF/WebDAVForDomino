/* ========================================================================== *
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
package biz.taoconsulting.dominodav.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Vector;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import biz.taoconsulting.dominodav.exceptions.DAVNotFoundException;
import biz.taoconsulting.dominodav.interfaces.IDAVRepository;
import biz.taoconsulting.dominodav.interfaces.IDAVResource;
import biz.taoconsulting.dominodav.repository.DAVRepositoryFiles;

/**
 * @author Bastian Buch (TAO Consulting)
 * 
 */
public class DAVResourceFile extends DAVAbstractResource {

	/**
	 * Logger for Errors
	 */
	private static final Log LOGGER = LogFactory.getLog(DAVResourceFile.class);

	/**
	 *
	 */
	private File file;

	/**
	 * @param rep
	 *            The containing Repository
	 * @param url
	 *            the access to the resource from the browser relative to the
	 *            repository
	 * @throws DAVNotFoundException
	 *             -- if the repository is not there
	 */
	public DAVResourceFile(IDAVRepository rep, String url)
			throws DAVNotFoundException {
		LOGGER.debug("DAVResourceFile from " + url);
		this.setMember(false);
		this.setup(rep, url);
	}

	/**
	 * @param rep
	 *            The repository
	 * @param url
	 *            the access to the resource from the browser relative to the
	 *            repository
	 * @param isMember
	 *            - for directories: is it listed as part of the parent or by
	 *            itself?
	 * @throws DAVNotFoundException
	 *             -- resource might not be there
	 */
	public DAVResourceFile(IDAVRepository rep, String url, boolean isMember)
			throws DAVNotFoundException {
		LOGGER.debug("DAVResourceFile from " + url);
		this.setMember(isMember);
		this.setup(rep, url);
	}

	/**
	 * @param rep
	 *            the containing repository
	 * @param url
	 *            the access to the resource from the browser relative to the
	 *            repository
	 * @throws DAVNotFoundException
	 *             -- we might ask for an non-existing resource
	 */
	private void setup(IDAVRepository rep, String url)
			throws DAVNotFoundException {

		// Store a link to the repository
		this.setOwner(rep);

		// The path can't be null and can't be empty. if it is empty we use "/"
		if (url == null || url.equals("")) {
			url = new String("/");
		}

		// Memorize the url requested
		this.setPublicHref(url);

		// Get the file-path and a new file
		String fpath = rep.getInternalAddressFromExternalUrl(url,
				"DAVREsourceFile-setup");

		File curFile = new File(fpath);

		// Memorize the File
		this.setFile(curFile);

		// Give the resource a name
		this.setName(curFile.getName());

		// Keep the address
		this.setInternalAddress(curFile.getAbsolutePath());

		if (file.isDirectory()) {
			LOGGER.debug(curFile.getAbsolutePath() + " is a directory");
			// flag / mark as collection
			this.setCollection(true);
			// search for members (children)
			if (!this.isMember()) {
				this.fetchChildren();
			}
		} else if (this.file.isFile()) {
			LOGGER.debug(curFile.getAbsolutePath() + " is a file");
			this.setCollection(false);
			// Check for ReadOnly property
			if (!file.canWrite()) {
				this.setReadOnly(true);
			}
			this.setLastModified(new Date(file.lastModified()));
		} else {
			LOGGER.debug(curFile.getAbsolutePath() + " does actually not exist");
			throw new DAVNotFoundException(curFile.getAbsolutePath()
					+ " does actually not exist");
		}
	}

	/**
	 *
	 */
	private void fetchChildren() {

		// We only fetchChildren for directories that exit
		if (((DAVRepositoryFiles) (this.getOwner())).getRootAsFile() == null) {
			return;
		}
		if (!this.isCollection()) {
			return;
		}

		// This is the start
		this.setMembers(new Vector<IDAVResource>());
		String names[] = this.file.list();
		String pName = this.getPublicHref();
		IDAVRepository rep = this.getOwner();

		for (int i = 0; i < names.length; i++) {

			// ToDo Needs fixing
			String curResourceName = pName + (pName.endsWith("/") ? "" : "/")
					+ names[i];

			IDAVResource res = null;

			try {
				res = rep.getResource(curResourceName, true);
			} catch (DAVNotFoundException rnfException) {
				LOGGER.error("Fetch Children failed", rnfException);
			}

			// We only add resources if they are allowed extensions
			if (res != null
					&& (res.isCollection() || rep.getRepositoryListing()
							.isAllowedExtension(res.getExtension()))) {
				this.getMembers().add(res);
			}

		}
	}

	/**
	 * @return The current file
	 */
	public File getFile() {
		return this.file;
	}

	/**
	 * @see biz.taoconsulting.dominodav.resource.DAVAbstractResource#getExtension()
	 */
	public String getExtension() {
		if (!this.isCollection()) {
			String extension;
			try {
				extension = this.file.getName().substring(
						this.file.getName().lastIndexOf('.') + 1);
			} catch (StringIndexOutOfBoundsException ex) {
				LOGGER.error("getExtionsion failed for " + this.file.getName(),
						ex);
				extension = "";
			}
			return extension;
		} else {
			return "";
		}
	}

	/**
	 * 
	 * @param file
	 *            File Object
	 */
	protected void setFile(File file) {
		this.file = file;
		// Update the last modified date and the
		// created date (kind of fake)
		Date modDate = new Date(file.lastModified());
		this.setCreationDate(modDate);
		this.setLastModified(modDate);
		// Update the content lenght
		long cl = file.length();
		this.setContentLength(new Long(cl));
	}

	/**
	 * @see biz.taoconsulting.dominodav.resource.DAVAbstractResource#getStream()
	 */
	public InputStream getStream() {
		try {
			InputStream stream = (InputStream) (new FileInputStream(this.file));
			return stream;
		} catch (FileNotFoundException exc) {
			LOGGER.error("getInputstream failed for " + this.file.getName(),
					exc);
			return null;
		}
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.resource.DAVAbstractResource#getMimeType(javax.servlet.ServletContext)
	 */
	public String getMimeType(ServletContext context) {
		// TODO: can we keep the mimetype from Domino
		String curName = this.file.getAbsolutePath();
		String curNameLower = curName.toLowerCase();
		String returnType = "application/octet-stream"; // Default value if
														// things go wrong
		try {
			// Hack to make sure xslt and xsl go as text/xml
			// To avoid funny errors on machines that don't know that file
			// type to serve
			if (curNameLower.endsWith(".xsl") || curNameLower.endsWith(".xslt")
					|| curNameLower.endsWith(".xml")) {
				returnType = "text/xml";
			} else {
				if (context != null) {
					returnType = context.getMimeType(curName);
				} else {
					returnType = "application/octet-stream";
				}
			}
			LOGGER.debug("File " + curName + " is of type \"" + returnType
					+ "\"");
		} catch (Exception e) {
			LOGGER.error("Mime Type retrieval failed", e);
		}

		return returnType;
	}

	/**
	 * @see biz.taoconsulting.dominodav.resource.DAVAbstractResource#getOutputStream()
	 */
	public OutputStream getOutputStream() {
		try {
			OutputStream out = (OutputStream) (new FileOutputStream(this.file));
			return out;
		} catch (Exception e) {
			LOGGER.error("Fetching of output stream failed", e);
			return null;
		}

	}

	/**
	 * Delete a file resource
	 * 
	 * @return true/false : did the deletion work?
	 */
	public boolean delete() {
		boolean success = true;
		try {
			success = this.file.delete();
		} catch (Exception e) {
			LOGGER.error("Deletion failed for " + this.file.getName(), e);
		}
		return success;
	}

	public void patchLastModified(Date dt) {
	}

	public void patchCreationDate(Date dt) {
	}
}
