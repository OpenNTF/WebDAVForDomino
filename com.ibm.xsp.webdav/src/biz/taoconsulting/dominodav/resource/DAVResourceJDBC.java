/* ========================================================================== *
 * Copyright (C) 2006, 2007 TAO Consulting Pte <http://www.taoconsulting.sg/> *
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
package biz.taoconsulting.dominodav.resource;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import biz.taoconsulting.dominodav.exceptions.DAVNotFoundException;
import biz.taoconsulting.dominodav.interfaces.IDAVAddressInformation;
import biz.taoconsulting.dominodav.interfaces.IDAVRepository;

import com.ibm.xsp.webdav.repository.DAVRepositoryMETA;

/**
 * @author Stephan H. Wissel
 * 
 */
public class DAVResourceJDBC extends DAVAbstractResource {

	/**
	 * Logger for Errors
	 */
	private static final Log LOGGER = LogFactory.getLog(DAVResourceJDBC.class);

	private String DBFileID;
	private String DBDocID;
	private String mimeType;
	private DAVRepositoryMETA repositoryMeta;

	/**
	 * @param rep
	 *            The containing Repository
	 * @param url
	 *            the access to the resource from the browser relative to the
	 *            repository
	 * @throws DAVNotFoundException
	 *             -- if the repository is not there
	 */
	public DAVResourceJDBC(IDAVRepository rep, String url)
			throws DAVNotFoundException {
		this.setMember(false);
		this.setup(rep, url);
		LOGGER.debug("New DAVResourceJDBC created" + url);
	}

	/**
	 * @param rep
	 *            The repository
	 * @param url
	 *            the access to the resource from the browser relative to the
	 *            repository
	 * @param isMember
	 *            - for directories: is it listed as part of the parent or by
	 *            itself?
	 * @throws DAVNotFoundException
	 *             -- resource might not be there
	 */
	public DAVResourceJDBC(IDAVRepository rep, String url, boolean isMember)
			throws DAVNotFoundException {
		this.setMember(isMember);
		this.setup(rep, url);
	}

	/**
	 * @param rep
	 *            the containing repository
	 * @param url
	 *            the access to the resource from the browser relative to the
	 *            repository
	 * @throws DAVNotFoundException
	 *             -- we might ask for an non-existing resource
	 */
	private void setup(IDAVRepository rep, String url)
			throws DAVNotFoundException {

		this.repositoryMeta = DAVRepositoryMETA.getRepository(null);
		// Store a link to the repository
		this.setOwner(rep);

		// The path can't be null and can't be empty. if it is empty we use the
		// "/"
		if (url == null || url.equals("")) {
			url = new String("/");
		}

		// Memorize the url requested
		this.setPublicHref(url);

		// Memorize the full URI;
		this.setInternalAddress(((IDAVAddressInformation) rep).getPublicHref()
				+ url);
		this.setDBDocID(url.split("/")[1]);
		this.getInitDBFileValues();
	}

	/**
	 * /**
	 * 
	 * @see biz.taoconsulting.dominodav.resource.DAVAbstractResource#getStream()
	 */
	public InputStream getStream() {

		Connection conn = null;
		Statement stmt = null;

		InputStream blobStream = null;

		try {
			Context initCtx = new InitialContext();
			Context envCtx = (Context) initCtx.lookup("java:comp/env");
			DataSource ds = (DataSource)

			// THe internal address of a JDBC source is the data source
			envCtx.lookup(this.repositoryMeta.getInternalAddress());

			conn = ds.getConnection();
			stmt = conn.createStatement();
			// XXX: THat is plain wrong -- need to rework the JDBC data source
			// query
			ResultSet rs = stmt
					.executeQuery("select f.fil_blocksize,f.fil_contents_blob from ibkuis_pp_files f where  f.fil_id="
							+ this.getDBFileID());
			if (rs.next()) {
				Blob blob = rs.getBlob(2);
				blobStream = blob.getBinaryStream();
			}
		} catch (NamingException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (stmt != null)
					stmt.close();

				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				/** Exception handling **/
			}
		}
		return blobStream;
	}

	/**
	 * @see biz.taoconsulting.dominodav.resource.DAVAbstractResource#getOutputStream()
	 */
	public OutputStream getOutputStream() {
		// TODO Implement
		return null;
	}

	/**
	 * Delete a file resource
	 * 
	 * @return true/false : did the deletion work?
	 */
	public boolean delete() {
		// TODO Implement
		return false;
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.resource.DAVAbstractResource#getMimeType(javax.servlet.ServletContext)
	 */
	public String getMimeType(ServletContext context) {
		return this.mimeType;
	}

	/**
	 * @param mimeType
	 */
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	private void getInitDBFileValues() {
		Statement stmt = null;
		Statement stmt1 = null;
		Connection conn = null;
		try {
			Context initCtx = new InitialContext();
			Context envCtx = (Context) initCtx.lookup("java:comp/env");
			DataSource ds = (DataSource)

			envCtx.lookup(this.repositoryMeta.getInternalAddress());
			conn = ds.getConnection();
			stmt1 = conn.createStatement();
			// TODO fix this
			stmt1.executeQuery("select uis_mid_onetimeinitweb('DATENADMIN','umsys','DE') as a from dual");

			stmt = conn.createStatement();
			// TODO: Fix that SQL
			ResultSet rs = stmt
					.executeQuery("select t.dtyp_mimetype,d.*,"
							+ "f.fil_length,f.fil_id from  umsys.ibkuis_co_filetype t,"
							+ "uis_pp_documents_v d, ibkuis_pp_files f where d.D_ID ="
							+ this.getDBDocID()
							+ "and f.fil_id = d.D_FIL_ID and t.dtyp_kz = upper(d.D_TYP)");
			if (rs.next()) {
				this.setName(rs.getString("D_FILE"));
				this.setExtension(rs.getString("D_TYP"));
				this.setContentLength(rs.getString("FIL_LENGTH"));
				this.setDBFileID(rs.getString("FIL_ID"));
				this.setMimeType(rs.getString("dtyp_mimetype"));
				System.out.println("BREAK");
			}
		} catch (NamingException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (stmt != null)
					stmt.close();
				if (stmt1 != null)
					stmt1.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				/** Exception handling **/
			}
		}
	}

	/**
	 * @return fileID
	 */
	public String getDBFileID() {
		return DBFileID;
	}

	/**
	 * @param fileID
	 */
	public void setDBFileID(String fileID) {
		DBFileID = fileID;
	}

	/**
	 * @return DocID
	 */
	public String getDBDocID() {
		return DBDocID;
	}

	/**
	 * @param docID
	 */
	public void setDBDocID(String docID) {
		DBDocID = docID;
	}

	public void patchLastModified(Date dt) {
	}

	public void patchCreationDate(Date dt) {
	}

}
