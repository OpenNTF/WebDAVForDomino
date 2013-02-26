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
import lotus.domino.Document;
import lotus.domino.DocumentCollection;
import lotus.domino.EmbeddedObject;
import lotus.domino.NotesException;
import lotus.domino.Session;
import lotus.domino.View;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xerces.impl.dv.util.Base64;

import biz.taoconsulting.dominodav.exceptions.DAVNotFoundException;
import biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation;
import biz.taoconsulting.dominodav.interfaces.IDAVRepository;
import biz.taoconsulting.dominodav.interfaces.IDAVResource;

import com.ibm.xsp.webdav.domino.AttachmentInputStream;
import com.ibm.xsp.webdav.domino.AttachmentOutputStream;
import com.ibm.xsp.webdav.domino.DominoProxy;

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
public class DAVResourceDominoAttachments extends DAVResourceDomino {

	/**
	 * Directory composed out of temp directory, username, unid and File name
	 */
	private String tempFileDir;

	/**
	 * We need to ensure that a document is considered a collection
	 */
	@Override
	public boolean isCollection() {
		if ("NotesDocument".equals(this.getResourceType())) {
			return true;
		}
		return super.isCollection();
	}

	/**
	 * Logger for Errors
	 */
	private static final Log LOGGER = LogFactory
			.getLog(DAVResourceDominoAttachments.class);

	/**
	 * @param repository
	 *            the repository the Resource is in
	 * @param url
	 *            the path relative to the repository -- as seen from the
	 *            browser
	 * @throws DAVNotFoundException
	 *             --- if the file is not there
	 */
	public DAVResourceDominoAttachments(IDAVRepository repository, String url)
			throws DAVNotFoundException {
		super(repository, url);
		this.populateAttachmentPropertiesFromNotesDoc();

	}

