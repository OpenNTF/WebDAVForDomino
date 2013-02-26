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

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Bastian Buch (TAO Consulting)
 * 
 */
public class OPTIONS extends AbstractDAVMethod {

	private static final Log LOGGER = LogFactory.getLog(OPTIONS.class);

	/**
	 * @see biz.taoconsulting.dominodav.methods.AbstractDAVMethod#action()
	 */
	protected void action() {
		// Options have a path associated -- and do we need to reply repository
		// specific
		// ToDo: Resource Specific Options!!!
		String methods = this.getRepository().getAvailableMethods();
		LOGGER.debug("Methods: " + methods);

		this.getResp().addHeader("Allow", methods);
		this.setHTTPStatus(HttpServletResponse.SC_OK); // We are good here
		this.getResp().setContentLength(0);
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.methods.AbstractDAVMethod#writeInitialHeader()
	 */
	protected void writeInitialHeader() {
		HttpServletResponse resp = this.getResp();
		// Caldav header TODO: do we need to complete header methods for webDAV
		// with access-control, calendar-access?
		resp.setHeader("DAV", "1,2");
		resp.setHeader("MS-Author-Via", "DAV"); // needed so Office knows what
												// to do

	}

}
