/**
 * 
 */
package biz.taoconsulting.dominodav.resource;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

/**
 * @author eugen.cretu
 * 
 */
public class DAVResourceADS extends DAVAbstractResource {
	// implement Abstract Data Stream for a file resource
	// used in a NTFS -compatible file system

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * biz.taoconsulting.dominodav.interfaces.IDAVResource#patchLastModified
	 * (java.util.Date)
	 */
	public void patchLastModified(Date dt) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * biz.taoconsulting.dominodav.interfaces.IDAVResource#patchCreationDate
	 * (java.util.Date)
	 */
	public void patchCreationDate(Date dt) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.resource.DAVAbstractResource#delete()
	 */
	@Override
	public boolean delete() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * biz.taoconsulting.dominodav.resource.DAVAbstractResource#getOutputStream
	 * ()
	 */
	@Override
	public OutputStream getOutputStream() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see biz.taoconsulting.dominodav.resource.DAVAbstractResource#getStream()
	 */
	@Override
	public InputStream getStream() {
		// TODO Auto-generated method stub
		return null;
	}

}
