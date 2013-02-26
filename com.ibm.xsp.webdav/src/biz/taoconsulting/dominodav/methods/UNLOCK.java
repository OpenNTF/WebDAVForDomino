/* ========================================================================== *
 * Copyright (C) 2006, 2007 TAO Consulting Pte <http://www.taoconsulting.sg/> *
 *           based on work of                                                 *
 *           (C) 2004-2005 Pier Fumagalli <http://www.betaversion.org/~pier/> *
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

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import biz.taoconsulting.dominodav.LockManager;
import biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation;
import biz.taoconsulting.dominodav.interfaces.IDAVRepository;
import biz.taoconsulting.dominodav.interfaces.IDAVResource;

/**
 * @author Bastian Buch (TAO Consulting)
 * 
 */
public class UNLOCK extends AbstractDAVMethod {
	/**
	 * The logger object for event logging
	 */
	private static final Log LOGGER = LogFactory.getLog(UNLOCK.class);

	/**
	 * @see biz.taoconsulting.dominodav.methods.AbstractDAVMethod#action()
	 */
	protected void action() throws Exception {
		// Resource-Path is stripped by the repository name!
		String curPath = (String) this.getHeaderValues().get("resource-path");
		LOGGER.debug("Unlocking for " + curPath);
		String curURI = (String) this.getHeaderValues().get("uri");
		IDAVRepository rep = this.getRepository();
		IDAVResource resource = rep.getResource(curURI);
		try {
			curURI = java.net.URLDecoder.decode(curURI, "UTF-8");
		} catch (Exception e) {
		}
		LockManager lm = this.getLockManager();
		HttpServletRequest req = this.getReq();

		try {
			String lockToken = req.getHeader("Lock-Token");
			LOGGER.debug("Lock-Token:" + lockToken);
			lm.unlock(((IDAVAddressInformation) resource).getInternalAddress(),
					lockToken);
		} catch (Exception e) {
			LOGGER.error(e);
		}

	}

	/**
	 * @see biz.taoconsulting.dominodav.methods.AbstractDAVMethod#writeInitialHeader()
	 */
	protected void writeInitialHeader() {
		// No action needed

	}

}
