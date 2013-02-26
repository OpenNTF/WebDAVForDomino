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
import java.util.HashMap;
import java.util.Vector;

import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.View;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import biz.taoconsulting.dominodav.exceptions.DAVNotFoundException;
import biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation;
import biz.taoconsulting.dominodav.interfaces.IDAVListener;
import biz.taoconsulting.dominodav.interfaces.IDAVRepository;
import biz.taoconsulting.dominodav.interfaces.IDAVResource;
import biz.taoconsulting.dominodav.repository.AbstractRepositoryImplementation;
import biz.taoconsulting.dominodav.repository.DAVRepositoryListing;
import biz.taoconsulting.dominodav.resource.DAVAbstractResource;
import biz.taoconsulting.dominodav.resource.DAVResourceMETA;

import com.ibm.domino.osgi.core.context.ContextInfo;
import com.ibm.xsp.webdav.WebDavManager;
import com.ibm.xsp.webdav.domino.DominoProxy;

/**
 * DAVRepositorMETA holds all the information about the Repositories used in
 * DominoWebDAV It is used to display the toplevel Entries in DominoWEebDAV
 * Servlet There is only one Meta Repository used at a time, so it is
 * implemented as Singleton
 * 
 * @author Stephan H. Wissel
 * 
 */
public class DAVRepositoryMETA extends AbstractRepositoryImplementation
		implements IDAVRepository, IDAVAddressInformation {
	/**
	 * The internal Object to hold the singleTon
	 */
	private static DAVRepositoryMETA internalRepository;

	/**
	 * The logger object for event logging
	 */
	private static final Log LOGGER = LogFactory
			.getLog(DAVRepositoryMETA.class);

	/**
	 * Where is the repository located
	 */
	public static final String DEFAULT_REPOSITORY_LOCATION = "notes:///webdavconfig.nsf/repositories";

	/**
	 * 
	 */
	public static final String INTERNAL_REPOSITORY_NAME = "DAVinternal";

	/**
	 * 
	 */
	public static final String FOLDER_INFOFILE = "desktop.ini";

	/**
	 * 
	 */
	public static final String DEFAULT_PROPFIND_STYLE = "/webdav/"
			+ INTERNAL_REPOSITORY_NAME + "/propfind.xslt";

	/**
	 * 
	 */
	public static final String DEFAULT_ROOT_STYLE = "/webdav/"
			+ INTERNAL_REPOSITORY_NAME + "/repositories.xslt";

	/**
	 * What is the name of the view to read the repositories from
	 */
	private boolean autoReloadRepositoryList = true;

	/**
	 * Where do the current repositories come from
	 */
	private String repositoryConfigLocation = null;

	public String getRepositoryConfigLocation() {
		if (this.repositoryConfigLocation == null) {
			this.repositoryConfigLocation = DAVRepositoryMETA.DEFAULT_REPOSITORY_LOCATION;
		}
		return repositoryConfigLocation;
	}

	public void setRepositoryConfigNSFLocation(
			String repositoryConfigNSFLocation) {
		this.repositoryConfigLocation = repositoryConfigNSFLocation;
	}

	public boolean isAutoReloadRepositoryList() {
		return autoReloadRepositoryList;
	}

	public void setAutoReloadRepositoryList(
			boolean reloadRepositoryListwhenNotFound) {
		autoReloadRepositoryList = reloadRepositoryListwhenNotFound;
	}

	/**
	 * @param configNSFLocation
	 *            Path to the NSF containing the configuration
	 * @return DAVRepositoryMETA - the single instance
	 */
	public static DAVRepositoryMETA getRepository(String configNSFLocation) {

		// Initialize the lock Manager Singleton
		if (internalRepository == null) {
			synchronized (DAVRepositoryMETA.class) {
				internalRepository = new DAVRepositoryMETA();
				internalRepository
						.setRepositoryConfigNSFLocation(configNSFLocation);
				internalRepository.loadListOfRepositories(configNSFLocation);
			}
		}

		return internalRepository;

	}

	/**
	 * Where is the Domino html directory
	 */
	private String DominoHTMLDir = ContextInfo.getDataDirectory()
			+ "/domino/html/";
	String directoryField = null;

	/**
	 * List of Repositories, not initialized, read from Notes view
	 */
	private HashMap<String, DAVRepositoryListing> repositoryList = null;

	/**
	 * Default constructor is private to implement a singleton
	 */
	private DAVRepositoryMETA() {
		// The methods for the meta directory
		this.setAvailableMethods("GET,PROPFIND,HEAD,OPTIONS");
	}

	@Override
	public String getPublicHref() {
		return WebDavManager.getManager(null).getServletPath();
	}

	/**
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#addListener(biz.taoconsulting.dominodav.interfaces.IDAVListener)
	 */
	public void addListener(IDAVListener listener) {
		// Not implemented

	}

	/**
	 * Make sure our singleton can't be cloned
	 * 
	 * @return actually nothing -- throws an exception
	 * @throws CloneNotSupportedException
	 *             -- we don't Clown around
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
		// that'll teach 'em
	}

	/**
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#createNewCollection(java.lang.String)
	 */
	public DAVAbstractResource createNewCollection(String uri) {
		// Not implemented
		return null;
	}

	/**
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#createNewResource(java.lang.String)
	 */
	public DAVAbstractResource createNewResource(String uri) {
		// Not implemented
		return null;
	}

	/**
	 * @return Returns the repositoryList.
	 */
	public HashMap<String, DAVRepositoryListing> getRepositoryList() {
		return this.repositoryList;
	}

	/**
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#getRepositoryListing()
	 */
	public DAVRepositoryListing getRepositoryListing() {
		// Not implemented for Meta
		return null;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation#getInternalAddress()
	 */
	public String getInternalAddress() {
		// Default for META
		return "/";
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation#getName()
	 */
	public String getName() {
		return "METArepository";
	}

	/**
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#getResource(java.lang.String,
	 *      java.lang.String)
	 */
	public DAVAbstractResource getResource(String requestURI)
			throws DAVNotFoundException {
		DAVAbstractResource resc = (DAVAbstractResource) (new DAVResourceMETA(
				this, requestURI));
		return resc;
	}

	/**
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#getResource(java.lang.String,
	 *      java.lang.String, boolean)
	 */
	public DAVAbstractResource getResource(String requestURI,
			boolean isChildResource) throws DAVNotFoundException {
		DAVAbstractResource resc = (DAVAbstractResource) (new DAVResourceMETA(
				this, requestURI, isChildResource));
		return resc;
	}

	/**
	 * Loads List of Repositories
	 * 
	 * @author Stephan H. Wissel servletcontext ServletContext - The servletcontext to use for
	 *         the parameters Loads the list of available repositories
	 *         information: start directory, name and class into a global list.
	 *         Available to all users. It does not initialize the repository
	 *         instances
	 * @param repositorylistlocation
	 *            String - the name of the configuration file witht all
	 *            repositories Default is repositories.xml together with the
	 *            ServletContext the absolute Path is determined and loaded. the
	 *            fime MUST reside in the /WEB-INF/ directory
	 * 
	 */
	private void loadListOfRepositories(String repositorylistlocation) {

		LOGGER.debug("Start loading repository list");
		int repCount = 0;
		// Initialize the list of repositories
		repositoryList = new HashMap<String, DAVRepositoryListing>();
		// Read list of all repository locations from the repositories view
		// from the database stated in the parameter repositorylist

		LOGGER.debug("Load repository list from: " + repositorylistlocation);
		this.repositoryConfigLocation = repositorylistlocation; // Memorize for
																// autoload

		Document doc = null;
		View v = DominoProxy.getView(repositorylistlocation);

		if (v == null) {
			LOGGER.fatal("Repository List can't be loaded from:"
					+ repositorylistlocation);
			return;
		}

		try {

			int repMax = v.getEntryCount();
			LOGGER.debug("Number of repositories found:"
					+ new Integer(repMax).toString());

			doc = v.getFirstDocument();

			while (doc != null) {
				// TODO: Is that right to loop that way?
				this.addOneRepositoryToList(doc);
				repCount++;
				doc = v.getNextDocument(doc);
			}
		} catch (NotesException e) {
			LOGGER.error("Loading repositories didn't work:" + e.getMessage());
			LOGGER.error(e);
		} finally {
			// Cleanup after the act
			try {
				if (doc != null) {
					doc.recycle();
				}

				if (v != null) {
					v.recycle();
				}
			} catch (NotesException e) {

				LOGGER.error(e);
			}

			this.addInternalRepositoryToList();

		}

		LOGGER.info("Repository List loaded, Repositories found: "
				+ new Integer(repCount).toString());
	}

	/**
	 * Adds the repository DAVinternal to the repository list This is where the
	 * XSLT and the img resources for the display come from
	 */
	private void addInternalRepositoryToList() {
		String repositoryName = "DAVinternal";
		Vector<String> supportedMethods = new Vector<String>();
		supportedMethods.add("GET");
		supportedMethods.add("OPTIONS");
		supportedMethods.add("PROPFIND");
		String repositoryClass = "com.ibm.xsp.webdav.repository.WebDAVInternalRepository";
		String repositoryRoot = "properties::";
		String tempDir = this.getTempDir();
		Vector<String> additionalParameters = null;
		DAVRepositoryListing curRepository = new DAVRepositoryListing(
				repositoryName, repositoryClass, repositoryRoot,
				supportedMethods, tempDir, additionalParameters);
		curRepository.setURI(repositoryName);

		repositoryList.put(repositoryName, curRepository);
		LOGGER.debug("Repository listing initialized: " + repositoryName);

	}

	/**
	 * Extracts the repository from the view entry Col 0 : Name Col 1 : Methods
	 * Col 2 : class Col 3 : isRelative Col 4 : path Col 5 : temp file Col 6 :
	 * Parameters
	 * 
	 * @param doc
	 *            ViewEntry with the repository definition
	 * @throws NotesException
	 */
	private void addOneRepositoryToList(Document doc) {

		String repositoryName = null;
		Boolean isRelative = new Boolean(false);
		@SuppressWarnings("rawtypes")
		Vector supportedMethods = null;
		String repositoryClass = null;
		String repositoryRoot = null;
		String tempDir = null;
		@SuppressWarnings("rawtypes")
		Vector additionalParameters = null;
		try {
			repositoryName = doc.getItemValueString("Subject");
			supportedMethods = doc.getItemValue("supportedMethods");
			repositoryClass = doc.getItemValueString("webdavclass");
			if (doc.hasItem("Directoryname")) {
				this.directoryField = doc.getItemValueString("Directoryname");
			}
			if (doc.hasItem("relativePath")) {
				isRelative = new Boolean(doc.getItemValueString("relativePath"));
			}
			repositoryRoot = doc.getItemValueString("RepositoryRoot"); // Something
																		// that
																		// can
																		// be
																		// translated
																		// by
																		// the
																		// repository
																		// class
			if (doc.hasItem("TempDir")) {
				tempDir = doc.getItemValueString("TempDir");
			}
			if (doc.hasItem("AdditionalParameters")) {
				additionalParameters = doc.getItemValue("AdditionalParameters");
			}

		} catch (NotesException e) {
			LOGGER.error(e);
		}

		if (isRelative) {
			// Ensure that on Windows we have backslashes instead of forward
			// ones
			repositoryRoot = (this.DominoHTMLDir + repositoryRoot).replace(
					"/".toCharArray()[0], File.separatorChar);

		}

		DAVRepositoryListing curRepository = new DAVRepositoryListing(
				repositoryName, repositoryClass, repositoryRoot,
				supportedMethods, tempDir, additionalParameters);
		curRepository.setURI(repositoryName);

		repositoryList.put(repositoryName, curRepository);
		// LOGGER.info("Repository listing initialized: " + repositoryName);

	}

	/**
	 * @param repositoryName
	 *            The name of the repository to load
	 * @param reload
	 *            if possible and needed
	 * 
	 * @return the repository found
	 */
	private IDAVRepository loadRepository(String repositoryName,
			boolean reloadIfPossibleAndNeeded) {
		IDAVRepository repository = null;

		// Ensure our list of repositories is loaded properly
		if (this.repositoryList == null) {
			this.loadListOfRepositories(this.getRepositoryConfigLocation());
		}

		if (this.repositoryList.containsKey(repositoryName)) {
			// We know the repository, so we initialize it to have access
			LOGGER.debug("Found " + repositoryName + " in repositoryList");
			DAVRepositoryListing rl = this.repositoryList.get(repositoryName);

			repository = rl.getRepository();

			if (repository == null) {
				// Repository could not be loaded from listing
				LOGGER.error("Repository could not be loaded from listing: "
						+ repositoryName);
			}

		} else if (repositoryName.equalsIgnoreCase(INTERNAL_REPOSITORY_NAME)
				|| repositoryName.equalsIgnoreCase(FOLDER_INFOFILE)) {
			// We need pseudo repository for the Desktop.ini request file
			repository = new WebDAVInternalRepository();

		} else {
			// No repository could be loaded - it could be just added to the
			// config
			// so we try again after refreshing the config
			if (reloadIfPossibleAndNeeded && this.autoReloadRepositoryList) {
				// Load repository list
				this.loadListOfRepositories(this.repositoryConfigLocation);
				// recursive call ONCE (false means: no refresh of the
				// repository list)
				repository = this.loadRepository(repositoryName, false);

			} else {
				// ToDo: Should we throw an error here?
				LOGGER.error("Repository not found: " + repositoryName);
			}
		}

		return repository;
	}

	/**
	 * Public function to load a repository, calls internal method
	 * 
	 * @param repositoryName
	 *            The name of the repository to load
	 * @return the repository found
	 */
	public IDAVRepository loadRepository(String repositoryName) {
		return this.loadRepository(repositoryName, true);
	}

	/**
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#moveResource(java.lang.String,
	 *      java.lang.String)
	 */
	public int moveResource(String from, String to) {
		// Not implemented
		return 0;
	}

	/**
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#removeListener(biz.taoconsulting.dominodav.interfaces.IDAVListener)
	 */
	public void removeListener(IDAVListener listener) {
		// not implemented

	}

	/*
	 * Extension to the DAVMetaRepository: List of Repositories
	 */

	/**
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#setRepositoryListing(biz.taoconsulting.dominodav.repository.DAVRepositoryListing)
	 */
	public void setRepositoryListing(DAVRepositoryListing repositoryListing) {
		// not implemented

	}

	/**
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#writeResource(biz.taoconsulting.dominodav.resource.DAVAbstractResource)
	 */
	public void writeResource(IDAVResource resc) {

		// Not implemented

	}

	public String getInternalAddressFromExternalUrl(String externalURL,
			String callee) {
		return "/"; // The meta address is always the same
	}

}
