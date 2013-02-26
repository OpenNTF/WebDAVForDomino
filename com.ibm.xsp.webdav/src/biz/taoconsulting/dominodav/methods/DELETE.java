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

/**
 * @author Bastian Buch (TAO Consulting)
 * 
 */
public class DELETE extends AbstractDAVMethod {

	/**
	 * @see biz.taoconsulting.dominodav.methods.AbstractDAVMethod#action()
	 */
	protected void action() {
		// is resource locked? user doesnt matter.... if file is opened by
		// another application id should not be deleted...
		IDAVRepository rep = this.getRepository();
		// Resource-Path is stripped by the repository name!
		// String curPath = (String)
		// this.getHeaderValues().get("resource-path");
		// uri is the unique identifier on the host includes servlet and
		// repository but not server
		String curURI = (String) this.getHeaderValues().get("uri");
		LockManager lm = this.getLockManager();

		try {
			IDAVResource resource = rep.getResource(curURI, true);
			if (!lm.isLocked(resource) && (!resource.isReadOnly())) {
				if (resource.delete()) {
					this.setHTTPStatus(200);
					this.getResp().setStatus(200);
				} else {
					this.setErrorMessage("Can't delete " + curURI, 401);
				}
			} else {
				this.setErrorMessage("Can't delete " + curURI, 401);
			}
		} catch (DAVNotFoundException exc) {
			this.setErrorMessage("Resource not found " + curURI, 404);
		}
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.methods.AbstractDAVMethod#writeInitialHeader()
	 */
	protected void writeInitialHeader() {
		// No special header here

	}

}