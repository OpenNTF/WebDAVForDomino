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
package com.ibm.xsp.webdav.domino;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ibm.xsp.webdav.resource.DAVResourceDomino;

/**
 * @author Stephan H. Wissel
 * 
 */
public abstract class DominoOutputStream extends OutputStream {
	/**
	 * Logger for Errors
	 */
	private static final Log LOGGER = LogFactory
			.getLog(DominoOutputStream.class);

	/**
	 * The outputstream leading to the temp file or other stream resource
	 */
	protected OutputStream out;

	/**
	 * The current Domino resource
	 */
	protected DAVResourceDomino res;

	/**
	 * Parameter-less function to be used with reflection
	 * 
	 */
	public DominoOutputStream() {
		super();
		LOGGER.debug("Empty DominoOutputStream created");
	}

	/**
	 * Creates a OutputStream that streams to a temporary file This file will
	 * later be attached in Domino as attachment
	 * 
	 * @param res
	 *            DAVResourceDomino
	 */
	public DominoOutputStream(DAVResourceDomino res) {
		super();
		this.setResource(res);
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see java.io.OutputStream#close()
	 */
	public void close() throws IOException {
		// Write out the output stream
		this.getOutputStream().flush();
		this.getOutputStream().close();
	}

	/**
	 * @see java.io.OutputStream#flush()
	 */
	public void flush() throws IOException {
		this.getOutputStream().flush();
	}

	/**
	 * Gets the output stream
	 * 
	 * @return OutputStream
	 */
	private OutputStream getOutputStream() throws IOException {
		if (this.out == null) {
			this.initOutputStream();
		}
		return this.out;
	}

	/**
	 * getResource returns the resource stored in the stream
	 * 
	 * @retuns res DAVResourceDomino
	 */
	protected DAVResourceDomino getResource() {
		return this.res;
	}

	/**
	 * lazy init of an output stream Needs to be overwritten in sub classes!
	 */
	protected void initOutputStream() throws IOException {
		throw new IOException();
	}

	/**
	 * setResource needs to be called when constructor was used without
	 * parameter;
	 * 
	 * @param res
	 *            DAVResourceDomino
	 */
	protected void setResource(DAVResourceDomino res) {
		this.res = res;
	}

	/**
	 * @see java.io.OutputStream#write(byte[])
	 */
	public void write(byte[] b) throws IOException {
		// Writes the bytes into the Fileoutputstream
		this.getOutputStream().write(b);
	}

	/**
	 * @see java.io.OutputStream#write(byte[], int, int) Writes into the output
	 *      stream in the temp file
	 */
	public void write(byte[] b, int off, int len) throws IOException {
		this.getOutputStream().write(b, off, len);
	}

	/**
	 * @see java.io.OutputStream#write(int)
	 */
	public void write(int b) throws IOException {
		this.getOutputStream().write(b);
	}

}
