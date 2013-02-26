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
package com.ibm.xsp.webdav;

import java.io.IOException;
import java.io.Serializable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import lotus.domino.NotesException;
import lotus.domino.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ibm.misc.BASE64Decoder;
import com.ibm.xsp.webdav.domino.DominoProxy;

// 2008-06-13 Stephan H. Wissel deactivated websphere stuff to move back to domino

/**
 * DAV Credentials provide a single object to hold username, password and
 * LTPA-Token
 * 
 * @author Stephan H. Wissel
 * 
 */
public class DAVCredentials implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The logger object for event logging
	 */
	private static final Log LOGGER = LogFactory.getLog(DAVCredentials.class);

	/**
	 * Stores common credentials from a cookie
	 */
	private String ltpaToken = "";

	/**
	 * The username of the current session;
	 */
	private String userName = "";

	/**
	 * The password, retrievable when Basic authentication is used
	 */
	private String passWord = "";

	/**
	 * Stores common credentials two from a cookie
	 */
	private String ltpaToken2 = "";

	/**
	 * The default constructor is empty to allow serialization/deserialization
	 * after creating a credentials object update it calling the
	 * updateCredentials method
	 * 
	 */
	public DAVCredentials() {
		// No action taken, needed for serialization/deserialization
	}

	/**
	 * 
	 * @return true if the LTPA Token has a value
	 */
	public boolean hasLTPAtoken() {
		return (this.ltpaToken != null && !this.ltpaToken.equals(""));
	}

	/**
	 * 
	 * @return true if the LTPA Token has a value
	 */
	public boolean hasLTPAtoken2() {
		return (this.ltpaToken2 != null && !this.ltpaToken2.equals(""));
	}

	/**
	 * @return Returns the passWord.
	 */
	public String getPassWord() {
		return this.passWord;
	}

	/**
	 * @return Returns the userName.
	 */
	public String getUserName() {
		return this.userName;
	}

	/**
	 * @see java.lang.Object#toString() Some meaningfull stuff regarding the
	 *      credentials
	 */
	public String toString() {
		StringBuffer b = new StringBuffer(128);
		b.append("User: \"");
		b.append(this.userName);
		b.append("\" ");
		if (this.passWord != null && !this.passWord.equals("")) {
			// if (this.unsaveDebug) {
			// b.append("Password: \"");
			// b.append(this.passWord);
			// b.append("\"");
			// } else {
			b.append(" with a password. ");
			// }
		} else {
			b.append(" -no password- ");
		}
		if (this.hasLTPAtoken()) {
			b.append(" LTPA: ");
			b.append(this.ltpaToken);
		}

		if (this.hasLTPAtoken2()) {
			b.append(" LTPA2: ");
			b.append(this.ltpaToken2);
		}

		if (!(this.hasLTPAtoken() || this.hasLTPAtoken2())) {
			b.append(" (no LTPA token)");
		}

		return b.toString();
	}

	/**
	 * @return the LTPA Token
	 */
	public String getLTPAtoken() {
		return this.ltpaToken;
	}

	/**
	 * @return the LTPA Token 1
	 */
	public String getLTPAtoken2() {
		return this.ltpaToken2;
	}

	/**
	 * Updates a new or existing Credential with the user's authentication
	 * information
	 * 
	 * @param req
	 *            Servlet Request with credential header information
	 */
	public void updateCredentials(HttpServletRequest req) {
		// We retrieve username from the session and check
		// if we have a password from basic authentication
		// in the header. LTPATokens are retrieved from the
		// session or request header

		Session s = null;

		String authType = req.getAuthType(); // Web Authentication type;
		LOGGER.debug("Authentication type: " + authType);

		// First the cookies
		this.updateLTPAfromRequest(req);

		try {
			s = DominoProxy.getUserSession(); // NotesSession
			// TODO: Figure out if we can use s.getCredentials();
			this.userName = s.getUserName();
			this.updateLTPAfromSession(s, req);

		} catch (NotesException e) {
			LOGGER.error(
					"Failed to retrieve username from NotesSession:"
							+ e.getMessage(), e);
		}

		// Get data from the basic authorization - might overwrite the
		// username
		String authHeader = req.getHeader("Authorization");
		if (authHeader == null) {
			// There is no authentication information
			LOGGER.trace("No Authorization header for new user information found, User:"
					+ this.userName);
		} else {
			String decLog = authHeader.substring(authType.length() + 1);
			BASE64Decoder d = new BASE64Decoder();

			try {
				String result = new String(d.decodeBuffer(decLog));
				// Now we have a String username:password
				if (result.indexOf(":") < 0) {
					// Something went wrong, we don't have a : in the string
					LOGGER.error("Maleformed username/password: " + result);
				} else {
					// Store it
					String usrpwd[] = result.split(":");
					// The username in the basic authentication might be used in
					// other
					// places so we keep that one for the moment
					this.userName = usrpwd[0].trim().equals("") ? this.userName
							: usrpwd[0];
					this.passWord = usrpwd[1];
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		LOGGER.debug(this.toString());

	}

	/**
	 * Gets the LTPA Tokens from the request if they can be found
	 * 
	 * @param req
	 */
	private void updateLTPAfromRequest(HttpServletRequest req) {
		// Look for LTPA Tokens in the cookies
		Cookie[] cookies = req.getCookies();
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				if (cookies[i].getName().equals("LtpaToken")) {
					this.ltpaToken = cookies[i].getValue();
					LOGGER.debug("Found LTPA Token:" + this.ltpaToken);
				}
				if (cookies[i].getName().equals("LtpaToken2")) {
					this.ltpaToken2 = cookies[i].getValue();
					LOGGER.debug("Found LTPA Token 2:" + this.ltpaToken2);
				}
			}
		}
	}

	/**
	 * Gets session information from the NotesSession
	 * 
	 * @param req
	 */
	private void updateLTPAfromSession(Session s, HttpServletRequest req) {
		// Now Session token from Domino session

		// Seems to have issues here
		return; // Disabled for now
		/*
		 * if (s == null) { return; }
		 * 
		 * try { String sessionToken = s.getSessionToken(); if (sessionToken !=
		 * null && !sessionToken.equals("")) { this.ltpaToken = sessionToken; }
		 * } catch (NotesException e) { LOGGER.error(e); }
		 */
	}

}
