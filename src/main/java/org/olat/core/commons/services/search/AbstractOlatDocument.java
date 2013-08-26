/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.
*/

package org.olat.core.commons.services.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.olat.core.util.StringHelper;


/**
 * Lucene document mapper.
 * @author Christian Guretzki
 */
public abstract class AbstractOlatDocument implements Serializable {

	private static final long serialVersionUID = 3477625468662703214L;

	// Field names
	public  static final String TITLE_FIELD_NAME = "title";

	public  static final String DESCRIPTION_FIELD_NAME = "description";

	public  static final String CONTENT_FIELD_NAME = "content";

	public  static final String DOCUMENTTYPE_FIELD_NAME = "documenttype";

	public  static final String FILETYPE_FIELD_NAME = "filetype";

	public  static final String RESOURCEURL_FIELD_NAME = "resourceurl";

	public  static final String AUTHOR_FIELD_NAME = "author";

	public  static final String CREATED_FIELD_NAME = "created";

	public  static final String CHANGED_FIELD_NAME = "changed";

	public  static final String TIME_STAMP_NAME = "timestamp";

	public static final String PARENT_CONTEXT_TYPE_FIELD_NAME = "parentcontexttype";

	public static final String PARENT_CONTEXT_NAME_FIELD_NAME = "parentcontextname";
	
	public static final String CSS_ICON = "cssicon";
	
	public static final String RESERVED_TO = "reservedto";

	
  // Lucene Attributes
	private String title = "";
	protected String description = "";
	/** E.g. 'Group','ForumMessage'. */
	private String documentType = "";
	private String fileType = "";
	/** JumpInUrl to E.g. 'Group:123456:Forum:342556:Message:223344'. */ 
	private String resourceUrl = "";
	private String author = "";
	private Date   createdDate = null;
	private Date   lastChange = null;
	private Date   timestamp = null;
	/** Various metadata, most likely doublin core **/
	protected Map<String, List<String>> metadata = null;
	/* e.g. Course */
	private String parentContextType = "";
	/* e.g. Course-name */
	private String parentContextName = "";
	private String cssIcon;
	private String reservedTo;
	
	public AbstractOlatDocument() {
		timestamp = new Date();
	}
	
	
	
	public AbstractOlatDocument(Document document) {
		title        = document.get(TITLE_FIELD_NAME);
		description  = document.get(DESCRIPTION_FIELD_NAME);
		documentType = document.get(DOCUMENTTYPE_FIELD_NAME);
		fileType     = document.get(FILETYPE_FIELD_NAME);
		resourceUrl  = document.get(RESOURCEURL_FIELD_NAME);
		author       = document.get(AUTHOR_FIELD_NAME);
		reservedTo	 = document.get(RESERVED_TO);
		try {
			String f = document.get(CREATED_FIELD_NAME);
			if(StringHelper.containsNonWhitespace(f)) {
				createdDate  = DateTools.stringToDate(f);
			}
		} catch (Exception e) {
			//can happen
		}
		try {
			String f = document.get(CHANGED_FIELD_NAME);
			if(StringHelper.containsNonWhitespace(f)) {
				lastChange   = DateTools.stringToDate(f);
			}
		} catch (Exception e) {
			//can happen
		}
		try {
			String f = document.get(TIME_STAMP_NAME);
			if(StringHelper.containsNonWhitespace(f)) {
				timestamp   = DateTools.stringToDate(f);
			}
		} catch (Exception e) {
			//can happen
		}
		parentContextType = document.get(PARENT_CONTEXT_TYPE_FIELD_NAME);
		parentContextName = document.get(PARENT_CONTEXT_NAME_FIELD_NAME);
		cssIcon = document.get(CSS_ICON);
	}


	/**
	 * @return Returns the author.
	 */
	public String getAuthor() {
		if (author == null) {
			return ""; // Do not return null
		}
		return author;
	}


	/**
	 * @param author The author to set.
	 */
	public void setAuthor(String author) {
		this.author = author;
	}


	/**
	 * @return Returns the description.
	 */
	public String getDescription() {
		if (description == null) {
			return ""; // Do not return null
		}
		return description;
	}


	/**
	 * @param description The description to set.
	 */
	public void setDescription(String description) {
		this.description = description;
	}


