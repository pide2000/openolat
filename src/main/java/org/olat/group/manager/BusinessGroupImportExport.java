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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.olat.collaboration.CollaborationTools;
import org.olat.collaboration.CollaborationToolsFactory;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.FileUtils;
import org.olat.core.util.StringHelper;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupService;
import org.olat.group.area.BGArea;
import org.olat.group.area.BGAreaManager;
import org.olat.group.model.BGAreaReference;
import org.olat.group.model.BusinessGroupEnvironment;
import org.olat.group.model.BusinessGroupReference;
import org.olat.properties.Property;
import org.olat.resource.OLATResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
@Service("businessGroupImportExport")
public class BusinessGroupImportExport {
	
	private final OLog log = Tracing.createLoggerFor(BusinessGroupImportExport.class);

	private GroupXStream xstream = new GroupXStream();
	
	@Autowired
	private BGAreaManager areaManager;
	@Autowired
	private BusinessGroupService businessGroupService;
	@Autowired
	private BusinessGroupPropertyDAO businessGroupPropertyManager;
	
	
	public void exportGroups(List<BusinessGroup> groups, List<BGArea> areas, File fExportFile, BusinessGroupEnvironment env, boolean backwardsCompatible) {
		if (groups == null || groups.isEmpty()) {
			return; // nothing to do... says Florian.
		}

		OLATGroupExport root = new OLATGroupExport();
		// export areas
		root.setAreas(new AreaCollection());
		root.getAreas().setGroups(new ArrayList<Area>());
		for (BGArea area : areas) {
			Area newArea = new Area();
			newArea.key = backwardsCompatible ? null : area.getKey();
			newArea.name = area.getName();
			if(backwardsCompatible && env != null) {
				String newName = env.getAreaName(area.getKey());
				if(StringHelper.containsNonWhitespace(newName)) {
					newArea.name = newName;
				}
			}
			newArea.description = Collections.singletonList(area.getDescription());
			root.getAreas().getGroups().add(newArea);
		}

		// export groups
		root.setGroups(new GroupCollection());
		root.getGroups().setGroups(new ArrayList<Group>());
		for (BusinessGroup group : groups) {
			String groupName = null;
			if(backwardsCompatible && env != null) {
				groupName = env.getGroupName(group.getKey());
			}
			Group newGroup = exportGroup(fExportFile, group, groupName, backwardsCompatible);
			root.getGroups().getGroups().add(newGroup);
		}
		saveGroupConfiguration(fExportFile, root);
	}
	
	public void exportGroup(BusinessGroup group, File fExportFile, boolean backwardsCompatible) {
		OLATGroupExport root = new OLATGroupExport();
		Group newGroup = exportGroup(fExportFile, group, null, backwardsCompatible);
		root.setGroups(new GroupCollection());
		root.getGroups().setGroups(new ArrayList<Group>());
		root.getGroups().getGroups().add(newGroup);
		saveGroupConfiguration(fExportFile, root);
	}
	
	private Group exportGroup(File fExportFile, BusinessGroup group, String groupName, boolean backwardsCompatible) {
		Group newGroup = new Group();
		newGroup.key = backwardsCompatible ? null : group.getKey();
		newGroup.name = StringHelper.containsNonWhitespace(groupName) ? groupName : group.getName();
		if (group.getMinParticipants() != null) {
			newGroup.minParticipants = group.getMinParticipants();
		}
		if (group.getMaxParticipants() != null) {
			newGroup.maxParticipants = group.getMaxParticipants();
		}
		if (group.getWaitingListEnabled() != null) {
			newGroup.waitingList = group.getWaitingListEnabled();
		}
		if (group.getAutoCloseRanksEnabled() != null) {
			newGroup.autoCloseRanks = group.getAutoCloseRanksEnabled();
		}
		if(StringHelper.containsNonWhitespace(group.getDescription())) {
			newGroup.description = Collections.singletonList(group.getDescription());
		}
		// collab tools

		CollabTools toolsConfig = new CollabTools();
		CollaborationTools ct = CollaborationToolsFactory.getInstance().getOrCreateCollaborationTools(group);
		for (int i = 0; i < CollaborationTools.TOOLS.length; i++) {
			try {
				Field field = toolsConfig.getClass().getField(CollaborationTools.TOOLS[i]);
				field.setBoolean(toolsConfig, ct.isToolEnabled(CollaborationTools.TOOLS[i]));
			} catch (Exception e) {
				log.error("", e);
			}
		}
		newGroup.tools = toolsConfig;

		Long calendarAccess = ct.lookupCalendarAccess();
		if (calendarAccess != null) {
			newGroup.calendarAccess = calendarAccess;
		}
		//fxdiff VCRP-8: collaboration tools folder access control
		Long folderAccess = ct.lookupFolderAccess();
		if(folderAccess != null) {
			newGroup.folderAccess = folderAccess;
		}
		String info = ct.lookupNews();
		if (info != null && !info.trim().equals("")) {
			newGroup.info = info.trim();
		}

		log.debug("fExportFile.getParent()=" + fExportFile.getParent());
		ct.archive(fExportFile.getParent());
		// export membership
		List<BGArea> bgAreas = areaManager.findBGAreasOfBusinessGroup(group);
		newGroup.areaRelations = new ArrayList<String>();
		for (BGArea areaRelation : bgAreas) {
			newGroup.areaRelations.add(areaRelation.getName());
		}
		// export properties
		Property property = businessGroupPropertyManager.findProperty(group);
		boolean showOwners = businessGroupPropertyManager.showOwners(property);
		boolean showParticipants = businessGroupPropertyManager.showPartips(property);
		boolean showWaitingList = businessGroupPropertyManager.showWaitingList(property);

		newGroup.showOwners = showOwners;
		newGroup.showParticipants = showParticipants;
		newGroup.showWaitingList = showWaitingList;
		return newGroup;
	}
	
