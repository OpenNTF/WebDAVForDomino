/** ========================================================================= *
 * Copyright (C) 2011, 2012 IBM Corporation                                   *
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

import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ibm.domino.osgi.core.context.ContextInfo;

import lotus.domino.Base;
import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.Session;
import lotus.domino.View;

/**
 * The class bundles all access to Domino related classes to reduce the
 * dependencies on the Domino access classes in the rest of the application So
 * every method that depends on the deployment model (local, server, OSGi
 * plug-in will be in this class. General methods like forms/views/documents can
 * stay where they are
 * 
 * @author notessensei
 * 
 */
public class DominoProxy {

	/**
	 * The logger object for event logging
	 */
	private static final Log LOGGER = LogFactory.getLog(DominoProxy.class);

	/**
	 * Returns a NotesSession with whatever mechanism is needed
	 * 
	 * @return
	 */
	public static Session getUserSession() {

		// TODO: make this configurable so contextInfo might not be needed
		Session s = ContextInfo.getUserSession();
		LOGGER.debug("Created NotesSession:" + s.toString());
		return s;

	}

	/**
	 * Returns a Notes Java object. Can be a database, view or document
	 * 
	 * @param notesURL
	 * @return
	 */
	public static Base resolve(String notesURL) {
		Session s = DominoProxy.getUserSession();
		Base result = null;

		try {
			result = s.resolve(notesURL);
			LOGGER.debug("Notes URL resolves (general) for :" + notesURL
					+ " into " + result.getClass().getName());
		} catch (NotesException e) {
			LOGGER.error(e);
			LOGGER.error("Notes resolve (general) failed with "
					+ e.getMessage() + " for :" + notesURL);
			int lastNSF = notesURL.lastIndexOf(".nsf");
			if (lastNSF > 0) {
				String restPath = notesURL.substring(lastNSF + 4);
				String[] tok = restPath.split("/");
				// LOGGER.info("URL input="+restPath);
				if (tok.length > 0) {
					for (int i = 0; i < tok.length; i++) {
						// LOGGER.info("Token ["+new Integer(i).toString()+
						// "]="+tok[i]);
					}

				}
			}
			result = null;
		}

		return result;
	}

	/**
	 * Returns a database based on a NotesURL given
	 * 
	 * @param notesURL
	 * @return the NotesDatabase
	 */
	public static Database getDatabase(String notesURL) {
		Base notesObj = DominoProxy.resolve(notesURL);
		if (notesObj == null) {
			return null;
		}
		// TODO: do we need to check the parameters?
		if (notesObj instanceof Database) {
			return (Database) notesObj;
		}

		LOGGER.error("Notes database resolve failed for :" + notesURL);
		// It is something else
		return null;
	}

	/**
	 * Returns a Notes View/Folder based on a NotesURL given
	 * 
	 * @param notesURL
	 * @return the NotesView
	 */
	public static View getView(String notesURL) {
		Base notesObj = DominoProxy.resolve(notesURL);
		if (notesObj == null) {
			return null;
		}

		if (notesObj instanceof View) {
			return (View) notesObj;
		}
		LOGGER.error("Notes view resolve failed for :" + notesURL);
		// It is something else
		return null;
	}

	/**
	 * Returns a document based on a NotesURL given
	 * 
	 * @param notesURL
	 * @return the NotesDocument
	 */
	public static Document getDocument(String notesURL) {
		Base notesObj = DominoProxy.resolve(notesURL);
		if (notesObj == null) {
			return null;
		}

		if (notesObj instanceof Document) {
			return (Document) notesObj;
		}

		LOGGER.error("Notes document resolve failed for :" + notesURL);
		// It is something else
		return null;
	}

	@SuppressWarnings("rawtypes")
	public static Vector evaluate(String formula, Document doc) {

		Session s = DominoProxy.getUserSession();
		Vector result = null;
		try {
			result = s.evaluate(formula, doc);
		} catch (NotesException e) {
			LOGGER.error(e);
			result = null;
		}

		return result;
	}

	public static String getUserName() {
		Session s = DominoProxy.getUserSession();
		if (s != null) {
			try {
				return s.getUserName();
			} catch (NotesException e) {
				LOGGER.error(e);
			}
		}
		return "Anonymous";
	}
}
