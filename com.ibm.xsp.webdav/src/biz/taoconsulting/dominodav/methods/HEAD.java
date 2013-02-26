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

import javax.servlet.http.HttpServletResponse;

import biz.taoconsulting.dominodav.exceptions.DAVNotFoundException;
import biz.taoconsulting.dominodav.interfaces.IDAVRepository;

/**
 * @author Bastian Buch (TAO Consulting)
 * 
 */
public class HEAD extends AbstractDAVMethod {

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.methods.AbstractDAVMethod#action()
	 */
	protected void action() {
		// TODDO what does HEAD exactly?
		IDAVRepository rep = this.getRepository();
		// uri is the unique identifier on the host includes servlet and
		// repository but not server

		String curURI = (String) this.getHeaderValues().get("uri");
		try {
			curURI = java.net.URLDecoder.decode(curURI, "UTF-8");
		} catch (Exception e) {
		}
		try {
			rep.getResource(curURI);
			this.setHTTPStatus(200);
		} catch (DAVNotFoundException e) {
			this.setErrorMessage("Resource not found:" + curURI,
					HttpServletResponse.SC_NOT_FOUND);
		}

	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.methods.AbstractDAVMethod#writeInitialHeader()
	 */
	protected void writeInitialHeader() {
		// No special attention needed here

	}

}
