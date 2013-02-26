/* ========================================================================== *
 * Copyright (C) 2006, 2007 TAO Consulting Pte <http://www.taoconsulting.sg/> *
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import biz.taoconsulting.dominodav.interfaces.IDAVRepository;

import com.ibm.xsp.webdav.DAVCredentials;

/**
 * @author Stephan H. Wissel
 * 
 *         The DAVRepositoryListing provides all information that is needed to
 *         access a repository. It also contains a method to initialize a
 *         repository object
 * 
 */
public class DAVRepositoryListing {
	/**
	 * Name of the Repository = primary URL part after servlet name
	 */
	private String repositoryName;

	/**
	 * primary URL part including servlet name, but no server name
	 */
	private String repositoryURI;

	/**
	 * Class to instantiate the new Repository
	 */
	private String repositoryClass;

	/**
	 * Repository root, specific to the repository implementation
	 */
	private String repositoryRoot;

	/**
	 * Supported file extensions
	 */
	private String allowedExtensions = "";

	/**
	 * File extensions not accepted
	 */
	private String restrictedExtensions = "";

	/**
	 * Should only allowed or all (but restricted) extensions be allowed?
	 */
	private boolean useOnlyAllowedExtensions = false;

	/**
	 * List of supported methods
	 */
	private HashSet<String> supportedMethods;

	/**
	 * List of additional parameters if any
	 */
	private HashMap<String, String> additionalParameters;

	/**
	 * Temporary directory for this repository
	 */
	private String tempDir;

	/**
	 * Logger for Log4J
	 */
	private static final Log LOGGER = LogFactory
			.getLog(DAVRepositoryListing.class);

	/**
	 * Constructor for new Repository Listing
	 * 
	 * @param name
	 *            Name of the repository
	 * @param classname
	 *            Name of the repository classe
	 * @param root
	 *            Root URN for repository Location
	 * @param supportedMethods
	 *            What HTTP methods are supported by this repository
	 */
	public DAVRepositoryListing(String name, String classname, String root,
			@SuppressWarnings("rawtypes") Vector supportedMethods) {
		this.setupListing(name, classname, root, supportedMethods, null, null);
	}

	/**
	 * Constructor for new Repository Listing
	 * 
	 * @param name
	 *            Name of the repository
	 * @param classname
	 *            Name of the repository classe
	 * @param root
	 *            Root URN for repository Location
	 * @param supportedMethods
	 *            What HTTP methods are supported by this repository
	 * @param tempDir
	 *            The temporary directory
	 */
	public DAVRepositoryListing(String name, String classname, String root,
			@SuppressWarnings("rawtypes") Vector supportedMethods,
			String tempDir) {
		this.setupListing(name, classname, root, supportedMethods, tempDir,
				null);
	}

	/**
	 * Constructor for new Repository Listing
	 * 
	 * @param name
	 *            Name of the repository
	 * @param classname
	 *            Name of the repository classe
	 * @param root
	 *            Root URN for repository Location
	 * @param supportedMethods
	 *            What HTTP methods are supported by this repository
	 * @param tempDir
	 *            The temporary directory
	 */
	public DAVRepositoryListing(String name, String classname, String root,
			@SuppressWarnings("rawtypes") Vector supportedMethods,
			String tempDir,
			@SuppressWarnings("rawtypes") Vector additionalParameter) {
		this.setupListing(name, classname, root, supportedMethods, tempDir,
				additionalParameter);
	}

	/**
	 * 
	 * @param name
	 *            Name of the repository
	 * @param classname
	 *            Name of the repository classe
	 * @param root
	 *            Root URN for repository Location
	 * @param supportedMethods
	 *            What HTTP methods are supported by this repository
	 * @param tempDir
	 *            The temporary directory
	 */
	private void setupListing(String name, String classname, String root,
			@SuppressWarnings("rawtypes") Vector supportedMethods,
			String tempDir,
			@SuppressWarnings("rawtypes") Vector additionalParameter) {
		this.repositoryClass = classname;
		this.repositoryName = name;
		this.repositoryRoot = root;
		this.tempDir = tempDir;

		// Copy the Vector of methods into a the HashSet
		if (supportedMethods != null) {
			if (this.supportedMethods == null) {
				this.supportedMethods = new HashSet<String>();
			}
			for (Object m : supportedMethods) {
				String ms = m.toString().trim();
				if (!ms.equals("")) {
					this.supportedMethods.add(ms);
				}
			}
		}

		// Now add the parameters
		if (additionalParameter != null) {
			for (Object m : additionalParameter) {
				String ms = m.toString().trim();
				if (!ms.equals("")) {
					this.addAdditionalParameter(ms);
				}
			}
		}

		LOGGER.debug("Created DAVRepositoryListing: " + classname + ":" + name);

	}

	/**
	 * Adds a parameter. Takes the part before the = as name and after as value
	 * 
	 * @param ms
	 */
	private void addAdditionalParameter(String ms) {

		if (ms == null || ms.trim().equals("")) {
			return; // No empty stuff
		}

		if (this.additionalParameters == null) {
			this.additionalParameters = new HashMap<String, String>();
		}

		int delimiterPos = ms.indexOf("=");

		if (delimiterPos < 0) {
			this.additionalParameters.put(ms, ms);
		} else {
			// FIXME: Does this actually work?
			this.additionalParameters.put(ms.substring(0, delimiterPos).trim(),
					ms.substring(delimiterPos + 1).trim());
		}
	}

