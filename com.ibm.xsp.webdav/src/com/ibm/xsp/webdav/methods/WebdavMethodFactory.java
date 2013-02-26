/** ========================================================================= *
 * Copyright (C) 2012 IBM Corporation                                         *
 *           based on work of                                                 *
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
 * ========================================================================== **/

package com.ibm.xsp.webdav.methods;

import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ibm.xsp.webdav.WebDavManager;

import biz.taoconsulting.dominodav.interfaces.IDAVProcessable;

/**
 * Factory encapsulating the creation of WebdavMethod implementations.
 * 
 * @author Bastian Buch (TAO Consulting)
 */
public class WebdavMethodFactory {

	/**
	 * We cache the classes
	 */
	@SuppressWarnings("rawtypes")
	private static HashMap<String, Class> cachedClasses = new HashMap<String, Class>();

	/**
	 * The logger object for event logging
	 */
	private static final Log LOGGER = LogFactory
			.getLog(WebdavMethodFactory.class);

	/**
	 * Creates a new instance of a WebdavMethodFactory implementation.
	 * 
	 * @param className
	 *            configuration of the WebDAV servlet
	 * @param manager
	 *            The webDAV Manager that knows the repositories
	 * @return new Instance of an Method Object
	 */
	public static IDAVProcessable newInstance(String className,
			WebDavManager manager) {

		IDAVProcessable method = null;
		@SuppressWarnings("rawtypes")
		Class factoryClass = null;
		if (className != null) {

			if (cachedClasses.containsKey(className)) {
				factoryClass = cachedClasses.get(className);
			} else {
				try {
					factoryClass = Class.forName(manager
							.getClassForMethod(className));
					cachedClasses.put(className, factoryClass);
				} catch (ClassNotFoundException e) {
					LOGGER.error(e);
				}
			}

			try {
				method = (IDAVProcessable) factoryClass.newInstance();
			} catch (IllegalAccessException e) {
				LOGGER.error(e);
			} catch (InstantiationException e) {
				LOGGER.error(e);
			}

		}

		return method;
	}

}
