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
package biz.taoconsulting.dominodav.interfaces;

import java.util.HashSet;
import java.util.Set;

import com.ibm.xsp.webdav.DAVCredentials;
import biz.taoconsulting.dominodav.exceptions.DAVNotFoundException;
import biz.taoconsulting.dominodav.repository.DAVRepositoryListing;

/**
 * @author Stephan H. Wissel
 * 
 *         Interface that defines all repository methods a lightweight
 *         repository needs to support.
 * 
 * 
 */
public interface IDAVRepository {

	/**
	 * <p>
	 * A {@link String} of all acceptable characters in an URI.
	 * </p>
	 */
	String ACCEPTABLE = "ABCDEFGHIJLKMNOPQRSTUVWXYZ" + // ALPHA
			// (UPPER)
			"abcdefghijklmnopqrstuvwxyz" + // ALPHA (LOWER)
			"0123456789" + // DIGIT
			"_-!.~'()*" + // UNRESERVED
			",;:$&+=" + // PUNCT
			"?/[]@"; // RESERVED

	/**
	 *
	 */
	int RESOURCE_TYPE_ALL = 2;

	/**
	 *
	 */
	int RESOURCE_TYPE_COLLECTION = 1;

	/**
	 *
	 */
	int RESOURCE_TYPE_FILE = 0;

	/**
	 * @param listener
	 *            adds a listener
	 */
	void addListener(IDAVListener listener);

	/**
	 * @param uri
	 *            The location as seen in the browser
	 * @return the collection
	 */
	IDAVResource createNewCollection(String uri);

	/**
	 * @param externalAddress
	 *            The location as seen in the browser
	 * @return the resource
	 */
	IDAVResource createNewResource(String externalAddress);

	/**
	 * 
	 * @return Space separated String with method names
	 */
	String getAvailableMethods();

	/**
	 * @return The RepositoryListing object
	 */
	DAVRepositoryListing getRepositoryListing();

	/**
	 * @param requestURL
	 *            - The path in the URL minus protocol/server to request this
	 *            resource
	 * @return Resourceobject
	 * @throws DAVNotFoundException
	 *             If Resource could not be localized
	 */
	IDAVResource getResource(String requestURL) throws DAVNotFoundException;

	/**
	 * @param requestURL
	 *            - The path in the URL minus protocol/server to request this
	 *            resource
	 * @param withoutChildren
	 *            mark Resource as member (hide its children)
	 * @return Resourceobject
	 * @throws DAVNotFoundException
	 *             If Resource could not be localized
	 */
	IDAVResource getResource(String requestURL, boolean withoutChildren)
			throws DAVNotFoundException;

	/**
	 * 
	 * @return credential DAVcredentials
	 */
	DAVCredentials getCredentials();

	/**
	 * 
	 * @param method
	 *            Method to check
	 * @return boolean isSupportedMethod
	 */
	boolean isSupportedMethod(String method);

	/**
	 * 
	 * @param from
	 *            original location
	 * @param to
	 *            new location
	 * @return true/false
	 */
	int moveResource(String from, String to);

	/**
	 * @param listener
	 *            removes a listener
	 */
	void removeListener(IDAVListener listener);

	/**
	 * 
	 * @param availableMethods
	 *            method names in Uppercase
	 */
	void setAvailableMethods(HashSet<String> availableMethods);

	/**
	 * 
	 * @param availableMethods
	 *            String method names in Uppercase separated by comma or space
	 */
	void setAvailableMethods(String availableMethodsString);

	/**
	 * @param cred
	 *            The DAVCredential object
	 * @return True if Login was successful, otherwise False
	 */
	boolean setCredentials(DAVCredentials cred);

	/**
	 * @param repositoryListing
	 *            {...}
	 */
	void setRepositoryListing(DAVRepositoryListing repositoryListing);

	/**
	 * @param resc
	 *            The resource which shall be written
	 */
	void writeResource(IDAVResource resc);

	/**
	 * 
	 * @return The current listeners configured for the resource
	 */
	Set<IDAVListener> getListeners();

	/**
	 * 
	 * @param listeners
	 *            Listeners to get notified on changes
	 */
	void setListeners(Set<IDAVListener> listeners);

	// FIXME: Do we need the temp dir PER repository or is one global enough?

	/**
	 * @param tempDir
	 *            the directory for temporary file operations
	 */
	void setTempDir(String tempDir);

	/**
	 * @return The current temporary directory
	 */
	String getTempDir();

	/**
	 * Convert the external URL into the respective internal URI. Can be a file
	 * name or a database path or a call to an external system. The mapping is
	 * per repository
	 * 
	 * @param externalURL
	 * @return the internal URL
	 */
	String getInternalAddressFromExternalUrl(String externalURL, String callee);

}
