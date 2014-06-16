/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.repository.ui.author;

import java.util.Date;
import java.util.List;

import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.filter.FilterFactory;
import org.olat.core.util.resource.OresHelper;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryAuthorView;
import org.olat.repository.RepositoryEntryLight;
import org.olat.repository.RepositoryEntryRef;
import org.olat.repository.model.RepositoryEntryLifecycle;
import org.olat.repository.ui.PriceMethod;

/**
 * 
 * Initial date: 28.04.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class AuthoringEntryRow implements RepositoryEntryRef, RepositoryEntryLight {
	private boolean marked;
	private boolean selected;
	
	private Long key;
	private String name;
	private String author;
	private String authors;
	private String shortenedDescription;
	
	private boolean membersOnly;
	private int access;
	private int statusCode;

	private Date lastUsage;
	private Date creationDate;
	
	private String externalId;
	private String externalRef;
	
	private String lifecycleLabel;
	private String lifecycleSoftKey;
	private Date lifecycleStart;
	private Date lifecycleEnd;
	
	private List<PriceMethod> accessTypes;

	private OLATResourceable olatResource;
	
	private FormLink markLink;
	
	public AuthoringEntryRow(RepositoryEntryAuthorView view, String fullnameAuthor) {
		key = view.getKey();
		name = view.getDisplayname();
		author = fullnameAuthor;
		authors = view.getAuthors();
		if(view.getDescription() != null) {
			String shortDesc = FilterFactory.getHtmlTagsFilter().filter(view.getDescription());
			if(shortDesc.length() > 255) {
				shortenedDescription = shortDesc.substring(0, 255);
			} else {
				shortenedDescription = shortDesc;
			}
		} else {
			shortenedDescription = "";
		}

		lastUsage = view.getLastUsage();
		creationDate = view.getCreationDate();
		
		externalId = view.getExternalId();
		externalRef = view.getExternalRef();
		
		membersOnly = view.isMembersOnly();
		access = view.getAccess();
		statusCode = view.getStatusCode();
		
		olatResource = OresHelper.clone(view.getOlatResource());
		
		RepositoryEntryLifecycle lifecycle = view.getLifecycle();
		if(lifecycle != null) {
			lifecycleStart = lifecycle.getValidFrom();
			lifecycleEnd = lifecycle.getValidTo();
			if(!lifecycle.isPrivateCycle()) {
				lifecycleLabel = lifecycle.getLabel();
				lifecycleSoftKey = lifecycle.getSoftKey();
			}
		}
	}
	
	public AuthoringEntryRow(RepositoryEntry entry, String fullnameAuthor) {
		key = entry.getKey();
		name = entry.getDisplayname();
		author = fullnameAuthor;
		authors = entry.getAuthors();
		if(entry.getDescription() != null) {
			String shortDesc = FilterFactory.getHtmlTagsFilter().filter(entry.getDescription());
			if(shortDesc.length() > 255) {
				shortenedDescription = shortDesc.substring(0, 255);
			} else {
				shortenedDescription = shortDesc;
			}
		} else {
			shortenedDescription = "";
		}

		creationDate = entry.getCreationDate();
		
		externalId = entry.getExternalId();
		externalRef = entry.getExternalRef();
		
		membersOnly = entry.isMembersOnly();
		access = entry.getAccess();
		statusCode = entry.getStatusCode();
		
		olatResource = OresHelper.clone(entry.getOlatResource());
		
		RepositoryEntryLifecycle lifecycle = entry.getLifecycle();
		if(lifecycle != null) {
			lifecycleStart = lifecycle.getValidFrom();
			lifecycleEnd = lifecycle.getValidTo();
			if(!lifecycle.isPrivateCycle()) {
				lifecycleLabel = lifecycle.getLabel();
				lifecycleSoftKey = lifecycle.getSoftKey();
			}
		}
	}
	
	public String getCssClass() {
		return "o_CourseModule_icon";
	}
	
	@Override
	public Long getKey() {
		return key;
	}
	
	public Date getCreationDate() {
		return creationDate;
	}
	
	@Override
	public int getStatusCode() {
		return statusCode;
	}

	@Override
	public boolean isMembersOnly() {
		return membersOnly;
	}

	@Override
	public int getAccess() {
		return access;
	}

	public Date getLastUsage() {
		return lastUsage;
	}

	@Override
	public String getDisplayname() {
		return name;
	}

	@Override
	public String getDescription() {
		return getShortenedDescription();
	}
	
	public String getShortenedDescription() {
		return shortenedDescription;
	}
	
	public String getExternalId() {
		return externalId;
	}

	public String getExternalRef() {
		return externalRef;
	}

	public String getLifecycleLabel() {
		return lifecycleLabel;
	}

	public String getLifecycleSoftKey() {
		return lifecycleSoftKey;
	}

	public Date getLifecycleStart() {
		return lifecycleStart;
	}

	public Date getLifecycleEnd() {
		return lifecycleEnd;
	}
	
	public List<PriceMethod> getAccessTypes() {
		return accessTypes;
	}

	public void setAccessTypes(List<PriceMethod> accessTypes) {
		this.accessTypes = accessTypes;
	}

	public OLATResourceable getRepositoryEntryResourceable() {
		return OresHelper.createOLATResourceableInstance("RepositoryEntry", getKey());
	}
	
	@Override
	public String getResourceType() {
		return olatResource.getResourceableTypeName();
	}

	/**
	 * This is a clone of the repositoryEntry.getOLATResource();
	 * @return
	 */
	public OLATResourceable getOLATResourceable() {
		return olatResource;
	}

	public String getAuthor() {
		return author;
	}

	public String getAuthors() {
		return authors;
	}

	
	public boolean isMarked() {
		return marked;
	}
	
	public void setMarked(boolean marked) {
		this.marked = marked;
	}
	
	public boolean isSelected() {
		return selected;
	}
	
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public FormLink getMarkLink() {
		return markLink;
	}

	public void setMarkLink(FormLink markLink) {
		this.markLink = markLink;
	}
}