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
package org.olat.group.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.TypedQuery;

import org.olat.basesecurity.Constants;
import org.olat.basesecurity.Group;
import org.olat.basesecurity.GroupRoles;
import org.olat.basesecurity.IdentityRef;
import org.olat.basesecurity.NamedGroupImpl;
import org.olat.basesecurity.SecurityGroupMembershipImpl;
import org.olat.basesecurity.manager.GroupDAO;
import org.olat.basesecurity.model.GroupMembershipImpl;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.PersistenceHelper;
import org.olat.core.id.Identity;
import org.olat.core.util.StringHelper;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupImpl;
import org.olat.group.BusinessGroupRef;
import org.olat.group.BusinessGroupShort;
import org.olat.group.model.BGRepositoryEntryRelation;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryRef;
import org.olat.repository.RepositoryEntryRelationType;
import org.olat.repository.RepositoryEntryShort;
import org.olat.repository.manager.RepositoryEntryRelationDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
@Service("businessGroupRelationDao")
public class BusinessGroupRelationDAO {

	@Autowired
	private DB dbInstance;
	@Autowired
	private GroupDAO groupDao;
	@Autowired
	private RepositoryEntryRelationDAO repositoryEntryRelationDao;
	
	public void addRelationToResource(BusinessGroup group, RepositoryEntry re) {
		repositoryEntryRelationDao.createRelation(((BusinessGroupImpl)group).getBaseGroup(), re);
	}
	
	public void addRole(Identity identity, BusinessGroup businessGroup, String role) {
		Group group = getGroup(businessGroup);
		groupDao.addMembership(group, identity, role);
	}
	
	public boolean removeRole(Identity identity, BusinessGroup businessGroup, String role) {
		StringBuilder sb = new StringBuilder();
		sb.append("select membership from ").append(BusinessGroupImpl.class.getName()).append(" as bgroup ")
		  .append(" inner join bgroup.baseGroup as baseGroup")
		  .append(" inner join baseGroup.members as membership")
		  .append(" where bgroup.key=:businessGroupKey and membership.identity.key=:identityKey and membership.role=:role");

		 List<GroupMembershipImpl> memberships = dbInstance.getCurrentEntityManager().createQuery(sb.toString(), GroupMembershipImpl.class)
				.setParameter("businessGroupKey", businessGroup.getKey())
				.setParameter("identityKey", identity.getKey())
				.setParameter("role", role)
				.getResultList();
		 if(memberships.size() > 0) {
			 dbInstance.getCurrentEntityManager().remove(memberships.get(0)); 
		 }
		 return memberships.size() > 0;
	}
	
	public Group getGroup(BusinessGroup businessGroup) {
		StringBuilder sb = new StringBuilder();
		sb.append("select baseGroup from ").append(BusinessGroupImpl.class.getName()).append(" as bgroup ")
		  .append(" inner join bgroup.baseGroup as baseGroup")
		  .append(" where bgroup.key=:businessGroupKey");

		return dbInstance.getCurrentEntityManager().createQuery(sb.toString(), Group.class)
				.setParameter("businessGroupKey", businessGroup.getKey())
				.getSingleResult();
	}
	
	public List<String> getRoles(Identity identity, BusinessGroup group) {
		StringBuilder sb = new StringBuilder();
		sb.append("select membership.role from ").append(BusinessGroupImpl.class.getName()).append(" as bgroup ")
		  .append(" inner join bgroup.baseGroup as baseGroup")
		  .append(" inner join baseGroup.members as membership")
		  .append(" where bgroup.key=:businessGroupKey and membership.identity.key=:identityKey");

		return dbInstance.getCurrentEntityManager().createQuery(sb.toString(), String.class)
				.setParameter("businessGroupKey", group.getKey())
				.setParameter("identityKey", identity.getKey())
				.getResultList();
	}
	
