/** ========================================================================= *
 * Copyright (C) 2011       IBM Corporation ( http://www.ibm.com/ )           *
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ibm.xsp.webdav.WebDavManager;

import com.ibm.xsp.webdav.DAVCredentials;
import biz.taoconsulting.dominodav.exceptions.DAVNotFoundException;
import biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation;
import biz.taoconsulting.dominodav.interfaces.IDAVListener;
import biz.taoconsulting.dominodav.interfaces.IDAVRepository;
import biz.taoconsulting.dominodav.interfaces.IDAVResource;
import biz.taoconsulting.dominodav.resource.DAVAbstractResource;

/**
 * @author Stephan H. Wissel
 * 
 */
public abstract class AbstractRepositoryImplementation implements
		IDAVAddressInformation, IDAVRepository {

	/**
	 * The logger object for event logging
	 */
	private static final Log LOGGER = LogFactory
			.getLog(AbstractRepositoryImplementation.class);

	/**
	 * <p>
	 * The {@link Set} of all configured {@link IDAVListener}s.
	 * </p>
	 */
	private Set<IDAVListener> listeners = new HashSet<IDAVListener>();

	/**
	 * The name of the repository
	 */
	private String name = null;

	/**
	 * The users credentials
	 */
	private DAVCredentials credentials;

	/**
	 *
	 */
	private DAVRepositoryListing repositoryListing;

	/**
	 *
	 */
	private String internalAddress = null;

	/**
	 * List of supported HTTP Methods loaded from web.xml
	 */
	private HashSet<String> supportedMethods;

	/**
	 * Path to the repository as seen from the browser
	 */
	private String publicHref;

	/**
	 * Location of the temporary directory for file operations
	 */
	private String tempDir;

	/**
	 * Public constructor without parameters so it can be created by reflection!
	 */
	public AbstractRepositoryImplementation() {
		// Empty method for reflection
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#addListener(biz.taoconsulting.dominodav.interfaces.IDAVListener)
	 */
	public void addListener(IDAVListener listener) {
		if (listener != null) {
			this.listeners.add(listener);
		}
	}

	/**
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#getAvailableMethods()
	 */
	public String getAvailableMethods() {
		StringBuffer out = new StringBuffer();
		LOGGER.debug("# of supported methods "
				+ new Integer(this.supportedMethods.size()).toString());

		for (String curM : this.supportedMethods) {
			out.append(curM);
			// Don't change this! OPTIONS needs a space!
			out.append(" ");
		}

		out.deleteCharAt(out.length() - 1); // Remove the last space
		return out.toString();
	}

	/**
	 * 
	 * @return User Credentials for the current user
	 */
	public DAVCredentials getCredentials() {
		return this.credentials;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation#getInternalAddress()
	 */
	public String getInternalAddress() {
		return this.internalAddress;
	}

	/**
	 * 
	 * @return Listeners for event changes
	 */
	public Set<IDAVListener> getListeners() {
		return this.listeners;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation#getName()
	 */
	public String getName() {
		if (this.name != null && this.name.equals("")) {
			return "/";
		}
		return this.name;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation#getPublicHref()
	 */
	public String getPublicHref() {
		return this.publicHref;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#getRepositoryListing()
	 */
	public DAVRepositoryListing getRepositoryListing() {
		return this.repositoryListing;
	}

	/**
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#getResource(java.lang.String,
	 *      java.lang.String)
	 */
	public abstract IDAVResource getResource(String requestURI)
			throws DAVNotFoundException;

	/**
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#getResource(java.lang.String,
	 *      java.lang.String, boolean)
	 */
	public abstract IDAVResource getResource(String requestURI, boolean b)
			throws DAVNotFoundException;

	/**
	 * @return Returns the tempDir.
	 */
	public String getTempDir() {
		if (this.tempDir == null || this.tempDir.equals("")) {
			this.tempDir = WebDavManager.getManager(null).getTempDir();
		}
		return this.tempDir;
	}

	/**
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#isSupportedMethod(java.lang.String)
	 */
	public boolean isSupportedMethod(String method) {
		// The Vector makes checking very fast
		return this.supportedMethods.contains(method);
	}

	/**
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#removeListener(biz.taoconsulting.dominodav.interfaces.IDAVListener)
	 */
	public void removeListener(IDAVListener listener) {
		if (listener != null) {

			this.listeners.remove(listener);
		}
	}

	/**
	 * Implement available methods
	 */
	public void setAvailableMethods(HashSet<String> availableMethods) {
		this.supportedMethods = availableMethods;
	}

	public void setAvailableMethods(String availableMethods) {

		this.supportedMethods = new HashSet<String>();
		String[] supportedMethodsTmp;

		// Intercepting missing parameter
		if (availableMethods == null || availableMethods.equals("")) {
			// Minimal list of supported methods
			availableMethods = "GET,PROPFIND,HEAD,OPTIONS";
			LOGGER.error("setAvailableMethods empty in Repository: "
					+ this.name);
		} else {
			LOGGER.debug("Available methods for " + this.getName() + ":"
					+ availableMethods);
		}

		// The methods might be split by space or comma
		String splitter = (availableMethods.indexOf(",") < 0) ? " " : ",";

		supportedMethodsTmp = availableMethods.split(splitter);

		LOGGER.debug("# of supported methods "
				+ new Integer(supportedMethodsTmp.length).toString());

		for (int i = 0; i < supportedMethodsTmp.length; i++) {
			String newMethod = supportedMethodsTmp[i].trim();
			if (!newMethod.equals("")) {
				this.supportedMethods.add(newMethod);
			}
		}
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#setCredentials(biz.taoconsulting.dominodav.DAVCredentials)
	 */
	public boolean setCredentials(DAVCredentials cred) {
		this.credentials = cred;
		return true;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation#setInternalAddress(java.lang.String)
	 */
	public boolean setInternalAddress(String location)
			throws DAVNotFoundException {
		this.internalAddress = location;
		return true;
	}

	/**
	 * 
	 * @param listeners
	 *            Listeners for event changes
	 * 
	 */
	public void setListeners(Set<IDAVListener> listeners) {
		this.listeners = listeners;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation#setName(java.lang.String)
	 */
	public void setName(String string) {
		this.name = string;

	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation#setPublicHref(java.lang.String)
	 */
	public void setPublicHref(String uri) {
		this.publicHref = uri;
	}

	/**
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#setRepositoryListing(biz.taoconsulting.dominodav.repository.DAVRepositoryListing)
	 */
	public void setRepositoryListing(DAVRepositoryListing repositoryListing) {

		WebDavManager manager = WebDavManager.getManager(null);

		this.repositoryListing = repositoryListing;
		this.setName(repositoryListing.getRepositoryName());
		this.setPublicHref(repositoryListing.getURI());
		this.setAvailableMethods(repositoryListing.getSupportedMethods());
		String tmpDir = repositoryListing.getTempDir();
		this.setTempDir((tmpDir == null) ? manager.getTempDir() : tmpDir);
		this.setPublicHref(manager.getServletPath() + "/" + this.getName());
		// Important: must come last since a new location is checked instantly!
		try {
			this.setInternalAddress(repositoryListing.getRepositoryRoot());
		} catch (DAVNotFoundException e) {
			LOGGER.error("Repository not found:", e);
		}

	}

	/**
	 * @param tempDir
	 *            The tempDir to set.
	 */
	public void setTempDir(String tempDir) {
		this.tempDir = tempDir;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#writeResource(biz.taoconsulting.dominodav.resource.DAVAbstractResource)
	 */
	public abstract void writeResource(IDAVResource resc);

	/**
	 * <p>
	 * Notify all configured {@link IDAVListener}s of an event.
	 * </p>
	 * 
	 * @param resource
	 *            sender
	 * @param event
	 *            Eventtype
	 */
	protected void notify(DAVAbstractResource resource, int event) {
		if (resource == null) {
			throw new NullPointerException("Null resource");
		}
		if (resource.getRepository() != this) {
			throw new IllegalArgumentException("Invalid resource");
		}

		Iterator<IDAVListener> iterator = this.listeners.iterator();
		while (iterator.hasNext()) {
			try {
				iterator.next().notify(resource, event);
			} catch (RuntimeException exception) {
				// Swallow any RuntimeException thrown by listeners.
				LOGGER.error(exception);
			}
		}
	}

	/**
	 * Returns the external part of an URL that is NOT the repository part - to
	 * determine the internal path
	 * 
	 * @param rep
	 *            the current repository address interface
	 * @param externalURL
	 *            the external URL (starting with repository)
	 * @return the relative url, can be an empty string
	 */
	protected String getRelativeURL(IDAVAddressInformation repAdr,
			String externalURL) {
		// Get the part that is not repository
		String result;

		String repPubUrl = repAdr.getPublicHref();
		if (!repPubUrl.endsWith("/")) {
			repPubUrl += "/";
		}
		if (externalURL.length() < repPubUrl.length() + 1) {
			// We are looking at the root of this repository
			result = "";
		} else {
			// somewhere deeper
			result = externalURL.substring(repPubUrl.length());
		}

		return result;
	}

}