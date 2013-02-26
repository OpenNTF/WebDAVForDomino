/* ========================================================================== *
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
package biz.taoconsulting.dominodav;

import java.util.HashMap;

import javax.xml.transform.sax.TransformerHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Bastian Buch (TAO Consulting)
 * 
 */
public final class DAVProperties {
	/**
	 * Logger for Errors
	 */
	private static final Log LOGGER = LogFactory.getLog(DAVProperties.class);

	/**
	 * A Hashmap which represents Resourceproperties as key/value pairs
	 */
	private HashMap<String, String> properties;

	/**
	 * ToDo: Set with parameter names should be passed
	 * 
	 * @param vals
	 *            A Map with property values
	 */
	public DAVProperties(HashMap<String, String> vals) {
		this.properties = new HashMap<String, String>();

		this.properties = vals;
	}

	/**
	 * @return The number of collected properties
	 */
	public int size() {
		return this.properties.size();
	}

	/**
	 * @param key
	 *            Key for the focussed Value
	 * @return Returns the value for a specific key
	 */
	public String getVal(String key) {
		String returnValue = this.properties.get(key);
		LOGGER.trace(key + "=" + returnValue);
		return (returnValue == null ? "" : returnValue);
	}

	/**
	 * @return Properties as key/value hashmap
	 */
	public HashMap<String, String> getMap() {
		return this.properties;
	}

	/**
	 * @param kxml
	 *            KXml Object to which the properties shall be added
	 */
	public void addToKxml(TransformerHandler kxml) {

	}

	/**
	 * Status code for multipart ToDo: Auslagern!
	 */
	public static final int STATUS_MULTIPART = 207;

	public static final String STATUS_MULTIPART_STRING = "Multi-Status";

	/**
	 *
	 */
	public static final int RESOURCETYPE_COLLECTION = 1;

	/**
	 *
	 */
	public static final String TYPE_XML = "text/xml;charset=UTF-8";

}
