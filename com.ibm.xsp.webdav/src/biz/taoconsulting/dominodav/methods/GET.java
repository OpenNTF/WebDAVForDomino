/*
 * ========================================================================== *
 * Copyright (C) 2006, 2007 TAO Consulting Pte <http://www.taoconsulting.sg/> *
 * based on work of * C) 2004-2005 Pier Fumagalli
 * <http://www.betaversion.org/~pier/> * All rights reserved. *
 * ========================================================================== *
 * * Licensed under the Apache License, Version 2.0 (the "License"). You may *
 * not use this file except in compliance with the License. You may obtain a *
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>. * *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT *
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the *
 * License for the specific language governing permissions and limitations *
 * under the License. * *
 * ==========================================================================
 */
package biz.taoconsulting.dominodav.methods;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation;
import biz.taoconsulting.dominodav.interfaces.IDAVRepository;
import biz.taoconsulting.dominodav.interfaces.IDAVResource;

/**
 * Method to retrieve Resources. If the requested resource is a Collection the
 * atcion delegates the Propfind-Method to handle the request.
 * 
 * @author Bastian Buch (TAO Consulting)
 */
public class GET extends AbstractDAVMethod {
	/**
	 * The logger object for event logging
	 */
	private static final Log LOGGER = LogFactory.getLog(GET.class);

	/**
	 * @see biz.taoconsulting.dominodav.methods.AbstractDAVMethod#action()
	 */
	public void action() {
		InputStream stream = null; // The input stream coming from the resource
		OutputStream out = null; // The Servlet Response
		HttpServletResponse res = this.getResp();

		try {
			// check if focused resource is a collection or a file resource...
			// collections are handled by propfind method
			// files / data will be send through the response stream writer...
			IDAVRepository rep = this.getRepository();
			// Resource-Path is stripped by the repository name!
			// String curPath = (String)
			// this.getHeaderValues().get("resource-path");
			// uri is the unique identifier on the host includes servlet and
			// repository but not server
			String curURI = (String) this.getHeaderValues().get("uri");
			IDAVResource resource = rep.getResource(curURI);

			// Not ideal for browser access: we get the resource twice
			if (resource.isCollection()) {
				// LOGGER.info("Resource "+curURI +" is a collection");
				PROPFIND p = new PROPFIND();
				// Transfer the context if any
				p.setContext(this.getContext());
				p.calledFromGET();
				// LOGGER.info("After Called from GET");
				p.process(this.getReq(), res, this.getRepository(),
						this.getLockManager());
				// LOGGER.info("After Process ");
			} else {
				// LOGGER.info("Resource "+curURI +" is NOT a collection");
				res.setHeader("Content-Type", resource.getMimeType());
				res.setHeader("Pragma", "no-cache");
				res.setHeader("Expires", "0");
				res.setHeader("ETag", resource.getETag());

				// TODO: Do we need content-disposition attachment or should it
				// be inline?
				res.setHeader("Content-disposition", "inline; filename*="
						+ ((IDAVAddressInformation) resource).getName());
				// LOGGER.info("Set header finnish");
				Long curLen = resource.getContentLength();
				if (curLen != null && curLen.longValue() > 0) {
					// LOGGER.info("CurrLen is not null and positive="+
					// curLen.toString());
					res.setHeader("Content-Length", resource.getContentLength()
							.toString());
				} else {
					// LOGGER.info("CurrLen is null!!!");
				}
				// Get the stream of the resource object and copy it into
				// the outputstream which is provided by the response object

				try {
					// LOGGER.info("Before resource getStream; class="+resource.getClass().toString());
					stream = resource.getStream();

					int read = 0;
					byte[] bytes = new byte[32768]; // ToDo is 32k a good
													// buffer?

					int totalread = 0;

					// TODO: Improve on this - is there a better way from input
					// to output stream?
					out = this.getOutputStream();
					while ((read = stream.read(bytes)) != -1) {
						out.write(bytes, 0, read);
						totalread += read;
					}

					// Capture the result length
					// LOGGER.info("Before resource setContentLength");
					res.setContentLength(totalread);
					// LOGGER.info("After resource setContentLength");

				} catch (IOException iox) {
					this.setErrorMessage(
							"Get method failed with:" + iox.getMessage(),
							HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					LOGGER.error(iox);
				}

			}
		} catch (Exception exc) {
			LOGGER.error("Get method failed with:" + exc.getMessage(), exc);
			this.setErrorMessage("Get method failed with:" + exc.getMessage(),
					HttpServletResponse.SC_NOT_FOUND);
		} finally {
			// Close of all resources
			try {

				if (out != null) {
					out.flush();
					out.close();
				}

				if (stream != null) {
					stream.close();
				}

			} catch (Exception e) {
				LOGGER.error("GET Object cleanup failed:" + e.getMessage(), e);
			}
		}
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.methods.AbstractDAVMethod#writeInitialHeader()
	 */
	protected void writeInitialHeader() {
		// we have to write the header in the action method because it might
		// depend
		// on different things (e.g. resource type etc)
	}

}
