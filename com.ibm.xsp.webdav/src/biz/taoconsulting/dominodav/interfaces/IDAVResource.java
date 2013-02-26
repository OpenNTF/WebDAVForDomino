/*
 * ========================================================================== *
 * Copyright (C) 2006, 2007 TAO Consulting Pte <http://www.taoconsulting.sg/> *
 * All rights reserved. *
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

package biz.taoconsulting.dominodav.interfaces;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Vector;

import com.ibm.xsp.webdav.interfaces.IDAVXMLResponse;

//import biz.taoconsulting.dominodav.DAVProperties;

/**
 * IDAVResource represents an entry in a WebDAV Repository. It can be a
 * collection (a.k.a a directory) containing other Resources or it can be a
 * "file" which is something that returns a stream or can be written to as a
 * stream. A resource has 3 identifiers The href = access to the resource
 * relative to the repository as seen from the browser The path = access to the
 * resource as seen from it's internal mechanism like (the absolute) path in a
 * file system or url or query statement The uri = access to the resource from
 * the browser including servlet and repository
 * 
 * @author Stephan H. Wissel
 */
public interface IDAVResource {

	/**
	 * addToKXml adds reponse parts to the XML output
	 * 
	 * @param dxr
	 *            - Object that writes XML information for responses in webDAV
	 *            standard
	 * @throws IOException
	 *             for XML Errors
	 */
	public void addToDavXMLResponse(IDAVXMLResponse dxr) throws IOException;

	public void addToDavXMLResponsePROPPATCH(IDAVXMLResponse dxr)
			throws IOException;

	/**
	 * @return true/false - Success of delete operation
	 */
	public abstract boolean delete();

	/**
	 * @return Length of content
	 */
	public Long getContentLength();

	/**
	 * @return Date CreationDate (not supported in Java)
	 */
	public Date getCreationDate();

	/**
	 * eTag is to identify a resource compared to other versions of it
	 * 
	 * @return String eTag
	 */
	public String getETag();

	/**
	 * @return String the file extension
	 */
	public String getExtension();

	/**
	 * @return Date LastModified Date
	 */
	public Date getLastModified();

	/**
	 * @return mime type of file resource
	 */
	public String getMimeType();

	/**
	 * @return Outputstream to update resource
	 */
	public abstract OutputStream getOutputStream();

	/**
	 * @return Owner of this resource
	 */
	public IDAVRepository getOwner();

	/**
	 * @return DAVProperties: all properties of the resource
	 */
	// public DAVProperties getProperties();

	/**
	 * @return repository - the owning repository
	 */
	public IDAVRepository getRepository();

	/**
	 * @return The internal type of resource Externally we only have files and
	 *         directories internally it can be anything
	 */
	public String getResourceType();

	/**
	 * @return InputStream - Stream Object to read resource
	 */
	public InputStream getStream();

	/**
	 * @return boolean: is it a collection/directory
	 */
	public boolean isCollection();

	/**
	 * @return is it a member, so there won't be any sub elements in it
	 */
	public boolean isMember();

	/**
	 * @return Returns the readOnly.
	 */
	public boolean isReadOnly();

	/**
	 * @param isCollection
	 *            declare it a collection with true
	 */
	public void setCollection(boolean isCollection);

	/**
	 * @param isMember
	 *            Make it a member
	 */
	public void setMember(boolean isMember);

	/**
	 * @param members
	 *            Vector with DAVResourceObjects
	 */
	public void setMembers(Vector<IDAVResource> members);

	/**
	 * @param the
	 *            mime type of file resource
	 */
	public void setMimeType(String newMimeType);

	/**
	 * @param owner
	 *            Owner of this resource
	 */
	public void setOwner(IDAVRepository owner);

	/**
	 * @param type
	 *            the internal resource type (String)
	 */
	public void setResourceType(String type);

	public abstract void patchLastModified(Date dt);

	public abstract void patchCreationDate(Date dt);

	// public void embed();

}
