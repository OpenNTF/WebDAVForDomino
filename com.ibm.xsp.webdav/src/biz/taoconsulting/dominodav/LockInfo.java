/* ========================================================================== *
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
package biz.taoconsulting.dominodav;

import java.security.MessageDigest;
import java.util.Date;

import biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation;
import biz.taoconsulting.dominodav.interfaces.IDAVResource;

/**
 * @author Bastian Buch (TAO Consulting), Stephan H. Wissel
 * 
 */
public class LockInfo {

	/**
	 * Path to the resource, including the repository
	 */
	private String pathinfo;

	/**
	 * The current Lock Token
	 */
	private String token;

	/**
	 * Timestamp for Lock creattion
	 */
	private long timestamp;

	/**
	 * Expiry of the lock date/time
	 */
	private Date expirydate;

	/**
	 * Timeout in Seconds (typically 180 sec)
	 */
	private Long timeout;

	/**
	 * User of the lock
	 */
	private String userName;

	/**
	 * The userName used in the lock request might be different since we request
	 * cross domain
	 */
	private String localUsername;

	/**
	 * 
	 * @param res
	 *            the resource to obtain the lockinfo
	 */
	public LockInfo(IDAVResource res) {
		this.setUsername(res.getOwner().getCredentials().getUserName());
		this.setLocalUsername(this.getUsername()); // Remote and local user name
													// are initially the same
		this.setPathinfo(((IDAVAddressInformation) res).getInternalAddress());
		this.setTimestamp(System.currentTimeMillis());
		this.setToken(this.createToken());
	}

	/**
	 * Generation of a lock token
	 * 
	 * @return the token
	 */
	private String createToken() {
		String token = "";
		// Timestamp: The creation time of the Lock
		String txt = Long.toString(this.timestamp);
		// the path of the locked resource
		txt += this.pathinfo;
		// the userName of the lock owner
		txt += "/" + this.userName;
		// token: MD5(time + owner + path) als Hex-String
		try {
			// TODO Is UTF-8 encoding needed?
			byte[] theTextToDigestAsBytes = txt.getBytes("8859_1"); // encoding
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(theTextToDigestAsBytes);
			byte[] digest = md.digest();
			// dump out the hash
			for (int i = 0; i < digest.length; i++) {
				token += Integer.toHexString(digest[i] & 0xff);
			}
			token = "opaquelocktoken:" + token;
		} catch (Exception e) {
			token = txt;
		}
		return token;
	}

	/**
	 * 
	 * @return the new expire date
	 */
	public Date extenExpiryDate() {
		this.setExpiryDate(new Date(System.currentTimeMillis()
				+ (timeout.longValue() * 1000)));
		return this.getExpirydate();
	}

	/**
	 * 
	 * @return The expirydate of the Lock
	 */
	public Date getExpirydate() {
		return this.expirydate;
	}

	/**
	 * Link to the locked resource
	 */
	// private DAVAbstractResource resource;
	public String getLocalUsername() {
		return localUsername;
	}

	/**
	 * 
	 * @return String - path of the resource
	 */
	public String getPathinfo() {
		return this.pathinfo;
	}

	/**
	 * 
	 * @return timeout value of the lock
	 */
	public Long getTimeout() {
		return this.timeout;
	}

	/**
	 * 
	 * @return Timestamp information
	 */
	public long getTimestamp() {
		return this.timestamp;
	}

	/**
	 * 
	 * @return The lock token
	 */
	public String getToken() {
		return this.token;
	}

	/**
	 * 
	 * @return The user of this lock-Token
	 */
	public String getUsername() {
		return this.userName;
	}

	/**
	 * @return boolean is the Lock expired
	 */
	public boolean isExpired() {
		long now = System.currentTimeMillis();
		long exp = this.expirydate.getTime();
		return (now > exp);
	}

	/**
	 * 
	 * @param expirydate
	 *            The expirydate of the Lock
	 */
	private void setExpiryDate(Date expirydate) {
		this.expirydate = expirydate;
	}

	/**
	 * Since we might work cross Domain we need to distinguish local (client)
	 * and remote (Domino) user names
	 * 
	 * @param localUsername
	 */
	public void setLocalUsername(String localUsername) {
		if (localUsername == null) {
			this.localUsername = this.userName;
		} else {
			this.localUsername = localUsername;
		}
	}

	/**
	 * 
	 * @param pathinfo
	 *            Path to the resource
	 */
	private void setPathinfo(String pathinfo) {
		this.pathinfo = pathinfo;
	}

	/**
	 * 
	 * @param timeout
	 *            timeout value of the lock
	 */
	public void setTimeout(Long timeout) {
		// assuming that timout is given in seconds, we compute ms by ...
		this.timeout = timeout;
		long timeoutInMs = timeout.longValue() * 1000;
		this.setExpiryDate(new Date(this.timestamp + timeoutInMs));
	}

	/**
	 * 
	 * @param timestamp
	 *            Timestamp information
	 */
	private void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * 
	 * @param token
	 *            The lock token
	 */
	private void setToken(String token) {
		this.token = token;
	}

	/**
	 * 
	 * @param userName
	 *            The user of this lock-Token
	 */
	private void setUsername(String username) {
		this.userName = username;
	}

}
