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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Vector;

import lotus.domino.Base;
import lotus.domino.Database;
import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.DocumentCollection;
import lotus.domino.EmbeddedObject;
import lotus.domino.Item;
import lotus.domino.NotesException;
import lotus.domino.Session;
import lotus.domino.View;
import lotus.domino.ViewEntryCollection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xerces.impl.dv.util.Base64;

import biz.taoconsulting.dominodav.exceptions.DAVNotFoundException;
import biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation;
import biz.taoconsulting.dominodav.interfaces.IDAVRepository;
import biz.taoconsulting.dominodav.interfaces.IDAVResource;

import com.ibm.xsp.webdav.domino.CategorizedDocumentInputStream;
import com.ibm.xsp.webdav.domino.CategorizedDocumentOutputStream;
import com.ibm.xsp.webdav.domino.DominoProxy;
import com.ibm.xsp.webdav.repository.DAVRepositoryDominoCategorizedDocuments;

/**
 * A WebDAV resource is an addressable Web object, such as a file or directory.
 * An ordinary resource can be viewed as a file. It can have a content body of
 * any MIME type [RFC2045], [RFC2046], including HTML-formatted text, other
 * text, an image, an executable, or an Office document. It can have locks and
 * properties. The specification for URI Syntax [RFC2396] defines a resource as
 * "anything that has identity." HTTP/1.1 [RFC2616] defines a resource both as
 * "a network data object or service" and "anything that has a URI." The WebDAV
 * specification does not redefine a resource but works from these definitions.
 * 
 * @author Stephan H. Wissel
 */
public class DAVResourceDominoCategorizedDocuments extends DAVResourceDomino {

	/**
	 * Directory composed out of temp directory, username, unid and File name
	 */
	private String tempFileDir;

	/**
	 * We need to ensure that a document is considered a collection
	 */
	@Override
	public boolean isCollection() {
		return super.isCollection();
		// if (!("NotesAttachment".equals(this.getResourceType()))) {
		// return true;
		// }
		// return false;
	}

	/**
	 * Logger for Errors
	 */
	private static final Log LOGGER = LogFactory
			.getLog(DAVResourceDominoDocuments.class);

	/**
	 * @param repository
	 *            the repository the Resource is in
	 * @param url
	 *            the path relative to the repository -- as seen from the
	 *            browser
	 * @throws DAVNotFoundException
	 *             --- if the file is not there
	 */
	public DAVResourceDominoCategorizedDocuments(IDAVRepository repository,
			String url) throws DAVNotFoundException {
		setup(repository, url, false);
		// this.setOwner(repository);
		// this.setPublicHref(url);
		// this.populateAttachmentPropertiesFromNotesDoc();

	}

	/**
	 * Light version
	 * 
	 * @param repository
	 */
	public DAVResourceDominoCategorizedDocuments(IDAVRepository repository) {
		this.setOwner(repository);
		this.setPublicHref("");
	}

	/**
	 * @param rep
	 *            The repository
	 * @param url
	 *            the requested path -- as seen from the browser
	 * @param isMember
	 *            - for directories: is it listed as part of the parent or by
	 *            itself?
	 * @throws DAVNotFoundException
	 *             -- resource might not be there
	 */
	public DAVResourceDominoCategorizedDocuments(IDAVRepository rep,
			String url, boolean isMember) throws DAVNotFoundException {
		setup(rep, url, isMember);
		// this.setOwner(rep);
		// this.setPublicHref(url);
		// this.setMember(isMember);
		// this.populateAttachmentPropertiesFromNotesDoc();

	}

	public DAVResourceDominoCategorizedDocuments(IDAVRepository rep,
			String url, boolean isMember, boolean forceCreate)
			throws DAVNotFoundException {
		setup(rep, url, isMember, forceCreate);
		// this.setOwner(rep);
		// this.setPublicHref(url);
		// this.setMember(isMember);
		// this.populateAttachmentPropertiesFromNotesDoc();

	}

	/**
	 * @see biz.taoconsulting.dominodav.resource.DAVAbstractResource#delete()
	 */
	public boolean delete() {
		Document curDoc = null;
		String notesURL = this.getInternalAddress();
		curDoc = DominoProxy.getDocument(notesURL);
		if (curDoc != null) {
			try {
				curDoc.remove(true);
				return true;
			} catch (NotesException ne) {
				return false;
			}
		}
		return false;
	}

	/**
	 * @see biz.taoconsulting.dominodav.resource.DAVAbstractResource#getOutputStream()
	 */
	public OutputStream getOutputStream() {
		// This returns the OutputStream when writing back to the class

		if (this.out == null) {
			this.out = new CategorizedDocumentOutputStream(this);
		}

		return this.out;

	}

	/**
	 * @see biz.taoconsulting.dominodav.resource.DAVAbstractResource#getStream()
	 *      Originally I tried EmbeddedObject.getInputStream(); but that didn't
	 *      go down well with the servlet, so now I use a temp file approach
	 *      where an attachment is stored into a temp file and then served to
	 *      the servlet as fileinput stream
	 */
	public InputStream getStream() {
		Session s = null;
		InputStream curStream = null;
		String notesURL = this.getInternalAddress();
		// LOGGER.info("NotesUrl="+notesURL);
		Document curDoc = null;

		s = DominoProxy.getUserSession();

		if (s != null) {
			try {
				// We need to find the $File to isolate the document
				int dollarFile = notesURL.lastIndexOf("/$File");

				if (dollarFile < 0) {
					// This is not an attachment
					return null;
				}

				String docURL = notesURL.substring(0, dollarFile);
				LOGGER.info("docURL=" + docURL);
				curDoc = (Document) s.resolve(docURL);
				String curAttName = this.getName();
				curAttName = java.net.URLDecoder.decode(curAttName, "utf-8");
				LOGGER.info("curAttName=" + curAttName);
				EmbeddedObject curAttachment = curDoc.getAttachment(curAttName);
				if (curAttachment == null) {
					LOGGER.info("�ttachment is null");
				}
				File tempFile = this.getTempfile();
				// Delete if it exists
				if (tempFile.exists()) {
					tempFile.delete();
				}
				curAttachment.extractFile(tempFile.getAbsolutePath());
				LOGGER.info("Current attachment path extracted is ="
						+ tempFile.getAbsolutePath());
				curStream = new CategorizedDocumentInputStream(tempFile, this);
			} catch (Exception e) {
				LOGGER.error(e);
			}
		}
		return curStream;
	}

