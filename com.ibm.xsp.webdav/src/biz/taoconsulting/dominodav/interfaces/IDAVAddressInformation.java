/** ========================================================================= *
 * Copyright (C) 2006, 2007 TAO Consulting Pte <http://www.taoconsulting.sg/> *
 * Copyright (C) 2011       IBM Corporation ( http://www.ibm.com/ )           *
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

import biz.taoconsulting.dominodav.exceptions.DAVNotFoundException;

/**
 * The Interface defines common address properties for Repositories and
 * Resources like the public and the internal addresses
 * 
 * @author Stephan H. Wissel
 * 
 */
public interface IDAVAddressInformation {

	/**
	 * 
	 * @return href external URL of the resource
	 */
	public String getPublicHref();

	/**
	 * 
	 * @param href
	 *            URL of the resource
	 */
	public void setPublicHref(String href);

	/**
	 * 
	 * @return String Name of the resource - no path included
	 */
	public String getName();

	/**
	 * 
	 * @param name
	 *            name of the resource
	 */
	public void setName(String name);

	/**
	 * @return Returns the internal address, this can be an URL a Notes URI, a
	 *         file system path, an XQuery or a SQL
	 */
	public String getInternalAddress();

	/**
	 * @param location
	 *            Location identifier (e.g. path)
	 * @return True if successful (path is valid), otherwise False
	 * @throws DAVNotFoundException
	 *             If the path is valid but the Repository could not be found
	 */
	boolean setInternalAddress(String location) throws DAVNotFoundException;

}
