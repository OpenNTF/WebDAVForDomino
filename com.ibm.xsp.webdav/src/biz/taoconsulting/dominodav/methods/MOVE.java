/* ========================================================================== *
 * Copyright (C) 2006, 2007 TAO Consulting Pte <http://www.taoconsulting.sg/> *
 *           based on work of                                                 *
 *           C) 2004-2005 Pier Fumagalli <http://www.betaversion.org/~pier/>  *
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
package biz.taoconsulting.dominodav.methods;

import biz.taoconsulting.dominodav.LockManager;
import biz.taoconsulting.dominodav.exceptions.DAVNotFoundException;
import biz.taoconsulting.dominodav.interfaces.IDAVRepository;
import biz.taoconsulting.dominodav.interfaces.IDAVResource;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Move / Rename resources
 * 
 * @author Bastian Buch (TAO Consulting)
 * 
 */
public class MOVE extends AbstractDAVMethod {
	private static final Log LOGGER = LogFactory.getLog(PUT.class);

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.methods.AbstractDAVMethod#action()
	 */
	protected void action() {
		// check if locked - locked files / foldes should not be moved... (???)
		// TODO fix the path mess

		IDAVRepository rep = this.getRepository();
		// Resource-Path is stripped by the repository name!
		// String curPath = (String)
		// this.getHeaderValues().get("resource-path");
		// uri is the unique identifier on the host includes servlet and
		// repository but not server
		String curURI = (String) this.getHeaderValues().get("uri");
		LockManager lm = this.getLockManager();
		IDAVResource resource;

		String des = (String) this.getReq().getHeader("Destination");
		// des =
		// des.replaceAll(this.getReq().getRequestURL().toString().replaceAll(curPath,
		// ""), "");
		try {
			curURI = java.net.URLDecoder.decode(curURI, "UTF-8");
			des = java.net.URLDecoder.decode(des, "UTF-8");

		} catch (Exception e) {
		}
		LOGGER.info("DESTINATION ADDRESS=" + des);
		try {
			resource = rep.getResource(curURI);
			if (lm.isLocked(resource)) {
				this.setHTTPStatus(423);
				return;
			} else {
				if (resource.isReadOnly()) {
					this.setHTTPStatus(HttpServletResponse.SC_FORBIDDEN);
				} else {
					this.setHTTPStatus(rep.moveResource(curURI, des));
					return;
				}
			}
		} catch (DAVNotFoundException e) {
			this.setErrorMessage("Resource not found" + curURI, 404);
			return;
		}
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.methods.AbstractDAVMethod#writeInitialHeader()
	 */
	protected void writeInitialHeader() {
		// Nothing special here

	}

}