	/**
	 * @return Returns the documentType.
	 */
	public String getDocumentType() {
		if (documentType == null) {
			return ""; // Do not return null
		}
		return documentType;
	}


	/**
	 * @param documentType The documentType to set.
	 */
	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}


	/**
	 * @return Returns the fileType.
	 */
	public String getFileType() {
		return fileType;
	}


	/**
	 * @param fileType The fileType to set.
	 */
	public void setFileType(String fileType) {
		this.fileType = fileType;
	}


	/**
	 * @return Returns the lastChange.
	 */
	public Date getLastChange() {
		return lastChange;
	}


	/**
	 * @param lastChange The lastChange to set.
	 */
	public void setLastChange(Date lastChange) {
		this.lastChange = lastChange;
	}


	/**
	 * @return Returns the resourceUrl.
	 */
	public String getResourceUrl() {
		if (resourceUrl == null) {
			return ""; // Do not return null
		}
		return resourceUrl;
	}


	/**
	 * @param resourceUrl The resourceUrl to set.
	 */
	public void setResourceUrl(String resourceUrl) {
		this.resourceUrl = resourceUrl;
	}


	/**
	 * @return Returns the title.
	 */
	public String getTitle() {
		if (title == null) {
			return ""; // Do not return null
		}
		return title;
	}


	/**
	 * @param title The title to set.
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	
	/**
	 * The list of identities who can see the document. It's an optimized
	 * check access for private documents.
	 * @return Return a list of identity keys separated by spaces
	 */
	public String getReservedTo() {
		return reservedTo;
	}

	public void setReservedTo(String reservedTo) {
		this.reservedTo = reservedTo;
	}

	/**
	 * Add generic metadata. It is strongly recommended not to use anything else
	 * than the doublin core metadata namespace here. See {@link http
	 * ://en.wikipedia.org/wiki/Dublin_Core} for more information.
	 * <p>
	 * A metadata element consists of a key-value pair. It is possible to have
	 * more than one value for a key. In this case use the method multiple times
	 * with the same key.
	 * <p>
	 * Example:<br>
	 * DC.subject		OLAT - the best Open Source LMS<br>
	 * DC.creator		Florian GnÔøΩgi
	 * 
	 * @param key The metadata key
	 * @param value The metadata value
	 */
	public synchronized void addMetadata(String key, String value) {
		if (key == null || ! StringHelper.containsNonWhitespace(value)) return;
		// initialize metadata map if never done before
		if (metadata == null) metadata = new HashMap<String, List<String>>();
		// get list of already added values for this key
		List<String> values = metadata.get(key);
		if (values == null) {
			// this meta key has never been added so far
			values = new ArrayList<String>();
			metadata.put(key, values);
		}
		values.add(value);
	}

	/**
	 * Get the list of metadata values for the given key. This might return NULL
	 * if no such metadata is linked to this document.
	 * 
	 * @param key The metadata key, e.g. DC.subject
	 * @return The list of values or NULL if not found
	 */
	public List<String> getMetadataValues(String key) {
		List<String> values = null;
		if (metadata != null) {
				values = metadata.get(key);
		}
		return values;
	}

	public String getParentContextType() {
		if (parentContextType == null) {
			return ""; // Do not return null
		}
		return parentContextType;
	}

	public void setParentContextType(String parentContextType) {
		this.parentContextType = parentContextType;
	}

	public String getParentContextName() {
		if (parentContextName == null) {
			return ""; // Do not return null
		}
		return parentContextName;
	}

	public void setParentContextName(String parentContextName) {
		this.parentContextName = parentContextName;
	}

	public String getCssIcon() {
		return cssIcon;
	}

	public void setCssIcon(String cssIcon) {
		this.cssIcon = cssIcon;
	}

	public Date getTimestamp() {
		return timestamp;
	}


	/**
	 * @return Returns the createdDate.
	 */
	public Date getCreatedDate() {
		return createdDate;
	}


	/**
	 * @param createdDate The createdDate to set.
	 */
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}
	

	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append(this.getDocumentType());
		buf.append("|");
		buf.append(getTitle());
		buf.append("|");
		if (getDescription() != null) buf.append(getDescription());
		buf.append("|");
		buf.append(getResourceUrl());
		
		return buf.toString();
	}

}