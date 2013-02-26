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
package biz.taoconsulting.dominodav.exceptions;

/**
 * @author Bastian Buch (TAO Consulting), Stephan H. Wissel
 * 
 */
public class DAVNotFoundException extends Exception {

	private String errorMsg = null;

	public DAVNotFoundException() {
		super();
	}

	public DAVNotFoundException(String msg) {
		super();
		this.errorMsg = msg;
	}

	@Override
	public String getMessage() {
		if (this.errorMsg == null) {
			return "A webDAV Resource was not found";
		}
		return this.errorMsg;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