	/**
	 * @param notesURL
	 *            The attachment/document to be processed
	 * @param notesViewMember
	 *            Name of document/attachment in view
	 * @return true/false if resource population worked or not
	 */
	public void fetchChildren() {
		// LOGGER.info(getCallingMethod()+":"+"Start fetchChildren..");

		if (this.isMember()) {
			// LOGGER.info(getCallingMethod()+":"+"No fetch children for isMember()= true");
			return;
		}

		// We fetch the right type of children
		String resType = this.getResourceType();
		// LOGGER.info(getCallingMethod()+":"+"This resource is not a member; so start fetching..");

		try {
			if ("NotesDocument".equals(resType)) {
				// LOGGER.info(getCallingMethod()+":"+"Resource is a NotesDocument; Start fetchingChildren for it...!");
				this.fetchChildrenForNotesDocument();
				// LOGGER.info(getCallingMethod()+":"+"End fetching children for NotesDocument OK!");
			} else if ("NotesAttachment".equals(resType)) {
				// Notes Attachments don't have children
				// LOGGER.info(getCallingMethod()+":"+"Resource is a NotesAttachment.. NO IMPLEMENTATION!");

			} else if ("NotesDatabase".equals(resType)) {
				// LOGGER.info(getCallingMethod()+":"+"Resource is NotesDatabase; Start fetchingChildren for it..!");
				this.fetchChildrenForNotesDatabase();
				// LOGGER.info(getCallingMethod()+":"+"End fetching children for NotesDatabase OK!");
			} else {
				// We presume a view for the resource type
				// LOGGER.info(getCallingMethod()+":"+"Resource is a View; Start fetching Children for it....!");
				fetchChildrenForNotesView();
				// LOGGER.info(getCallingMethod()+":"+"End fetching children for NotesView OK!");
			}
		} catch (NotesException e) {
		}
		// LOGGER.info(getCallingMethod()+":"+"End function fetchChildren OK!");
	}

