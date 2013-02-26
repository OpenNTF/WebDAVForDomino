/** ========================================================================= *
 * Copyright (C) 2011, 2012 IBM Corporation                                   *
 *           based on work of                                                 *
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
 * ========================================================================== **/
package com.ibm.xsp.webdav.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ibm.xsp.webdav.interfaces.IDAVXMLResponse;

import biz.taoconsulting.dominodav.interfaces.IDAVRepository;
import biz.taoconsulting.dominodav.resource.DAVAbstractResource;

/**
 * @author NotesSensei
 * 
 */
public class DAVResourceInternal extends DAVAbstractResource {

	/**
	 * Logger for Errors
	 */
	private static final Log LOGGER = LogFactory
			.getLog(DAVResourceInternal.class);

	/**
	 * What resource to deliver
	 */
	private String resourceName = null;

	/**
	 * 
	 * Returns static content of a Desktop.ini file
	 */
	public DAVResourceInternal(String whatResource, IDAVRepository owner) {
		this.resourceName = whatResource;
		this.setName(whatResource);
		this.setReadOnly(true);
		if (whatResource.equals("/")) {
			this.setMember(false);
			this.setCollection(true);
		} else {
			this.setMember(true);
			this.setCollection(false);
		}
		this.setInternalAddress(whatResource);
		this.setPublicHref(whatResource);
		this.setOwner(owner);

		Date fakeDate = new Date(59735894400000L); // Just picked one
		this.setCreationDate(fakeDate);
		this.setLastModified(fakeDate);
		LOGGER.debug("DAVResourceInternal created for " + whatResource);
	}

	/**
	 * @see biz.taoconsulting.dominodav.resource.DAVAbstractResource#getExtension()
	 */
	public String getExtension() {
		int dot = resourceName.lastIndexOf(".");
		return resourceName.substring(dot);
	}

	/**
	 * @see biz.taoconsulting.dominodav.resource.DAVAbstractResource#getStream()
	 */
	public InputStream getStream() {
		// LOGGER.info("DAVResourceInternal stream requested for "+this.resourceName+"; Class="+this.getClass().toString());
		// LOGGER.info("Current location is "+this.getClass().getPackage().getName().replace('.',
		// '/'));
		InputStream stream = this.getClass().getResourceAsStream(
				"/" + this.getClass().getPackage().getName().replace('.', '/')
						+ "/" + this.resourceName);
		// LOGGER.info("Stream OK is null?"+((stream == null)?" yes":"no"));
		return stream;
	}

	/**
	 * @see biz.taoconsulting.dominodav.resource.DAVAbstractResource#getOutputStream()
	 */
	public OutputStream getOutputStream() {
		return null; // We don't write this back
	}

	/**
	 * Delete a file resource
	 * 
	 * @return true/false : did the deletion work?
	 */
	public boolean delete() {
		return false; // We don't delete this
	}

	/**
	 * Overwritten and simplified method since we don't need all of these for
	 * internal resources
	 */
	public void addToDavXMLResponse(IDAVXMLResponse dxr) throws IOException {

		// Generate the default header
		dxr.openTag("response");

		// Intercept if the Href is missing
		String curHref = this.getPublicHref(); // .properties.getVal("href");
		dxr.simpleTag("href", (curHref == null ? "null" : curHref));

		// Now the properties wrapped into propstat and prop
		dxr.openTag("propstat");
		dxr.openTag("prop");

		if (this.isCollection()) {
			dxr.openTag("resourcetype");
			dxr.emptyTag("collection");
			dxr.closeTag(1);
		} else {
			dxr.emptyTag("resourcetype");
		}

		dxr.simpleTag("source", this.getInternalAddress());
		dxr.cdataTag("displayname", this.getName());
		dxr.simpleTag("isreadonly", "true");
		// End of prop
		dxr.closeTag(1);
		dxr.simpleTag("status", "HTTP/1.1 200 OK");

		// End of our content
		dxr.closeTag(2); // propstat and response

		LOGGER.debug("XML written for " + curHref);
	}

	public void patchLastModified(Date dt) {
	}

	public void patchCreationDate(Date dt) {
	}
}
