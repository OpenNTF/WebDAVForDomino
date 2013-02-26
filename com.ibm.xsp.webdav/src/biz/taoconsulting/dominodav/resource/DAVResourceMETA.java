/* ========================================================================== *
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
 * ========================================================================== */
package biz.taoconsulting.dominodav.resource;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import biz.taoconsulting.dominodav.exceptions.DAVNotFoundException;
import biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation;
import biz.taoconsulting.dominodav.interfaces.IDAVRepository;
import biz.taoconsulting.dominodav.interfaces.IDAVResource;
import biz.taoconsulting.dominodav.repository.DAVRepositoryListing;

import com.ibm.xsp.webdav.repository.DAVRepositoryMETA;

/**
 * DAVResourceMETA hold information about a DAVRepository rather than a
 * traditional resource. DAVRepositories appear as webDAV Collections a.k.a
 * directories
 * 
 * @author Stephan H. Wissel
 * 
 */
public class DAVResourceMETA extends DAVAbstractResource {

	/**
	 * Logger for Errors
	 */
	private static final Log LOGGER = LogFactory.getLog(DAVResourceMETA.class);

	/**
	 * @param rep
	 *            The containing Repository
	 * @param path
	 *            the path of the resource as seen from the URL below the
	 *            repository
	 * @throws DAVNotFoundException
	 *             -- if the repository is not there
	 */
	public DAVResourceMETA(IDAVRepository rep, String path)
			throws DAVNotFoundException {
		this.setMember(false);
		this.setup(rep, path);
		LOGGER.debug("Metaresource (non member mode) created: " + path);

	}

	/**
	 * @param rep
	 *            The repository
	 * @param path
	 *            the path of the resource as seen from the URL below the
	 *            repository
	 * @param isMember
	 *            - for directories: is it listed as part of the parent or by
	 *            itself?
	 * @throws DAVNotFoundException
	 *             -- resource might not be there
	 */
	public DAVResourceMETA(IDAVRepository rep, String path, boolean isMember)
			throws DAVNotFoundException {
		this.setMember(isMember);
		this.setup(rep, path);
		LOGGER.debug("Metaresource (" + (isMember ? "" : "Non ")
				+ "member mode) created: " + path);

	}

	/**
	 * @param rep
	 *            The DAVRepository
	 * @param path
	 *            The Path to the repository
	 */
	private void setup(IDAVRepository rep, String path) {
		this.setOwner(rep); // Store a link to the repository
		IDAVAddressInformation repAdr = (IDAVAddressInformation) rep;

		String repHRef = repAdr.getPublicHref();
		String resName = path.substring(repHRef.length());

		this.setPublicHref(repHRef
				+ ((resName.equals("")) ? "" : "/" + resName));
		this.setInternalAddress("/" + resName); // Meta is top level, so href
												// and uri are the same
		// This is top level, so the path can only contain a slash in front or
		// the end
		// By removing it we get a reliable name
		this.setName(resName);

		// search for members (children)
		if (!this.isMember()) {
			this.fetchChildren();
		}
		// flag / mark as collection
		this.setCollection(true);
	}

	/**
	 * List all Repositories
	 */
	private void fetchChildren() {
		DAVRepositoryMETA rep = (DAVRepositoryMETA) this.getOwner();
		this.setMembers(new Vector<IDAVResource>());
		HashMap<String, DAVRepositoryListing> allrep = rep.getRepositoryList();
		String repHref = rep.getPublicHref();

		for (String key : allrep.keySet()) {

			try {
				String curURI = repHref + key;
				if (!key.endsWith(DAVRepositoryMETA.INTERNAL_REPOSITORY_NAME)) {
					DAVAbstractResource res = rep.getResource(curURI, true);
					this.getMembers().add(res);
				}
			} catch (DAVNotFoundException exc) {
				LOGGER.error(exc);
			}
		}
	}

	/**
	 * @see biz.taoconsulting.dominodav.resource.DAVAbstractResource#getExtension()
	 */
	public String getExtension() {
		// Extensions are used to determine the mime-type in Windows, so we
		// don't return them
		return "";
	}

	/**
	 * @see biz.taoconsulting.dominodav.resource.DAVAbstractResource#getCreationDate()
	 */
	public Date getCreationDate() {
		// Not supported by Java
		return null;
	}

	/**
	 * @see biz.taoconsulting.dominodav.resource.DAVAbstractResource#getLastModified()
	 */
	public Date getLastModified() {
		// It is always today
		return new Date();
	}

	/**
	 * @see biz.taoconsulting.dominodav.resource.DAVAbstractResource#getStream()
	 */
	public InputStream getStream() {
		// Also we don't write back
		return null;
	}

	/**
	 * @see biz.taoconsulting.dominodav.resource.DAVAbstractResource#getContentLength()
	 */
	public Long getContentLength() {
		// Directories don't have a lenght
		return new Long(0);
	}

	/**
	 * @see biz.taoconsulting.dominodav.resource.DAVAbstractResource#getMimeType(javax.servlet.ServletContext)
	 */
	public String getMimeType(ServletContext context) {
		// TODO what is the correct mime-type for a directory
		return "";
	}

	/**
	 * @see biz.taoconsulting.dominodav.resource.DAVAbstractResource#getOutputStream()
	 */
	public OutputStream getOutputStream() {
		// There is no outputstream for Repositories
		return null;
	}

	/**
	 * @see biz.taoconsulting.dominodav.resource.DAVAbstractResource#delete()
	 */
	public boolean delete() {
		// Repositories can't be deleted either
		return false;
	}

	public void patchLastModified(Date dt) {
	}

	public void patchCreationDate(Date dt) {
	}
}