	@SuppressWarnings("unchecked")
	private void fetchChildrenForNotesDocument() throws NotesException {
		// LOGGER.info(getCallingMethod()+":"+"Start fetchChildrenForNotesDocument; Fetching children for doc with UNID="
		// + this.getDocumentUniqueID());
		Document curDoc = null;
		String docID = null;
		Vector<IDAVResource> resMembers = null;
		this.setMembers(new Vector<IDAVResource>());
		String notesURL = this.getInternalAddress();
		DAVRepositoryDominoCategorizedDocuments repository = (DAVRepositoryDominoCategorizedDocuments) this
				.getRepository();
		// LOGGER.info("Internal Address is "+notesURL);
		// LOGGER.info("PublicHref is "+this.getPublicHref());
		curDoc = DominoProxy.getDocument(notesURL);
		// LOGGER.info(getCallingMethod()+":"+"Currdoc not null ; OK");
		if (curDoc == null) {
			LOGGER.error("Could not retrieve the document");
			return;
		}
		boolean readOnly = this.checkReadOnlyAccess(curDoc);
		Date curCreationDate = curDoc.getCreated().toJavaDate();
		if (curDoc.hasItem("DAVCreated")) {
			@SuppressWarnings("rawtypes")
			Vector times = curDoc.getItemValueDateTimeArray("DAVCreated");
			Object time = times.elementAt(0);
			if (time.getClass().getName().endsWith("DateTime")) {
				curCreationDate = ((DateTime) time).toJavaDate();
			}
		}
		Date curChangeDate = curDoc.getLastModified().toJavaDate();
		if (curDoc.hasItem("DAVModified")) {
			@SuppressWarnings("rawtypes")
			Vector times = curDoc.getItemValueDateTimeArray("DAVModified");
			Object time = times.elementAt(0);
			if (time.getClass().getName().endsWith("DateTime")) {
				curChangeDate = ((DateTime) time).toJavaDate();
			}
		}
		this.setCreationDate(curCreationDate);
		this.setLastModified(curChangeDate);
		this.setReadOnly(readOnly);
		// Read the repository list to get the view
		try {
			LOGGER.info(getCallingMethod() + ":"
					+ "Currdoc not null ; OK; Has UNID="
					+ curDoc.getUniversalID());
			docID = curDoc.getUniversalID();

			LOGGER.info(getCallingMethod() + ":" + "Openend document " + docID);

			// No children if there are no attachments
			if (!curDoc.hasEmbedded()) { // folder

				ViewEntryCollection responses = repository
						.getAllEntriesByKey(curDoc
								.getItemValueString(repository
										.getPubHrefField()));
				LOGGER.info(getCallingMethod() + ":" + "Get Responses...");
				int numOfResponses = responses.getCount();
				LOGGER.info(getCallingMethod() + ":" + "Current doc has "
						+ String.valueOf(numOfResponses) + " responses");
				if (numOfResponses > 1) {
					resMembers = new Vector<IDAVResource>(numOfResponses - 1);
					LOGGER.info(getCallingMethod() + ":"
							+ "Start Process responses");
					lotus.domino.ViewEntry ve = responses.getFirstEntry();
					ve = responses.getNextEntry();
					Document docResp = null;
					while (ve != null) {
						docResp = ve.getDocument();
						// if(docResp.getUniversalID()!=docID){
						LOGGER.info(getCallingMethod() + ":"
								+ "Doc response has unid="
								+ docResp.getUniversalID()
								+ "; Try find attachment(s)");
						Vector<String> allEmbedded = DominoProxy.evaluate(
								"@AttachmentNames", docResp);
						int numOfAttachments = allEmbedded.isEmpty() ? 0
								: (allEmbedded.get(0).toString().equals("") ? 0
										: allEmbedded.size());
						LOGGER.info(getCallingMethod() + ":" + "Doc has "
								+ String.valueOf(numOfAttachments)
								+ " attachment(s)");
						if (numOfAttachments == 0) { // No attachments in here!
							LOGGER.info(getCallingMethod()
									+ ":"
									+ "Doc "
									+ docResp.getUniversalID()
									+ " response has no attachment; is a directory; Create resource for it");
							DAVResourceDominoCategorizedDocuments resAtt = new DAVResourceDominoCategorizedDocuments(
									this.getRepository(),
									this.getPublicHref()
											+ "/"
											+ docResp
													.getItemValueString(((DAVRepositoryDominoCategorizedDocuments) (this
															.getRepository()))
															.getDirectoryField()),
									true);
							resAtt.setup(docResp);
							if (resAtt != null) {
								LOGGER.info(getCallingMethod()
										+ ":"
										+ "Created DavResourceDomino Attachments from getDocumentResource-OK");
								if (resAtt.filter()) {
									this.getMembers().add(resAtt);
								}
								resMembers.add(resAtt);
								LOGGER.info(getCallingMethod() + ":"
										+ "Resource successfull added");
							}
						} else {
							LOGGER.info(getCallingMethod() + ":"
									+ "Doc response "
									+ docResp.getUniversalID()
									+ " has attachments >0; ");

							String curAttName = allEmbedded.get(0).toString();
							if ((curAttName != null)
									&& (!curAttName.equals(""))) {
								LOGGER.info(getCallingMethod()
										+ ":"
										+ "Doc response fitrst attachment has name "
										+ curAttName);
								DAVResourceDominoCategorizedDocuments resAtt = new DAVResourceDominoCategorizedDocuments(
										this.getRepository(),
										this.getPublicHref() + "/" + curAttName,
										true);
								resAtt.setup(docResp);
								if (resAtt != null) {
									// Now add it to the Vector
									LOGGER.info(getCallingMethod()
											+ ":"
											+ "Created DAVResourceDominoDocuments with getAttachmentResource-OK!\n Start load resource");
									// resMembers.add(curAttachment);
									if (resAtt.filter()) {
										this.getMembers().add(resAtt);
									}
									LOGGER.info(getCallingMethod() + ":"
											+ "Resource successfull added");
									Date viewDate = this.getLastModified();
									Date docDate = resAtt.getLastModified();
									if (viewDate == null
											|| (docDate != null && viewDate
													.before(docDate))) {
										this.setLastModified(docDate);
									}
									LOGGER.info(getCallingMethod()
											+ ":"
											+ "Resource successfull updated last modified");

									LOGGER.info(getCallingMethod() + ":"
											+ "Processing complete attachment:"
											+ curAttName);
								}
							}
						}
						LOGGER.info(getCallingMethod() + ":"
								+ "Start recycling..");
						// Document docTmp=docResp;
						// } //end
						// if(docResp.getUniversalID()!=curDoc.getUniversalID()){
						ve = responses.getNextEntry();
						// docTmp.recycle();
						LOGGER.info(getCallingMethod() + ":" + "Recycling OK!");
					} // end while

				} // end if numresp>0

				try {
					LOGGER.info(getCallingMethod() + ":" + "Final recycling..");
					if (curDoc != null) {
						curDoc.recycle();
					}
					LOGGER.info(getCallingMethod() + ":"
							+ "End FINAL recycling OK!");

				} catch (Exception e) {
					LOGGER.error(e);
				}
				// Now save back the members to the main object
				LOGGER.info(getCallingMethod()
						+ ":"
						+ "Finish processing current doc as a directory; No more attachment(s) in it; Return!");
				return;
			}

			// Get all attachments
			LOGGER.info(getCallingMethod() + ":"
					+ "Current doc has attachments!");
			@SuppressWarnings("rawtypes")
			Vector allEmbedded = DominoProxy.evaluate("@AttachmentNames",
					curDoc);
			int numOfAttchments = allEmbedded.size();
			if (numOfAttchments == 0) { // No attachments in here!
				LOGGER.info(getCallingMethod() + ":"
						+ "Something wrong:  Doc + " + docID
						+ " has no attachments (@AttachmentNames)");
				return;
			}
			LOGGER.info(getCallingMethod() + ":" + docID + " has "
					+ new Integer(numOfAttchments).toString()
					+ " attachment(s)");
			// Initialize an empty vector at the right size
			// We might need to enlarge it if we have more attachments
			resMembers = new Vector<IDAVResource>(numOfAttchments);
			LOGGER.info(getCallingMethod() + ":"
					+ "Start processing attachment(s)..");
			for (int i = 0; i < numOfAttchments; i++) {

				String curAttName = allEmbedded.get(i).toString();
				DAVResourceDominoCategorizedDocuments curAttachment = getDocumentResource(curDoc);

				if (curAttachment != null) {
					// Now add it to the Vector
					LOGGER.info(getCallingMethod() + ":"
							+ "Resource attachment successfully created!");
					// resMembers.add(curAttachment);
					if (curAttachment.filter()) {
						this.getMembers().add(curAttachment);
					}
					LOGGER.info("Resource  attachment successfully added: "
							+ curAttName + "; OK!");
				} else {
					LOGGER.error("Could not load attachment#" + curAttName
							+ "#");
				}
			}

		} catch (NotesException ne) {
			LOGGER.error(ne);

		} catch (Exception e) {
			LOGGER.error(e);

		} finally {

			try {
				LOGGER.info(getCallingMethod() + ":"
						+ "Final block; Start Recycling!");

				if (curDoc != null) {
					curDoc.recycle();
				}

			} catch (Exception e) {
				LOGGER.error(e);
			}
			// Now save back the members to the main object
			// this.setMembers(resMembers);
			LOGGER.info("Completed reading attachments resources from Notes document; OK!");
		}

	}

