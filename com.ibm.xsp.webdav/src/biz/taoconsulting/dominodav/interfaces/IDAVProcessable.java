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

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import biz.taoconsulting.dominodav.LockManager;

/**
 * @author Bastian Buch (TAO Consulting)
 * 
 */
public interface IDAVProcessable {

	/**
	 * The string if no method had been set
	 */
	public final String NO_STATUS_SET = "none";

	/**
	 * Did the method succeed in execution, to handle errors
	 * 
	 * @return
	 */
	boolean didMethodSucceed();

	/**
	 * Used to retrieve MIME Type for file resources
	 * 
	 * @return ServletContext
	 */
	ServletContext getContext();

	/**
	 * The error message the methods wants to return to the user
	 * 
	 * @return the message
	 */
	String getErrorMessage();

	/**
	 * Shows the status the method has assigned to the response "none" if no
	 * assignment happened
	 * 
	 * @return
	 */
	String getLastHttpStatus();

	/**
	 * @param req
	 *            HttpServletRequest
	 * @param resp
	 *            HttpServletResponse
	 * @param repository
	 *            DAVRepository
	 * @param lockManager
	 *            LockManager -- for File Locking
	 * @throws IOException
	 *             inherited from standard process interface
	 */
	void process(HttpServletRequest req, HttpServletResponse resp,
			IDAVRepository repository, LockManager lockManager)
			throws IOException;

	/**
	 * Needed for retrieving Mime types
	 * 
	 * @param con
	 *            ServletContext
	 */
	void setContext(ServletContext con);

	/**
	 * The lockManager Singleton
	 * 
	 * @param lockManager
	 */
	void setLockManager(LockManager lockManager);

	/**
	 * Did the method use the HTTPServletResponse.OutputStream;
	 * 
	 * @return
	 */
	boolean streamUsed();
}
