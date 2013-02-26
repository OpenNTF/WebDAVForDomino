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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation;
import biz.taoconsulting.dominodav.interfaces.IDAVResource;

/**
 * @author Bastian Buch (TAO Consulting), Stephan H. Wissel Implemented as Singleton
 */
public class LockManager {
	/**
	 * 
	 * Run periodical cleanup of the LockManager
	 * 
	 * @author Stephan H. Wissel
	 * 
	 */
	private class LockCleaner extends TimerTask {

		/**
		 * The parent class lockmanager
		 */
		private LockManager lockmanager;

		/**
		 * The timer for the cleaning interval
		 */
		private Timer timer = null;

		/**
		 * 
		 * @param lockmanager
		 *            The LockManager to be cleaned
		 */
		LockCleaner(LockManager lockmanager) {
			this.lockmanager = lockmanager;
		}

		/**
		 * Scheduled execution of the cleaner
		 */
		public void run() {
			// Do a cleanup
			this.lockmanager.clean();

		}

		/**
		 * Start the cleanup task
		 * 
		 */
		public void startCleaning() {
			try {
				this.timer = new Timer();
				this.timer.schedule(this, 0, 30000);
			} catch (Exception e) {
				LOGGER.error("StartClean failed", e);
			}
		}
	}

	/**
	 * What is our maximum timeout value we accept
	 */
	public static final Long MAX_LOCK_DURATION_SEC = new Long(300); // 5 minutes
	/**
	 * The logger object for event logging
	 */
	private static final Log LOGGER = LogFactory.getLog(LockManager.class);

	/**
	 * Table with currently locked elements
	 */
	private Hashtable<String, LockInfo> lockTable;

	/**
	 * The lock cleaner background task
	 * 
	 */
	private LockCleaner lockcleaner;

	/**
	 * The internal Object to hold the singleTon
	 */
	private static LockManager internalLockManager;

	/**
	 * 
	 * @return LockManager - the single instance
	 */
	public static synchronized LockManager getLockManager() {

		// Initialize the lock Manager Singleton
		if (internalLockManager == null) {
			internalLockManager = new LockManager();
		}

		return internalLockManager;

	}

	/**
	 * Default constructor is private to implement a singleton
	 */
	private LockManager() {
		this.lockTable = new Hashtable<String, LockInfo>();
		this.lockcleaner = new LockCleaner(this);
		this.lockcleaner.startCleaning();
	}

	/**
	 *
	 */
	public synchronized void clean() {

		List<LockInfo> morituri = new ArrayList<LockInfo>();

		// We collect all the keys to delete in
		for (Map.Entry<String, LockInfo> entry : this.lockTable.entrySet()) {
			LockInfo li = entry.getValue();
			if (li.isExpired()) {
				morituri.add(li);
			}
		}

		// Now we have them, unlock them all
		try {

			for (LockInfo li : morituri) {
				this.unlock(li.getPathinfo(), li.getToken());
			}

		} catch (ConcurrentModificationException c) {
			return;
		}
	}

