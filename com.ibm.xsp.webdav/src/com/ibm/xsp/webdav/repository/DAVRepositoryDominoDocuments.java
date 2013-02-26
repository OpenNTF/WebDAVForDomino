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
import biz.taoconsulting.dominodav.interfaces.IDAVResource;
//import biz.taoconsulting.dominodav.resource.DAVAbstractResource;
import biz.taoconsulting.dominodav.repository.DAVRepositoryListing;

import com.ibm.xsp.webdav.WebDavManager;
import com.ibm.xsp.webdav.domino.DominoProxy;
import com.ibm.xsp.webdav.resource.DAVResourceDominoDocuments;

/**
 * 
 * Repository to access Attachments in documents in a given Domino view
 * attachments
 * 
 * @author Stephan H. Wissel
 * 
 */
public class DAVRepositoryDominoDocuments extends DAVRepositoryDomino implements
		IDAVRepository {

	/**
	 * Logger for Errors
	 */
	private static final Log LOGGER = LogFactory
			.getLog(DAVRepositoryDominoDocuments.class);

	/**
	 * Default constructor for use with reflection
	 * 
	 */
	public DAVRepositoryDominoDocuments() {
		// DAVRepositoryMETA.getRepository("portal").;
		// DAVRepositoryMETA.getRepository("portal")

		LOGGER.debug("Empty Domino Attachment Repository created");

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

		DAVResourceDominoDocuments result = (new DAVResourceDominoDocuments(
				this, requestURI));

		if ("NotesDocument".equals(result.getResourceType())) {
			// A Notes document is always a collection in the
			// AttachmentRepository!
			result.setCollection(true);
		} else {
			// if((!requestURI.equals(this.getPublicHref()))&&this.getInternalAddress().equals(this.getInternalAddressFromExternalUrl(requestURI,
			// null))){
			// result =null;
			// throw new DAVNotFoundException();

			// }
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
		// // LOGGER.info("getResource for request uri="+requestURI);
		if (requestURI.endsWith(DAVRepositoryMETA.FOLDER_INFOFILE)) {
			return this.getDesktopIni();
		}

		DAVResourceDominoDocuments result = (new DAVResourceDominoDocuments(
				this, requestURI, b));

		if ("NotesDocument".equals(result.getResourceType())) {
			// A Notes document is always a collection in the
			// AttachmentRepository!
			result.setCollection(true);
		} else {
			// if((!requestURI.equals(this.getPublicHref()))&&this.getInternalAddress().equals(this.getInternalAddressFromExternalUrl(requestURI,
			// null))){
			// result =null;
			// throw new DAVNotFoundException();

			// }
		}
		// //
		// LOGGER.info("New resource: isMember="+((result.isMember())?"true":"false")+"; isCollection="+((result.isCollection()?"true":"false"))+
		// "; type="+result.getResourceType()+"; requestURI="+requestURI);

		return (IDAVResource) result;
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
		// // LOGGER.info("New object request: uRI="+requestURI);
		// TODO Implement
		DAVResourceDominoDocuments res;
		try {
			res = new DAVResourceDominoDocuments(this, requestURI, true, true);
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
		// // LOGGER.info("New object request: uRI="+requestURI);
		// TODO Implement
		DAVResourceDominoDocuments res;
		try {
			res = new DAVResourceDominoDocuments(this, requestURI, false, true);
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
		DAVResourceDominoDocuments resourceSrc = null, resourceDest = null;
		try {
			resourceSrc = (DAVResourceDominoDocuments) this.getResource(from);
		} catch (DAVNotFoundException dnfe) {
			resourceSrc = null;
		}
		if (resourceSrc == null) {
			return 204;
		}

		try {
			resourceDest = (DAVResourceDominoDocuments) this.getResource(to);
		} catch (DAVNotFoundException dnfe) {
			resourceDest = null;
		}
		if (resourceDest != null) {
			return 424;
		}
		try {
			resourceDest = (DAVResourceDominoDocuments) this.getResource(to,
					true);
		} catch (DAVNotFoundException dnfe) {
			resourceDest = null;
		}
		if (resourceDest != null) {
			return 502;
		}
		// resourcedest is OK
		Document docSrc = null;
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
						docSrc.save();
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
		// //
		// LOGGER.info("Start externalURLraw="+externalURLraw+"; Callee="+callee);
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
		String intAdr = repAdr.getPublicHref();
		if (externalURLraw.endsWith("/")) {
			if ((!intAdr.endsWith("/"))
					&& (externalURLraw.equals(intAdr + "/"))) {
				intAdr += "/";
			}
		}
		// // LOGGER.info("Internal address="+intAdr);
		String restPath = null;
		if (externalURLraw.indexOf(intAdr) >= 0) {
			restPath = externalURLraw.substring(intAdr.length());
			// // LOGGER.info("Traverse path "+restPath);
			if (restPath.indexOf("/") == 0) {
				// // LOGGER.info("Split ..");
				String[] part = restPath.split("/");
				for (int i = 0; i < part.length; i++) {
					// // LOGGER.info("Part["+new
					// Integer(i).toString()+"]=#"+part[i]+"#");
				}
				if (restPath.endsWith("/")) { // is a directory
					// // LOGGER.info("1.Is a directory");
					if (part.length > 1) {
						// // LOGGER.info("1.1 part.length>1");
						Base notesObj = DominoProxy.resolve(repAdr
								.getInternalAddress());
						if (notesObj == null) {
							// //
							// LOGGER.info("1.1.1 Obj resolved by proxy=null");
							return "";
							// repAdr.getInternalAddress();
						}
						// // LOGGER.info("1.2 Start for...");
						for (int i = 1; i < part.length; i++) {
							notesObj = findDocumentInCollection(notesObj,
									part[i], false);
							// // LOGGER.info("1.2.1 In for after i="+new
							// Integer(i).toString());
						} // end for
							// // LOGGER.info("1.3 End for");
						if (notesObj instanceof Document) {
							// // LOGGER.info("1.3.1 Instance is a Document");
							Document doc = (Document) notesObj;
							try {
								// //
								// LOGGER.info("1.3.1.1 In try; doc unid ="+doc.getUniversalID());
								return repAdr.getInternalAddress() + "/"
										+ doc.getUniversalID();
							} catch (NotesException e) {
							}
						}

					}
				} else { // is a file but not sure
							// // LOGGER.info("2. Is a file ");
					if (part.length > 1) {
						// // LOGGER.info("2.1 part.length>1");
						Base notesObj = DominoProxy.resolve(repAdr
								.getInternalAddress());
						if (notesObj == null) {
							// //
							// LOGGER.info("2.1.1 obj resolved by proxy is null");
							return "";
							// repAdr.getInternalAddress();
						}
						// // LOGGER.info("2.2 Start for ...");
						for (int i = 1; i < part.length - 1; i++) {
							notesObj = findDocumentInCollection(notesObj,
									part[i], false);
							// // LOGGER.info("2.2.1 In for after i="+new
							// Integer(i).toString());
						}
						// // LOGGER.info("2.3 End for");
						Base notesObj1 = findDocumentInCollection(notesObj,
								part[part.length - 1], true);
						if (notesObj1 == null) {
							notesObj = findDocumentInCollection(notesObj,
									part[part.length - 1], false);
						} else {
							notesObj = notesObj1;
						}
						if (notesObj == null) {
							// // LOGGER.info("2.4 Error Object is null");
							return "";
							// repAdr.getInternalAddress();
						}
						// // LOGGER.info("2.5 Obj is not null ");
						if (notesObj instanceof Document) {
							// //
							// LOGGER.info("2.6 Obj is a Document instance of");
							Document doc = (Document) notesObj;
							try {
								if (doc.hasEmbedded()) {
									// //
									// LOGGER.info(repAdr.getInternalAddress()+"/"+
									// doc.getUniversalID()+"/$File/"+part[part.length-1]);
									return repAdr.getInternalAddress() + "/"
											+ doc.getUniversalID() + "/$File/"
											+ part[part.length - 1];
								} else {
									// //
									// LOGGER.info(repAdr.getInternalAddress()+"/"+
									// doc.getUniversalID());
									return repAdr.getInternalAddress() + "/"
											+ doc.getUniversalID();
								}
							} catch (NotesException e) {
							}

						}

					} else { // part.length==1
						Base notesObj = DominoProxy.resolve(repAdr
								.getInternalAddress());
						if (notesObj == null) {
							LOGGER.error("5.1 Obj resolved by proxy=null");
							return "";
							// repAdr.getInternalAddress();
						}
						Base notesObj1 = findDocumentInCollection(notesObj,
								part[part.length - 1], false);
						if (notesObj1 == null) {
							notesObj = findDocumentInCollection(notesObj,
									part[part.length - 1], true);
						} else {
							notesObj = notesObj1;
						}
						if (notesObj == null) {
							LOGGER.error("5.1 Obj find In Collection=null");
							return "";
							// repAdr.getInternalAddress();
						}
						Document doc = (Document) notesObj;
						try {
							if (doc.hasEmbedded()) {
								// //
								// LOGGER.info(repAdr.getInternalAddress()+"/"+
								// doc.getUniversalID()+"/$File/"+part[part.length-1]);
								return repAdr.getInternalAddress() + "/"
										+ doc.getUniversalID() + "/$File/"
										+ part[part.length - 1];

							} else {
								// //
								// LOGGER.info(repAdr.getInternalAddress()+"/"+
								// doc.getUniversalID());
								return repAdr.getInternalAddress() + "/"
										+ doc.getUniversalID();
							}
						} catch (NotesException e) {
							LOGGER.error("Error finding object: ");
							return "";
							// repAdr.getInternalAddress();
						}

					}
				}
			}
		}
		// // LOGGER.info("3. Sometring wrong");
		return this.getInternalAddress();
	}

	private Base findDocumentInCollection(Base notesObj, String key,
			boolean isLast) {
		try {
			key = URLDecoder.decode(key, "UTF-8");
			// // LOGGER.info("Start findDocumentInCollection Key="+key);
			if (notesObj instanceof View) {
				// // LOGGER.info("isView");
				View notesView = (View) notesObj;
				if (notesView.getEntryCount() > 0) {
					// // LOGGER.info("Has "+new
					// Integer(notesView.getEntryCount()).toString()+" entries");
					for (int i = 0; i < notesView.getEntryCount(); i++) {
						Document doc = notesView.getNthDocument(i + 1);
						if (doc != null) {
							// // LOGGER.info("Doc "+new
							// Integer(i).toString()+" is not null and has Unid="+doc.getUniversalID());
							if ((isLast) && (doc.hasEmbedded())) {
								// // LOGGER.info("Doc has embedded");
								@SuppressWarnings("rawtypes")
								Vector allEmbedded = DominoProxy.evaluate(
										"@AttachmentNames", doc);
								String curAttName = allEmbedded.get(0)
										.toString();
								// // LOGGER.info("Embedded name="+curAttName);
								if (curAttName.equals(key)) {
									// //
									// LOGGER.info("Has view with doc unid="+doc.getUniversalID());
									return doc;
								}

							} else {
								if (!isLast) {
									if (doc.getItemValueString(
											getDirectoryField()).equals(key)) {
										return doc;
									}
								}
							}

						}
					}
				}

			} else {
				if (notesObj instanceof Document) {
					// // LOGGER.info("isDoc");
					Document doc = (Document) notesObj;
					DocumentCollection docColl = doc.getResponses();
					if (docColl.getCount() > 0) {
						// // LOGGER.info("Has "+new
						// Integer(docColl.getCount()).toString()+" responses");
						for (int i = 0; i < docColl.getCount(); i++) {
							Document docResp = docColl.getNthDocument(i + 1);
							if (docResp != null) {
								if ((isLast) && (docResp.hasEmbedded())) {
									// //
									// LOGGER.info("DocResp "+docResp.getUniversalID()+"has embedded");
									@SuppressWarnings("rawtypes")
									Vector allEmbedded = DominoProxy.evaluate(
											"@AttachmentNames", docResp);
									String curAttName = allEmbedded.get(0)
											.toString();
									// // LOGGER.info("Curattname="+curAttName);
									if (curAttName.equals(key)) {
										// //
										// LOGGER.info("Has doc resp  with doc unid="+docResp.getUniversalID());
										return docResp;
									}

								} else {
									if (!isLast) {
										if (docResp.getItemValueString(
												getDirectoryField())
												.equals(key)) {
											return docResp;
										}
									}
								}

							}
						}
					}
				}
			}
		} catch (NotesException ne) {
			LOGGER.error("Error:" + ne.getMessage());
		} catch (UnsupportedEncodingException ue) {
			LOGGER.error("Error:" + ue.getMessage());
		}
		return null;
	}

	public boolean versioning() {
		String versioning = getAdditionalParameterValue("Version")
				.toLowerCase();
		if (versioning.equals("true") || versioning.equals("yes")) {
			return true;
		}
		return false;
	}

	public String getPubHrefField() {
		String pubHrefName = getAdditionalParameterValue("PubHrefName");
		pubHrefName = (pubHrefName.equals("")) ? "DAVPubHref" : pubHrefName;
		LOGGER.info("PubHrefFieldName=" + pubHrefName);
		return pubHrefName;
	}

	public String getDirectoryField() {
		return getAdditionalParameterValue("DirectoryName");
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

	public String getFilter() {
		String filter = getAdditionalParameterValue("Filter");
		return filter;
	}
}
