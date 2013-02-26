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

import java.io.File;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Vector;

import lotus.domino.ACL;
import lotus.domino.Base;
import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.Item;
import lotus.domino.Name;
import lotus.domino.NotesException;
import lotus.domino.Session;
import lotus.domino.View;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import biz.taoconsulting.dominodav.exceptions.DAVNotFoundException;
import biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation;
import biz.taoconsulting.dominodav.interfaces.IDAVRepository;
import biz.taoconsulting.dominodav.resource.DAVAbstractResource;

import com.ibm.xsp.webdav.WebDavManager;
import com.ibm.xsp.webdav.domino.DominoProxy;
import com.ibm.xsp.webdav.repository.DAVRepositoryDomino;
import com.ibm.xsp.webdav.repository.DAVRepositoryMETA;

/**
 * Abstract class that is the base for all Domino related resources:
 * Attachments, Documents, calendar, view etc.
 * 
 * @author Stephan H. Wissel
 * 
 */
public abstract class DAVResourceDomino extends DAVAbstractResource {

	/**
	 * The outputstream leading to the temp file or other stream resource
	 */
	protected OutputStream out;

	/**
	 * Logger for Errors
	 */
	private static final Log LOGGER = LogFactory
			.getLog(DAVResourceDomino.class);

	/**
	 * The document UniqueID for document and attachment resources;
	 */
	private String documentUniqueID = null;

	public DAVResourceDomino() {
		// Needed for type casting
	}

	/**
	 * 
	 * @param repository
	 *            the repository the Resource is in
	 * @param url
	 *            the path relative to the repository -- as seen from the
	 *            browser
	 * @throws DAVNotFoundException
	 *             --- if the file is not there
	 */
	public DAVResourceDomino(IDAVRepository repository, String url)
			throws DAVNotFoundException {

		this.setup(repository, url, false);

	}

	/**
	 * 
	 * @param repository
	 *            the repository the Resource is in
	 * @param url
	 *            the path relative to the repository -- as seen from the
	 *            browser
	 * @param isMember
	 *            : is is a file in a sub directory
	 * @throws DAVNotFoundException
	 *             --- if the file is not there
	 */
	public DAVResourceDomino(IDAVRepository repository, String url,
			boolean isMember) throws DAVNotFoundException {
		this.setup(repository, url, isMember);
	}