	/**
	 * 
	 * @return className to Create the Repository
	 */
	public String getRepositoryClass() {
		return repositoryClass;
	}

	/**
	 * 
	 * @param repositoryClass
	 *            className to Create the Repository
	 */
	public void setRepositoryClass(String repositoryClass) {
		this.repositoryClass = repositoryClass;
	}

	/**
	 * 
	 * @return Name of the Repository = used in servlet URL
	 */
	public String getRepositoryName() {
		return repositoryName;
	}

	/**
	 * 
	 * @param repositoryName
	 *            Name of the Repository = used in servlet URL
	 */
	public void setRepositoryName(String repositoryName) {
		this.repositoryName = repositoryName;
	}

	/**
	 * 
	 * @return Absolute location of Repository in file system or RDBMS or Domino
	 */
	public String getRepositoryRoot() {
		return repositoryRoot;
	}

	/**
	 * 
	 * @param repositoryRoot
	 *            Absolute location of Repository in file system or RDBMS or
	 *            Domino
	 */
	public void setRepositoryRoot(String repositoryRoot) {
		this.repositoryRoot = repositoryRoot;
	}

	/**
	 * 
	 * @param cred
	 *            Credentials with username/password and LTPA if possible
	 * @return DAVRepository -- the initialized repository
	 */
	public IDAVRepository getRepository(DAVCredentials cred) {
		IDAVRepository repository = this.getRepository();
		if (repository != null) {
			repository.setCredentials(cred);
		}

		return repository;
	}

	/**
	 * @return DAVRepository
	 */
	@SuppressWarnings("unchecked")
	public IDAVRepository getRepository() {
		IDAVRepository repository = null;
		if (this.repositoryClass != null) {
			LOGGER.debug("Creating log for " + this.repositoryClass);
			try {
				// Create the class with reflection
				Class<IDAVRepository> factoryClass = (Class<IDAVRepository>) Class
						.forName(this.repositoryClass);
				LOGGER.debug(factoryClass.getName());
				repository = (IDAVRepository) factoryClass.newInstance();
				if (repository != null) {
					// In setRepositoryListing all other values are updated
					repository.setRepositoryListing(this);
				}
			} catch (Exception e) {
				LOGGER.error(e);
			}
		}

		return repository;
	}

	/**
	 * 
	 * @return String the file extensions supported
	 */
	public String getAllowedExtensions() {
		return this.allowedExtensions;
	}

	/**
	 * 
	 * @param extensions
	 *            the file extensions supported
	 */
	public void setAllowedExtensions(String extensions) {
		this.allowedExtensions = " " + extensions.toLowerCase() + " ";
		figureOutExtensionMode();
	}

	/**
	 * If allowed extensions are set, activate use only allowed extensions
	 * 
	 */
	private void figureOutExtensionMode() {
		this.useOnlyAllowedExtensions = (this.allowedExtensions.trim().length() > 0);
	}

	/**
	 * 
	 * @return List of restricted/forbidden file extensions
	 */
	public String getRestrictedExtensions() {
		return this.restrictedExtensions;
	}

	/**
	 * 
	 * @param restrictedExtensions
	 *            List of restricted/forbidden file extensions
	 */
	public void setRestrictedExtensions(String restrictedExtensions) {
		this.restrictedExtensions = " " + restrictedExtensions.toLowerCase()
				+ " ";
		figureOutExtensionMode();
	}

	/**
	 * 
	 * @param extension
	 *            The extension to check
	 * @return boolean - is it allowed
	 */
	public boolean isAllowedExtension(String extension) {
		boolean b = false;
		if (this.useOnlyAllowedExtensions) {
			b = (this.allowedExtensions.indexOf(" " + extension.toLowerCase()
					+ " ") > -1);
		} else {
			b = (this.restrictedExtensions.indexOf(" "
					+ extension.toLowerCase() + " ") < 0);
		}
		return b;
	}

	/**
	 * @return Returns the supportedMethods.
	 */
	public HashSet<String> getSupportedMethods() {
		return this.supportedMethods;
	}

	/**
	 * @param supportedMethods
	 *            The supportedMethods to set.
	 */
	public void setSupportedMethods(HashSet<String> supportedMethods) {
		this.supportedMethods = supportedMethods;
	}

	/**
	 * @return Returns the repositoryURI.
	 */
	public String getURI() {
		return this.repositoryURI;
	}

	/**
	 * @param repositoryURI
	 *            The repositoryURI to set.
	 */
	public void setURI(String repositoryURI) {
		this.repositoryURI = repositoryURI;
	}

	/**
	 * @return the tempDir
	 */
	public String getTempDir() {
		return tempDir;
	}

	/**
	 * @param tempDir
	 *            the tempDir to set
	 */
	public void setTempDir(String tempDir) {
		this.tempDir = tempDir;
	}

	public HashMap<String, String> getAdditionalParameters() {
		return this.additionalParameters;
	}

}
