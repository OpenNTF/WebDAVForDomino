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

import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import biz.taoconsulting.dominodav.exceptions.DAVNotFoundException;
import biz.taoconsulting.dominodav.interfaces.IDAVResource;

/**
 * @author Bastian Buch (TAO Consulting)
 * 
 */
public class COPY extends AbstractDAVMethod {
	/**
	 * Logger for Errors
	 */
	private static final Log LOGGER = LogFactory.getLog(COPY.class);

	/**
	 * the destination Resource
	 */
	private IDAVResource destinationResource;

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.methods.AbstractDAVMethod#action()
	 */
	protected void action() {
		// TODO Needs fixing!
		String src = (String) this.getHeaderValues().get("path-info");
		String uri = (String) this.getHeaderValues().get("uri");
		String des = (String) this.getReq().getHeader("Destination");
		des = des.replaceAll(this.getReq().getRequestURL().toString()
				.replaceAll(src, ""), "");
		try {
			this.resource = this.getRepository().getResource(uri, true);

		} catch (DAVNotFoundException e) {
			this.setErrorMessage("Not found: " + src,
					HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		try {
			// FIXME: needs fixin
			this.destinationResource = this.getRepository().getResource(des,
					true);
		} catch (DAVNotFoundException e) {
			// TODO Very BAD, we didn't fix the URI!
			this.destinationResource = this.getRepository().createNewResource(
					des);
		}

		InputStream instream = this.resource.getStream();

		OutputStream out = this.destinationResource.getOutputStream();

		try {
			int read = 0;
			byte[] bytes = new byte[1024];
			while ((read = instream.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			out.flush();
			instream.close();
			out.close();
		} catch (Exception ex) {
			LOGGER.error(ex);
		}
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.methods.AbstractDAVMethod#writeInitialHeader()
	 */
	protected void writeInitialHeader() {
		// No action needed!

	}

}
