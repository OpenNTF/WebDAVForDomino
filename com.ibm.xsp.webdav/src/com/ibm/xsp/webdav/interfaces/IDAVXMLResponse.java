package com.ibm.xsp.webdav.interfaces;

import java.util.Date;

public interface IDAVXMLResponse {

	/**
	 * @param comment
	 *            Adds a comment to the document
	 */
	public abstract void addComment(String comment);

	/**
	 * Writes out a tag with simple text content in the auxiliary namespace Aux
	 * Tag can't be the first. It gets ignored if it is
	 * 
	 * @param tagName
	 *            the tag name
	 * @param tagValue
	 *            the tag value
	 */
	public abstract void auxTag(String tagName, String tagValue);

	/**
	 * Writes out a tag with text content in cdata format
	 * 
	 * @param tagName
	 *            the tag name
	 * @param tagValue
	 *            the tag value
	 */
	public abstract void cdataTag(String tagName, String tagValue);

	/**
	 * To be called when everything is ready
	 */
	public abstract void closeDocument();

	/**
	 * Closes tags that had been open before
	 * 
	 * @param howMany
	 *            minimum is 1, if -1 is specified all close
	 */
	public abstract void closeTag(int howMany);

	/**
	 * Closes tags that had been open before until and inclusive one specific
	 * tag if that tag is not found all tags are closed
	 * 
	 * @param lastTagToClose
	 * @return true if the tag was encountered, false if not
	 * 
	 */
	public abstract boolean closeTag(String lastTagToClose);

	/**
	 * Writes out an empty tag
	 * 
	 * @param tagName
	 *            the tag name
	 */
	public abstract void emptyTag(String tagName);

	/**
	 * Opens a tag and memorizes the closing tag, so we don't miss it
	 * 
	 * @param tagName
	 *            the TagName
	 */
	public abstract void openTag(String tagName);

	/**
	 * Writes out a tag with simple text content
	 * 
	 * @param tagName
	 *            the tag name
	 * @param tagValue
	 *            the tag value
	 */
	public abstract void simpleTag(String tagName, String tagValue);

	/**
	 * Creates a date tag in the format used for create dates
	 * 
	 * @param TagName
	 * @param date
	 */
	public abstract void dateTagForCreateDate(String TagName, Date date);

	/**
	 * Creates a tag with a date in long format as used in webDAV
	 * 
	 * @param TagName
	 * @param date
	 */
	public abstract void dateTag(String TagName, Date date);

}