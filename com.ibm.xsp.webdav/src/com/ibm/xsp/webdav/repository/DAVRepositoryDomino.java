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
package com.ibm.xsp.webdav.repository;

import biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation;
import biz.taoconsulting.dominodav.interfaces.IDAVRepository;
import biz.taoconsulting.dominodav.repository.AbstractRepositoryImplementation;

/**
 * 
 * Repository with shared functions that all Domino related repositories inherit
 * and share
 * 
 * Domino Attachments = attachments from Documents in a view Domino Documents =
 * the raw DXL of a Domino document, might go through transformation / back
 * forth Domino View = tabular entries in Domino, eventual with transformation
 * (A view as a spreadsheet) Domino iCalendar = access via iCalendar/calDAV to
 * personal or shared calendars
 * 
 * @author Stephan H. Wissel
 * 
 */
public abstract class DAVRepositoryDomino extends
		AbstractRepositoryImplementation implements IDAVAddressInformation,
		IDAVRepository {

	// FIXME: refactor common methods here
	public abstract String getFilter();

}
