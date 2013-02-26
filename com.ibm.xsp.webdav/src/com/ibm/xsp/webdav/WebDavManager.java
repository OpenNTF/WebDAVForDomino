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
package com.ibm.xsp.webdav;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import biz.taoconsulting.dominodav.LockManager;
import com.ibm.xsp.webdav.repository.DAVRepositoryMETA;

/**
 * The webDAV manager handles the webDAV configuration, the supported methods
 * and the potential webDAV methods, refactored out of the original servlet
 * Implemented as singleton
 * 
 * @author Stephan H. Wissel
 * 
 */
public class WebDavManager {

	/**
	 * Key to load the supported HTTP methods like get, post etc.
	 */
	private static final String SUPPORTEDMETHODSFILE = "methods.properties";

	/**
	 * Properties of the Meta repository like the styles to use and where to
	 * find the repositories
	 */
	public static final String METAREPOPROPFILE = "metarepository.properties";

	/**
	 * Key to load temp directory
	 */
	private static final String INIT_KEY_TEMPDIR = "temp-directory";

	/**
	 * The instance of the class we return
	 */
	private static WebDavManager internalWebDavManager = null;

	/**
	 * The logger object for event logging
	 */
	private static final Log LOGGER = LogFactory.getLog(WebDavManager.class);

	private static final String DEFAULT_TMP_DIR = "/tmp";

	/**
	 * Style for PROPFIND in XSLT
	 */
	private String propfindStyle;

	/**
	 * Style for PROPFIND in XSLT for the list of repositories
	 */
	private String rootPropfindStyle;

	/**
	 * tempDir the directory for temporary file operations
	 */
	private String tempDir;

	/**
	 * Can the manager been reset, should be false for production The property
	 * doesn't prevent anything but the servlet is querying it
	 */
	private boolean allowReset = true;

	/**
	 * The path of the servlet, needs to prefix repositories and files
	 */
	private String servletPath;

	/**
	 * Mimetype registry
	 */
	private HashMap<String, String> knownMimeTypes;

	/**
	 * 
	 * @return the servlet Path
	 */
	public String getServletPath() {
		return servletPath;
	}

	/**
	 * 
	 * @param servletPath
	 *            prefix for making repositories complete
	 */
	public void setServletPath(String servletPath) {
		this.servletPath = servletPath;
	}

	/**
	 * Should the Manager be resettable
	 * 
	 * @return
	 */
	public boolean isAllowReset() {
		return allowReset;
	}

	/**
	 * Initialize the Manager and make sure we did load everything
	 */
	private synchronized static void initializeWebDavManager(String servletPath) {
		if (internalWebDavManager == null) {
			internalWebDavManager = new WebDavManager();
		}
		if (servletPath != null) {
			internalWebDavManager.setServletPath(servletPath);
		}
		internalWebDavManager.getLockManager(); // Start the lock manager
		internalWebDavManager.loadSupportedMethods(); // The maximum of methods
														// we currently support
		internalWebDavManager.loadMetaRepository(); // The repository of our
													// repositories
		internalWebDavManager.loadMimeTypes(); // All the mimetypes we know
	}

