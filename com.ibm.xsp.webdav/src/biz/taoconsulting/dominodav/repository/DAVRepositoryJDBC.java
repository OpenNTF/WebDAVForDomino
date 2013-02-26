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
package biz.taoconsulting.dominodav.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import biz.taoconsulting.dominodav.exceptions.DAVNotFoundException;
import biz.taoconsulting.dominodav.interfaces.IDAVRepository;
import biz.taoconsulting.dominodav.interfaces.IDAVResource;
import biz.taoconsulting.dominodav.resource.DAVAbstractResource;
import biz.taoconsulting.dominodav.resource.DAVResourceJDBC;

/**
 * @author Stephan H. Wissel
 * 
 */
@Deprecated
public class DAVRepositoryJDBC extends AbstractRepositoryImplementation
		implements IDAVRepository {

	/**
	 * Logger for log4J
	 */
	private static final Log LOGGER = LogFactory
			.getLog(DAVRepositoryJDBC.class);

	/**
	 * Default constructor, needed for reflection access
	 */
	public DAVRepositoryJDBC() {
		LOGGER.debug("Empty JDBC repository created");
	}

	/**
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#createNewCollection(java.lang.String)
	 */
	public DAVAbstractResource createNewCollection(String uri) {
		return null;
		// TODO implement
	}

	/**
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#createNewResource(java.lang.String)
	 */
	public DAVAbstractResource createNewResource(String uri) {
		// TODO implement
		return null;
	}

	/**
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#getResource(java.lang.String)
	 */
	public DAVAbstractResource getResource(String requestURI)
			throws DAVNotFoundException {
		DAVAbstractResource resc = (DAVAbstractResource) (new DAVResourceJDBC(
				this, requestURI));
		return resc;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.repository.AbstractStreamBasedRepository#getResource(java.lang.String,
	 *      boolean)
	 */
	public DAVAbstractResource getResource(String requestURI,
			boolean isChildResource) throws DAVNotFoundException {
		DAVAbstractResource resc = (DAVAbstractResource) (new DAVResourceJDBC(
				this, requestURI, isChildResource));
		return resc;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#moveResource(java.lang.String,
	 *      java.lang.String)
	 */
	public int moveResource(String from, String to) {
		// TODO Implement
		return 500;
	}

	/**
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#writeResource(biz.taoconsulting.dominodav.resource.DAVAbstractResource)
	 */
	public void writeResource(IDAVResource resc) {
		// TODO Auto-generated method stub

	}

	public String getInternalAddressFromExternalUrl(String externalURL,
			String callee) {
		// TODO Auto-generated method stub
		return null;
	}

}