	/**
	 * Make sure our singleton can't be cloned
	 * 
	 * @return actually nothing -- throws an exception
	 * @throws CloneNotSupportedException
	 *             -- we don't Clown around
	 */
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
		// that'll teach 'em
	}

	/**
	 * 
	 * @param res
	 *            Resource to check
	 * @return Owner of the lock, null if not locked
	 */
	public String getLockOwner(IDAVResource res) {
		String lockKey = ((IDAVAddressInformation) res).getInternalAddress();
		boolean isLocked = this.lockTable.containsKey(lockKey);

		if (isLocked) {
			LockInfo li = this.lockTable.get(lockKey);
			return li.getUsername();
		}

		return null;
	}

	/**
	 * 
	 * @param res
	 *            Resource to check
	 * @return Status of the lock
	 */
	public boolean isLocked(IDAVResource res) {
		return this.lockTable.containsKey(((IDAVAddressInformation) res)
				.getInternalAddress());
	}

	/**
	 * 
	 * @param path
	 *            Resource to check
	 * @return Lockstatus
	 */
	public boolean isLocked(String path) {
		return this.lockTable.containsKey(path);
	}

	/**
	 * Creates a new lock and returns it when successful
	 * 
	 * @param resource
	 * @param lockrequestorName
	 *            - can be different from logged in username
	 * @param timeOutValue
	 *            The timeout value requested
	 * @return a lock object or null if it didn't work
	 */

	public synchronized LockInfo lock(IDAVResource resource,
			String lockrequestorName, Long timeOutValue) {

		LockInfo li = new LockInfo(resource);
		li.setTimeout(timeOutValue);
		li.setLocalUsername(lockrequestorName);

		if (this.lock(li)) {
			return li;
		}

		// Didn't work, so we don't give back the lock-info
		return null;

	}

	/**
	 * @param li
	 *            - LockInfo we want to use to lock
	 * @return true if it worked
	 * 
	 *         Even the same user can lock only once otherwise you overwrite
	 *         your own work
	 */
	private synchronized boolean lock(LockInfo li) {

		String curPath = li.getPathinfo();

		if (this.lockTable.containsKey(curPath)) {
			return false;
		}

		// Lock it in
		li.extenExpiryDate(); // Update the lock
		this.lockTable.put(curPath, li);
		return true;

	}

	/**
	 * @param href
	 *            Resource to lock
	 * @param token
	 *            Existing lock token
	 * @param timeout
	 *            timeout in senconds
	 * @return LockInfo Object
	 */
	public synchronized LockInfo relock(IDAVResource resource, String token,
			long timeout) {
		String href = ((IDAVAddressInformation) resource).getInternalAddress();
		if (this.lockTable.containsKey(href)) {
			LockInfo li = this.lockTable.get(href);
			// You have to present the token properly
			// ToDo: Should we also check for the username?
			String oldToken = li.getToken();
			boolean isStillValid = !li.isExpired();
			if (oldToken.equals(token) && isStillValid) {
				li.extenExpiryDate();
				return li;
			}
		}

		// Didn't work for all other cases
		return null;

	}

	/**
	 * Shuts down the lock manager thread
	 * 
	 */
	public synchronized void shutdown() {
		this.lockcleaner.cancel();
		this.lockcleaner = null;

	}

	/**
	 * @return Status of the lock
	 */
	public String status() {
		StringBuffer s = new StringBuffer(); // Faster than Strings
		Set<String> keys = this.lockTable.keySet();
		Iterator<String> it = keys.iterator();
		while (it.hasNext()) {
			LockInfo li = this.lockTable.get(it.next());
			s.append(" \n"
					+ li.getUsername()
					+ " --> "
					+ li.getToken()
					+ " ("
					+ (DateFormat.getDateTimeInstance(DateFormat.LONG,
							DateFormat.LONG).format(li.getExpirydate())) + ")");
		}
		return s.toString();
	}

	/**
	 * Explicit unlock!
	 * 
	 * @param pathinfo
	 *            What element to unlock
	 * @param token
	 *            empty if unlock not implicit (expire check)
	 * @return Success of the unlock operation
	 */
	public synchronized boolean unlock(String pathinfo /* key */, String token) {

		boolean result = false;

		// Without a token no unlock
		if (token == null) {
			return result;
		}

		if (token.startsWith("<")) {
			token = token.substring(1);
			token = token.substring(0, token.length() - 1);
		}
		if (!this.lockTable.containsKey(pathinfo)) {
			result = false;
		} else {

			LockInfo li = this.lockTable.get(pathinfo);
			if (li.isExpired() || li.getToken().equals(token)) {
				this.lockTable.remove(pathinfo);
			}

			result = true;
		}

		// Return the success
		return result;
	}
}