	/**
	 * The function checks if a document is read-only for the current user. Step
	 * 1: get the user access level, if Reader = ReadOnly, If Editor = ReadWrite
	 * if Author = Step 2 Step 2: Get UsernameList for current user and loop
	 * through the document to get all items. If a item is an author field, then
	 * add it to the list, loop the list to see the access.
	 * 
	 * @param s
	 *            Notessession
	 * @param curDoc
	 *            Document to evaluate
	 * @return true if ReadOnly Access is granted
	 */
	protected boolean checkReadOnlyAccess(Document curDoc) {

		boolean result = true; // Readonly until proven better
		HashMap<String, String> allAuthors = new HashMap<String, String>();
		Vector<String> v = null;
		@SuppressWarnings("rawtypes")
		Vector allItems = null;
		String unid = null;

		try {
			unid = curDoc.getUniversalID();
			// Step 1 --- Database access
			@SuppressWarnings("unused")
			Session s = DominoProxy.getUserSession();
			Database db = curDoc.getParentDatabase();
			int accessLevel = db.getCurrentAccessLevel();
			LOGGER.info("Starting check access for docunid=" + unid);
			if (accessLevel == ACL.LEVEL_EDITOR
					|| accessLevel == ACL.LEVEL_DESIGNER
					|| accessLevel == ACL.LEVEL_MANAGER) {
				result = false; // it is not read only, it can be processed
				// // LOGGER.info("Access level is Editor or better");

			} else if (accessLevel == ACL.LEVEL_AUTHOR) {
				// Step 2 --- Document access
				// //
				// LOGGER.info("Access level is Author, checking local access for:"
				// +
				// DominoProxy.getUserName()+"; effective="+s.getEffectiveUserName());

				allItems = curDoc.getItems();
				Item item;
				for (int i = 0; i < allItems.size(); i++) {
					item = (Item) allItems.get(i);
					if (item != null && item.isAuthors()) {
						// Record it to the Map
						@SuppressWarnings("rawtypes")
						Vector values = item.getValues();
						if (values != null) {
							for (int x = 0; x < values.size(); x++) {
								if (values.get(x) != null) {
									String curAuthor = (String) values.get(x);
									// // LOGGER.info("Author found in " +
									// item.getName() + ": " + curAuthor);
									allAuthors.put(curAuthor.toLowerCase(),
											curAuthor.toLowerCase());
								}
							}
						}
					}
					Item itemO = item;
					item = null;
					itemO.recycle();
				}

				// //
				// LOGGER.info("-- Author retrieval completed, now checking usernameslist --");

				// Might not work, if the db is local and consisten acl
				// is not enforced, so we add username and common username
				v = this.getUsernamesList(curDoc);

				if (v == null) {
					// Get Username list has failed, we can't allow editing of
					// the file
					result = true; // READ ONLY
					// // LOGGER.info("getUsernamesList returned NULL");
				} else {
					// Now check the userlist
					for (int i = 0; i < v.size(); i++) {
						if (v.get(i) != null) {
							String curName = v.get(i);
							// // LOGGER.info("Checking access for:" + curName);
							if (!curName.equals("")) {
								if (allAuthors.containsKey(curName
										.toLowerCase())) {
									result = false; // We can write!
									// // LOGGER.info("Author access found for "
									// + curName);
									break;
								}
							}
						}
					}
				}

			}

		} catch (NotesException e) {
			// // LOGGER.info("ReadOnlyAccess Check failed", e);
			result = true; // It is readonly if the check failed
		}
		// // LOGGER.info("Document "+unid+
		// ((result)?" readonly":" normal access"));
		return result;
	}

	/**
	 * Extracts the UNID (16) from the internal url. String 16 chars long
	 * 
	 * @return the UNID
	 */
	private String extractUNIDfromInternalAddress() {
		String result = null;
		String[] parts = this.getInternalAddress().split("/");
		for (int i = 0; i < parts.length; i++) {
			if (parts[i].length() == 32) {
				result = parts[i];
				break;
			}
		}
		return result;
	}

	/**
	 * Get the members of the Domino resource. Most likely traversing a view
	 */
	public abstract void fetchChildren();

	public String getDocumentUniqueID() {
		if (this.documentUniqueID == null || this.documentUniqueID == "") {
			this.documentUniqueID = this.extractUNIDfromInternalAddress();
		}
		return documentUniqueID;
	}

	// This function gets overwritten by sub functions
	protected String getNameFromInternalAddress(String internalName)
			throws Exception {
		throw new Exception(
				"The function getNameFromInternalAddress must be overwritten");
	}

	/**
	 * @return A temp file if we need to write into the file system e.g. for DXL
	 *         or attachments. Needs to be overwritten
	 */
	public abstract File getTempfile();

	/**
	 * Retrieves the current associations with roles and groups
	 * 
	 * @param s
	 *            NotesSession
	 * @param doc
	 *            NotesDocument
	 * @return List of Names and Groups
	 */
	@SuppressWarnings("unchecked")
	protected Vector<String> getUsernamesList(Document doc) {
		Vector<String> retVector;
		Name nName = null;
		Session s = DominoProxy.getUserSession();

		try {
			nName = s.createName(s.getEffectiveUserName());
		} catch (NotesException e1) {
			LOGGER.error("getEffectiveUsernamesList Name resolution failed", e1);
		}

		try {
			retVector = s.evaluate("@Usernameslist", doc);
		} catch (NotesException e) {
			LOGGER.error("@Usernameslist failed", e);
			retVector = new Vector<String>();
			retVector.add("");
		}

		try {
			// Eventually retvector has only an empty value, which we consider
			// an error
			if (retVector.get(0).equals("")) {
				LOGGER.equals("@Usernameslist for " + nName.getAbbreviated()
						+ " failed");
			}

		} catch (NotesException e) {
			LOGGER.error("getEffectiveUsername() failed", e);
		}

		try {
			retVector.add(s.getEffectiveUserName());
			retVector.add(nName.getAbbreviated());
		} catch (NotesException e) {
			LOGGER.error("getUsername() failed", e);
		}

		return retVector;
	}

