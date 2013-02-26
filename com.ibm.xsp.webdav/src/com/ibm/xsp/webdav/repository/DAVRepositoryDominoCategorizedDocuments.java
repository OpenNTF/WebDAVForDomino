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
package com.ibm.xsp.webdav.repository;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.Map.Entry;

import lotus.domino.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import biz.taoconsulting.dominodav.exceptions.DAVNotFoundException;
import biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation;
import biz.taoconsulting.dominodav.interfaces.IDAVRepository;
import biz.taoconsulting.dominodav.interfaces.IDAVResource; //import biz.taoconsulting.dominodav.resource.DAVAbstractResource;
import biz.taoconsulting.dominodav.repository.DAVRepositoryListing;

import com.ibm.xsp.webdav.WebDavManager;
import com.ibm.xsp.webdav.domino.DominoProxy;
import com.ibm.xsp.webdav.resource.DAVResourceDominoCategorizedDocuments;

/**
 * 
 * Repository to access Attachments in documents in a given Domino view
 * attachments
 * 
 * @author Stephan H. Wissel
 * 
 */
public class DAVRepositoryDominoCategorizedDocuments extends
		DAVRepositoryDomino implements IDAVRepository {

	/**
	 * Logger for Errors
	 */
	private static final Log LOGGER = LogFactory
			.getLog(DAVRepositoryDominoDocuments.class);

	/**
	 * Default constructor for use with reflection
	 * 
	 */
	public DAVRepositoryDominoCategorizedDocuments() {
		// DAVRepositoryMETA.getRepository("portal").;
		// DAVRepositoryMETA.getRepository("portal")

		LOGGER.info("Empty Domino Attachment Repository created");

	}

	/**
	 * @see biz.taoconsulting.dominodav.repository.AbstractStreamBasedRepository#getResource(java.lang.String)
	 */
	public IDAVResource getResource(String requestURI)
			throws DAVNotFoundException {

		// Intercepting requests for desktop.ini to please Windows7 explorer
		// // LOGGER.info("Start getResource with requestURI="+requestURI);
		if (requestURI.endsWith(DAVRepositoryMETA.FOLDER_INFOFILE)) {
			return this.getDesktopIni();
		}

		DAVResourceDominoCategorizedDocuments result = (new DAVResourceDominoCategorizedDocuments(
				this, requestURI));

		if (result.getInternalAddress().equals("")) {
			return null;
		}

		if ("NotesDocument".equals(result.getResourceType())) {
			result.setCollection(true);
		} else {
		}
		// //
		// LOGGER.info("New resource: isMember="+((result.isMember())?"true":"false")+"; isCollection="+((result.isCollection()?"true":"false"))+
		// "; type="+result.getResourceType()+"; requestURI="+requestURI);
		return (IDAVResource) result;
	}

	/**
	 * @see biz.taoconsulting.dominodav.repository.AbstractStreamBasedRepository#getResource(java.lang.String,
	 *      boolean)
	 */
	public IDAVResource getResource(String requestURI, boolean b)
			throws DAVNotFoundException {

		// Intercepting requests for desktop.ini to please Windows7 explorer
		LOGGER.info("getResource for request uri=" + requestURI);
		if (requestURI.endsWith(DAVRepositoryMETA.FOLDER_INFOFILE)) {
			return this.getDesktopIni();
		}

		DAVResourceDominoCategorizedDocuments result = (new DAVResourceDominoCategorizedDocuments(
				this, requestURI, b));

		if (result.getInternalAddress().equals("")) {
			throw new DAVNotFoundException();
		}

		if ("NotesDocument".equals(result.getResourceType())) {
			// A Notes document is always a collection in the
			// AttachmentRepository!
			result.setCollection(true);
		} else {

		}
		// //
		// LOGGER.info("New resource: isMember="+((result.isMember())?"true":"false")+"; isCollection="+((result.isCollection()?"true":"false"))+
		// "; type="+result.getResourceType()+"; requestURI="+requestURI);

		return (IDAVResource) result;
	}

	public Document getDocumentByKey(String key) {
		LOGGER.info("Get doc by key=" + key);
		Document doc = null;
		if (key.equals("")) {
			LOGGER.info("Error key null");
			return null;
		}
		String notesURL = this.getInternalAddress();
		View vw = DominoProxy.getView(notesURL);
		if (vw == null) {
			LOGGER.info("Error; view returned;");
			return doc;
		}
		try {
			doc = vw.getDocumentByKey(key, true);
		} catch (NotesException ne) {
			LOGGER.info("Key " + key + "not found!");
			return null;
		}
		if (doc != null) {
			LOGGER.info("OK doc found " + key);
		} else {
			LOGGER.info("Doc  not found " + key);
		}
		return doc;
	}

	public DocumentCollection getAllDocumentsByKey(String key) {
		LOGGER.info("Get All doc by key=" + key);
		DocumentCollection doc = null;
		if (key.equals("")) {
			LOGGER.info("Error key null");
			return null;
		}
		String notesURL = this.getInternalAddress();
		View vw = DominoProxy.getView(notesURL);
		if (vw == null) {
			LOGGER.info("Error; view returned;");
			return doc;
		}
		try {
			doc = vw.getAllDocumentsByKey(key, true);
		} catch (NotesException ne) {
			LOGGER.info("Key " + key + "not found!");
			return null;
		}
		LOGGER.info("OK doc collection found " + key);
		return doc;
	}

	public ViewEntryCollection getAllEntriesByKey(String key) {
		LOGGER.info("Get All doc by key=" + key);
		ViewEntryCollection doc = null;
		if (key.equals("")) {
			LOGGER.info("Error key null");
			return null;
		}
		String notesURL = this.getInternalAddress();
		View vw = DominoProxy.getView(notesURL);
		if (vw == null) {
			LOGGER.info("Error; view returned;");
			return doc;
		}
		try {
			doc = vw.getAllEntriesByKey(key, true);
		} catch (NotesException ne) {
			return null;
		}
		LOGGER.info("OK doc collection found " + key);
		return doc;
	}

	/**
	 * Shortcut to a valid desktop.ini file
	 * 
	 * @return
	 * @throws DAVNotFoundException
	 */
	private IDAVResource getDesktopIni() throws DAVNotFoundException {
		WebDAVInternalRepository internal = new WebDAVInternalRepository();
		return internal.getInternalResource(DAVRepositoryMETA.FOLDER_INFOFILE);
	}

	/**
	 * @see biz.taoconsulting.dominodav.repository.AbstractStreamBasedRepository#writeResource(biz.taoconsulting.dominodav.resource.DAVAbstractResource)
	 */
	public void writeResource(IDAVResource resc) {
		// TODO Implement

	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#createNewResource(java.lang.String)
	 */
	public IDAVResource createNewResource(String requestURI) {
		LOGGER.info("New object request: uRI=" + requestURI);
		// TODO Implement
		DAVResourceDominoCategorizedDocuments res;
		try {
			res = new DAVResourceDominoCategorizedDocuments(this, requestURI,
					true, true);
		} catch (DAVNotFoundException dnfe) {
			return null;
		}

		return res;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#createNewCollection(java.lang.String)
	 */
	public IDAVResource createNewCollection(String requestURI) {
		LOGGER.info("New object request: uRI=" + requestURI);
		// TODO Implement
		DAVResourceDominoCategorizedDocuments res;
		try {
			res = new DAVResourceDominoCategorizedDocuments(this, requestURI,
					false, true);
		} catch (DAVNotFoundException dnfe) {
			return null;
		}

		return res;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.interfaces.IDAVRepository#moveResource(java.lang.String,
	 *      java.lang.String)
	 */
	public int moveResource(String from, String to) {
		// TODO Implement
		RichTextItem body = null;
		String pubHref = ((IDAVAddressInformation) this).getPublicHref();
		if (to.indexOf(pubHref) > 0) {
			to = to.substring(to.indexOf(pubHref));
		}
		// // LOGGER.info("NEW DEST="+to);
		if (from.equals(to)) {
			return 403;
		}
		DAVResourceDominoCategorizedDocuments resourceSrc = null, resourceDest = null;
		try {
			resourceSrc = (DAVResourceDominoCategorizedDocuments) this
					.getResource(from);
		} catch (DAVNotFoundException dnfe) {
			resourceSrc = null;
		}
		if (resourceSrc == null) {
			return 204;
		}

		try {
			resourceDest = (DAVResourceDominoCategorizedDocuments) this
					.getResource(to);
		} catch (DAVNotFoundException dnfe) {
			resourceDest = null;
		}
		if (resourceDest != null) {
			return 424;
		}
		try {
			resourceDest = (DAVResourceDominoCategorizedDocuments) this
					.getResource(to, true);
		} catch (DAVNotFoundException dnfe) {
			resourceDest = null;
		}
		if (resourceDest != null) {
			return 502;
		}
		// resourcedest is OK
		Document docSrc = null; // , docDest=null;
		// // LOGGER.info("MOVE SRC="+resourceSrc.getInternalAddress());
		Base notesObj = DominoProxy.resolve(resourceSrc.getInternalAddress());
		if (notesObj == null) {
			return 205;
		}
		if (notesObj instanceof Document) {
			docSrc = (Document) notesObj;
		} else {
			return 206;
		}
		String folderSrc = null, fileSrc = null, folderDest = null, fileDest = null;
		if (resourceSrc.isCollection()) {
			if (from.lastIndexOf("/") > 0) {
				folderSrc = from.substring(0, from.lastIndexOf("/"));
				fileSrc = from.substring(from.lastIndexOf("/") + 1);
			}
			if (to.lastIndexOf("/") > 0) {
				folderDest = to.substring(0, to.lastIndexOf("/"));
				fileDest = to.substring(to.lastIndexOf("/") + 1);
			}
			if (folderSrc.equals(folderDest)) {
				resourceSrc.setName(fileDest);
				try {
					docSrc.replaceItemValue(getDirectoryField(), fileDest);
					docSrc.save(true);
					resourceSrc.setPublicHref(folderSrc + "/" + fileDest);
					Document doc = resourceSrc.getDocument();
					if (doc != null) {
						doc.replaceItemValue(getPubHrefField(),
								folderSrc.substring(pubHref.length()) + "/"
										+ fileDest);
						doc.save(true);
					}
					return 201;
				} catch (NotesException ne) {
				}
			}

		} else { // is file
			if (from.lastIndexOf("/") > 0) {
				folderSrc = from.substring(0, from.lastIndexOf("/"));
				fileSrc = from.substring(from.lastIndexOf("/") + 1);
			}
			if (to.lastIndexOf("/") > 0) {
				folderDest = to.substring(0, to.lastIndexOf("/"));
				fileDest = to.substring(to.lastIndexOf("/") + 1);
			}
			LOGGER.info("File rename; Folders are " + folderSrc + " to "
					+ folderDest + "; Files are " + fileSrc + " to " + fileDest);
			if (folderSrc.equals(folderDest)) {
				LOGGER.info("Same folder");
				resourceSrc.setName(fileDest);
				try {
					EmbeddedObject att = docSrc.getAttachment(fileSrc);
					if (att != null) {
						LOGGER.info("Attachment " + fileSrc + " not null");
						File f = new File(resourceSrc.getTempfile().getParent()
								+ File.separator + fileSrc);
						if (f.exists()) {
							LOGGER.info("File temp "
									+ resourceSrc.getTempfile().getParent()
									+ File.separator + fileSrc + " exist");
							f.delete();
							LOGGER.info("..deleted!");
						}
						LOGGER.info("Start extract ");
						att.extractFile(resourceSrc.getTempfile().getParent()
								+ File.separator + fileSrc);
						LOGGER.info("Extracted!");
						f = new File(resourceSrc.getTempfile().getParent()
								+ File.separator + fileSrc);
						File fd = new File(resourceSrc.getTempfile()
								.getParent() + File.separator + fileDest);
						if (fd.exists()) {
							LOGGER.info("File temp "
									+ resourceSrc.getTempfile().getParent()
									+ File.separator + fileDest + " exist");
							fd.delete();
							LOGGER.info("..deleted!");
						}
						f.renameTo(fd);
						LOGGER.info("renamed " + fileSrc + " to=>" + fileDest);
						if (docSrc.hasItem("Body")) {
							Item bodyCandidate = docSrc.getFirstItem("Body");
							if (bodyCandidate.getType() == Item.RICHTEXT) {
								body = (RichTextItem) bodyCandidate;
							} else {
								// TODO: is this OK or do we need to do
								// something about it?
								docSrc.removeItem("Body");
								body = docSrc.createRichTextItem("Body");
							}
						} else {
							body = docSrc.createRichTextItem("Body");
						}
						LOGGER.info("Body resolved ok!");
						body.embedObject(EmbeddedObject.EMBED_ATTACHMENT, null,
								resourceSrc.getTempfile().getParent()
										+ File.separator + fileDest, fileDest);
						LOGGER.info("FileDest attached");
						att.remove();
						docSrc.replaceItemValue(getPubHrefField(),
								folderSrc.substring(pubHref.length()) + "/"
										+ fileDest);
						docSrc.save();
						resourceSrc.updateHierarchy();
						LOGGER.info("Saved doc");
						fd.delete();
						LOGGER.info("Temp file deleted");

						att.recycle();
						att = null;
						docSrc.recycle();
						LOGGER.info("Finish rename file");
					}
					resourceSrc.setPublicHref(folderSrc + "/" + fileDest);
					LOGGER.info("New pubhref=" + resourceSrc.getPublicHref());
					return 201;
				} catch (NotesException ne) {
					return 401;
				}
			}

		}

		return 412;
	}

	public String getInternalAddressFromExternalUrl(String externalURLraw,
			String callee) {
		// The repository address to "translate from external to internal
		// address
		LOGGER.info("Start externalURLraw=" + externalURLraw + "; Callee="
				+ callee);
		String externalURL = null;

		if (externalURLraw != null) {
			try {
				externalURL = URLDecoder.decode(externalURLraw, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				LOGGER.error(e);
				externalURL = externalURLraw; // We take it unencoded then
			}
		}
		LOGGER.info("externalURL=" + externalURL);
		IDAVAddressInformation repAdr = (IDAVAddressInformation) this;
		String key = externalURLraw, file = "", repAdrS = repAdr
				.getPublicHref();
		if (externalURLraw.equals("")) {
			return repAdr.getInternalAddress();
		}
		if (externalURLraw.lastIndexOf("/") >= 0) {
			file = externalURLraw.substring(externalURLraw.lastIndexOf("/"));
		}
		if (file.startsWith("/")) {
			file = file.substring(1);
		}
		if (externalURLraw.startsWith(repAdrS)) {
			key = externalURLraw.substring(repAdrS.length());
		}
		LOGGER.info("RepAdr=" + repAdrS + ";key=" + key);
		if ((key != null) && (!key.equals(""))) {
			Document doc = getDocumentByKey(key);
			if (doc != null) {
				LOGGER.info("doc not null");
				try {
					if (doc.hasEmbedded()) {
						LOGGER.info(this.getInternalAddress() + "/"
								+ doc.getUniversalID() + "/$File/" + file);
						return this.getInternalAddress() + "/"
								+ doc.getUniversalID() + "/$File/" + file;
					} else {
						LOGGER.info("Internal address for " + key + "="
								+ this.getInternalAddress() + "/"
								+ doc.getUniversalID());
						return this.getInternalAddress() + "/"
								+ doc.getUniversalID();
					}
				} catch (NotesException ne) {
					LOGGER.info("Error " + ne.getMessage());
				}
			} else {
				LOGGER.info("Error; resource " + key + " not found!");
				return "";
			}
		}
		LOGGER.info("Internal address for " + key + "="
				+ this.getInternalAddress());
		return this.getInternalAddress();
	}

	public String getDirectoryField() {
		return getAdditionalParameterValue("DirectoryName");
	}

	public String getPubHrefField() {
		String pubHrefName = getAdditionalParameterValue("PubHrefName");
		pubHrefName = (pubHrefName.equals("")) ? "DAVPubHref" : pubHrefName;
		LOGGER.info("PubHrefFieldName=" + pubHrefName);
		return pubHrefName;
	}

	public String getFilter() {
		String filter = getAdditionalParameterValue("Filter");
		return filter;
	}

	public boolean versioning() {
		String versioning = getAdditionalParameterValue("Version")
				.toLowerCase();
		if (versioning.equals("true") || versioning.equals("yes")) {
			return true;
		}
		return false;
	}

	public String getFormName() {
		return getAdditionalParameterValue("Form");
	}

	public String getFileFormName() {
		String fileForm = getAdditionalParameterValue("FileForm");
		return (fileForm.equals("")) ? getAdditionalParameterValue("Form")
				: fileForm;
	}

	public String getDirectoryFormName() {
		String directoryForm = getAdditionalParameterValue("DirectoryForm");
		return (directoryForm.equals("")) ? getAdditionalParameterValue("Form")
				: directoryForm;
	}

	public String getAdditionalParameterValue(String key) {
		DAVRepositoryMETA drm = WebDavManager.getManager(null)
				.getRepositoryMeta();
		if (drm == null) {
			// // LOGGER.info("Error; dm is null");
			return "";
		}
		DAVRepositoryListing drl;
		HashMap<String, DAVRepositoryListing> hm = drm.getRepositoryList();
		IDAVAddressInformation repAdr = (IDAVAddressInformation) this;
		String intAdr = repAdr.getPublicHref();
		String repName = intAdr;
		// // LOGGER.info("Pub Href="+repName);
		String[] part = repName.split("/");
		for (int i = 0; i < part.length; i++) {
			// // LOGGER.info("Part["+new Integer(i).toString()+"]="+part[i]);
		}
		drl = null;
		if (part.length > 2) {
			drl = hm.get(part[2]);
			if (drl == null) {
				// // LOGGER.info("Error; drl is null for repositor ##");
				return "";
			}
			HashMap<String, String> additionalParameters = drl
					.getAdditionalParameters();
			if (additionalParameters == null) {
				// // LOGGER.info("Error; additionalParameters is null");
				return "";
			}

			Iterator<Entry<String, String>> i = additionalParameters.entrySet()
					.iterator();
			while (i.hasNext()) {
				Map.Entry<String, String> me = (Map.Entry<String, String>) i
						.next();
				if (me.getKey().toString().equals(key)) {
					return me.getValue().toString();
				}
				// // LOGGER.info("MAP name="+
				// me.getKey().toString()+"  value="+me.getValue().toString());
			}
		}

		return "";
	}
}