	private void saveGroupConfiguration(File fExportFile, OLATGroupExport root) {
		FileOutputStream fOut = null;
		try {
			fOut = new FileOutputStream(fExportFile);
			xstream.toXML(root, fOut);
		} catch (IOException ioe) {
			throw new OLATRuntimeException(
					"Error writing group configuration during group export.",
					ioe);
		} catch (Exception cfe) {
			throw new OLATRuntimeException(
					"Error writing group configuration during group export.",
					cfe);
		} finally {
			FileUtils.closeSafely(fOut);
		}
	}

	public BusinessGroupEnvironment importGroups(OLATResource resource, File fGroupExportXML) {
		if (!fGroupExportXML.exists())
			return new BusinessGroupEnvironment();

		OLATGroupExport groupConfig = null;
		try {
			groupConfig = xstream.fromXML(fGroupExportXML);
		} catch (Exception ce) {
			throw new OLATRuntimeException("Error importing group config.", ce);
		}
		if (groupConfig == null) {
			throw new AssertException(
					"Invalid group export file. Root does not match.");
		}
		
		BusinessGroupEnvironment env = new BusinessGroupEnvironment();
		Set<BGArea> areaSet = new HashSet<BGArea>();
		// get areas
		if (groupConfig.getAreas() != null && groupConfig.getAreas().getGroups() != null) {
			for (Area area : groupConfig.getAreas().getGroups()) {
				String areaName = area.name;
				String areaDesc = (area.description != null && !area.description.isEmpty()) ? area.description.get(0) : "";
				BGArea newArea = areaManager.createAndPersistBGArea(areaName, areaDesc, resource);
				if(areaSet.add(newArea)) {
					env.getAreas().add(new BGAreaReference(newArea, area.key, area.name));
				}
			}
		}

		// get groups
		if (groupConfig.getGroups() != null && groupConfig.getGroups().getGroups() != null) {
			for (Group group : groupConfig.getGroups().getGroups()) {
				// create group
				String groupName = group.name;
				String groupDesc = (group.description != null && !group.description.isEmpty()) ? group.description.get(0) : "";

				// get min/max participants
				int groupMinParticipants = group.minParticipants == null ? -1 : group.minParticipants.intValue();
				int groupMaxParticipants = group.maxParticipants == null ? -1 : group.maxParticipants.intValue();

				// waiting list configuration
				boolean waitingList = false;
				if (group.waitingList != null) {
					waitingList = group.waitingList.booleanValue();
				}
				boolean enableAutoCloseRanks = false;
				if (group.autoCloseRanks != null) {
					enableAutoCloseRanks = group.autoCloseRanks.booleanValue();
				}
				
				BusinessGroup newGroup = businessGroupService.createBusinessGroup(null, groupName, groupDesc, groupMinParticipants, groupMaxParticipants, waitingList, enableAutoCloseRanks, resource);
				//map the group
				env.getGroups().add(new BusinessGroupReference(newGroup, group.key, group.name));
				// get tools config
				CollabTools toolsConfig = group.tools;
				CollaborationTools ct = CollaborationToolsFactory.getInstance().getOrCreateCollaborationTools(newGroup);
				for (int i = 0; i < CollaborationTools.TOOLS.length; i++) {
					try {
						Field field = toolsConfig.getClass().getField(CollaborationTools.TOOLS[i]);
						Boolean val = field.getBoolean(toolsConfig);
						if (val != null) {
							ct.setToolEnabled(CollaborationTools.TOOLS[i], val);
						}
					} catch (Exception e) {
						log.error("", e);
					}
				}
				if (group.calendarAccess != null) {
					Long calendarAccess = group.calendarAccess;
					ct.saveCalendarAccess(calendarAccess);
				}
				//fxdiff VCRP-8: collaboration tools folder access control
				if(group.folderAccess != null) {
				  ct.saveFolderAccess(group.folderAccess);				  
				}
				if (group.info != null) {
					ct.saveNews(group.info);
				}

				// get memberships
				List<String> memberships = group.areaRelations;
				if(memberships != null) {
					for (String membership : memberships) {
						BGArea area = areaManager.findBGArea(membership, resource);
						if (area == null) {
							throw new AssertException("Group-Area-Relationship in export, but area was not created during import.");
						}
						areaManager.addBGToBGArea(newGroup, area);
					}
				}

				// get properties
				boolean showOwners = true;
				boolean showParticipants = true;
				boolean showWaitingList = true;
				if (group.showOwners != null) {
					showOwners = group.showOwners;
				}
				if (group.showParticipants != null) {
					showParticipants = group.showParticipants;
				}
				if (group.showWaitingList != null) {
					showWaitingList = group.showWaitingList;
				}
				businessGroupPropertyManager.updateDisplayMembers(newGroup, showOwners, showParticipants, showWaitingList, false, false, false);
			}
		}
		return env;
	}

}