	private void fetchChildrenForNotesView() {
		// LOGGER.info(getCallingMethod()+":"+"Start fetchChildrenForNotesView; Fetching children for "
		// +
		// this.getName()+" with internal address= "+this.getInternalAddress()+"  and public href= "+this.getPublicHref());

		Document curDoc = null;

		View curView = null;
		Database curDb = null;
		Vector<IDAVResource> resMembers = new Vector<IDAVResource>();
		this.setMembers(resMembers);
		String notesURL = this.getInternalAddress();

		curView = DominoProxy.getView(notesURL);

		if (curView == null) {
			LOGGER.error("Could not retrieve view: " + notesURL);
			return;
		}
		DAVRepositoryDominoCategorizedDocuments repository = (DAVRepositoryDominoCategorizedDocuments) this
				.getRepository();

		// Read the repository list to get the view
		try {
			// LOGGER.info(getCallingMethod()+":"+"Openend view " +
			// curView.getName());

			// Initialize an empty vector at the right size
			// We might need to enlarge it if we have more attachments
			// LOGGER.info( "View num doc is "+new
			// Integer(curView.getEntryCount()).toString());
			DocumentCollection docColl = repository.getAllDocumentsByKey("/");
			curDoc = docColl.getFirstDocument();
			while (curDoc != null) {
				// LOGGER.info( "Start processing doc no "+ new
				// Integer(i).toString());

				// LOGGER.info(getCallingMethod()+":"+"Start process view ");
				DAVResourceDominoCategorizedDocuments docRes = null;
				if (curDoc.hasEmbedded()) {
					// LOGGER.info(getCallingMethod()+":"+"Document "+curDoc.getUniversalID()+
					// " has embedded; Start Add Attachments");
					// docRes = this.addAttachmentsFromDocument(curDoc);
					docRes = new DAVResourceDominoCategorizedDocuments(
							this.getRepository());

					docRes.setup(curDoc);
					// docRes.setCollection(false);
					// docRes.setMember(true);
					// LOGGER.info(getCallingMethod()+":"+"Successfully create attachments for "+curDoc.getUniversalID());
				} else {
					// LOGGER.info(getCallingMethod()+":"+"Document "+
					// curDoc.getUniversalID()+
					// " has no attachment; try to get a resource");
					docRes = new DAVResourceDominoCategorizedDocuments(
							this.getRepository());
					docRes.setup(curDoc);
					docRes.setCollection(true);
					docRes.setMember(false);
					// LOGGER.info(getCallingMethod()+":"+"Successfully create resource from doc "+docRes.getName());
				}
				// TODO: Fix this!

				if (docRes != null) {

					// LOGGER.info(getCallingMethod()+":"+"Resource "+docRes.getName()+"created OK; Try to add to resMembers");
					if (docRes.filter()) {
						this.getMembers().add(docRes);
					}
					// LOGGER.info(getCallingMethod()+":"+"Resources added OK;");

					// Capture last modified based on the latest date of the
					// documents in view
					Date viewDate = this.getLastModified();
					Date docDate = docRes.getLastModified();
					if (viewDate == null
							|| (docDate != null && viewDate.before(docDate))) {
						this.setLastModified(docDate);
					}
					// LOGGER.info(getCallingMethod()+":"+"Resource processes  OK");

				}
				// //
				// LOGGER.info(getCallingMethod()+":"+"Try recycle and find the next for "+curDoc.getUniversalID()+"....");
				// Document oldDoc=curDoc;
				// curDoc = curView.getNextDocument(curDoc);
				// oldDoc.recycle();
				// // LOGGER.info(getCallingMethod()+":"+"Recycle OK!");
				// //
				// LOGGER.info("Next doc is "+((curDoc!=null)?"Not null":"null"));
				curDoc = docColl.getNextDocument();

			} // end for

		} catch (NotesException ne) {
			LOGGER.error(ne);

		} catch (Exception e) {
			LOGGER.error(e);

		} finally {

			try {
				// LOGGER.info(getCallingMethod()+":"+"Final recycle....");

				if (curDoc != null) {
					// curDoc.recycle();
				}

				if (curView != null) {
					curView.recycle();
				}

				if (curDb != null) {
					curDb.recycle();
				}
				// LOGGER.info(getCallingMethod()+":"+"Final recycle OK!");

			} catch (Exception e) {
				LOGGER.error(e);
			}

			// LOGGER.info(getCallingMethod()+":"+"Completed reading file resources from Domino view");
			// LOGGER.info(getCallingMethod()+":"+"Start to setMembers...");
			// this.setMembers(resMembers);
			// LOGGER.info(getCallingMethod()+":"+"SetMembers OK! Exit fetchChildrenforNotesView");
		}
		// Now save back the members to the main object

	}