	/**
	 * Light version
	 * 
	 * @param repository
	 */
	public DAVResourceDominoAttachments(IDAVRepository repository) {
		this.setOwner(repository);
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
	public DAVResourceDominoAttachments(IDAVRepository rep, String url,
			boolean isMember) throws DAVNotFoundException {
		super(rep, url, isMember);
		this.populateAttachmentPropertiesFromNotesDoc();

	}

	/**
	 * @see biz.taoconsulting.dominodav.resource.DAVAbstractResource#delete()
	 */
	public boolean delete() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @see biz.taoconsulting.dominodav.resource.DAVAbstractResource#getOutputStream()
	 */
	public OutputStream getOutputStream() {
		// This returns the OutputStream when writing back to the class

		if (this.out == null) {
			this.out = new AttachmentOutputStream(this);
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
				curDoc = (Document) s.resolve(docURL);
				String curAttName = this.getName();
				EmbeddedObject curAttachment = curDoc.getAttachment(curAttName);
				File tempFile = this.getTempfile();
				// Delete if it exists
				if (tempFile.exists()) {
					tempFile.delete();
				}
				curAttachment.extractFile(tempFile.getAbsolutePath());
				curStream = new AttachmentInputStream(tempFile, this);
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

		if (this.isMember()) {
			LOGGER.debug("No fetch children for isMember()= true");
			return;
		}

		// We fetch the right type of children
		String resType = this.getResourceType();

		if ("NotesDocument".equals(resType)) {
			this.fetchChildrenForNotesDocument();
		} else if ("NotesAttachment".equals(resType)) {
			// Notes Attachments don't have children
		} else if ("NotesDatabase".equals(resType)) {
			this.fetchChildrenForNotesDatabase();
		} else {
			// We presume a view for the resource type
			fetchChildrenForNotesView();
		}

	}

	private void fetchChildrenForNotesDocument() {
		LOGGER.debug("Fetching children for " + this.getName());

		Document curDoc = null;
		String docID = null;
		Vector<IDAVResource> resMembers = new Vector<IDAVResource>();
		String notesURL = this.getInternalAddress();

		curDoc = DominoProxy.getDocument(notesURL);

		if (curDoc == null) {
			LOGGER.error("Could not retrieve the document");
			return;
		}

		// Read the repository list to get the view
		try {
			docID = curDoc.getUniversalID();

			LOGGER.debug("Openend document " + docID);

			// No children if there are no attachments
			if (!curDoc.hasEmbedded()) {
				// E.C. It is a directory, so fetch the children documents as
				// resources
				LOGGER.error("Current doc with unid=" + curDoc.getUniversalID()
						+ " has no embedded files. Try to find children");
				DocumentCollection responses = curDoc.getResponses();
				int numOfResponses = responses.getCount();
				if (numOfResponses > 0) {
					LOGGER.error("Current doc has " + numOfResponses
							+ " responses");
					Document docResp = responses.getFirstDocument();
					while (docResp != null) {
						LOGGER.error("Doc response has unid="
								+ docResp.getUniversalID());
						@SuppressWarnings("rawtypes")
						Vector allEmbedded = DominoProxy.evaluate(
								"@AttachmentNames", docResp);
						int numOfAttchments = allEmbedded.size();
						if (numOfAttchments == 0) { // No attachments in here!
							LOGGER.error("Doc response has no attachment; is a directory");
							LOGGER.debug(docID
									+ " has no attachments (@AttachmentNames)"); // embed
																					// as
																					// doc
							DAVResourceDominoAttachments resAtt = getDocumentResource(docResp);
							if (resAtt != null) {
								LOGGER.error("Created DavResourceDomino Attachments from getDocumentResource-OK");
								resMembers.add(resAtt);
								LOGGER.error("Resource successfull added");
							} else {
								LOGGER.error("Problem, DavResourceDomino Attachments from getDocumentResource- FAILED");
							}
						} else {
							LOGGER.error("Doc response has attachments;");
							String curAttName = allEmbedded.get(0).toString();
							DAVResourceDominoAttachments curAttachment = getAttachmentResource(
									this.isReadOnly(), docResp, curAttName);
							if (curAttachment != null) {
								// Now add it to the Vector
								LOGGER.error("Created DavResourceDominoAttachments with getAttachmentResource-OK");
								resMembers.add(curAttachment);
								LOGGER.error("Resource successfull added");
								Date viewDate = this.getLastModified();
								Date docDate = curAttachment.getLastModified();
								if (viewDate == null
										|| (docDate != null && viewDate
												.before(docDate))) {
									this.setLastModified(docDate);
								}
								LOGGER.error("Resource successfull updated last modified");

								LOGGER.debug("Processing complete attachment "
										+ curAttName);
							} else {
								LOGGER.error("Could not load attachment "
										+ curAttName);
							}
						}
						Document docTmp = docResp;
						docResp = responses.getNextDocument(docResp);
						docTmp.recycle();
					} // end while

				} // end if numresp>0

				try {

					if (curDoc != null) {
						curDoc.recycle();
					}

				} catch (Exception e) {
					LOGGER.error(e);
				}
				// Now save back the members to the main object
				this.setMembers(resMembers);
				return;
			}

			// Get all attachments
			@SuppressWarnings("rawtypes")
			Vector allEmbedded = DominoProxy.evaluate("@AttachmentNames",
					curDoc);
			int numOfAttchments = allEmbedded.size();
			if (numOfAttchments == 0) { // No attachments in here!
				LOGGER.debug(docID + " has no attachments (@AttachmentNames)");
				return;
			}
			LOGGER.debug(docID + " has "
					+ new Integer(numOfAttchments).toString()
					+ " attachment(s)");
			// Initialize an empty vector at the right size
			// We might need to enlarge it if we have more attachments
			resMembers = new Vector<IDAVResource>(numOfAttchments);

			for (int i = 0; i < numOfAttchments; i++) {

				String curAttName = allEmbedded.get(i).toString();
				DAVResourceDominoAttachments curAttachment = getAttachmentResource(
						this.isReadOnly(), curDoc, curAttName);

				if (curAttachment != null) {
					// Now add it to the Vector
					resMembers.add(curAttachment);
					LOGGER.debug("Processing complete attachment " + curAttName);
				} else {
					LOGGER.error("Could not load attachment " + curAttName);
				}
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

			} catch (Exception e) {
				LOGGER.error(e);
			}
			// Now save back the members to the main object
			this.setMembers(resMembers);
			LOGGER.debug("Completed reading attachments resources from Notes document");
		}

	}

	private void fetchChildrenForNotesView() {
		LOGGER.debug("Fetching children for " + this.getName());

		Document curDoc = null;
		Document nextDoc = null;

		View curView = null;
		Database curDb = null;
		Vector<IDAVResource> resMembers = null;
		String notesURL = this.getInternalAddress();

		curView = DominoProxy.getView(notesURL);

		if (curView == null) {
			LOGGER.error("Could not retrieve view: " + notesURL);
			return;
		}

		// Read the repository list to get the view
		try {
			LOGGER.debug("Openend view " + curView.getName());

			// Initialize an empty vector at the right size
			// We might need to enlarge it if we have more attachments
			resMembers = new Vector<IDAVResource>(curView.getEntryCount());

			curDoc = curView.getFirstDocument();

			if (curDoc == null) {
				LOGGER.info(this.getName()
						+ " does not (yet) contain resources");
				return;
			}

			while (curDoc != null) {
				nextDoc = curView.getNextDocument(curDoc);
				// TODO: Fix this!
				DAVResourceDominoAttachments docRes = this
						.addAttachmentsFromDocument(curDoc);

				if (docRes != null) {
					resMembers.add(docRes);

					// Capture last modified based on the latest date of the
					// documents in view
					Date viewDate = this.getLastModified();
					Date docDate = docRes.getLastModified();
					if (viewDate == null
							|| (docDate != null && viewDate.before(docDate))) {
						this.setLastModified(docDate);
					}

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

				if (curView != null) {
					curView.recycle();
				}

				if (curDb != null) {
					curDb.recycle();
				}

			} catch (Exception e) {
				LOGGER.error(e);
			}

			LOGGER.debug("Completed reading file resources from Domino view");
		}
		// Now save back the members to the main object
		this.setMembers(resMembers);
	}

	private void fetchChildrenForNotesDatabase() {
		LOGGER.debug("Fetching children for " + this.getName());

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
			LOGGER.debug("Openend databasew " + curDb.getFileName());

			// Initialize an empty vector at the right size
			// We might need to enlarge it if we have more attachments
			curEntries = curDb.getAllDocuments();
			resMembers = new Vector<IDAVResource>(curEntries.getCount());

			curDoc = curEntries.getFirstDocument();

			if (curDoc == null) {
				LOGGER.info(this.getName()
						+ " does not (yet) contain resources");
				return;
			}

			while (curDoc != null) {
				nextDoc = curEntries.getNextDocument(curDoc);

				DAVResourceDominoAttachments docRes = this
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

			LOGGER.debug("Completed reading file resources from Domino view");
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
	private DAVResourceDominoAttachments addAttachmentsFromDocument(
			Document curDoc) {

		String docID = null;
		DAVResourceDominoAttachments docRes = null;

		try {
			docID = curDoc.getUniversalID();
			if (!curDoc.hasEmbedded()) {
				LOGGER.debug(docID + " has no attachments (hasEmbedded)");
			} else {
				docRes = this.getDocumentResource(curDoc);
			}
		} catch (NotesException e) {
			LOGGER.error(e);
			docRes = null;
		}

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
	private DAVResourceDominoAttachments getAttachmentResource(
			boolean readOnly, Document curDoc, String curAttName) {

		LOGGER.debug("Retrieving attachment [" + curAttName + "]");
		EmbeddedObject curAttachment = null;
		DAVResourceDominoAttachments curRes = null;

		try {
			curAttachment = curDoc.getAttachment(curAttName);
			if (curAttachment == null) {
				return null;
			}
			String docID = curDoc.getUniversalID();
			Date curCreationDate = curDoc.getCreated().toJavaDate();
			Date curChangeDate = curDoc.getLastModified().toJavaDate();
			Long curSize = new Long(curAttachment.getFileSize());
			String curName = curAttachment.getName();
			String curInternalPath = docID + "/$File/"
					+ curAttachment.getName();
			// We hide the $File from external resources
			String curExternalPath = docID + "/" + curAttachment.getName();

			LOGGER.debug("Processing Attachment " + curName + " ("
					+ curInternalPath + ")");

			// Now the repository
			String pubHRef = ((IDAVAddressInformation) this.getRepository())
					.getPublicHref() + "/" + curExternalPath;
			curRes = new DAVResourceDominoAttachments(this.getRepository());
			curRes.setPublicHref(pubHRef);
			curRes.setMember(true);
			curRes.setName(curName);
			curRes.setInternalAddress(((IDAVAddressInformation) this
					.getRepository()).getInternalAddress()
					+ "/"
					+ curInternalPath);
			curRes.setOwner(this.getRepository());
			curRes.setResourceType("NotesAttachment");
			curRes.setCreationDate(curCreationDate);
			curRes.setLastModified(curChangeDate);
			curRes.setContentLength(curSize);
			curRes.setReadOnly(readOnly);
		} catch (NotesException e) {
			LOGGER.error(e);
		}

		return curRes;
	}

	private DAVResourceDominoAttachments getDocumentResource(Document curDoc) {

		// The result we are giving back
		DAVResourceDominoAttachments curRes = null;

		try {
			String docID = curDoc.getUniversalID();
			LOGGER.debug("Creating resource for document [" + docID + "]");

			boolean readOnly = this.checkReadOnlyAccess(curDoc);
			Date curCreationDate = curDoc.getCreated().toJavaDate();
			Date curChangeDate = curDoc.getLastModified().toJavaDate();
			String curPath = docID; // ?OpenDocument doesn't work with DAV +
			// "?OpenDocument";
			String curName = docID; // ToDo -- better naming scheme for
			// documents e.g. when view is sorted
			// Now the repository
			String pubHRef = this.getPublicHref() + "/" + curPath;
			curRes = new DAVResourceDominoAttachments(this.getRepository(),
					pubHRef, true);
			curRes.setResourceType("NotesDocument");
			curRes.setMember(true);
			curRes.setCollection(true); // We treat it as a directory
			curRes.setName(curName);
			curRes.setInternalAddress(((IDAVAddressInformation) this
					.getRepository()).getInternalAddress() + "/" + curPath);
			curRes.setOwner(this.getRepository());
			curRes.setCreationDate(curCreationDate);
			curRes.setLastModified(curChangeDate);
			curRes.setContentLength(0L);
			curRes.setReadOnly(readOnly);
		} catch (NotesException e) {
			LOGGER.error(e);
		} catch (DAVNotFoundException e) {
			LOGGER.error(e);
		}
		return curRes;
	}

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
	private void populateAttachmentPropertiesFromNotesDoc() {
		String thisResType = this.getResourceType();
		if (!"NotesAttachment".equals(thisResType)
				&& !"NotesDocument".equals(thisResType)) {
			return; // This is for attachments and documents only
		}

		String adr = this.getInternalAddress();
		String notesURL = "NotesAttachment".equals(thisResType) ? this
				.getDocURLFromAttachmentURL(adr) : adr;
		Document curDoc = DominoProxy.getDocument(notesURL);

		try {

			if (curDoc == null) {
				LOGGER.error("Could not retrieve Notes doc " + notesURL);
			} else {

				boolean readOnly = this.checkReadOnlyAccess(curDoc);
				Date curCreationDate = curDoc.getCreated().toJavaDate();
				Date curChangeDate = curDoc.getLastModified().toJavaDate();
				this.setCreationDate(curCreationDate);
				this.setLastModified(curChangeDate);
				this.setReadOnly(readOnly);

				EmbeddedObject curAtt = curDoc.getAttachment(this.getName());
				// Content length
				if (curAtt != null) {
					this.setContentLength(new Long(curAtt.getFileSize())
							.longValue());
				}
			}
		} catch (NotesException e) {
			LOGGER.error(e);
		} finally {
			try {
				if (curDoc != null) {
					curDoc.recycle();
				}
			} catch (NotesException e) {
				LOGGER.error(e);
			}
		}

	}

	/**
	 * We return the NotesURL including ?OpenDocument
	 * 
	 * @param internalAddress
	 * @return
	 */
	private String getDocURLFromAttachmentURL(String internalAddress) {

		int maxLen = internalAddress.indexOf("/$File");
		if (maxLen < 0) {
			maxLen = internalAddress.lastIndexOf("/");
		}
		if (maxLen < 0) {
			LOGGER.error("No proper NotesURL found for " + internalAddress);
			return internalAddress;
		}

		return internalAddress.substring(0, maxLen) + "?OpenDocument";

	}

	public void patchLastModified(Date dt) {
	}

	public void patchCreationDate(Date dt) {
	}

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
}
