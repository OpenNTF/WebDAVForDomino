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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.ibm.xsp.webdav.resource.DAVResourceDominoAttachments;

/**
 * @author Stephan H. Wissel
 * 
 *         Implements a standard file outputstream that deletes the file on
 *         close - after all they are temp files only
 * 
 */
public class AttachmentInputStream extends FileInputStream {

	private DAVResourceDominoAttachments res;

	public AttachmentInputStream(File file, DAVResourceDominoAttachments res)
			throws FileNotFoundException {
		super(file);
		this.res = res;
	}

	@Override
	public void close() throws IOException {
		super.close();
		this.res.removeTempFiles();
	}

}