	private void fetchChildrenForNotesDatabase() {
		// LOGGER.info(getCallingMethod()+":"+"Fetching children for " +
		// this.getName());

		Document curDoc = null;
		Document nextDoc = null;

		DocumentCollection curEntries = null;
		Vector<IDAVResource> resMembers = null;
		String notesURL = this.getInternalAddress();
		Database curDb = DominoProxy.getDatabase(notesURL);

		if (curDb == null) {
			LOGGER.error("Could not get the database " + notesURL);
			return;
		}

		// Read the repository list to get the view
		try {
			// LOGGER.info(getCallingMethod()+":"+"Openend databasew " +
			// curDb.getFileName());

			// Initialize an empty vector at the right size
			// We might need to enlarge it if we have more attachments
			curEntries = curDb.getAllDocuments();
			resMembers = new Vector<IDAVResource>(curEntries.getCount());

			curDoc = curEntries.getFirstDocument();

			if (curDoc == null) {
				// LOGGER.info(getCallingMethod()+":"+this.getName() +
				// " does not (yet) contain resources");
				return;
			}

			while (curDoc != null) {
				nextDoc = curEntries.getNextDocument(curDoc);

				DAVResourceDominoCategorizedDocuments docRes = this
						.addAttachmentsFromDocument(curDoc);

				if (docRes != null) {
					resMembers.add(docRes);
				}

				curDoc.recycle();
				curDoc = nextDoc;
			}

		} catch (NotesException ne) {
			LOGGER.error(ne);

		} catch (Exception e) {
			LOGGER.error(e);

		} finally {

			try {

				if (curDoc != null) {
					curDoc.recycle();
				}

				if (nextDoc != null) {
					nextDoc.recycle();
				}

				if (curEntries != null) {
					curEntries.recycle();
				}

				if (curDb != null) {
					curDb.recycle();
				}

			} catch (Exception e) {
				LOGGER.error(e);
			}

			// LOGGER.info(getCallingMethod()+":"+"Completed reading file resources from Domino view");
		}
		// Now save back the members to the main object
		this.setMembers(resMembers);
	}

	/**
	 * Reads all attachments in a document and adds them to the resourcelist.
	 * Since a document can contain more than one attachment we treat documents
	 * as directories since webDAV from Windows/Linux ignores the URL and uses
	 * the name to build the request URL
	 * 
	 * @param s
	 * @param curDoc
	 * @param resMembers
	 */
	private DAVResourceDominoCategorizedDocuments addAttachmentsFromDocument(
			Document curDoc) {

		String docID = null;
		DAVResourceDominoCategorizedDocuments docRes = null;

		try {
			// LOGGER.info(getCallingMethod()+":"+"Start addAttachmentsFromDocument "+curDoc.getUniversalID());
			docID = curDoc.getUniversalID();
			if (!curDoc.hasEmbedded()) {
				LOGGER.info(getCallingMethod() + ":" + docID
						+ " has no attachments (hasEmbedded)");
			} else {
				LOGGER.info(getCallingMethod()
						+ ":"
						+ docID
						+ " has attachments; start creating resource of type attachment(file)");
				docRes = this.getDocumentResource(curDoc);
				// LOGGER.info(getCallingMethod()+":"+"End creating resource of type attachment!");
			}
		} catch (NotesException e) {
			LOGGER.error(e);
			docRes = null;
		}
		// LOGGER.info(getCallingMethod()+":"+"Return docRes OK! ");
		return docRes;
	}

	/**
	 * Create a Attachment resource for an attachment inside of a document
	 * 
	 * @param s
	 * @param curDoc
	 * @param curAttName
	 * @return
	 */

	// This function overwrites the main function in DAVResourceDomino
	protected String getNameFromInternalAddress(String internalName)
			throws Exception {
		int lastSlash = internalName.lastIndexOf("/");
		if (lastSlash < 0) {
			return internalName;
		}
		String candidate = internalName.substring(lastSlash + 1);
		int questionMarkPos = candidate.indexOf("?");

		if (questionMarkPos < 0) {
			return candidate;
		}

		return candidate.substring(0, questionMarkPos);
	}

	/**
	 * @return The file to read/write the data to since stream in attachments
	 *         doesn't seem to be reliable
	 */
	public File getTempfile() {
		// We check for a directory with the UNID of the resource
		// and write the file there
		String unid = this.getDocumentUniqueID();
		File returnFile = null;

		// Check for the root temp Dir
		String rootTempDir = this.getRepository().getTempDir();

		// Get the username in base64 encoded, so we can use it as temp file
		// name

		String userDir = null;
		try {
			String userName = DominoProxy.getUserName();
			userDir = Base64.encode(userName.getBytes());
		} catch (Exception e) {
			LOGGER.error(e);
			userDir = "Anonymous";
		}

		// Save the values for reuse
		this.tempFileDir = rootTempDir + File.separator + userDir
				+ File.separator + unid;

		// Create the directory if needed
		File curTempDir = new File(this.tempFileDir);
		if (!curTempDir.exists()) {
			// Create the directory structure - we keep the user dir
			curTempDir.mkdirs();
		} else if (!curTempDir.isDirectory()) {
			// Very bad
			LOGGER.error("Tempdir exists and is not a directory: "
					+ this.tempFileDir);
			return null;
		}
		// Update the full value
		this.tempFileDir = curTempDir.getAbsolutePath();

		// Now create the file object
		String trueFileName = this.tempFileDir + File.separator
				+ this.getName();
		returnFile = new File(trueFileName);

		return returnFile;
	}