	public int countRoles(BusinessGroup group, String... role) {
		StringBuilder sb = new StringBuilder();
		sb.append("select count(membership) from ").append(BusinessGroupImpl.class.getName()).append(" as bgroup ")
		  .append(" inner join bgroup.baseGroup as baseGroup")
		  .append(" inner join baseGroup.members as membership")
		  .append(" where bgroup.key=:businessGroupKey");
		
		List<String> roleList = GroupRoles.toList(role);
		if(roleList.size() > 0) {
			sb.append(" and membership.role in (:roles)");
		}
		TypedQuery<Number> countQuery = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Number.class)
				.setParameter("businessGroupKey", group.getKey());
		if(roleList.size() > 0) {
			countQuery.setParameter("roles", roleList);
		}		
		Number count = countQuery.getSingleResult();
		return count == null ? 0 : count.intValue();
	}
	
	/**
	 * Return the number of coaches with author rights.
	 * @param group
	 * @return
	 */
	public int countAuthors(BusinessGroup group) {
		StringBuilder sb = new StringBuilder();
		sb.append("select count(membership) from ").append(BusinessGroupImpl.class.getName()).append(" as bgroup ")
		  .append(" inner join bgroup.baseGroup as baseGroup")
		  .append(" inner join baseGroup.members as membership")
		  .append(" where bgroup.key=:businessGroupKey and membership.role='").append(GroupRoles.coach.name()).append("'")
		  .append(" and exists (")
		  .append("   select sgmsi from ").append(SecurityGroupMembershipImpl.class.getName()).append(" as sgmsi, ").append(NamedGroupImpl.class.getName()).append(" as ngroup ")
		  .append("     where sgmsi.identity=membership.identity and sgmsi.securityGroup=ngroup.securityGroup")
		  .append("     and ngroup.groupName in ('").append(Constants.GROUP_AUTHORS).append("')")
		  .append(" )");
	
		Number count = dbInstance.getCurrentEntityManager().createQuery(sb.toString(), Number.class)
				.setParameter("businessGroupKey", group.getKey())
				.getSingleResult();
		return count == null ? 0 : count.intValue();
	}
	
	/**
	 * Match the list of roles with the list of specfified roles
	 * @param identity
	 * @param group
	 * @param roles
	 * @return
	 */
	public boolean hasRole(IdentityRef identity, BusinessGroupRef group, String role) {
		StringBuilder sb = new StringBuilder();
		sb.append("select count(membership) from ").append(BusinessGroupImpl.class.getName()).append(" as bgroup ")
		  .append(" inner join bgroup.baseGroup as baseGroup")
		  .append(" inner join baseGroup.members as membership")
		  .append(" where bgroup.key=:businessGroupKey and membership.identity.key=:identityKey and membership.role=:role");

		Number count = dbInstance.getCurrentEntityManager().createQuery(sb.toString(), Number.class)
				.setParameter("businessGroupKey", group.getKey())
				.setParameter("identityKey", identity.getKey())
				.setParameter("role", role)
				.getSingleResult();
		return count == null ? false : count.intValue() > 0;
	}
	
	public void touchMembership(IdentityRef identity, BusinessGroupRef group) {
		StringBuilder sb = new StringBuilder();
		sb.append("select membership from ").append(BusinessGroupImpl.class.getName()).append(" as bgroup ")
		  .append(" inner join bgroup.baseGroup as baseGroup")
		  .append(" inner join baseGroup.members as membership")
		  .append(" where bgroup.key=:businessGroupKey and membership.identity.key=:identityKey");

		List<GroupMembershipImpl> memberships = dbInstance.getCurrentEntityManager().createQuery(sb.toString(), GroupMembershipImpl.class)
				.setParameter("businessGroupKey", group.getKey())
				.setParameter("identityKey", identity.getKey())
				.getResultList();
		for(GroupMembershipImpl membership:memberships) {
			membership.setLastModified(new Date());
		}
	}
	
	public List<Identity> getMembers(BusinessGroupRef group, String... roles) {
		StringBuilder sb = new StringBuilder();
		sb.append("select membership.identity from ").append(BusinessGroupImpl.class.getName()).append(" as bgroup ")
		  .append(" inner join bgroup.baseGroup as baseGroup")
		  .append(" inner join baseGroup.members as membership")
		  .append(" where bgroup.key=:businessGroupKey and membership.role in (:roles)");
		
		List<String> roleList = GroupRoles.toList(roles);
		List<Identity> members = dbInstance.getCurrentEntityManager().createQuery(sb.toString(), Identity.class)
				.setParameter("businessGroupKey", group.getKey())
				.setParameter("roles", roleList)
				.getResultList();
		return members;
	}
	
	public List<Long> getMemberKeys(List<? extends BusinessGroupRef> groups, String... roles) {
		if(groups == null || groups.isEmpty()) return Collections.emptyList();
		
		StringBuilder sb = new StringBuilder();
		sb.append("select membership.identity.key from ").append(BusinessGroupImpl.class.getName()).append(" as bgroup ")
		  .append(" inner join bgroup.baseGroup as baseGroup")
		  .append(" inner join baseGroup.members as membership")
		  .append(" where bgroup.key in (:businessGroupKeys) and membership.role in (:roles)");
		
		List<String> roleList = GroupRoles.toList(roles);
		List<Long> groupKeys = new ArrayList<>(groups.size());
		for(BusinessGroupRef group:groups) {
			groupKeys.add(group.getKey());
		}
		
		List<Long> members = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Long.class)
				.setParameter("businessGroupKeys", groupKeys)
				.setParameter("roles", roleList)
				.getResultList();
		return members;
	}
	
	public List<Identity> getMembersOrderByDate(BusinessGroupRef group, String... roles) {
		StringBuilder sb = new StringBuilder();
		sb.append("select membership.identity from ").append(BusinessGroupImpl.class.getName()).append(" as bgroup ")
		  .append(" inner join bgroup.baseGroup as baseGroup")
		  .append(" inner join baseGroup.members as membership")
		  .append(" where bgroup.key=:businessGroupKey and membership.role in (:roles) order by membership.creationDate");

		List<String> roleList = GroupRoles.toList(roles);
		List<Identity> members = dbInstance.getCurrentEntityManager().createQuery(sb.toString(), Identity.class)
				.setParameter("businessGroupKey", group.getKey())
				.setParameter("roles", roleList)
				.getResultList();
		return members;
	}
	
	public void deleteRelation(BusinessGroup group, RepositoryEntry entry) {
		repositoryEntryRelationDao.removeRelation(group.getBaseGroup(), entry);
	}
	
	public void deleteRelationsToRepositoryEntry(BusinessGroup group) {
		repositoryEntryRelationDao.removeRelation(group.getBaseGroup());
	}

	public boolean isIdentityInBusinessGroup(IdentityRef identity, Long groupKey, boolean ownedById, boolean attendeeById,
			RepositoryEntryRef resource) {
		StringBuilder sb = new StringBuilder();
		sb.append("select count(bgi) from ").append(BusinessGroupImpl.class.getName()).append(" bgi")
		  .append(" inner join bgi.baseGroup as baseGroup")
		  .append(" inner join baseGroup.members as bmember")
		  .append(" where bmember.identity.key=:identityKey and bmember.role in (:roles)");
		if(groupKey != null) {
			sb.append(" and bgi.key=:groupKey");
		}

		List<String> roles = new ArrayList<>(2);
		if(ownedById) {
			roles.add(GroupRoles.coach.name());
		}
		if(attendeeById) {
			roles.add(GroupRoles.participant.name());
		}
		
		if(resource != null) {
			sb.append(" and exists (")
				.append("   select relation from repoentrytogroup as relation where relation.group=baseGroup and relation.entry.key=:resourceKey")
				.append(" )");
		}

		TypedQuery<Number> query = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Number.class)
				.setParameter("identityKey", identity.getKey())
				.setParameter("roles", roles);
		if(resource != null) {
				query.setParameter("resourceKey", resource.getKey());
		}
		if(groupKey != null) {
			query.setParameter("groupKey", groupKey);
		}
		query.setHint("org.hibernate.cacheable", Boolean.TRUE);
		Number count = query.getSingleResult();
		return count.intValue() > 0;
	}

	public List<Identity> getMembersOf(RepositoryEntryRef resource, boolean coach, boolean participant) {
		String[] roles;
		if(coach && participant) {
			roles = new String[]{ GroupRoles.coach.name(), GroupRoles.participant.name() };
		} else if(coach) {
			roles = new String[]{ GroupRoles.coach.name() };
		} else if(participant) {
			roles = new String[]{ GroupRoles.participant.name() };
		} else {
			return Collections.emptyList();
		}
		return repositoryEntryRelationDao.getMembers(resource, RepositoryEntryRelationType.both, roles);
	}
	
	public int countResources(BusinessGroup group) {
		return repositoryEntryRelationDao.countRelations(group.getBaseGroup());
	}
	
	public boolean hasResources(List<BusinessGroup> groups) {
		if(groups == null || groups.isEmpty()) return false;
		
		List<Group> baseGroups = new ArrayList<>(groups.size());
		for(BusinessGroup group:groups) {
			baseGroups.add(group.getBaseGroup());
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("select count(rel) from repoentrytogroup as rel")
		  .append(" where rel.group in (:groups)");

		Number count = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), Number.class)
			.setParameter("groups", baseGroups)
			.getSingleResult();
		return count == null ? false : count.intValue() > 0;
	}
	
	public List<RepositoryEntry> findRepositoryEntries(Collection<BusinessGroup> groups, int firstResult, int maxResults) {
		if(groups == null || groups.isEmpty()) {
			return Collections.emptyList();
		}

		StringBuilder sb = new StringBuilder();
		sb.append("select distinct v from ").append(RepositoryEntry.class.getName()).append(" as v ")
			.append(" inner join fetch v.olatResource as ores ")
			.append(" left join fetch v.lifecycle as lifecycle")
			.append(" inner join v.groups as relGroup")
			.append(" where exists (")
			.append("   select bgi from ").append(BusinessGroupImpl.class.getName()).append(" as bgi where bgi.baseGroup=relGroup.group and bgi.key in (:groupKeys)")
			.append(" )");

		TypedQuery<RepositoryEntry> query = dbInstance.getCurrentEntityManager().createQuery(sb.toString(), RepositoryEntry.class);
		query.setFirstResult(firstResult);
		if(maxResults > 0) {
			query.setMaxResults(maxResults);
		}
		
		List<Long> groupKeys = PersistenceHelper.toKeys(groups);
		query.setParameter("groupKeys", groupKeys);
		return query.getResultList();
	}

	public List<RepositoryEntryShort> findShortRepositoryEntries(Collection<BusinessGroupShort> groups, int firstResult, int maxResults) {
		if(groups == null || groups.isEmpty()) {
			return Collections.emptyList();
		}

		StringBuilder sb = new StringBuilder();
		sb.append("select new org.olat.group.model.BGRepositoryEntryShortImpl(v.key, v.displayname) from ").append(RepositoryEntry.class.getName()).append(" as v ")
			.append(" inner join v.olatResource as ores ")
			.append(" inner join v.groups as relGroup")
			.append(" where exists (")
			.append("   select bgi from ").append(BusinessGroupImpl.class.getName()).append(" as bgi where bgi.baseGroup=relGroup.group and bgi.key in (:groupKeys)")
			.append(" )");

		TypedQuery<RepositoryEntryShort> query = dbInstance.getCurrentEntityManager().createQuery(sb.toString(), RepositoryEntryShort.class);
		query.setFirstResult(firstResult);
		if(maxResults > 0) {
			query.setMaxResults(maxResults);
		}

		List<Long> groupKeys = new ArrayList<Long>();
		for(BusinessGroupShort group:groups) {
			groupKeys.add(group.getKey());
		}
		query.setParameter("groupKeys", groupKeys);
		query.setHint("org.hibernate.cacheable", Boolean.TRUE);
		return query.getResultList();
	}
	
	public List<BGRepositoryEntryRelation> findRelationToRepositoryEntries(Collection<Long> groupKeys, int firstResult, int maxResults) {
		if(groupKeys == null || groupKeys.isEmpty()) {
			return Collections.emptyList();
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("select rel from ").append(BGRepositoryEntryRelation.class.getName()).append(" as rel ")
			.append(" where rel.relationId.groupKey in (:groupKeys)");

		TypedQuery<BGRepositoryEntryRelation> query = dbInstance.getCurrentEntityManager().createQuery(sb.toString(), BGRepositoryEntryRelation.class);

		if(firstResult >= 0 && maxResults >= 0) {
			query.setFirstResult(firstResult);
			if(maxResults > 0) {
				query.setMaxResults(maxResults);
			}
			query.setParameter("groupKeys", groupKeys);
			return query.getResultList();
		}

		List<Long> groupKeyList = new ArrayList<Long>(groupKeys);
		List<BGRepositoryEntryRelation> relations = new ArrayList<BGRepositoryEntryRelation>(groupKeys.size());
		
		int count = 0;
		int batch = 500;
		do {
			int toIndex = Math.min(count + batch, groupKeyList.size());
			List<Long> toLoad = groupKeyList.subList(count, toIndex);
			List<BGRepositoryEntryRelation> batchOfRelations = query.setParameter("groupKeys", toLoad).getResultList();
			relations.addAll(batchOfRelations);
			count += batch;
		} while(count < groupKeyList.size());
		return relations;
	}
	
	public List<Long> toGroupKeys(String groupNames, RepositoryEntryRef repoEntry) {
		if(!StringHelper.containsNonWhitespace(groupNames)) return Collections.emptyList();
		
		String[] groupNameArr = groupNames.split(",");
		List<String> names = new ArrayList<String>();
		for(String name:groupNameArr) {
			names.add(name.trim());
		}
		
		if(names.isEmpty()) return Collections.emptyList();
			
		StringBuilder sb = new StringBuilder();
		sb.append("select bgi.key from ").append(BusinessGroupImpl.class.getName()).append(" as bgi ")
		  .append(" inner join bgi.baseGroup as baseGroup")
		  .append(" where bgi.name in (:names)")
		  .append(" and exists (")
		  .append("   select relation from repoentrytogroup as relation where relation.group=baseGroup and relation.entry.key=:repoEntryKey")
		  .append(" )");

		List<Long> keys = dbInstance.getCurrentEntityManager().createQuery(sb.toString(), Long.class)
				.setParameter("repoEntryKey", repoEntry.getKey())
				.setParameter("names", names)
				.setHint("org.hibernate.cacheable", Boolean.TRUE)
				.getResultList();
		return keys;
	}
}
