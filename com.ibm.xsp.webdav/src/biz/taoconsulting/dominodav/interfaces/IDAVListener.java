/** ========================================================================== *
 * Copyright (C) 2006, 2007 TAO Consulting Pte <http://www.taoconsulting.sg/> *
 *           based on work of                                                 *
 * Copyright (C) 2004-2005 Pier Fumagalli <http://www.betaversion.org/~pier/> *
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
package biz.taoconsulting.dominodav.interfaces;

/**
 * @author Bastian Buch (TAO Consulting)
 * 
 */
public interface IDAVListener {

	/**
	 * <p>
	 * An event representing the creation of a collection.
	 * </p>
	 */
	int COLLECTION_CREATED = 1;

	/**
	 * <p>
	 * An event representing the deletion of a collection.
	 * </p>
	 */
	int COLLECTION_REMOVED = 2;

	/**
	 * <p>
	 * An event representing the creation of a resource.
	 * </p>
	 */
	int RESOURCE_CREATED = 3;

	/**
	 * <p>
	 * An event representing the deletion of a resource.
	 * </p>
	 */
	int RESOURCE_REMOVED = 4;

	/**
	 * <p>
	 * An event representing the modification of a resource.
	 * </p>
	 */
	int RESOURCE_MODIFIED = 5;

	/**
	 * <p>
	 * Notify this {@link IDAVListener} of an action occurred on a specified
	 * {@link DAVResourceDomino}.
	 * </p>
	 * 
	 * @param resource
	 *            the {@link DAVResourceDomino} associated with the
	 *            notification.
	 * @param event
	 *            a number identifying the type of the notification.
	 */
	void notify(IDAVResource resource, int event);

}