	/**
	 * Removes the temporary file
	 */
	public void removeTempFiles() {
		try {
			// First the file, then the temp dir
			File fullFile = new File(this.tempFileDir + File.separator
					+ this.getName());
			if (fullFile.exists()) {
				fullFile.delete();
			}
			fullFile = null;

			// Temp dir
			File docTempDir = new File(this.tempFileDir);

			// We only can remove the directory if it isn't empty
			// Other files might be opened in other tabs or windows
			if (docTempDir.exists()) {

				File[] content = docTempDir.listFiles();

				if (content == null || content.length == 0) {
					String userTempDirName = docTempDir.getParent();
					docTempDir.delete();
					docTempDir = null;

					File userTempDir = new File(userTempDirName);

					if (userTempDir.exists()) {
						File[] otherTemp = userTempDir.listFiles();
						if (otherTemp == null || otherTemp.length == 0) {
							userTempDir.delete();
						}
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error(e);
		}

	}

	/**
	 * Gets to the Notes document and populates all the attachment properties
	 * that need the NotesSession
	 */

	/**
	 * We return the NotesURL including ?OpenDocument
	 * 
	 * @param internalAddress
	 * @return
	 */
	public Document getDocument() {
		String intAddress = this.getInternalAddress();
		Base notesObj = DominoProxy.resolve(intAddress);
		if (notesObj == null) {
			return null;
		}
		if (notesObj instanceof Document) {
			return (Document) notesObj;
		}
		return null;
	}

	public void updateHierarchy() {
		Document doc = getDocument();
		DAVRepositoryDominoCategorizedDocuments rep = ((DAVRepositoryDominoCategorizedDocuments) this
				.getRepository());
		try {
			if (doc.isResponse()) {
				doc.removeItem("$Ref");
			}
			String pubHref = doc.getItemValueString(rep.getPubHrefField());
			if (!pubHref.equals("")) {
				if (pubHref.lastIndexOf("/") > 0) {
					String parentPubHref = pubHref.substring(0,
							pubHref.lastIndexOf("/"));
					Document docP = rep.getDocumentByKey(parentPubHref);
					if (docP != null) {
						doc.makeResponse(docP);
					}
				}
			}
			doc.save(true);
		} catch (NotesException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public String getCallingMethod() {
		return trace(Thread.currentThread().getStackTrace(), 2);
	}

	public String getCallingMethod(int level) {
		return trace(Thread.currentThread().getStackTrace(), 2 + level);
	}

	private String trace(StackTraceElement e[], int level) {
		if (e != null && e.length >= level) {
			StackTraceElement s = e[level];
			if (s != null) {
				return s.getMethodName();
			}
		}
		return null;
	}

	private void setup(Document curDoc) {
		try {
			DAVRepositoryDominoCategorizedDocuments rep = (DAVRepositoryDominoCategorizedDocuments) this
					.getRepository();

			LOGGER.info("Start setup with doc ..."
					+ curDoc.getItemValueString(rep.getPubHrefField()));
			@SuppressWarnings("rawtypes")
			Vector allEmbedded = DominoProxy.evaluate("@AttachmentNames",
					curDoc);
			// LOGGER.info(getCallingMethod()+":"+"All Embedded computed");
			int numOfAttachments = allEmbedded.isEmpty() ? 0 : (allEmbedded
					.get(0).equals("") ? 0 : allEmbedded.size());
			String docID = curDoc.getUniversalID();
			this.setDocumentUniqueID(docID);
			LOGGER.info("Num of attachments="
					+ new Integer(numOfAttachments).toString());
			boolean readOnly = this.checkReadOnlyAccess(curDoc);
			this.setReadOnly(readOnly);
			// LOGGER.info("Creation date for "+curDoc.getUniversalID()+" ="+curDoc.getCreated().toString()+"; Time zone="+curDoc.getCreated().getZoneTime()+"; Local time="+curDoc.getCreated().getLocalTime());

			Date curCreationDate = curDoc.getCreated().toJavaDate();
			// LOGGER.info("Current date in Java is "+curCreationDate.toString()+"Time zone="+new
			// Integer(curCreationDate.getTimezoneOffset()).toString()+"; Locale time is:"+curCreationDate.toLocaleString());
			if (curDoc.hasItem("DAVCreated")) {
				// Item davCreated=curDoc.getFirstItem("DAVCreated");
				@SuppressWarnings("rawtypes")
				Vector times = curDoc.getItemValueDateTimeArray("DAVCreated");
				if (times != null) {
					if (times.size() > 0) {
						Object time = times.elementAt(0);
						if (time != null) {
							if (time.getClass().getName().endsWith("DateTime")) {
								curCreationDate = ((DateTime) time)
										.toJavaDate();
								if (curCreationDate == null) {
									curCreationDate = curDoc.getCreated()
											.toJavaDate();
								}
							}
						}
					}
				}
			}
			Date curChangeDate = curDoc.getLastModified().toJavaDate();
			if (curDoc.hasItem("DAVModified")) {
				@SuppressWarnings("rawtypes")
				Vector times = curDoc.getItemValueDateTimeArray("DAVModified");
				if (times != null) {
					if (times.size() > 0) {
						Object time = times.elementAt(0);
						if (time != null) {
							if (time.getClass().getName().endsWith("DateTime")) {
								curChangeDate = ((DateTime) time).toJavaDate();
								if (curChangeDate == null) {
									curChangeDate = curDoc.getLastModified()
											.toJavaDate();
								}
							}
						}
					}
				}
			}
			this.setCreationDate(curCreationDate);
			this.setLastModified(curChangeDate);
			// LOGGER.info("Creation date is set to "+this.getCreationDate().toString());
			// LOGGER.info("Last modified date is set to "+this.getLastModified().toString());
			String pubHRef = ((IDAVAddressInformation) this.getRepository())
					.getPublicHref();
			// LOGGER.info("THIS getpublichref="+this.getPublicHref());
			String curAttName = null;
			if (numOfAttachments == 0) {
				// LOGGER.info(getCallingMethod()+":"+"Start setting resource");
				String name = curDoc
						.getItemValueString(((DAVRepositoryDominoCategorizedDocuments) (this
								.getRepository())).getDirectoryField());
				this.setName(name);
				if (this.getPublicHref().equals("")) {
					this.setPublicHref(pubHRef
							+ curDoc.getItemValueString(((DAVRepositoryDominoCategorizedDocuments) (this
									.getRepository())).getPubHrefField()));
				}
				this.setCollection(true);
				this.setInternalAddress(((IDAVAddressInformation) this
						.getRepository()).getInternalAddress() + "/" + docID);

				this.setResourceType("NotesDocument");
				this.setMember(false);
				this.setContentLength(0L);
				// this.fetchChildren();
			} else {
				curAttName = allEmbedded.get(0).toString();
				// LOGGER.info("Attachment name is "+curAttName);
				this.setMember(true);
				this.setResourceType("NotesAttachment");
				if (this.getPublicHref().equals("")) {
					try {
						this.setPublicHref(pubHRef
								+ curDoc.getItemValueString(((DAVRepositoryDominoCategorizedDocuments) (this
										.getRepository())).getPubHrefField()));
					} catch (Exception e) {
						LOGGER.error(e);
					}

					// this.setPublicHref( pubHRef+"/"+curAttName);
				}
				this.setInternalAddress(((IDAVAddressInformation) this
						.getRepository()).getInternalAddress()
						+ "/"
						+ docID
						+ "/$File/" + curAttName);
				this.setCollection(false);
				this.setName(curAttName);
				EmbeddedObject curAtt = curDoc.getAttachment(curAttName);
				if (curAtt == null) {
					return;
				}
				Long curSize = new Long(curAtt.getFileSize());
				this.setContentLength(curSize);
			}
			// LOGGER.info("Current res realized! pubHREF="+this.getPublicHref()+"; Internal Address="+this.getInternalAddress()+"; ");
		} catch (NotesException ne) {
			LOGGER.error("ERROR! Can not set; " + ne.getMessage());
		}
	}

	private void setup(IDAVRepository rep, String url, boolean isMember,
			boolean forceCreate) throws DAVNotFoundException {
		LOGGER.info("Start setup with force create; url=" + url);
		this.setOwner(rep);
		String folderUrl = null;
		String fileName = null;
		DAVRepositoryDominoCategorizedDocuments repository = (DAVRepositoryDominoCategorizedDocuments) rep;
		try {
			url = java.net.URLDecoder.decode(url, "UTF-8");
		} catch (Exception e) {
		}

		LOGGER.info("setup with url=" + url);
		if (url.lastIndexOf("/") > 0) {
			folderUrl = url.substring(0, url.lastIndexOf("/"));
			fileName = url.substring(url.lastIndexOf("/") + 1);
		}
		LOGGER.info("Folder=" + folderUrl + "; FileName=" + fileName);
		if ((fileName != null) && (folderUrl != null)) {
			LOGGER.info("in if");
			DAVResourceDominoCategorizedDocuments folder = new DAVResourceDominoCategorizedDocuments(
					rep, folderUrl, false);
			if (folder != null) {
				LOGGER.info("Folder not null");
				Base notesObj = DominoProxy.resolve(repository
						.getInternalAddressFromExternalUrl(folderUrl, ""));
				Database db = null;
				Document docParent = null;
				if (notesObj instanceof View) {
					LOGGER.info("isView");
					View notesView = (View) notesObj;
					try {
						db = notesView.getParent();
					} catch (NotesException e) {
					}
				} else {
					if (notesObj instanceof Document) {
						docParent = (Document) notesObj;
						LOGGER.info("is doc");
						try {
							db = docParent.getParentDatabase();
						} catch (NotesException e) {
						}
					}
				}
				if (db != null) {
					try {
						LOGGER.info("db not null");
						String pubhref = folderUrl;
						if (pubhref.startsWith(repository.getPublicHref())) {
							pubhref = pubhref.substring(repository
									.getPublicHref().length());
						}
						Document doc = db.createDocument();
						doc.replaceItemValue("Form",
								repository.getFileFormName());
						doc.computeWithForm(true, false);
						Session s = DominoProxy.getUserSession(); // NotesSession
						Item webdavAuthor = doc.replaceItemValue("AuthorDAV",
								s.getEffectiveUserName());
						webdavAuthor.setSummary(true);
						webdavAuthor.setAuthors(true);
						LOGGER.info("Set OK!FolderUrl=" + folderUrl
								+ "; fileName=" + fileName);
						doc.replaceItemValue(repository.getPubHrefField(),
								pubhref + "/" + fileName);
						if (docParent != null) {
							doc.makeResponse(docParent);
						}
						if (!isMember) { // is collection
							doc.replaceItemValue(
									"Form",
									((DAVRepositoryDominoCategorizedDocuments) rep)
											.getDirectoryFormName());
							doc.replaceItemValue(
									((DAVRepositoryDominoCategorizedDocuments) rep)
											.getDirectoryField(), fileName);
						}
						doc.save(true);
						LOGGER.info("Saved ok");
						this.setDocumentUniqueID(doc.getUniversalID());
						this.setPublicHref(url);
						this.setName(fileName);
						this.setMember(isMember);
						LOGGER.info("Set member ok");
						if (isMember) {
							this.setInternalAddress(((IDAVAddressInformation) this
									.getRepository()).getInternalAddress()
									+ "/"
									+ doc.getUniversalID()
									+ "/$File/"
									+ this.getName());
							this.setCollection(false);
						} else {
							this.setInternalAddress(((IDAVAddressInformation) this
									.getRepository()).getInternalAddress()
									+ "/" + doc.getUniversalID());
							this.setCollection(true);
						}
						LOGGER.info("Internal address ="
								+ this.getInternalAddress());
					} catch (NotesException ne) {
					}
				}
			}
		}

		// this.setDominoResourceType();

	}

	private void setup(IDAVRepository rep, String url, boolean isMember)
			throws DAVNotFoundException {

		// Store a link to the repository
		this.setOwner(rep);

		try {
			url = java.net.URLDecoder.decode(url, "UTF-8");
		} catch (Exception e) {
		}
		DAVRepositoryDominoCategorizedDocuments repository = (DAVRepositoryDominoCategorizedDocuments) this
				.getRepository();
		LOGGER.info("DAVResouceDomino for " + url + "; Directory field="
				+ repository.getDirectoryField());

		if (url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}
		// The path can't be null and can't be empty. if it is empty we use "/"
		if (url == null || url.equals("")) {
			url = new String("/");
		}
		IDAVAddressInformation repAdr = (IDAVAddressInformation) this
				.getRepository();
		String repUrl = repAdr.getPublicHref();
		LOGGER.info("Repository url=" + repUrl);
		if (url.indexOf(repUrl) == 0) {
			url = url.substring(repUrl.length());
		}
		LOGGER.info("New url=" + url);
		String fpath = rep.getInternalAddressFromExternalUrl(url,
				"DAVREsourceDomino-setup");
		// Keep the address
		// IDAVAddressInformation repAdr = (IDAVAddressInformation)
		// this.getRepository();
		// LOGGER.info("-----repositoryInternalAddr="+repAdr.getInternalAddress());
		LOGGER.info("FPATH=" + fpath);
		// LOGGER.info("-----repositorypubhref="+repAdr.getPublicHref());
		// LOGGER.info("url="+url);
		// if(repAdr.getInternalAddress().equals(fpath) &&
		// (!url.startsWith(repAdr.getPublicHref()))){
		// throw new DAVNotFoundException();
		// }

		this.setInternalAddress(fpath);
		if (fpath.equals("")) {
			return;
			// throw new DAVNotFoundException();
		}
		LOGGER.info("Output url=" + fpath);

		// FIXME XXX TODO Borked code!
		// Next check -- if Repository and Resource have the same path then the
		// resource it top level
		if (fpath.equals(((IDAVAddressInformation) rep).getInternalAddress())) {
			LOGGER.info("Repository and Resource address match:" + fpath);
			this.setName(((IDAVAddressInformation) rep).getName());
			LOGGER.info(this.getName() + " is the repository-resource at "
					+ this.getInternalAddress());
			this.setCollection(true);
			this.setResourceType("DominoRepository");
			// this.setMember(false);
			// this.fetchChildren();
			this.setPublicHref(repository.getPublicHref());
			this.setMember(isMember);
			if (!this.isMember()) {
				// search for members (children)
				this.fetchChildren();
			}
			return;
		}

		LOGGER.info("Url after extracting repository=" + url);
		if (url.equals("")) {
			return;
		}
		Document doc = repository.getDocumentByKey(url);
		if (doc == null) {
			LOGGER.info("ERROR doc is null");
			return;
		} else {
			LOGGER.info("OK found doc with key =" + url);
			this.setup(doc);
		}
		this.setMember(isMember);
		if (!this.isMember()) {
			// search for members (children)
			this.fetchChildren();
		}
	}

	public String getTempfileName() {
		// We check for a directory with the UNID of the resource
		// and write the file there
		String unid = this.getDocumentUniqueID();
		File returnFile = null;

		// Check for the root temp Dir
		String rootTempDir = this.getRepository().getTempDir();

		// Get the username in base64 encoded, so we can use it as temp file
		// name

		String userDir = null;
		try {
			String userName = DominoProxy.getUserName();
			userDir = Base64.encode(userName.getBytes());
		} catch (Exception e) {
			LOGGER.error(e);
			userDir = "Anonymous";
		}

		// Save the values for reuse
		this.tempFileDir = rootTempDir + File.separator + userDir
				+ File.separator + unid;

		// Create the directory if needed
		File curTempDir = new File(this.tempFileDir);
		if (!curTempDir.exists()) {
			// Create the directory structure - we keep the user dir
			curTempDir.mkdirs();
		} else if (!curTempDir.isDirectory()) {
			// Very bad
			LOGGER.error("Tempdir exists and is not a directory: "
					+ this.tempFileDir);
			return null;
		}
		// Update the full value
		this.tempFileDir = curTempDir.getAbsolutePath();

		// Now create the file object
		String trueFileName = this.tempFileDir + File.separator
				+ this.getName();
		returnFile = new File(trueFileName);
		if (returnFile.exists()) {
			return trueFileName;
		}
		return "";
	}

	public void patchCreationDate(Date date) {
		Document curDoc = null;
		String notesURL = this.getInternalAddress();
		curDoc = DominoProxy.getDocument(notesURL);
		if (curDoc == null) {
			return;
		}
		if (curDoc instanceof Document) {
			try {
				Session ses = DominoProxy.getUserSession();
				DateTime dt = ses.createDateTime(date);
				curDoc.replaceItemValue("DAVCreated", dt);
				curDoc.save(true);
				this.setCreationDate(date);
			} catch (Exception ne) {
				LOGGER.info("Error. Can not change DAVCreated");
			}
		}

	}

	public void patchLastModified(Date date) {
		Document curDoc = null;
		String notesURL = this.getInternalAddress();
		curDoc = DominoProxy.getDocument(notesURL);
		if (curDoc == null) {
			return;
		}
		if (curDoc instanceof Document) {
			try {
				Session ses = DominoProxy.getUserSession();
				DateTime dt = ses.createDateTime(date);
				curDoc.replaceItemValue("DAVModified", dt);
				curDoc.save(true);
				this.setLastModified(date);
			} catch (Exception ne) {
				LOGGER.info("Error. Can not change DAVCModified");
			}
		}

	}

	private DAVResourceDominoCategorizedDocuments getDocumentResource(
			Document curDoc) {

		// The result we are giving back
		DAVResourceDominoCategorizedDocuments curRes = null;

		curRes = new DAVResourceDominoCategorizedDocuments(this.getRepository());
		curRes.setup(curDoc);
		// LOGGER.info(getCallingMethod()+":"+"End getDocumentResource OK!");
		return curRes;
	}

}
