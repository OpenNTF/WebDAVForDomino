/** ========================================================================= *
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
 * ========================================================================== */
package biz.taoconsulting.dominodav.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import biz.taoconsulting.dominodav.DAVProperties;
import biz.taoconsulting.dominodav.LockManager;
import biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation;
import biz.taoconsulting.dominodav.interfaces.IDAVRepository;
import biz.taoconsulting.dominodav.interfaces.IDAVResource;

import com.ibm.xsp.webdav.WebDavManager;
import com.ibm.xsp.webdav.interfaces.IDAVXMLResponse;

/**
 * DAVAbstractResource represents an entry in a WebDAV Repository. It can be a
 * collection (a.k.a a directory) containing other Resources or it can be a
 * "file" which is something that returns a stream or can be written to as a
 * stream. A resource has 3 identifiers The href = access to the resource
 * relative to the repository as seen from the browser The path = access to the
 * resource as seen from it's internal mechanism like (the absolute) path in a
 * file system or url or query statement The uri = access to the resource from
 * the browser including servlet and repository
 * 
 * @author Stephan H. Wissel
 * 
 */
public abstract class DAVAbstractResource implements IDAVResource,
		IDAVAddressInformation {

	/**
	 * Logger for Errors
	 */
	private static final Log LOGGER = LogFactory
			.getLog(DAVAbstractResource.class);

	/**
	 * Length of the content expressed in bytes as a Long
	 */
	protected Long contentLenght = null;

	/**
	 * Creation date -- not supported in Java for files, but other members could
	 */
	private Date creationDate;

	/**
	 * The path to the resource = access to the resource from the repository as
	 * seen from it's internal mechanism if the browser types
	 * http://www.myserver.com/dav/special/jan/report.xls and the repository is
	 * "special", wich is located c:\test and the file is
	 * c:\test\jan\reports.xls then the path is = "\jan\reports.xls" In Domino
	 * or RDBMS this would be the query expression!
	 */
	private String internalAddress;

	/**
	 * Is the resource Read Only, we assume it is read write
	 */
	private boolean readOnly = false;

	/**
	 * Is this a directory yes/no
	 */
	private boolean isCollection;

	/**
	 * is it a member: Needed when a collection is parsed. To avoid endless
	 * recursion We set isMember when adding a resource inside a collection
	 */
	private boolean isMember;

	/**
	 * Last modified date
	 */
	private Date lastModifiedDate;

	/**
	 * List of members (for directory/collection only)
	 */
	private Vector<IDAVResource> members;

	/**
	 * Name of the resource --- used for display typically the filename without
	 * any path
	 */
	private String name;

	/**
	 * Who owns the resource
	 */
	private IDAVRepository owner;

	/**
	 * Properties of the resource
	 */
	private DAVProperties properties;

	/**
	 * The URL of the resource = access to the resource from the repository as
	 * seen from the browser if the browser types
	 * http://www.myserver.com/dav/special/jan/report.xls and the repository is
	 * "special", then the URL = "/jan/reports.xls"
	 */
	private String publicHref;

	/**
	 * The internal type of resource we are dealing with
	 */
	private String resourceType = null;

	/**
	 * Name of the current file extension
	 */
	private String fileExtension;

	/**
	 * addToKXml adds reponseparts to the XML output
	 * 
	 * @param dxr
	 *            - DAV XML Resonse
	 * @throws IOException
	 *             for XML Errors
	 */

	/**
	 * The unique eTag
	 */
	protected String eTag = null;

	/**
	 * The MimeType for this resource
	 */
	private String mimeType = null;

	/**
	 * Adds informatoin about locking to a resource output
	 * 
	 * @param dxr
	 */
	private void addLockStatus(IDAVXMLResponse dxr) {
		// Special for locked properties
		LockManager lm = LockManager.getLockManager();

		if (lm.isLocked(this)) {
			/*
			 * <D:lockdiscovery> <D:activelock>
			 * <D:locktype><D:write/></D:locktype>
			 * <D:lockscope><D:exclusive/></D:lockscope> <D:depth>0</D:depth>
			 * <D:owner>James Smith</D:owner> --- We don't reveal token and
			 * timeout --- <D:timeout>Infinite</D:timeout> <D:locktoken>
			 * <D:href>
			 * opaquelocktoken:f81de2ad-7f3d-a1b3-4f3c-00a0c91a9d76</D:href>
			 * </D:locktoken> </D:activelock> </D:lockdiscovery>
			 */
			String owner = lm.getLockOwner(this);
			dxr.openTag("lockdiscovery");
			dxr.openTag("activelock");
			dxr.openTag("locktype");
			dxr.emptyTag("write");
			dxr.closeTag(1);
			dxr.openTag("lockscope");
			dxr.emptyTag("exclusive");
			dxr.closeTag(1);
			dxr.simpleTag("depth", "0");
			dxr.simpleTag("owner", owner);
			dxr.closeTag(2); // activelock lockdiscovery
		}
	}

	/**
	 * Adds the XML needed for proper locking support
	 * 
	 * @param dxr
	 */
	private void addLockType(IDAVXMLResponse dxr) {
		/*
		 * the structure of the lock information: <supportedlock> <lockentry>
		 * <lockscope> <exclusive/> </lockscope> <locktype> <write/> </locktype>
		 * </lockentry> <lockentry> <lockscope> <shared/> </lockscope>
		 * <locktype> <write/> </locktype> </lockentry> </supportedlock>
		 */
		dxr.openTag("supportedlock");
		dxr.openTag("lockentry");
		dxr.openTag("lockscope");
		dxr.emptyTag("exclusive");
		dxr.closeTag(1);
		dxr.openTag("locktype");
		dxr.emptyTag("write");
		dxr.closeTag(2);
		// And the share one
		dxr.openTag("lockentry");
		dxr.openTag("lockscope");
		dxr.emptyTag("shared");
		dxr.closeTag(1);
		dxr.openTag("locktype");
		dxr.emptyTag("write");
		dxr.closeTag(3); // close until supportedlock
	}

	public void addToDavXMLResponse(IDAVXMLResponse dxr) throws IOException {

		// Format expected as result. multistatus is already set in PROPFIND
		/*
		 * <?xml version="1.0" encoding="utf-8" ?> <D:multistatus
		 * xmlns:D="DAV:"> <D:response> <D:href>/webdav/Demo/</D:href>
		 * <D:propstat> <D:prop>
		 * <D:creationdate>2011-06-28T09:36:29Z</D:creationdate>
		 * <D:displayname><![CDATA[Demo]]></D:displayname> <D:resourcetype>
		 * <D:collection/> </D:resourcetype> <D:source/> <D:supportedlock>
		 * <D:lockentry> <D:lockscope> <D:exclusive/> </D:lockscope>
		 * <D:locktype> <D:write/> </D:locktype> </D:lockentry> <D:lockentry>
		 * <D:lockscope> <D:shared/> </D:lockscope> <D:locktype> <D:write/>
		 * </D:locktype> </D:lockentry> </D:supportedlock> </D:prop>
		 * <D:status>HTTP/1.1 200 OK</D:status> </D:propstat> </D:response>
		 * <D:response> <D:href>/webdav/Demo/Test1.doc</D:href> <D:propstat>
		 * <D:prop> <D:creationdate>2011-06-26T11:53:49Z</D:creationdate>
		 * <D:displayname><![CDATA[Test1.doc]]></D:displayname>
		 * <D:getlastmodified>Sun, 26 Jun 2011 11:53:49 GMT</D:getlastmodified>
		 * <D:getcontentlength>19968</D:getcontentlength>
		 * <D:getcontenttype>application/msword</D:getcontenttype>
		 * <D:getetag>W/"19968-1309089229626"</D:getetag> <D:resourcetype/>
		 * <D:source/> <D:supportedlock> <D:lockentry> <D:lockscope>
		 * <D:exclusive/> </D:lockscope> <D:locktype> <D:write/> </D:locktype>
		 * </D:lockentry> <D:lockentry> <D:lockscope> <D:shared/> </D:lockscope>
		 * <D:locktype> <D:write/> </D:locktype> </D:lockentry>
		 * </D:supportedlock> </D:prop> <D:status>HTTP/1.1 200 OK</D:status>
		 * </D:propstat> </D:response> </D:multistatus>
		 */

		// Step 1: Get all variables
		// Intercept if the Href is missing - shouldn't happen
		String curHref = this.getPublicHref(); // .properties.getVal("href");
		String hrefToWrite = (curHref == null) ? "null" : curHref;
		// If it is a collection/directory it must end with /
		if (this.isCollection && !hrefToWrite.endsWith("/")) {
			hrefToWrite += "/";
		}

		String displayName = this.getName();
		Date creaDate = this.getCreationDate();
		Date modiDate = this.getLastModified();
		String contentLString = String.valueOf(this.getContentLength());
		String mimeString = this.getMimeType();
		String eTagString = this.getETag();

		// Step 2: Write out the result -- to better distinguish between files
		// and directories
		// the code below separates them

		if (this.isCollection) {
			// Step 2a: Write a collection
			dxr.openTag("response"); // <D:response>
			dxr.simpleTag("href", hrefToWrite); // <D:href>/webdav/Demo/</D:href>
			dxr.openTag("propstat"); // <D:propstat>
			dxr.openTag("prop"); // <D:prop>
			dxr.dateTagForCreateDate("creationdate", new Date()); // <D:creationdate>2011-06-28T09:36:29Z</D:creationdate>
			dxr.cdataTag("displayname", displayName); // <D:displayname><![CDATA[Demo]]></D:displayname>
			dxr.openTag("resourcetype"); // <D:resourcetype>
			dxr.emptyTag("collection"); // <D:collection/>
			dxr.closeTag(1); // </D:resourcetype>
			dxr.emptyTag("source"); // <D:source/>
			if (this.isReadOnly()) {
				dxr.simpleTag("isreadonly", "true"); // Readonly Status
			}
			this.addLockType(dxr); // <D:supportedlock> ... </D:supportedlock>
			dxr.closeTag(1); // </D:prop>
			dxr.simpleTag("status", "HTTP/1.1 200 OK"); // <D:status>HTTP/1.1
														// 200 OK</D:status>
			dxr.closeTag(2); // </D:propstat></D:response>

			// Once a collection has been written eventually the members
			// need to be written. In HTTP that's the header depth=1
			// Here we indicate that with !isMember()
			// If this resource is directory we are interested in the members
			if (!this.isMember && this.members != null) {
				LOGGER.debug("Now writing XML child entries for " + curHref);
				for (int i = 0; i < this.members.size(); i++) {
					((DAVAbstractResource) (this.members.get(i)))
							.addToDavXMLResponse(dxr);
				}
				LOGGER.debug("XML child entries written for " + curHref);
			}
		} else {
			// Step 2b: Write a file
			// LOGGER.info("DAV Locale="+Locale.getDefault().toString());
			dxr.openTag("response"); // <D:response>
			dxr.simpleTag("href", hrefToWrite); // <D:href>/webdav/Demo/Test1.doc</D:href>
			dxr.openTag("propstat"); // <D:propstat>
			dxr.openTag("prop"); // <D:prop>
			dxr.dateTagForCreateDate("creationdate", creaDate); // <D:creationdate>2011-06-26T11:53:49Z</D:creationdate>
			dxr.cdataTag("displayname", displayName); // <D:displayname><![CDATA[Test1.doc]]></D:displayname>
			dxr.dateTag("getlastmodified", modiDate); // <D:getlastmodified>Sun,
														// 26 Jun 2011 11:53:49
														// GMT</D:getlastmodified>"
			dxr.simpleTag("getcontentlength", contentLString); // <D:getcontentlength>19968</D:getcontentlength>
			dxr.simpleTag("getcontenttype", mimeString); // <D:getcontenttype>application/msword</D:getcontenttype>
			dxr.simpleTag("getetag", eTagString); // <D:getetag>W/"19968-1309089229626"</D:getetag>
			// dxr.auxTag("Subject", "Subject Eugen Cretu "+displayName);
			// dxr.auxTag("Title", "Title Eugen Cretu "+displayName);
			dxr.emptyTag("resourcetype"); // <D:resourcetype/>
			dxr.emptyTag("source"); // <D:source/>
			if (this.isReadOnly()) {
				dxr.simpleTag("isreadonly", "true"); // Readonly Status
			}
			this.addLockType(dxr); // <D:supportedlock> ... </D:supportedlock>
			this.addLockStatus(dxr); // <D:lockdiscovery> ... </D:lockdiscovery>
			dxr.closeTag(1); // </D:prop>
			dxr.simpleTag("status", "HTTP/1.1 200 OK"); // <D:status>HTTP/1.1
														// 200 OK</D:status>
			dxr.closeTag(2); // </D:propstat></D:response>
		}

		LOGGER.debug("XML written for " + curHref);
	}

	public void addToDavXMLResponsePROPPATCH(IDAVXMLResponse dxr)
			throws IOException {

		// Format expected as result. multistatus is already set in PROPFIND
		/*
		 * <?xml version="1.0" encoding="utf-8" ?> <D:multistatus
		 * xmlns:D="DAV:"> <D:response> <D:href>/webdav/Demo/</D:href>
		 * <D:propstat> <D:prop>
		 * <D:creationdate>2011-06-28T09:36:29Z</D:creationdate>
		 * <D:displayname><![CDATA[Demo]]></D:displayname> <D:resourcetype>
		 * <D:collection/> </D:resourcetype> <D:source/> <D:supportedlock>
		 * <D:lockentry> <D:lockscope> <D:exclusive/> </D:lockscope>
		 * <D:locktype> <D:write/> </D:locktype> </D:lockentry> <D:lockentry>
		 * <D:lockscope> <D:shared/> </D:lockscope> <D:locktype> <D:write/>
		 * </D:locktype> </D:lockentry> </D:supportedlock> </D:prop>
		 * <D:status>HTTP/1.1 200 OK</D:status> </D:propstat> </D:response>
		 * <D:response> <D:href>/webdav/Demo/Test1.doc</D:href> <D:propstat>
		 * <D:prop> <D:creationdate>2011-06-26T11:53:49Z</D:creationdate>
		 * <D:displayname><![CDATA[Test1.doc]]></D:displayname>
		 * <D:getlastmodified>Sun, 26 Jun 2011 11:53:49 GMT</D:getlastmodified>
		 * <D:getcontentlength>19968</D:getcontentlength>
		 * <D:getcontenttype>application/msword</D:getcontenttype>
		 * <D:getetag>W/"19968-1309089229626"</D:getetag> <D:resourcetype/>
		 * <D:source/> <D:supportedlock> <D:lockentry> <D:lockscope>
		 * <D:exclusive/> </D:lockscope> <D:locktype> <D:write/> </D:locktype>
		 * </D:lockentry> <D:lockentry> <D:lockscope> <D:shared/> </D:lockscope>
		 * <D:locktype> <D:write/> </D:locktype> </D:lockentry>
		 * </D:supportedlock> </D:prop> <D:status>HTTP/1.1 200 OK</D:status>
		 * </D:propstat> </D:response> </D:multistatus>
		 */

		// Step 1: Get all variables
		// Intercept if the Href is missing - shouldn't happen
		String curHref = this.getPublicHref(); // .properties.getVal("href");
		String hrefToWrite = (curHref == null) ? "null" : curHref;
		// If it is a collection/directory it must end with /
		if (this.isCollection && !hrefToWrite.endsWith("/")) {
			hrefToWrite += "/";
		}

		String displayName = this.getName();

		// Step 2: Write out the result -- to better distinguish between files
		// and directories
		// the code below separates them

		if (this.isCollection) {
			// Step 2a: Write a collection
			dxr.openTag("response"); // <D:response>
			dxr.simpleTag("href", hrefToWrite); // <D:href>/webdav/Demo/</D:href>
			dxr.openTag("propstat"); // <D:propstat>
			dxr.openTag("prop"); // <D:prop>
			dxr.dateTagForCreateDate("creationdate", new Date()); // <D:creationdate>2011-06-28T09:36:29Z</D:creationdate>
			dxr.cdataTag("displayname", displayName); // <D:displayname><![CDATA[Demo]]></D:displayname>
			dxr.openTag("resourcetype"); // <D:resourcetype>
			dxr.emptyTag("collection"); // <D:collection/>
			dxr.closeTag(1); // </D:resourcetype>
			dxr.emptyTag("source"); // <D:source/>
			this.addLockType(dxr); // <D:supportedlock> ... </D:supportedlock>
			dxr.closeTag(1); // </D:prop>
			dxr.simpleTag("status", "HTTP/1.1 200 OK"); // <D:status>HTTP/1.1
														// 200 OK</D:status>
			dxr.closeTag(2); // </D:propstat></D:response>

			// Once a collection has been written eventually the members
			// need to be written. In HTTP that's the header depth=1
			// Here we indicate that with !isMember()
			// If this resource is directory we are interested in the members
			if (!this.isMember && this.members != null) {
				LOGGER.debug("Now writing XML child entries for " + curHref);
				for (int i = 0; i < this.members.size(); i++) {
					((DAVAbstractResource) (this.members.get(i)))
							.addToDavXMLResponse(dxr);
				}
				LOGGER.debug("XML child entries written for " + curHref);
			}
		} else {
			// Step 2b: Write a file
			dxr.openTag("response"); // <D:response>
			dxr.simpleTag("href", hrefToWrite); // <D:href>/webdav/Demo/Test1.doc</D:href>
			dxr.openTag("propstat"); // <D:propstat>
			dxr.openTag("prop"); // <D:prop>
			dxr.emptyTag("creationdate");
			dxr.closeTag(1);
			dxr.simpleTag("status", "HTTP/1.1 200 OK");
			dxr.closeTag(1);

			dxr.openTag("propstat"); // <D:propstat>
			dxr.openTag("prop"); // <D:prop>
			dxr.emptyTag("lastaccessed");
			dxr.closeTag(1);
			dxr.simpleTag("status", "HTTP/1.1 200 OK");
			dxr.closeTag(1);

			dxr.openTag("propstat"); // <D:propstat>
			dxr.openTag("prop"); // <D:prop>
			dxr.emptyTag("getlastmodified");
			dxr.closeTag(1);
			dxr.simpleTag("status", "HTTP/1.1 200 OK");
			dxr.closeTag(1); // close propstat
			dxr.closeTag(1); // close response

		}

		LOGGER.debug("XML written for " + curHref);
	}

	/**
	 * 
	 * @return true/false - Success of delete operation
	 */
	public abstract boolean delete();

	/**
	 * 
	 * @return Length of content
	 */
	public Long getContentLength() {
		return this.contentLenght;
	}

	/**
	 * 
	 * @return Date CreationDate (not supported in Java)
	 */
	public Date getCreationDate() {

		return this.creationDate;

	}

	/**
	 * eTags are used to indicate if a resource has been overwritten
	 */
	public String getETag() {
		// If an eTag has been created however we return that one
		if (this.eTag != null && this.eTag.equals("")) {
			return this.eTag;
		}
		Date lm = (this.getLastModified() == null) ? this.getCreationDate()
				: this.getLastModified();
		if (lm == null) {
			lm = new Date();
			// here
			// LOGGER.info("No creation and modified date for "+this.getInternalAddress()+" found");
		}
		// Otherwise we take a weak eTag which is lastModified + content length
		String weakTag = "W/\"" + String.valueOf(this.getContentLength()) + "-"
				+ String.valueOf(lm.getTime()) + "\"";
		// Here
		this.setCreationDate(new Date());
		//
		return weakTag;
	}

	/**
	 * 
	 * @return String the file extension
	 */
	public String getExtension() {
		return this.fileExtension;
	}

	/**
	 * @return Returns the uri.
	 */
	public String getInternalAddress() {
		return this.internalAddress;
	}

	/**
	 * 
	 * @return Date LastModified Date
	 */
	public Date getLastModified() {
		return this.lastModifiedDate;
	}

	/**
	 * @param dateString
	 *            a String that looks like a date
	 * @return date the newly found date
	 */
	private Date getLocaleDate(String dateString) {
		// TODO Check if implementation is clean
		String creatFormat = "dd'/'mm'/'yyyy' 'H':'m':'s"; // "02/02/2006 09:34:20";
		SimpleDateFormat fmt = new SimpleDateFormat(creatFormat, Locale.UK);
		Date tmpDate;
		try {
			tmpDate = fmt.parse(dateString);
		} catch (ParseException e) {
			LOGGER.error(e);
			tmpDate = new Date(); // Default value just in case
		}
		return tmpDate;
	}

	/**
	 * 
	 * @return Vector with DAVResourceObjects
	 */
	protected Vector<IDAVResource> getMembers() {
		return this.members;
	}

	/**
	 * 
	 * @param context
	 *            ServletContext for mime conversion
	 * @return mime type of file
	 */
	public String getMimeType() {

		if (this.mimeType != null && !this.mimeType.equals("")) {
			return this.mimeType;
		}

		String curName = this.getName();
		String returnType = WebDavManager.getManager(null).getMimeType(curName);
		if (returnType == null) {
			returnType = "application/octet-stream";
		}
		LOGGER.debug("Mime type for [" + curName + "] is: \"" + returnType
				+ "\"");

		this.mimeType = returnType;
		return returnType;
	}

	/**
	 * 
	 * @return String Name of the resource - no path included
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * 
	 * @return Outputstream to update resource
	 */
	public abstract OutputStream getOutputStream();

	/**
	 * 
	 * @return Owner of this resource
	 */
	public IDAVRepository getOwner() {
		return this.owner;
	}

	/**
	 * 
	 * @return DAVProperties: all properties of the resource
	 */
	@Deprecated
	public DAVProperties getxxxProperties() {
		return this.properties;
	}

	/**
	 * 
	 * @return href URL of the resource
	 */
	public String getPublicHref() {
		// A huge headache is the possibility to
		// open directories with and without a trailing /
		// so we make sure that we only return the one without
		String result;
		if (this.publicHref == null) {
			this.publicHref = "";
		}
		if (this.publicHref.equals("/")) {
			result = "/"; // Special case for the root
		} else if (this.publicHref.endsWith("/")) {
			int hreflen = this.publicHref.length();
			result = this.publicHref.substring(0, hreflen - 1);
		} else {
			result = this.publicHref;
		}
		return result;
	}

	/**
	 * 
	 * @return repository - the owning repository
	 */
	public IDAVRepository getRepository() {
		return this.getOwner();
	}

	public String getResourceType() {
		return this.resourceType;
	}

	/**
	 * 
	 * @return InputStream - Stream Object to read resource
	 */
	public abstract InputStream getStream();

	/**
	 * 
	 * @return boolean: is it a collection/directory
	 */
	public boolean isCollection() {
		return this.isCollection;
	}

	/**
	 * @return is it a member, so there won't be any sub elements in it
	 */
	public boolean isMember() {
		return this.isMember;
	}

	/**
	 * @return Returns the readOnly.
	 */
	public boolean isReadOnly() {
		return this.readOnly;
	}

	/**
	 * 
	 * @param isCollection
	 *            declare it a collection with true
	 */
	public void setCollection(boolean isCollection) {
		this.isCollection = isCollection;
	}

	/**
	 * 
	 * @param newLength
	 *            Length of content
	 */
	protected void setContentLength(Long newLength) {
		this.contentLenght = newLength;

	}

	/**
	 * 
	 * @param newLengthString
	 *            Length of content
	 */
	protected void setContentLength(String newLengthString) {
		this.contentLenght = new Long(newLengthString);

	}

	/**
	 * 
	 * @param date
	 *            the CreationDate (not supported in Java)
	 */
	public void setCreationDate(Date date) {
		this.creationDate = date;
	}

	/**
	 * 
	 * @param dateString
	 *            String that looks like a CreationDate
	 */
	protected void setCreationDate(String dateString) {
		this.creationDate = this.getLocaleDate(dateString);
	}

	/**
	 * 
	 * @param newExtension
	 *            String the file extension
	 */
	protected void setExtension(String newExtension) {
		this.fileExtension = newExtension;
	}

	public boolean setInternalAddress(String uri) {
		this.internalAddress = uri;
		return true;
	}

	/**
	 * 
	 * @param date
	 *            Date LastModified Date
	 */
	public void setLastModified(Date date) {
		this.lastModifiedDate = date;
	}

	/**
	 * 
	 * @param dateString
	 *            String - Something that looks like a Date
	 */
	protected void setLastModified(String dateString) {
		this.lastModifiedDate = this.getLocaleDate(dateString);
	}

	/**
	 * 
	 * @param isMember
	 *            Make it a member
	 */
	public void setMember(boolean isMember) {
		this.isMember = isMember;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVResource#setMembers(java.util.Vector)
	 */
	public void setMembers(Vector<IDAVResource> members) {
		this.members = members;
	}

	public void setMimeType(String newMimeType) {
		this.mimeType = newMimeType;
	}

	/**
	 * 
	 * @param name
	 *            name of the resource
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 
	 * @param owner
	 *            Owner of this resource
	 */
	public void setOwner(IDAVRepository owner) {
		this.owner = owner;
	}

	/**
	 * 
	 * @param properties
	 *            all properties of the resource
	 */
	@Deprecated
	protected void setxxxProperties(DAVProperties properties) {
		this.properties = properties;
	}

	/**
	 * 
	 * @param href
	 *            URL of the resource
	 */
	public void setPublicHref(String href) {
		this.publicHref = href;
	}

	/**
	 * @param readOnly
	 *            The readOnly to set.
	 */
	protected void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public void setResourceType(String type) {
		this.resourceType = type;

	}

	// public void embed(){}
}
