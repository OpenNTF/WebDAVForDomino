/** ========================================================================= *
 * Copyright (C) 2012 IBM Corporation                                         *
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
package com.ibm.xsp.webdav.domino;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ibm.domino.osgi.core.context.ContextInfo;
import com.ibm.xsp.webdav.repository.DAVRepositoryDominoDocuments;
import com.ibm.xsp.webdav.resource.DAVResourceDominoDocuments;

import lotus.domino.Document;
import lotus.domino.EmbeddedObject;
import lotus.domino.Item;
import lotus.domino.NotesException;
import lotus.domino.RichTextItem;
import lotus.domino.Session;

/**
 * The Domino output stream takes the stream, stores it into a temporary file
 * 
 * @author Stephan H. Wissel
 * 
 */
public class DocumentOutputStream extends DominoOutputStream {
	/**
	 * Logger for Errors
	 */
	private static final Log LOGGER = LogFactory
			.getLog(DocumentOutputStream.class);

	/**
	 * Creates a OutputStream that streams to a temporary file This file will
	 * later be attached in Domino as attachment
	 * 
	 * @param res
	 *            DAVResourceDomino
	 */
	public DocumentOutputStream(DAVResourceDominoDocuments res) {
		super();
		this.setResource(res);
	}

	/**
	 * setResource needs to be called when constructor was used without
	 * parameter;
	 * 
	 * @param res
	 *            DAVResourceDomino
	 */
	public void setResource(DAVResourceDominoDocuments res) {
		super.setResource(res);
		File tempFile = res.getTempfile();
		try {
			this.out = new FileOutputStream(tempFile);
		} catch (FileNotFoundException e) {
			LOGGER.error("File stream creation failed", e);
		}
	}

	/**
	 * Closes the stream and copies the temp file into the Domino object
	 * 
	 * @throws IOException
	 *             If something odes wrong
	 */
	public void close() throws IOException {
		// Write out the file object
		super.close();
		// // LOGGER.info("Start close...");
		boolean success = true; // We assume it will work
		Document curDoc = null;
		String docURL = null;
		RichTextItem body = null;

		// NotesSession
		Session s = ContextInfo.getUserSession();
		if (s == null) {
			LOGGER.error("Could not establish Notes Session");
			success = false;
			return;
		}
		// // LOGGER.info("Before resource");
		DAVResourceDominoDocuments res = (DAVResourceDominoDocuments) this
				.getResource();
		DAVRepositoryDominoDocuments repository = (DAVRepositoryDominoDocuments) res
				.getRepository();

		if (res.isCollection()) {
			// // LOGGER.info("Problem! resource is collection");
			return;
		}
		String notesURL = res.getInternalAddress();
		// //
		// LOGGER.info("Resource is not a collection; OK! Internal address="+notesURL);

		// We need to find the $File to isolate the document
		int dollarFile = notesURL.lastIndexOf("/$File");
		if (dollarFile < 0) {
			// This is not an attachment - we presume it is a file
			docURL = notesURL;
		} else {
			docURL = notesURL.substring(0, dollarFile) + "?OpenDocument";
		}

		String trueFileName = null;
		Document oldVersion = null;
		// Now get to the document
		try {
			curDoc = (Document) s.resolve(docURL);

			String curAttName = res.getName();

			// Now check if we can get the attachment and remove it
			EmbeddedObject att = curDoc.getAttachment(curAttName);
			if (att != null) {
				if (repository.versioning()) {
					oldVersion = curDoc.getParentDatabase().createDocument();
					oldVersion.makeResponse(curDoc);
					curDoc.copyAllItems(oldVersion, false);
					String version = "2", oldversion = "1";
					if (curDoc.hasItem("DAVVersion")) {
						version = curDoc.getItemValueString("DAVVersion");
						try {
							if (version.equals("")) {
								oldversion = "1";
								version = "2";
							} else {
								oldversion = version;
								version = new Integer(
										Integer.parseInt(version) + 1)
										.toString();
							}

						} catch (Exception e) {
							version = "2";
							oldversion = "1";
						}
					}
					oldVersion.replaceItemValue("DAVVersion", oldversion);
					// curDoc.replaceItemValue("DAVVersion", version);
					oldVersion.save(true);
				}
				// // LOGGER.info("Attachment exists:"+curAttName);
				att.remove();
				att.recycle();
				att = null;
				// // LOGGER.info("Attachment removed");
			}

			// Now attach it to the Body field - we can't attach to the document
			// directly
			if (curDoc.hasItem("Body")) {
				// // LOGGER.info("Has item Body");
				Item bodyCandidate = curDoc.getFirstItem("Body");
				if (bodyCandidate.getType() == Item.RICHTEXT) {
					// // LOGGER.info("Body is rtichtext");
					body = (RichTextItem) bodyCandidate;
				} else {
					// TODO: is this OK or do we need to do something about it?
					// // LOGGER.info("Body is not a richtextitem; remove it");
					curDoc.removeItem("Body");
					body = curDoc.createRichTextItem("Body");
					// // LOGGER.info("Created rich text item");
				}
			} else {
				// // LOGGER.info("Doc has no Body field;");
				body = curDoc.createRichTextItem("Body");
				// // LOGGER.info("Created rich text item BODY");
			}

			// Finally time to write out
			trueFileName = res.getTempfile().getAbsolutePath();
			// // LOGGER.info("File name="+trueFileName);

			body.embedObject(EmbeddedObject.EMBED_ATTACHMENT, null,
					trueFileName, curAttName);
			// // LOGGER.info("Embeded ...OK");
			if (curDoc.hasItem("DAVCreated")) {
				curDoc.removeItem("DAVCreated");
			}
			if (curDoc.hasItem("DAVModified")) {
				curDoc.removeItem("DAVModified");
			}
			curDoc.save();
			// // LOGGER.info("Saved ...OK");
		} catch (NotesException e) {
			success = false;
			LOGGER.error(e);

		} catch (Exception e) {
			success = false;
			LOGGER.error(e);
		} finally {
			// Recyle the notes objects

			try {

				if (body != null) {
					body.recycle();
				}

				if (curDoc != null) {
					curDoc.recycle();
				}

			} catch (NotesException e) {
				// Not really critical, but we log it
				LOGGER.error("Notes objects recycle failed", e);
			}

		}

		// Remove the temporary file stuff but only if the write was sucessful
		// this way we might be able to recover

		if (success) {

			// // LOGGER.info("Removing the temp file(BUT NOT YET): " +
			// trueFileName);

			// We don't want an error here bubbling up
			try {

				res.removeTempFiles();

				// Close the parent object;
				super.close();

			} catch (Exception e) {
				LOGGER.error("Temp Dir/File cleanup failed", e);
			}

		} else {
			LOGGER.error("Write to Domino unsuccesful, temp files not cleaned for recovery!");
			// We need to throw an error
			throw new IOException();
		}
	}

	/**
	 * lazy init of an output stream
	 */
	protected void initOutputStream() throws IOException {
		if (this.res == null) {
			LOGGER.error("The DominoOutputStream has no ResourceObject");
			throw new IOException();
		}

		File destination = res.getTempfile();
		this.out = new FileOutputStream(destination);
	}

}
