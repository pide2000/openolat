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
package org.olat.admin.user.course;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.olat.NewControllerFactory;
import org.olat.basesecurity.BaseSecurityModule;
import org.olat.basesecurity.GroupRoles;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.table.BooleanColumnDescriptor;
import org.olat.core.gui.components.table.ColumnDescriptor;
import org.olat.core.gui.components.table.CustomCellRenderer;
import org.olat.core.gui.components.table.CustomRenderColumnDescriptor;
import org.olat.core.gui.components.table.DefaultColumnDescriptor;
import org.olat.core.gui.components.table.DefaultTableDataModel;
import org.olat.core.gui.components.table.StaticColumnDescriptor;
import org.olat.core.gui.components.table.Table;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableEvent;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.components.table.TableMultiSelectEvent;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.id.Identity;
import org.olat.core.util.Util;
import org.olat.core.util.mail.MailHelper;
import org.olat.core.util.mail.MailPackage;
import org.olat.core.util.mail.MailerResult;
import org.olat.course.CourseModule;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupManagedFlag;
import org.olat.group.BusinessGroupMembership;
import org.olat.group.BusinessGroupService;
import org.olat.group.BusinessGroupShort;
import org.olat.group.model.BGRepositoryEntryRelation;
import org.olat.group.ui.main.BusinessGroupTableModelWithType;
import org.olat.group.ui.main.CourseMembership;
import org.olat.group.ui.main.EditSingleMembershipController;
import org.olat.group.ui.main.MemberPermissionChangeEvent;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryManagedFlag;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryModule;
import org.olat.repository.RepositoryService;
import org.olat.repository.controllers.ReferencableEntriesSearchController;
import org.olat.repository.controllers.RepositoryEntryFilter;
import org.olat.repository.controllers.RepositorySearchController.Can;
import org.olat.repository.model.RepositoryEntryMembership;
import org.olat.repository.model.RepositoryEntryPermissionChangeEvent;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 23.09.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class CourseOverviewController extends BasicController  {
	private static final String CONFIG = CourseOverviewController.class.getName();
	private static final String TABLE_ACTION_LAUNCH = "reTblLaunch";
	private static final String TABLE_ACTION_EDIT = "edit";
	private static final String TABLE_ACTION_UNSUBSCRIBE = "unsubscribe";
	
	private final VelocityContainer vc;
	private final Link addAsOwner, addAsTutor, addAsParticipant;
	private TableController courseListCtr;
	private MembershipDataModel tableDataModel;
	
	private CloseableModalController cmc;
	private DialogBoxController confirmSendMailBox;
	private CourseLeaveDialogBoxController removeFromCourseDlg;
	private ReferencableEntriesSearchController repoSearchCtr;
	private EditSingleMembershipController editSingleMemberCtrl;
	
	private final Identity editedIdentity;
	
	@Autowired
	private BaseSecurityModule securityModule;
	@Autowired
	private RepositoryModule repositoryModule;
	@Autowired
	private RepositoryManager repositoryManager;
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private BusinessGroupService businessGroupService;
	
	public CourseOverviewController(UserRequest ureq, WindowControl wControl, Identity identity) {
		super(ureq, wControl, Util.createPackageTranslator(BusinessGroupTableModelWithType.class, ureq.getLocale()));
		this.setTranslator(Util.createPackageTranslator(RepositoryService.class, getLocale(), getTranslator()));

		editedIdentity = identity;

		boolean isLastVisitVisible = securityModule.isUserLastVisitVisible(ureq.getUserSession().getRoles());
		
		vc = createVelocityContainer("courseoverview");
		
		TableGuiConfiguration config = new TableGuiConfiguration();
		config.setPreferencesOffered(true, CONFIG);

		courseListCtr = new TableController(config, ureq, wControl, getTranslator());
		listenTo(courseListCtr);
		courseListCtr.addColumnDescriptor(false, new DefaultColumnDescriptor(MSCols.key.i18n(), MSCols.key.ordinal(), null, getLocale()));
		courseListCtr.addColumnDescriptor(new DefaultColumnDescriptor(MSCols.title.i18n(), MSCols.title.ordinal(), null, getLocale()));
		if(repositoryModule.isManagedRepositoryEntries()) {
			courseListCtr.addColumnDescriptor(false, new DefaultColumnDescriptor(MSCols.externalId.i18n(), MSCols.externalId.ordinal(), null, getLocale()));
		}
		courseListCtr.addColumnDescriptor(false, new DefaultColumnDescriptor(MSCols.externalRef.i18n(), MSCols.externalRef.ordinal(), null, getLocale()));
		CustomCellRenderer roleRenderer = new CourseRoleCellRenderer();
		courseListCtr.addColumnDescriptor(new CustomRenderColumnDescriptor(MSCols.role.i18n(), MSCols.role.ordinal(), null, getLocale(), ColumnDescriptor.ALIGNMENT_LEFT, roleRenderer));
		courseListCtr.addColumnDescriptor(new DefaultColumnDescriptor(MSCols.firstTime.i18n(), MSCols.firstTime.ordinal(), null, getLocale()));
		if(isLastVisitVisible) {
			courseListCtr.addColumnDescriptor(new DefaultColumnDescriptor(MSCols.lastTime.i18n(), MSCols.lastTime.ordinal(), null, getLocale()));
		}
		courseListCtr.addColumnDescriptor(new StaticColumnDescriptor(TABLE_ACTION_EDIT, "table.header.edit", translate("table.header.edit")));
		courseListCtr.addColumnDescriptor(new BooleanColumnDescriptor(MSCols.allowLeave.i18n(), MSCols.allowLeave.ordinal(), TABLE_ACTION_UNSUBSCRIBE, translate("table.header.leave"), null));

		courseListCtr.setMultiSelect(true);
		courseListCtr.addMultiSelectAction("table.leave", TABLE_ACTION_UNSUBSCRIBE);
		tableDataModel = new MembershipDataModel();
		courseListCtr.setTableDataModel(tableDataModel);
		
		updateModel();

		addAsOwner = LinkFactory.createButton("add.course.owner", vc, this);
		addAsTutor = LinkFactory.createButton("add.course.tutor", vc, this);		
		addAsParticipant = LinkFactory.createButton("add.course.participant", vc, this);		
		vc.put("table.courses", courseListCtr.getInitialComponent());	
		
		putInitialPanel(vc);
	}
	
	private void updateModel() {
		//course membership
		List<RepositoryEntryMembership> memberships = repositoryManager.getRepositoryEntryMembership(null, editedIdentity);

		//group membership
		List<BusinessGroupMembership> groupMemberships =  businessGroupService.getBusinessGroupMembership(Collections.<Long>emptyList(), editedIdentity);
		Collection<Long> groupKeys = new ArrayList<Long>(groupMemberships.size());
		for(BusinessGroupMembership membership: groupMemberships) {
			groupKeys.add(membership.getGroupKey());
		}

		//relation to course
		List<BGRepositoryEntryRelation> relations = businessGroupService.findRelationToRepositoryEntries(groupKeys, 0, -1);
		Map<Long,Long> groupKeyToRepoKeyMap = new HashMap<Long,Long>();
		for(BGRepositoryEntryRelation relation:relations) {
			groupKeyToRepoKeyMap.put(relation.getGroupKey(), relation.getRepositoryEntryKey());
		}

		Map<Long,CourseMemberView> repoKeyToViewMap = new HashMap<Long,CourseMemberView>();
		for(RepositoryEntryMembership membership: memberships) {
			Long repoKey = membership.getRepoKey();

			CourseMemberView memberView;
			if(repoKeyToViewMap.containsKey(repoKey)) {
				memberView = repoKeyToViewMap.get(repoKey);
			} else {
				memberView = new CourseMemberView(repoKey);
				repoKeyToViewMap.put(repoKey, memberView);
			}
			
			memberView.setFirstTime(membership.getCreationDate());
			memberView.setLastTime(membership.getLastModified());
			memberView.getMembership().setRepoOwner(membership.isOwner());
			memberView.getMembership().setRepoTutor(membership.isCoach());
			memberView.getMembership().setRepoParticipant(membership.isParticipant());
		}

		List<BusinessGroupShort> groups = businessGroupService.loadShortBusinessGroups(groupKeys);
		Map<Long,BusinessGroupShort> groupKeyToGroupMap = new HashMap<Long,BusinessGroupShort>();
		for(BusinessGroupShort group:groups) {
			groupKeyToGroupMap.put(group.getKey(), group);
		}
		
		for(BusinessGroupMembership membership: groupMemberships) {
			Long repoKey;
			if(groupKeyToRepoKeyMap.containsKey(membership.getGroupKey())) {
				repoKey = groupKeyToRepoKeyMap.get(membership.getGroupKey());
			} else {
				continue;
			}
			
			BusinessGroupShort group = groupKeyToGroupMap.get(membership.getGroupKey());
			if(group == null) {
				continue;
			}

			CourseMemberView memberView;
			if(repoKeyToViewMap.containsKey(repoKey)) {
				memberView = repoKeyToViewMap.get(repoKey);
			} else {
				memberView = new CourseMemberView(repoKey);
				repoKeyToViewMap.put(repoKey, memberView);
			}
			memberView.addGroup(group);
			memberView.setFirstTime(membership.getCreationDate());
			memberView.setLastTime(membership.getLastModified());
			switch(membership.getMembership()) {
				case owner: memberView.getMembership().setGroupTutor(true); break;
				case participant: memberView.getMembership().setGroupParticipant(true); break;
				case waiting: memberView.getMembership().setGroupWaiting(true); break;
			}
		}
		
		List<RepositoryEntry> entries = repositoryManager.lookupRepositoryEntries(repoKeyToViewMap.keySet());
		Map<Long,RepositoryEntry> entryKeyToRepoEntryMap = new HashMap<Long,RepositoryEntry>();
		for(RepositoryEntry entry:entries) {
			entryKeyToRepoEntryMap.put(entry.getKey(), entry);
		}
		
		for(CourseMemberView memberView:repoKeyToViewMap.values()) {
			RepositoryEntry entry = entryKeyToRepoEntryMap.get(memberView.getRepoKey());
			if(entry != null) {
				memberView.setDisplayName(entry.getDisplayname());
				memberView.setExternalId(entry.getExternalId());
				memberView.setExternalRef(entry.getExternalRef());
				
				boolean managedMembersRepo = RepositoryEntryManagedFlag.isManaged(entry,
						RepositoryEntryManagedFlag.membersmanagement);
				memberView.getMembership().setManagedMembersRepo(managedMembersRepo);
			}	
		}
		
		List<CourseMemberView> views = new ArrayList<CourseMemberView>(repoKeyToViewMap.values());
		tableDataModel.setObjects(views);
		courseListCtr.modelChanged();
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		if(source == addAsOwner) {
			doSearchRepoEntries(ureq, SearchType.owner, translate("add.course.owner"));
		} else if(source == addAsTutor) {
			doSearchRepoEntries(ureq, SearchType.tutor, translate("add.course.tutor"));
		} else if(source == addAsParticipant) {
			doSearchRepoEntries(ureq, SearchType.participant, translate("add.course.participant"));
		}
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if (source == courseListCtr){
			if (event.getCommand().equals(Table.COMMANDLINK_ROWACTION_CLICKED)) {
				TableEvent te = (TableEvent) event;
				CourseMemberView item = tableDataModel.getObject(te.getRowId());
				if (TABLE_ACTION_LAUNCH.equals(te.getActionId())) {
					NewControllerFactory.getInstance().launch("[RepositoryEntry:" + item.getRepoKey() + "]", ureq, getWindowControl());
				} else if (TABLE_ACTION_UNSUBSCRIBE.equals(te.getActionId())){
					doLeave(ureq, Collections.singletonList(item));
				} else if (TABLE_ACTION_EDIT.equals(te.getActionId())){
					doOpenEdit(ureq, item);
				}
			} else if (event instanceof TableMultiSelectEvent) {
				TableMultiSelectEvent mse = (TableMultiSelectEvent)event;
				List<CourseMemberView> items = tableDataModel.getObjects(mse.getSelection());
				if (TABLE_ACTION_UNSUBSCRIBE.equals(mse.getAction())){
					doLeave(ureq, items);
				}
			}
		} else if(source == repoSearchCtr) {
			SearchType type = (SearchType)repoSearchCtr.getUserObject();
			if (event == ReferencableEntriesSearchController.EVENT_REPOSITORY_ENTRY_SELECTED) {
				// repository search controller done
				RepositoryEntry re = repoSearchCtr.getSelectedEntry();
				removeAsListenerAndDispose(repoSearchCtr);
				cmc.deactivate();
				doConfirmSendEmail(ureq, Collections.singletonList(re), type);
			} else if(event == ReferencableEntriesSearchController.EVENT_REPOSITORY_ENTRIES_SELECTED) {
				// repository search controller done
				List<RepositoryEntry> res = repoSearchCtr.getSelectedEntries();
				removeAsListenerAndDispose(repoSearchCtr);
				cmc.deactivate();
				if (res != null && !res.isEmpty()) {
					doConfirmSendEmail(ureq, res, type);
				}
			}
		} else if(source == editSingleMemberCtrl) {
			cmc.deactivate();
			if(event instanceof MemberPermissionChangeEvent) {
				MemberPermissionChangeEvent e = (MemberPermissionChangeEvent)event;
				RepositoryEntry re = editSingleMemberCtrl.getRepositoryEntry();
				doConfirmChangePermission(ureq, e, re);
			}
		} else if(source == confirmSendMailBox) {
			boolean sendMail = DialogBoxUIFactory.isYesEvent(event) || DialogBoxUIFactory.isOkEvent(event);
			Object confirmation = confirmSendMailBox.getUserObject();
			if(confirmation instanceof ConfirmAdd) {
				ConfirmAdd addInfos = (ConfirmAdd)confirmation;
				doAddToRepositoryEntry(ureq, addInfos.getEntries(), addInfos.getType(), sendMail);
			} else if(confirmation instanceof ConfirmEdit) {
				ConfirmEdit editInfos = (ConfirmEdit)confirmation;
				doChangePermission(ureq, editInfos.getChangeEvent(), editInfos.getEntry(), sendMail);
			}
			updateModel();
		} else if(source == removeFromCourseDlg) {
			if(event == Event.DONE_EVENT) {
				boolean sendMail = removeFromCourseDlg.isSendMail();
				List<BusinessGroup> groupsToDelete = removeFromCourseDlg.getGroupsToDelete();
				List<BusinessGroup> groupsToLeave = removeFromCourseDlg.getGroupsToLeave();
				List<RepositoryEntry> repoEntriesToLeave = removeFromCourseDlg.getRepoEntriesToLeave();
				removeUserFromCourse(ureq, repoEntriesToLeave, groupsToLeave, groupsToDelete, sendMail);
			}
			
			cmc.deactivate();
			cleanUp();
		} else if(source == cmc) {
			cleanUp();
		}
	}
	
	private void cleanUp() {
		removeAsListenerAndDispose(removeFromCourseDlg);
		removeAsListenerAndDispose(repoSearchCtr);
		removeAsListenerAndDispose(cmc);
		removeFromCourseDlg = null;
		repoSearchCtr = null;
		cmc = null;
	}
	
	private void doOpenEdit(UserRequest ureq, CourseMemberView member) {
		RepositoryEntry repoEntry = repositoryManager.lookupRepositoryEntry(member.getRepoKey());
		editSingleMemberCtrl = new EditSingleMembershipController(ureq, getWindowControl(), editedIdentity, repoEntry, null);
		listenTo(editSingleMemberCtrl);
		cmc = new CloseableModalController(getWindowControl(), translate("close"), editSingleMemberCtrl.getInitialComponent(),
				true, translate("edit.member"));
		cmc.activate();
		listenTo(cmc);
	}
	
	private void doConfirmChangePermission(UserRequest ureq, MemberPermissionChangeEvent e, RepositoryEntry re) {
		boolean groupChangesEmpty = e.getGroupChanges() == null || e.getGroupChanges().isEmpty();
		boolean repoChangesEmpty = e.getRepoOwner() == null && e.getRepoParticipant() == null && e.getRepoTutor() == null;
		if(groupChangesEmpty && repoChangesEmpty) {
			//nothing to do
			return;
		}

		boolean mailMandatory = repositoryModule.isMandatoryEnrolmentEmail(ureq.getUserSession().getRoles());
		if(mailMandatory) {
			doChangePermission(ureq, e, re, true);
		} else {
			confirmSendMailBox = activateYesNoDialog(ureq, null, translate("dialog.modal.bg.send.mail"), confirmSendMailBox);
			confirmSendMailBox.setUserObject(new ConfirmEdit(e, re));
		}
	}
	
	private void doChangePermission(UserRequest ureq, MemberPermissionChangeEvent e, RepositoryEntry re, boolean sendMail) {
		MailPackage mailing = new MailPackage(sendMail);
		if(re != null) {
			List<RepositoryEntryPermissionChangeEvent> changes = Collections.singletonList((RepositoryEntryPermissionChangeEvent)e);
			repositoryManager.updateRepositoryEntryMembership(getIdentity(), ureq.getUserSession().getRoles(), re, changes, mailing);
		}

		businessGroupService.updateMemberships(getIdentity(), e.getGroupChanges(), mailing);
		//make sure all is committed before loading the model again (I see issues without)
		DBFactory.getInstance().commitAndCloseSession();
		updateModel();
	}
	
	private void doSearchRepoEntries(UserRequest ureq, SearchType type, String title) {
		removeAsListenerAndDispose(repoSearchCtr);
		removeAsListenerAndDispose(cmc);
		
		RepositoryEntryFilter filter = new ManagedEntryfilter();
		repoSearchCtr = new ReferencableEntriesSearchController(getWindowControl(), ureq,
				new String[]{ CourseModule.getCourseTypeName() }, filter,
				translate("choose"), false, false, true, true, true, Can.all);
		repoSearchCtr.setUserObject(type);
		listenTo(repoSearchCtr);
		cmc = new CloseableModalController(getWindowControl(), translate("close"), repoSearchCtr.getInitialComponent(),
				true, title);
		listenTo(cmc);
		cmc.activate();
	}
	
	private void doConfirmSendEmail(UserRequest ureq, Collection<RepositoryEntry> res, SearchType type) {
		boolean mailMandatory = repositoryModule.isMandatoryEnrolmentEmail(ureq.getUserSession().getRoles());
		if(mailMandatory) {
			doAddToRepositoryEntry(ureq, res, type, false);
			updateModel();
		} else {
			confirmSendMailBox = activateYesNoDialog(ureq, null, translate("dialog.modal.bg.send.mail"), confirmSendMailBox);
			confirmSendMailBox.setUserObject(new ConfirmAdd(type, res));
		}
	}
	
	private void doAddToRepositoryEntry(UserRequest ureq, Collection<RepositoryEntry> res, SearchType type, boolean sendEmail) {
		//commit changes to the repository entry
		MailerResult result = new MailerResult();
		MailPackage reMailing = new MailPackage(result, getWindowControl().getBusinessControl().getAsString(), sendEmail);
		
		RepositoryEntryPermissionChangeEvent changeEvent = new RepositoryEntryPermissionChangeEvent(editedIdentity);
		switch(type) {
			case owner: changeEvent.setRepoOwner(Boolean.TRUE); break;
			case tutor: changeEvent.setRepoTutor(Boolean.TRUE); break;
			case participant: changeEvent.setRepoParticipant(Boolean.TRUE); break;
		}
		List<RepositoryEntryPermissionChangeEvent> repoChanges = Collections.singletonList(changeEvent);
		for(RepositoryEntry repoEntry:res) {
			repositoryManager.updateRepositoryEntryMembership(getIdentity(), ureq.getUserSession().getRoles(), repoEntry, repoChanges, reMailing);
		}
		
		//make sure all is committed before loading the model again (I see issues without)
		DBFactory.getInstance().commitAndCloseSession();
		MailHelper.printErrorsAndWarnings(result, getWindowControl(), getLocale());
	}

	/**
	 * The method check the managed flags
	 * @param ureq
	 * @param views
	 */
	private void doLeave(UserRequest ureq, Collection<CourseMemberView> views) {
		List<Long> groupKeys = new ArrayList<Long>();
		List<RepositoryEntry> repoEntryToLeave = new ArrayList<RepositoryEntry>();
		for(CourseMemberView view:views) {
			for(BusinessGroupShort group:view.getGroups()) {
				if(!BusinessGroupManagedFlag.isManaged(group.getManagedFlags(), BusinessGroupManagedFlag.membersmanagement)) {
					groupKeys.add(group.getKey());
				}
			}

			RepositoryEntry re = repositoryManager.lookupRepositoryEntry(view.getRepoKey());	
			if(!RepositoryEntryManagedFlag.isManaged(re, RepositoryEntryManagedFlag.membersmanagement)) {
				repoEntryToLeave.add(re);
				if(view.getMembership().isRepoOwner()) {
					int numOfOwners = repositoryService.countMembers(re, GroupRoles.owner.name());
					if(numOfOwners == 1) {
						showError("error.atleastone", view.getDisplayName());
						return;//break the process
					}
				}
			}
		}
		
		List<BusinessGroup> groupsToLeave = businessGroupService.loadBusinessGroups(groupKeys);

		List<BusinessGroup> groupsToDelete = new ArrayList<BusinessGroup>(1);
		for(BusinessGroup group:groupsToLeave) {
			int numOfOwners = businessGroupService.countMembers(group, GroupRoles.coach.name());
			int numOfParticipants = businessGroupService.countMembers(group, GroupRoles.participant.name());
			if ((numOfOwners == 1 && numOfParticipants == 0) || (numOfOwners == 0 && numOfParticipants == 1)) {
				groupsToDelete.add(group);
			}
		}
		removeFromCourseDlg = new CourseLeaveDialogBoxController(ureq, getWindowControl(), editedIdentity,
				repoEntryToLeave, groupsToLeave, groupsToDelete);
		listenTo(removeFromCourseDlg);
		
		cmc = new CloseableModalController(getWindowControl(), translate("close"), removeFromCourseDlg.getInitialComponent(),
				true, translate("unsubscribe.title"));
		cmc.activate();
		listenTo(cmc);
	}
	
	private void removeUserFromCourse(UserRequest ureq, List<RepositoryEntry> repoEntriesToLeave, List<BusinessGroup> groupsToLeave, List<BusinessGroup> groupsToDelete, boolean doSendMail) {
		List<Identity> membersToRemove = Collections.singletonList(editedIdentity);
		
		for(BusinessGroup group:groupsToLeave) {
			if (groupsToDelete.contains(group)) {
				// really delete the group as it has no more owners/participants
				if(doSendMail) {
					String businessPath = getWindowControl().getBusinessControl().getAsString();
					businessGroupService.deleteBusinessGroupWithMail(group, businessPath, getIdentity(), getLocale());
				} else {
					businessGroupService.deleteBusinessGroup(group);
				}
			} else {
				// 1) remove as owner
				if (businessGroupService.hasRoles(editedIdentity, group, GroupRoles.coach.name())) {
					businessGroupService.removeOwners(ureq.getIdentity(), membersToRemove, group);
				}
				MailPackage mailing = new MailPackage(doSendMail);
				// 2) remove as participant
				businessGroupService.removeParticipants(getIdentity(), membersToRemove, group, mailing);
				MailHelper.printErrorsAndWarnings(mailing.getResult(), getWindowControl(), getLocale());
			}
		}
		
		for(RepositoryEntry repoEntry:repoEntriesToLeave) {
			if(isRepoMember(repoEntry)) {
				MailPackage mailing = new MailPackage(doSendMail);
				repositoryManager.removeMembers(getIdentity(), membersToRemove, repoEntry, mailing);
			}
		}

		updateModel();

		StringBuilder groupNames = new StringBuilder();
		for(BusinessGroup group:groupsToLeave) {
			if(groupNames.length() > 0) groupNames.append(", ");
			groupNames.append(group.getName());
		}
		showInfo("unsubscribe.successful");	
	}
	
	private boolean isRepoMember(RepositoryEntry repoEntry) {
		for(CourseMemberView view:tableDataModel.getObjects()) {
			if(repoEntry.getKey().equals(view.getRepoKey())) {
				CourseMembership membership = view.getMembership();
				return membership.isRepoOwner() || membership.isRepoTutor() || membership.isRepoParticipant();
			}
		}
		return false;
	}
	
	private enum SearchType {
		owner, tutor, participant
	}
	
	public enum MSCols {
		key("table.header.key"),
		title("table.header.displayName"),
		externalId("table.header.externalid"),
		externalRef("table.header.externalref"),
		role("table.header.role"),
		lastTime("table.header.lastTime"),
		firstTime("table.header.firstTime"),
		allowLeave("table.header.leave");
		
		private final String i18n;
		
		private MSCols(String i18n) {
			this.i18n = i18n;
		}
		
		public String i18n() {
			return i18n;
		}
	}
	
	private static class ConfirmAdd {
		private final SearchType type;
		private final Collection<RepositoryEntry> entries;
		
		public ConfirmAdd(SearchType type, Collection<RepositoryEntry> entries) {
			this.type = type;
			this.entries = entries;
		}

		public SearchType getType() {
			return type;
		}

		public Collection<RepositoryEntry> getEntries() {
			return entries;
		}
	}
	
	private class ConfirmEdit {
		private final RepositoryEntry entry;
		private final MemberPermissionChangeEvent e;
		
		public ConfirmEdit(MemberPermissionChangeEvent e, RepositoryEntry entry) {
			this.e = e;
			this.entry = entry;
		}

		public RepositoryEntry getEntry() {
			return entry;
		}

		public MemberPermissionChangeEvent getChangeEvent() {
			return e;
		}
	}

	private static class MembershipDataModel extends DefaultTableDataModel<CourseMemberView> {
		public MembershipDataModel() {
			super(Collections.<CourseMemberView>emptyList());
		}

		@Override
		public int getColumnCount() {
			return 4;
		}

		@Override
		public Object getValueAt(int row, int col) {
			CourseMemberView  view = getObject(row);
			switch(MSCols.values()[col]) {
				case key: return view.getRepoKey();
				case title: return view.getDisplayName();
				case externalId: return view.getExternalId();
				case externalRef: return view.getExternalRef();
				case role: return view;
				case firstTime: return view.getFirstTime();
				case lastTime: return view.getLastTime();
				case allowLeave: return view.isFullyManaged() ? Boolean.FALSE : Boolean.TRUE;
				default: return "ERROR";
			}
		}
	}
	
	private class CourseMemberView {
		private final Long repoKey;
		private String displayName;
		private String externalId;
		private String externalRef;
		private Date firstTime;
		private Date lastTime;
		private final CourseMembership membership = new CourseMembership();
		private List<BusinessGroupShort> groups;
		
		public CourseMemberView(Long repoKey) {
			this.repoKey = repoKey;
		}
		
		public Long getRepoKey() {
			return repoKey;
		}
		
		public String getDisplayName() {
			return displayName;
		}

		public void setDisplayName(String displayName) {
			this.displayName = displayName;
		}
		
		public String getExternalId() {
			return externalId;
		}

		public void setExternalId(String externalId) {
			this.externalId = externalId;
		}

		public String getExternalRef() {
			return externalRef;
		}

		public void setExternalRef(String externalRef) {
			this.externalRef = externalRef;
		}

		public Date getFirstTime() {
			return firstTime;
		}

		public void setFirstTime(Date firstTime) {
			if(firstTime == null) return;
			if(this.firstTime == null || this.firstTime.compareTo(firstTime) > 0) {
				this.firstTime = firstTime;
			}
		}

		public Date getLastTime() {
			return lastTime;
		}

		public void setLastTime(Date lastTime) {
			if(lastTime == null) return;
			if(this.lastTime == null || this.lastTime.compareTo(lastTime) < 0) {
				this.lastTime = lastTime;
			}
		}
		
		public void addGroup(BusinessGroupShort group) {
			if(groups == null) groups = new ArrayList<BusinessGroupShort>();
			groups.add(group);
		}

		public CourseMembership getMembership() {
			return membership;
		}
		
		public List<BusinessGroupShort> getGroups() {
			return groups == null ? Collections.<BusinessGroupShort>emptyList() : new ArrayList<BusinessGroupShort>(groups);
		}
		
		public boolean isFullyManaged() {
			if(membership != null && !membership.isManagedMembersRepo() &&
					(membership.isRepoOwner() || membership.isRepoTutor() || membership.isRepoParticipant())) {
				return false;
			}

			if(groups != null) {
				for(BusinessGroupShort group:groups) {
					if(!BusinessGroupManagedFlag.isManaged(group.getManagedFlags(), BusinessGroupManagedFlag.membersmanagement)) {
						return false;
					}
				}
			}
			return true;
		}
	}
	
	private class CourseRoleCellRenderer implements CustomCellRenderer {
		@Override
		public void render(StringOutput sb, Renderer renderer, Object val, Locale locale, int alignment, String action) {
			if (val instanceof CourseMemberView) {
				CourseMemberView membership = (CourseMemberView)val;
				
				boolean and = false;
				if(membership.getMembership().isRepoOwner()) {
					and = and(sb, and);
					sb.append(translate("role.repo.owner"));
				}
				if(membership.getMembership().isRepoTutor()) {
					and = and(sb, and);
					sb.append(translate("role.repo.tutor"));
				}
				if(membership.getMembership().isRepoParticipant()) {
					and = and(sb, and);
					sb.append(translate("role.repo.participant"));
				}
				if(membership.getMembership().isGroupTutor()) {
					and = and(sb, and);
					sb.append(translate("role.group.tutor"));
				}
				if(membership.getMembership().isGroupParticipant()) {
					and = and(sb, and);
					sb.append(translate("role.group.participant"));
				}
				if(membership.getMembership().isGroupWaiting()) {
					and = and(sb, and);
					sb.append(translate("role.group.waiting"));
				}
			}
		}
		
		private final boolean and(StringOutput sb, boolean and) {
			if(and) sb.append(", ");
			return true;
		}
	}
	
	private static class ManagedEntryfilter implements RepositoryEntryFilter {
		@Override
		public boolean accept(RepositoryEntry re) {
			return !RepositoryEntryManagedFlag.isManaged(re, RepositoryEntryManagedFlag.membersmanagement);
		}
	}
}