	private void loadMimeTypes() {
		// TODO Find a better way than hardcoding them here!
		this.knownMimeTypes = new HashMap<String, String>();
		this.knownMimeTypes.put("abs", "audio/x-mpeg");
		this.knownMimeTypes.put("ai", "application/postscript");
		this.knownMimeTypes.put("aif", "audio/x-aiff");
		this.knownMimeTypes.put("aifc", "audio/x-aiff");
		this.knownMimeTypes.put("aiff", "audio/x-aiff");
		this.knownMimeTypes.put("aim", "application/x-aim");
		this.knownMimeTypes.put("art", "image/x-jg");
		this.knownMimeTypes.put("asf", "video/x-ms-asf");
		this.knownMimeTypes.put("asx", "video/x-ms-asf");
		this.knownMimeTypes.put("au", "audio/basic");
		this.knownMimeTypes.put("avi", "video/x-msvideo");
		this.knownMimeTypes.put("avx", "video/x-rad-screenplay");
		this.knownMimeTypes.put("bcpio", "application/x-bcpio");
		this.knownMimeTypes.put("bin", "application/octet-stream");
		this.knownMimeTypes.put("bmp", "image/bmp");
		this.knownMimeTypes.put("body", "text/html");
		this.knownMimeTypes.put("cdf", "application/x-cdf");
		this.knownMimeTypes.put("cer", "application/x-x509-ca-cert");
		this.knownMimeTypes.put("class", "application/java");
		this.knownMimeTypes.put("cpio", "application/x-cpio");
		this.knownMimeTypes.put("csh", "application/x-csh");
		this.knownMimeTypes.put("css", "text/css");
		this.knownMimeTypes.put("dib", "image/bmp");
		this.knownMimeTypes.put("doc", "application/msword");
		this.knownMimeTypes
				.put("docx",
						"application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		this.knownMimeTypes.put("dtd", "application/xml-dtd");
		this.knownMimeTypes.put("dv", "video/x-dv");
		this.knownMimeTypes.put("dvi", "application/x-dvi");
		this.knownMimeTypes.put("eps", "application/postscript");
		this.knownMimeTypes.put("etx", "text/x-setext");
		this.knownMimeTypes.put("exe", "application/octet-stream");
		this.knownMimeTypes.put("gif", "image/gif");
		this.knownMimeTypes.put("gtar", "application/x-gtar");
		this.knownMimeTypes.put("gz", "application/x-gzip");
		this.knownMimeTypes.put("hdf", "application/x-hdf");
		this.knownMimeTypes.put("hqx", "application/mac-binhex40");
		this.knownMimeTypes.put("htc", "text/x-component");
		this.knownMimeTypes.put("htm", "text/html");
		this.knownMimeTypes.put("html", "text/html");
		this.knownMimeTypes.put("hqx", "application/mac-binhex40");
		this.knownMimeTypes.put("ief", "image/ief");
		this.knownMimeTypes.put("ini", "text/plain");
		this.knownMimeTypes.put("jad", "text/vnd.sun.j2me.app-descriptor");
		this.knownMimeTypes.put("jar", "application/java-archive");
		this.knownMimeTypes.put("java", "text/plain");
		this.knownMimeTypes.put("jnlp", "application/x-java-jnlp-file");
		this.knownMimeTypes.put("jpe", "image/jpeg");
		this.knownMimeTypes.put("jpeg", "image/jpeg");
		this.knownMimeTypes.put("jpg", "image/jpeg");
		this.knownMimeTypes.put("js", "text/javascript");
		this.knownMimeTypes.put("jsf", "text/plain");
		this.knownMimeTypes.put("jspf", "text/plain");
		this.knownMimeTypes.put("kar", "audio/x-midi");
		this.knownMimeTypes.put("latex", "application/x-latex");
		this.knownMimeTypes.put("log", "text/plain");
		this.knownMimeTypes.put("m3u", "audio/x-mpegurl");
		this.knownMimeTypes.put("mac", "image/x-macpaint");
		this.knownMimeTypes.put("man", "application/x-troff-man");
		this.knownMimeTypes.put("mathml", "application/mathml+xml");
		this.knownMimeTypes.put("me", "application/x-troff-me");
		this.knownMimeTypes.put("mid", "audio/x-midi");
		this.knownMimeTypes.put("midi", "audio/x-midi");
		this.knownMimeTypes.put("mif", "application/x-mif");
		this.knownMimeTypes.put("mov", "video/quicktime");
		this.knownMimeTypes.put("movie", "video/x-sgi-movie");
		this.knownMimeTypes.put("mp1", "audio/x-mpeg");
		this.knownMimeTypes.put("mp2", "audio/x-mpeg");
		this.knownMimeTypes.put("mp3", "audio/x-mpeg");
		this.knownMimeTypes.put("mp4", "video/mp4");
		this.knownMimeTypes.put("mpa", "audio/x-mpeg");
		this.knownMimeTypes.put("mpe", "video/mpeg");
		this.knownMimeTypes.put("mpeg", "video/mpeg");
		this.knownMimeTypes.put("mpega", "audio/x-mpeg");
		this.knownMimeTypes.put("mpg", "video/mpeg");
		this.knownMimeTypes.put("mpv2", "video/mpeg2");
		this.knownMimeTypes.put("ms", "application/x-wais-source");
		this.knownMimeTypes.put("nc", "application/x-netcdf");
		this.knownMimeTypes.put("ns2", "application/vnd.lotus-notes");
		this.knownMimeTypes.put("ns3", "application/vnd.lotus-notes");
		this.knownMimeTypes.put("ns4", "application/vnd.lotus-notes");
		this.knownMimeTypes.put("ns5", "application/vnd.lotus-notes");
		this.knownMimeTypes.put("ns6", "application/vnd.lotus-notes");
		this.knownMimeTypes.put("ns7", "application/vnd.lotus-notes");
		this.knownMimeTypes.put("ns8", "application/vnd.lotus-notes");
		this.knownMimeTypes.put("ns9", "application/vnd.lotus-notes");
		this.knownMimeTypes.put("nsf", "application/vnd.lotus-notes");
		this.knownMimeTypes.put("ntf", "application/vnd.lotus-notes");
		this.knownMimeTypes.put("oda", "application/oda");
		this.knownMimeTypes.put("odb",
				"application/vnd.oasis.opendocument.database");
		this.knownMimeTypes.put("odc",
				"application/vnd.oasis.opendocument.chart");
		this.knownMimeTypes.put("odf",
				"application/vnd.oasis.opendocument.formula");
		this.knownMimeTypes.put("odg",
				"application/vnd.oasis.opendocument.graphics");
		this.knownMimeTypes.put("odi",
				"application/vnd.oasis.opendocument.image");
		this.knownMimeTypes.put("odm",
				"application/vnd.oasis.opendocument.text-master");
		this.knownMimeTypes.put("odp",
				"application/vnd.oasis.opendocument.presentation");
		this.knownMimeTypes.put("ods",
				"application/vnd.oasis.opendocument.spreadsheet");
		this.knownMimeTypes.put("odt",
				"application/vnd.oasis.opendocument.text");
		this.knownMimeTypes.put("ogg", "application/ogg");
		this.knownMimeTypes.put("otg ",
				"application/vnd.oasis.opendocument.graphics-template");
		this.knownMimeTypes.put("oth",
				"application/vnd.oasis.opendocument.text-web");
		this.knownMimeTypes.put("otp",
				"application/vnd.oasis.opendocument.presentation-template");
		this.knownMimeTypes.put("ots",
				"application/vnd.oasis.opendocument.spreadsheet-template ");
		this.knownMimeTypes.put("ott",
				"application/vnd.oasis.opendocument.text-template");
		this.knownMimeTypes.put("pbm", "image/x-portable-bitmap");
		this.knownMimeTypes.put("pct", "image/pict");
		this.knownMimeTypes.put("pdf", "application/pdf");
		this.knownMimeTypes.put("pgm", "image/x-portable-graymap");
		this.knownMimeTypes.put("pic", "image/pict");
		this.knownMimeTypes.put("pict", "image/pict");
		this.knownMimeTypes.put("pls", "audio/x-scpls");
		this.knownMimeTypes.put("png", "image/png");
		this.knownMimeTypes.put("pnm", "image/x-portable-anymap");
		this.knownMimeTypes.put("pnt", "image/x-macpaint");
		this.knownMimeTypes.put("ppm", "image/x-portable-pixmap");
		this.knownMimeTypes.put("pps", "application/vnd.ms-powerpoint");
		this.knownMimeTypes.put("ppt", "application/vnd.ms-powerpoint");
		this.knownMimeTypes
				.put("pptx",
						"application/vnd.openxmlformats-officedocument.presentationml.presentation");
		this.knownMimeTypes.put("ps", "application/postscript");
		this.knownMimeTypes.put("psd", "image/x-photoshop");
		this.knownMimeTypes.put("qt", "video/quicktime");
		this.knownMimeTypes.put("qti", "image/x-quicktime");
		this.knownMimeTypes.put("qtif", "image/x-quicktime");
		this.knownMimeTypes.put("ras", "image/x-cmu-raster");
		this.knownMimeTypes.put("rdf", "application/rdf+xml");
		this.knownMimeTypes.put("rgb", "image/x-rgb");
		this.knownMimeTypes.put("rm", "application/vnd.rn-realmedia");
		this.knownMimeTypes.put("roff", "application/x-troff");
		this.knownMimeTypes.put("rtf", "application/rtf");
		this.knownMimeTypes.put("rtx", "text/richtext");
		this.knownMimeTypes.put("sh", "application/x-sh");
		this.knownMimeTypes.put("shar", "application/x-shar");
		this.knownMimeTypes.put("smf", "audio/x-midi");
		this.knownMimeTypes.put("sit", "application/x-stuffit");
		this.knownMimeTypes.put("snd", "audio/basic");
		this.knownMimeTypes.put("src", "application/x-wais-source");
		this.knownMimeTypes.put("sv4cpio", "application/x-sv4cpio");
		this.knownMimeTypes.put("sv4crc", "application/x-sv4crc");
		this.knownMimeTypes.put("svg", "image/svg+xml");
		this.knownMimeTypes.put("svgz", "image/svg+xml");
		this.knownMimeTypes.put("swf", "application/x-shockwave-flash");
		this.knownMimeTypes.put("t", "application/x-troff");
		this.knownMimeTypes.put("tar", "application/x-tar");
		this.knownMimeTypes.put("tcl", "application/x-tcl");
		this.knownMimeTypes.put("tex", "application/x-tex");
		this.knownMimeTypes.put("texi", "application/x-texinfo");
		this.knownMimeTypes.put("texinfo", "application/x-texinfo");
		this.knownMimeTypes.put("tif", "image/tiff");
		this.knownMimeTypes.put("tiff", "image/tiff");
		this.knownMimeTypes.put("tr", "application/x-troff");
		this.knownMimeTypes.put("tsv", "text/tab-separated-values");
		this.knownMimeTypes.put("txt", "text/plain");
		this.knownMimeTypes.put("ulw", "audio/basic");
		this.knownMimeTypes.put("ustar", "application/x-ustar");
		this.knownMimeTypes.put("vxml", "application/voicexml+xml");
		this.knownMimeTypes.put("xbm", "image/x-xbitmap");
		this.knownMimeTypes.put("xht", "application/xhtml+xml");
		this.knownMimeTypes.put("xhtml", "application/xhtml+xml");
		this.knownMimeTypes.put("xls", "application/vnd.ms-excel");
		this.knownMimeTypes
				.put("xlsx",
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		this.knownMimeTypes.put("xml", "application/xml");
		this.knownMimeTypes.put("xpm", "image/x-xpixmap");
		this.knownMimeTypes.put("xsl", "application/xml");
		this.knownMimeTypes.put("xslt", "application/xslt+xml");
		this.knownMimeTypes.put("xul", "application/vnd.mozilla.xul+xml");
		this.knownMimeTypes.put("xwd", "image/x-xwindowdump");
		this.knownMimeTypes.put("vsd", "application/x-visio");
		this.knownMimeTypes.put("wav", "audio/x-wav");
		this.knownMimeTypes.put("wbmp", "image/vnd.wap.wbmp");
		this.knownMimeTypes.put("webdav", "application/x-webdav");
		this.knownMimeTypes.put("wml", "text/vnd.wap.wml");
		this.knownMimeTypes.put("wmlc", "application/vnd.wap.wmlc");
		this.knownMimeTypes.put("wmls", "text/vnd.wap.wmlscript");
		this.knownMimeTypes.put("wmlscriptc", "application/vnd.wap.wmlscriptc");
		this.knownMimeTypes.put("wmv", "video/x-ms-wmv");
		this.knownMimeTypes.put("wrl", "x-world/x-vrml");
		this.knownMimeTypes.put("wspolicy", "application/wspolicy+xml");
		this.knownMimeTypes.put("Z", "application/x-compress");
		this.knownMimeTypes.put("z", "application/x-compress");
		this.knownMimeTypes.put("zip", "application/zip");
		// LOGGER.info("Loaded "+String.valueOf(this.knownMimeTypes.size())+" mime types");
	}

	// The next 4 methods ensure that it is a singleton

	/**
	 * @param reset
	 *            boolean -- if true the webDav manager gets setup freshly
	 * @return the Singleton instance of the webDAV manager, should be loaded
	 *         once only anyway
	 */
	public static synchronized WebDavManager getManager(String servletPath,
			boolean reset) {

		if (reset || internalWebDavManager == null) {
			if (reset) {
				LOGGER.error("WebDAVManager reset executed");
			}
			initializeWebDavManager(servletPath);
		}
		return internalWebDavManager;
	}

	/**
	 * @return the Singleton instance of the webDAV manager, should be loaded
	 *         once only anyway
	 */
	public static synchronized WebDavManager getManager(String servletPath) {
		return WebDavManager.getManager(servletPath, false);
	}

	/**
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected Object clone() throws CloneNotSupportedException {
		// Singletons must NOT be cloned
		throw new CloneNotSupportedException();
	}

	/**
	 * We make the constructor private, so you can't get to it
	 */
	private WebDavManager() {

	}

	// Starting here are the class instance methods we actually use to return
	// values

	/**
	 * List of maximum supported HTTP Methods loaded from methods.properties
	 * This is the maxium of supported methods. Individual repositories might
	 * implement only a subset of them
	 */
	private Properties supportedMethods;

	/**
	 * Lock Manager to allow locking of documents
	 */
	private LockManager lockManager;

	/**
	 * Meta Repository to hold the list of top level repositories
	 */
	private DAVRepositoryMETA repositoryMeta;

	/**
	 * @return Returns the lockManager.
	 */
	public LockManager getLockManager() {
		if (this.lockManager != null) {
			return this.lockManager;
		}
		this.lockManager = LockManager.getLockManager();
		return this.lockManager;
	}

	/**
	 * Loads all the supported HTTP keywords
	 */
	private void loadSupportedMethods() {
		this.supportedMethods = new Properties();
		InputStream in = this.getClass().getResourceAsStream(
				WebDavManager.SUPPORTEDMETHODSFILE);
		try {
			this.supportedMethods.load(in);
			// FIXME - remove rawtimes
			for (@SuppressWarnings("rawtypes")
			Map.Entry curMethod : this.supportedMethods.entrySet()) {
				LOGGER.debug("Method: " + curMethod.getKey().toString()
						+ " with class " + curMethod.getValue().toString());
			}

		} catch (IOException e) {
			LOGGER.error(e);
		}

	}

	/**
	 * loads the meta repository that contains all the repositories we currently
	 * use as well as the global parameters now stored in the webDAVManager
	 */
	private void loadMetaRepository() {
		// Load the Meta Repository -- and the style
		Properties metaProp = new Properties(); // to be loaded from file
		InputStream in = this.getClass().getResourceAsStream(
				WebDavManager.METAREPOPROPFILE);
		try {
			metaProp.load(in);
		} catch (IOException e) {
			LOGGER.error(e);
		}

		// Now load the values with sensible defaults
		String configLocation = metaProp.getProperty("repository-location",
				DAVRepositoryMETA.DEFAULT_REPOSITORY_LOCATION);
		String propfindStyle = metaProp.getProperty("style-name",
				DAVRepositoryMETA.DEFAULT_PROPFIND_STYLE);
		String rootStyle = metaProp.getProperty("root-style-name",
				DAVRepositoryMETA.DEFAULT_ROOT_STYLE);
		Boolean autoreload = new Boolean(metaProp.getProperty("auto-reload",
				"true"));
		Boolean canReset = new Boolean(metaProp.getProperty("allow-reset",
				"false"));
		String tempDir = metaProp
				.getProperty(INIT_KEY_TEMPDIR, DEFAULT_TMP_DIR);

		// Apply them to the repository and the manager
		this.repositoryMeta = DAVRepositoryMETA.getRepository(configLocation);
		this.repositoryMeta.setAutoReloadRepositoryList(autoreload
				.booleanValue());
		this.setRootPropfindStyle(rootStyle);
		this.setPropfindStyle(propfindStyle);
		this.setTempDir(tempDir);
		this.allowReset = canReset.booleanValue();

		// Write to the logger
		LOGGER.debug("Root-Style name:" + this.getRootPropfindStyle());
		LOGGER.debug("Repository-Style name:" + this.getPropfindStyle());
		LOGGER.debug("Configuration location:" + configLocation);
		LOGGER.debug("Can auto reload list:" + autoreload.toString());
		LOGGER.debug("Can reset:" + canReset.toString());
		LOGGER.debug("Temp Dir:" + tempDir);

	}

	/**
	 * @param methodName
	 *            the HTTP method we need
	 * @return the classname for that method
	 */
	public String getClassForMethod(String methodName) {

		if (this.supportedMethods == null) {
			this.loadSupportedMethods();
		}

		return this.supportedMethods.getProperty(methodName.toUpperCase());

	}

	/**
	 * @return the repositoryMeta
	 */
	public DAVRepositoryMETA getRepositoryMeta() {
		if (this.repositoryMeta == null) {
			this.loadMetaRepository();
		}
		return this.repositoryMeta;
	}

	/**
	 * @param repositoryMeta
	 *            the repositoryMeta to set
	 */
	public void setRepositoryMeta(DAVRepositoryMETA repositoryMeta) {
		this.repositoryMeta = repositoryMeta;
	}

	/**
	 * 
	 * @return String XSLT Style for Propfind to make it look pretty in the
	 *         browser
	 */
	public String getPropfindStyle() {
		return this.propfindStyle;
	}

	/**
	 * 
	 * @param propfindStyle
	 *            Style for Propfind
	 */
	public void setPropfindStyle(String propfindStyle) {
		this.propfindStyle = propfindStyle;
	}

	/**
	 * 
	 * @return String XSLT Style for Propfind to make it look pretty in the
	 *         browser
	 */
	public String getRootPropfindStyle() {
		return this.rootPropfindStyle;
	}

	/**
	 * 
	 * @param propfindStyle
	 *            Style for Propfind
	 */
	public void setRootPropfindStyle(String propfindStyle) {
		this.rootPropfindStyle = propfindStyle;
	}

	/**
	 * @param tempDir
	 *            the directory for temporary file operations
	 */
	public void setTempDir(String tempDir) {
		this.tempDir = tempDir;
	}

	/**
	 * @return The current temporary directory
	 */
	public String getTempDir() {
		if (this.tempDir == null) {
			this.tempDir = DEFAULT_TMP_DIR;
		}
		return this.tempDir;
	}

	/**
	 * Retrieves mime types for known extensions
	 * 
	 * @param fileName
	 * @return
	 */
	public String getMimeType(String fileName) {

		String returnType = "application/octet-stream"; // Default value if
														// things go wrong
		int lastDot = fileName.lastIndexOf(".");
		if (lastDot < 0) {
			return returnType; // For files without extension we can't tell here
		}

		String extension = fileName.substring(lastDot + 1).toLowerCase();

		if (this.knownMimeTypes.containsKey(extension)) {
			returnType = this.knownMimeTypes.get(extension);
		}

		return returnType;
	}
}