	public void setDocumentUniqueID(String documentUniqueID) {
		this.documentUniqueID = documentUniqueID;
	}

	/**
	 * Based on the internal URL we figure the resource type
	 */
	protected void setDominoResourceType() {
		String url = this.getInternalAddress().trim();

		String openArgument = null;
		int questPos = url.indexOf("?");

		if (questPos < 0) {
			if (url.endsWith(".nsf") || url.endsWith(".nsf/")) {
				this.setResourceType("NotesDatabase");
			} else if (!(url.indexOf("$File") < 0)) {
				this.setResourceType("NotesAttachment");
				this.setMember(true); // Attachments are always members!
			} else if (this.twoSlashAfterNsf(url)) {
				this.setResourceType("NotesDocument");
			} else {
				this.setResourceType("NotesView");
			}
		} else {
			openArgument = url.substring(questPos + 1) + "&";
			openArgument = openArgument.substring(0, openArgument.indexOf("&"));

			if (openArgument.equals("OpenDatabase")) {
				this.setResourceType("NotesDatabase");
			} else if (openArgument.equals("OpenDocument")) {
				this.setResourceType("NotesDocument");
			} else if (openArgument.equals("OpenView")) {
				this.setResourceType("NotesView");
			}
		}
	}

	/**
	 * @param rep
	 *            the containing repository
	 * @param url
	 *            the access to the resource from the browser relative to the
	 *            repository
	 * @throws DAVNotFoundException
	 *             -- we might ask for an non-existing resource
	 */
	private void setup(IDAVRepository rep, String url, boolean isMember)
			throws DAVNotFoundException {

		// Store a link to the repository
		this.setOwner(rep);

		// // LOGGER.info("DAVResouceDomino for "+url);

		// The path can't be null and can't be empty. if it is empty we use "/"
		if (url == null || url.equals("")) {
			url = new String("/");
		}

		if (url.equals("/")) {
			this.setCollection(true);
		} else {
			this.setCollection(false);
		}

		// Memorize the url requested
		this.setPublicHref(url);

		// Get the file-path and a new file
		// // LOGGER.info("Input url="+url);
		String fpath = rep.getInternalAddressFromExternalUrl(url,
				"DAVREsourceDomino-setup");
		// Keep the address
		this.setInternalAddress(fpath);
		// // LOGGER.info("Output url="+fpath);

		// FIXME XXX TODO Borked code!
		// Next check -- if Repository and Resource have the same path then the
		// resource it top level
		if (fpath.equals(((IDAVAddressInformation) rep).getInternalAddress())) {
			LOGGER.debug("Repository and Resource address match:" + fpath);
			this.setName(((IDAVAddressInformation) rep).getName());
			LOGGER.debug(this.getName() + " is the repository-resource at "
					+ this.getInternalAddress());
			this.setCollection(true);
			this.setResourceType("DominoRepository");
			this.setMember(false);
		} else {
			// It is a notes artifact
			String newName = null;
			try {
				newName = this.getNameFromInternalAddress(this
						.getInternalAddress());
			} catch (Exception e) {
				LOGGER.error(e);
			}
			this.setName(newName);
			this.setDominoResourceType();
			/*
			 * this.setMember(isMember);
			 * if(!this.getResourceType().equals("NotesAttachment")){
			 * this.setCollection(true); }else{ this.setMember(true);
			 * this.setCollection(false); }
			 */
			this.validateResourceExists();

			// TODO: figure out read/write access
			this.setReadOnly(false);
			LOGGER.debug(this.getName() + " is a " + this.getResourceType()
					+ " resource at " + this.getInternalAddress());
		}
		// this.setMember(isMember);
		if (!this.isMember()) {
			// search for members (children)
			this.fetchChildren();
		}
	}

