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
 * 12.10.2011 by frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.repository;

import java.util.ArrayList;
import java.util.List;

import org.olat.catalog.CatalogEntry;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;

/**
 * 
 * Initial date: 12.03.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class SearchMyRepositoryEntryViewParams {
	private Identity identity;
	private Roles roles;
	
	private Boolean marked;
	private boolean membershipMandatory = false;
	
	private OrderBy orderBy;
	private boolean asc;
	private List<Filter> filters;
	private CatalogEntry parentEntry;
	private List<String> resourceTypes;
	private List<Long> repoEntryKeys;
	
	public SearchMyRepositoryEntryViewParams(Identity identity, Roles roles, String... resourceTypes) {
		this.identity = identity;
		this.roles = roles;
		addResourceTypes(resourceTypes);
	}
	
	public CatalogEntry getParentEntry() {
		return parentEntry;
	}

	public void setParentEntry(CatalogEntry parentEntry) {
		this.parentEntry = parentEntry;
	}

	public boolean isMembershipMandatory() {
		return membershipMandatory;
	}

	public void setMembershipMandatory(boolean membershipMandatory) {
		this.membershipMandatory = membershipMandatory;
	}

	public List<Long> getRepoEntryKeys() {
		return repoEntryKeys;
	}

	public void setRepoEntryKeys(List<Long> repoEntryKeys) {
		this.repoEntryKeys = repoEntryKeys;
	}

	public OrderBy getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(OrderBy orderBy) {
		this.orderBy = orderBy;
	}

	public boolean isOrderByAsc() {
		return asc;
	}

	public void setOrderByAsc(boolean asc) {
		this.asc = asc;
	}

	public List<Filter> getFilters() {
		return filters;
	}

	public void setFilters(List<Filter> filters) {
		this.filters = filters;
	}
	
	public boolean isLifecycleFilterDefined() {
		return filters != null && (filters.contains(Filter.upcomingCourses)
				|| filters.contains(Filter.currentCourses)
				|| filters.contains(Filter.oldCourses));
	}
	
	public boolean isResourceTypesDefined() {
		return resourceTypes != null && resourceTypes.size() > 0;
	}

	public List<String> getResourceTypes() {
		return resourceTypes;
	}
	
	public void setResourceTypes(List<String> resourceTypes) {
		this.resourceTypes = resourceTypes;
	}
	
	public void addResourceTypes(String... resourceTypes) {
		if(this.resourceTypes == null) {
			this.resourceTypes = new ArrayList<String>();
		}
		if(resourceTypes != null) {
			for(String resourceType:resourceTypes) {
				this.resourceTypes.add(resourceType);
			}
		}
	}
	
	public Identity getIdentity() {
		return identity;
	}
	
	public Roles getRoles() {
		return roles;
	}
	
	public Boolean getMarked() {
		return marked;
	}

	public void setMarked(Boolean marked) {
		this.marked = marked;
	}
	
	public enum OrderBy {
		automatic,
		favorit,
		lastVisited,
		passed,
		score,
		title,
		lifecycle,
		author,
		creationDate,
		lastModified,
		rating	
	}
	
	public enum Filter {
		currentCourses,
		oldCourses,
		upcomingCourses,
		asParticipant,
		asCoach,
		asAuthor,
		notBooked,
		passed,
		notPassed,
		withoutPassedInfos
	}
}