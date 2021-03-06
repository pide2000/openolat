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
package org.olat.repository.manager;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.olat.basesecurity.Group;
import org.olat.basesecurity.GroupRoles;
import org.olat.basesecurity.IdentityRef;
import org.olat.basesecurity.manager.GroupDAO;
import org.olat.core.commons.persistence.DB;
import org.olat.core.id.Identity;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryRef;
import org.olat.repository.RepositoryEntryRelationType;
import org.olat.repository.model.RepositoryEntryToGroupRelation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * Initial date: 26.02.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Service
public class RepositoryEntryRelationDAO {
	
	@Autowired
	private DB dbInstance;
	@Autowired
	private GroupDAO groupDao;
	
	/**
	 * Get roles in the repository entry, with business groups too
	 * @param identity
	 * @param re
	 * @return
	 */
	public List<String> getRoles(IdentityRef identity, RepositoryEntryRef re) {
		StringBuilder sb = new StringBuilder();
		sb.append("select membership.role from ").append(RepositoryEntry.class.getName()).append(" as v")
		  .append(" inner join v.groups as relGroup")
		  .append(" inner join relGroup.group as baseGroup")
		  .append(" inner join baseGroup.members as membership")
		  .append(" where v.key=:repoKey and membership.identity.key=:identityKey");

		return dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), String.class)
				.setParameter("identityKey", identity.getKey())
				.setParameter("repoKey", re.getKey())
				.getResultList();
	}
	
	/**
	 * Load role and default information
	 * 
	 * @param identity
	 * @param re
	 * @return Return an array with the role and true if the relation is the default one.
	 */
	public List<Object[]> getRoleAndDefaults(IdentityRef identity, RepositoryEntryRef re) {
		StringBuilder sb = new StringBuilder();
		sb.append("select membership.role, relGroup.defaultGroup from ").append(RepositoryEntry.class.getName()).append(" as v")
		  .append(" inner join v.groups as relGroup")
		  .append(" inner join relGroup.group as baseGroup")
		  .append(" inner join baseGroup.members as membership")
		  .append(" where v.key=:repoKey and membership.identity.key=:identityKey");

		return dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Object[].class)
				.setParameter("identityKey", identity.getKey())
				.setParameter("repoKey", re.getKey())
				.getResultList();
	}

	/**
	 * Has role in the repository entry only (without business groups)
	 * @param identity
	 * @param re
	 * @param roles
	 * @return
	 */
	public boolean hasRole(IdentityRef identity, RepositoryEntryRef re, String... roles) {
		if(identity == null || re == null || re.getKey() == null) return false;
		List<String> roleList = GroupRoles.toList(roles);
		
		StringBuilder sb = new StringBuilder();
		sb.append("select count(membership) from ").append(RepositoryEntry.class.getName()).append(" as v")
		  .append(" inner join v.groups as relGroup on relGroup.defaultGroup=true")
		  .append(" inner join relGroup.group as baseGroup")
		  .append(" inner join baseGroup.members as membership")
		  .append(" where v.key=:repoKey and membership.identity.key=:identityKey");
		if(roleList.size() > 0) {
			sb.append(" and membership.role in (:roles)");
		}
		
		TypedQuery<Number> query = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Number.class)
				.setParameter("identityKey", identity.getKey())
				.setParameter("repoKey", re.getKey());
		if(roleList.size() > 0) {
			query.setParameter("roles", roleList);
		}
		
		Number count = query.getSingleResult();
		return count == null ? false : count.intValue() > 0;
	}
	
	public void addRole(Identity identity, RepositoryEntry re, String role) {
		Group group = getDefaultGroup(re);
		groupDao.addMembership(group, identity, role);
	}
	
	public int removeRole(IdentityRef identity, RepositoryEntry re, String role) {
		Group group = getDefaultGroup(re);
		return groupDao.removeMembership(group, identity, role);
	}
	
	public int removeRole(RepositoryEntry re, String role) {
		Group group = getDefaultGroup(re);
		return groupDao.removeMemberships(group, role);
	}

	public Group getDefaultGroup(RepositoryEntryRef re) {
		StringBuilder sb = new StringBuilder();
		sb.append("select baseGroup from ").append(RepositoryEntry.class.getName()).append(" as v ")
		  .append(" inner join v.groups as relGroup on relGroup.defaultGroup=true")
		  .append(" inner join relGroup.group as baseGroup")
		  .append(" where v.key=:repoKey");

		return dbInstance.getCurrentEntityManager().createQuery(sb.toString(), Group.class)
				.setParameter("repoKey", re.getKey())
				.getSingleResult();
	}
	
	/**
	 * Membership calculated with business groups too. Role are owner, coach and participant.
	 * 
	 * @param identity
	 * @param entry
	 * @return
	 */
	public boolean isMember(IdentityRef identity, RepositoryEntryRef entry) {
		StringBuilder sb = new StringBuilder();
		sb.append("select v.key, membership.identity.key ")
		  .append(" from ").append(RepositoryEntry.class.getName()).append(" as v ")
		  .append(" inner join v.groups as relGroup")
		  .append(" inner join relGroup.group as baseGroup")
		  .append(" inner join baseGroup.members as membership on membership.role in ")
		  .append("   ('").append(GroupRoles.owner.name()).append("','").append(GroupRoles.coach.name()).append("','").append(GroupRoles.participant.name()).append("')")
		  .append(" where membership.identity.key=:identityKey and v.key=:repositoryEntryKey ");

		List<Object[]> counter = dbInstance.getCurrentEntityManager().createQuery(sb.toString(), Object[].class)
				.setParameter("identityKey", identity.getKey())
				.setParameter("repositoryEntryKey", entry.getKey())
				.setHint("org.hibernate.cacheable", Boolean.TRUE)
				.getResultList();
		return !counter.isEmpty();
	}
	
	/**
	 * Membership calculated with business groups too
	 * 
	 * @param identity
	 * @param entry
	 * @return
	 */
	public void filterMembership(IdentityRef identity, List<Long> entries) {
		if(entries == null || entries.isEmpty()) return;
		
		StringBuilder sb = new StringBuilder();
		sb.append("select v.key, membership.identity.key ")
		  .append(" from ").append(RepositoryEntry.class.getName()).append(" as v ")
		  .append(" inner join v.groups as relGroup")
		  .append(" inner join relGroup.group as baseGroup")
		  .append(" inner join baseGroup.members as membership on membership.role in ")
		  .append("   ('").append(GroupRoles.owner.name()).append("','").append(GroupRoles.coach.name()).append("','").append(GroupRoles.participant.name()).append("')")
		  .append(" where membership.identity.key=:identityKey and v.key in (:repositoryEntryKey)");

		List<Object[]> membershipList = dbInstance.getCurrentEntityManager().createQuery(sb.toString(), Object[].class)
				.setParameter("identityKey", identity.getKey())
				.setParameter("repositoryEntryKey", entries)
				.getResultList();
		
		Set<Object> memberships = new HashSet<>();
		for(Object[] membership: membershipList) {
			memberships.add(membership[0]);
		}
		
		for(Iterator<Long> entryIt=entries.iterator(); entryIt.hasNext(); ) {
			if(!memberships.contains(entryIt.next())) {
				entryIt.remove();
			}
		}
	}
	
	/**
	 * It will count all members, business groups members too
	 * @param re
	 * @param roles
	 * @return
	 */
	public int countMembers(RepositoryEntryRef re, String... roles) {
		List<String> roleList = GroupRoles.toList(roles);
		
		StringBuilder sb = new StringBuilder();
		sb.append("select count(members) from ").append(RepositoryEntry.class.getName()).append(" as v")
		  .append(" inner join v.groups as relGroup")
		  .append(" inner join relGroup.group as baseGroup")
		  .append(" inner join baseGroup.members as members")
		  .append(" where v.key=:repoKey");
		if(roleList.size() > 0) {
				sb.append(" and members.role in (:roles)");
		}
		
		TypedQuery<Number> query = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Number.class)
				.setParameter("repoKey", re.getKey());
		if(roleList.size() > 0) {
				query.setParameter("roles", roleList);
		}
		
		Number count = query.getSingleResult();
		return count == null ? 0 : count.intValue();
	}
	
	public List<Long> getAuthorKeys(RepositoryEntryRef re) {
		
		StringBuilder sb = new StringBuilder();
		sb.append("select members.identity.key from ").append(RepositoryEntry.class.getName()).append(" as v")
		  .append(" inner join v.groups as relGroup on relGroup.defaultGroup=true")
		  .append(" inner join relGroup.group as baseGroup")
		  .append(" inner join baseGroup.members as members on members.role='").append(GroupRoles.owner.name()).append("'")
		  .append(" where v.key=:repoKey");
		
		TypedQuery<Long> query = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Long.class)
				.setParameter("repoKey", re.getKey());
		return query.getResultList();
	}
	
	public List<Identity> getMembers(RepositoryEntryRef re, RepositoryEntryRelationType type, String... roles) {
		List<String> roleList = GroupRoles.toList(roles);
		
		String def;
		switch(type) {
			case defaultGroup: def = " on relGroup.defaultGroup=true"; break;
			case notDefaultGroup: def = " on relGroup.defaultGroup=false"; break;
			default: def = "";
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("select members.identity from ").append(RepositoryEntry.class.getName()).append(" as v")
		  .append(" inner join v.groups as relGroup").append(def)
		  .append(" inner join relGroup.group as baseGroup")
		  .append(" inner join baseGroup.members as members")
		  .append(" where v.key=:repoKey");
		if(roleList.size() > 0) {
				sb.append(" and members.role in (:roles)");
		}
			
		TypedQuery<Identity> query = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Identity.class)
				.setParameter("repoKey", re.getKey());
		if(roleList.size() > 0) {
				query.setParameter("roles", roleList);
		}
		return query.getResultList();
	}
	
	public List<Long> getMemberKeys(RepositoryEntryRef re, RepositoryEntryRelationType type, String... roles) {
		List<String> roleList = GroupRoles.toList(roles);
		
		String def;
		switch(type) {
			case defaultGroup: def = " on relGroup.defaultGroup=true"; break;
			case notDefaultGroup: def = " on relGroup.defaultGroup=false"; break;
			default: def = "";
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("select members.identity.key from ").append(RepositoryEntry.class.getName()).append(" as v")
		  .append(" inner join v.groups as relGroup").append(def)
		  .append(" inner join relGroup.group as baseGroup")
		  .append(" inner join baseGroup.members as members")
		  .append(" where v.key=:repoKey");
		if(roleList.size() > 0) {
				sb.append(" and members.role in (:roles)");
		}
			
		TypedQuery<Long> query = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Long.class)
				.setParameter("repoKey", re.getKey());
		if(roleList.size() > 0) {
				query.setParameter("roles", roleList);
		}
		return query.getResultList();
	}
	
	public boolean removeMembers(RepositoryEntry re, List<Identity> members) {
		Group group = getDefaultGroup(re);
		for(Identity member:members) {
			groupDao.removeMembership(group, member);
		}
		return true;
	}
	
	public RepositoryEntryToGroupRelation createRelation(Group group, RepositoryEntry re) {
		RepositoryEntryToGroupRelation rel = new RepositoryEntryToGroupRelation();
		rel.setCreationDate(new Date());
		rel.setDefaultGroup(false);
		rel.setGroup(group);
		rel.setEntry(re);
		dbInstance.getCurrentEntityManager().persist(rel);
		return rel;
	}
	
	public int removeRelation(Group group, RepositoryEntryRef re) {
		EntityManager em = dbInstance.getCurrentEntityManager();
		List<RepositoryEntryToGroupRelation> rels = em.createNamedQuery("relationByRepositoryEntryAndGroup", RepositoryEntryToGroupRelation.class)
			.setParameter("repoKey", re.getKey())
			.setParameter("groupKey", group.getKey())
			.getResultList();

		for(RepositoryEntryToGroupRelation rel:rels) {
			em.remove(rel);
		}
		return rels.size();
	}
	
	/**
	 * This will remove all relations from the repository entry,
	 * the default one too.
	 * 
	 * @param re
	 * @return
	 */
	public int removeRelations(RepositoryEntryRef re) {
		EntityManager em = dbInstance.getCurrentEntityManager();
		List<RepositoryEntryToGroupRelation> rels = em.createNamedQuery("relationByRepositoryEntry", RepositoryEntryToGroupRelation.class)
			.setParameter("repoKey", re.getKey())
			.getResultList();
		for(RepositoryEntryToGroupRelation rel:rels) {
			em.remove(rel);
		}
		return rels.size();
	}

	public int removeRelation(Group group) {
		EntityManager em = dbInstance.getCurrentEntityManager();
		List<RepositoryEntryToGroupRelation> rels = em.createNamedQuery("relationByGroup", RepositoryEntryToGroupRelation.class)
			.setParameter("groupKey", group.getKey())
			.getResultList();
		
		int count = 0;
		for(RepositoryEntryToGroupRelation rel:rels) {
			if(!rel.isDefaultGroup()) {
				em.remove(rel);
				count++;
			}
		}
		return count;
	}
	
	/**
	 * Count the number of relation from a group to repository entries
	 * 
	 * @param group
	 * @return The number of relations
	 */
	public int countRelations(Group group) {
		StringBuilder sb = new StringBuilder();
		sb.append("select count(rel) from repoentrytogroup as rel")
		  .append(" where rel.group.key=:groupKey");

		Number count = dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), Number.class)
			.setParameter("groupKey", group.getKey())
			.getSingleResult();
		return count == null ? 0 : count.intValue();
	}
	
	/**
	 * Get the relation from a base group to the repository entries
	 * 
	 * @param groups
	 * @return The list of relations
	 */
	public List<RepositoryEntryToGroupRelation> getRelations(List<Group> groups) {
		if(groups == null || groups.isEmpty()) {
			return Collections.emptyList();
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("select rel from repoentrytogroup as rel")
		  .append(" inner join fetch rel.entry as entry")
		  .append(" inner join fetch rel.group as baseGroup")
		  .append(" where baseGroup in (:groups)");

		return dbInstance.getCurrentEntityManager()
			.createQuery(sb.toString(), RepositoryEntryToGroupRelation.class)
			.setParameter("groups", groups)
			.getResultList();
	}
}