	/**
	 * Checks if a resource really exists
	 * 
	 * @throws DAVNotFoundException
	 */
	protected void validateResourceExists() throws DAVNotFoundException {

		Session s = DominoProxy.getUserSession();
		String rt = this.getResourceType();

		// TODO: Do we need to check for empty?
		if (rt == null) {
			return;
		}
		String adr = this.getInternalAddress();

		if (rt.equals("NotesAttachment")) {
			adr = adr.substring(0, adr.indexOf("/$File"));
		}

		// If it doesn't resolve it doesn't exist!
		try {
			s.resolve(adr);
		} catch (NotesException e) {
			throw new DAVNotFoundException();
		}

	}

	/**
	 * A notes document has two slashes after the .nsf: .nsf/viewname/docid
	 * 
	 * @param url
	 * @return did we find 2 slashes
	 */
	private boolean twoSlashAfterNsf(String url) {

		int whereIsNsf = url.indexOf(".nsf");
		if (whereIsNsf < 0) {
			return false;
		}
		String inspect = url.substring(whereIsNsf);
		return (inspect.indexOf("/") != inspect.lastIndexOf("/"));
	}

	public boolean filter() {
		boolean ret = true;
		// // LOGGER.info("Start filter ");
		DAVRepositoryDomino rep = (DAVRepositoryDomino) this.getRepository();
		String filter = rep.getFilter();
		if (filter.equals("")) {
			// LOGGER.info("Filter null");
			return true;
		}
		// LOGGER.info("Filter is "+filter);
		Document doc = getDocument();
		if (doc == null) {
			// LOGGER.info("Get Document is null");
			return ret;
		}
		try {
			DAVRepositoryMETA drm = WebDavManager.getManager(null)
					.getRepositoryMeta();
			if (drm == null) {
				return true;
			}
			Base repDoc = (Base) DominoProxy.resolve(drm
					.getRepositoryConfigLocation());
			if (repDoc != null) {
				// LOGGER.info("Rep internal address not null; has class="+repDoc.getClass().toString());
				Database db = null;
				if (repDoc instanceof Document) {
					// LOGGER.info("rep doc is a Document");
					db = ((Document) repDoc).getParentDatabase();
				}
				if (repDoc instanceof View) {
					// LOGGER.info("repdoc is a view");
					View vw = ((View) repDoc);
					db = (Database) vw.getParent();
				}
				if (db != null) {
					// LOGGER.info("Parent database not null");
					Document docP = db.createDocument();
					// LOGGER.info("Create document parameters...");
					docP.replaceItemValue("Form", "webDavParameters");
					docP.computeWithForm(true, false);
					// LOGGER.info("Compute with form OK!");
					@SuppressWarnings("rawtypes")
					Vector items = docP.getItems();
					for (int i = 0; i < items.size(); i++) {
						Item itm = (Item) items.get(i);
						// LOGGER.info("Item "+itm.getName()+"="+itm.getValueString());
						filter = filter.replaceAll("\\x5B\\x5B" + itm.getName()
								+ "\\x5D\\x5D", "\"" + itm.getValueString()
								+ "\"");
					}
					if (!filter.equals("")) {
						// LOGGER.info("Filter="+filter);
						Session ses = DominoProxy.getUserSession();
						@SuppressWarnings("rawtypes")
						Vector eval = ses.evaluate(filter, doc);
						if (eval == null) {
							return true;
						}
						String retS = eval.firstElement().toString()
								.toLowerCase();
						// LOGGER.info("Evaluate result="+retS);
						if (retS.equals("1.0")) {
							// LOGGER.info("Filter pass OK!");
							return true;
						} else {
							// LOGGER.info("Filter didn't pass");
							return false;
						}
					}
				}
			}

		} catch (NotesException ne) {
			LOGGER.error("Error on filter;" + ne.getMessage());
			return ret;
		}
		return ret;
	}

	public abstract Document getDocument();
}
