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
package org.olat.course.assessment.manager;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.olat.basesecurity.GroupRoles;
import org.olat.core.commons.persistence.DB;
import org.olat.core.id.Identity;
import org.olat.course.assessment.AssessmentMode;
import org.olat.course.assessment.AssessmentMode.Target;
import org.olat.course.assessment.AssessmentModeManager;
import org.olat.course.assessment.AssessmentModeToArea;
import org.olat.course.assessment.AssessmentModeToGroup;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupService;
import org.olat.group.area.BGArea;
import org.olat.group.area.BGAreaManager;
import org.olat.group.manager.BusinessGroupRelationDAO;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.manager.RepositoryEntryRelationDAO;
import org.olat.test.JunitTestHelper;
import org.olat.test.OlatTestCase;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 12.12.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class AssessmentModeManagerTest extends OlatTestCase {
	
	@Autowired
	private DB dbInstance;
	@Autowired
	private BGAreaManager areaMgr;
	@Autowired
	private AssessmentModeManager assessmentModeMgr;
	@Autowired
	private BusinessGroupService businessGroupService;
	@Autowired
	private BusinessGroupRelationDAO businessGroupRelationDao;
	@Autowired
	private RepositoryEntryRelationDAO repositoryEntryRelationDao;
	
	@Test
	public void createAssessmentMode() {
		RepositoryEntry entry = JunitTestHelper.createAndPersistRepositoryEntry();
		AssessmentMode mode = assessmentModeMgr.createAssessmentMode(entry);
		
		mode.setName("Assessment in sight");
		mode.setDescription("Assessment description");
		
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.DATE, 2);
		Date begin = cal.getTime();
		cal.add(Calendar.HOUR_OF_DAY, 2);
		Date end = cal.getTime();
		mode.setBegin(begin);
		mode.setEnd(end);
		mode.setLeadTime(15);
		
		mode.setTargetAudience(Target.course);
		
		mode.setRestrictAccessElements(true);
		mode.setElementList("173819739,239472389");
		
		mode.setRestrictAccessIps(true);
		mode.setIpList("192.168.1.123");
		
		mode.setSafeExamBrowser(true);
		mode.setSafeExamBrowserKey("785rhqg47368ahfahl");
		mode.setSafeExamBrowserHint("Use the SafeExamBrowser");
		
		mode.setApplySettingsForCoach(true);
		
		AssessmentMode savedMode = assessmentModeMgr.persist(mode);
		dbInstance.commitAndCloseSession();
		Assert.assertNotNull(savedMode);
		Assert.assertNotNull(savedMode.getKey());
		Assert.assertNotNull(savedMode.getCreationDate());
		Assert.assertNotNull(savedMode.getLastModified());

		//reload and check
		AssessmentMode reloadedMode = assessmentModeMgr.getAssessmentModeById(savedMode.getKey());
		Assert.assertNotNull(reloadedMode);
		Assert.assertEquals(savedMode.getKey(), reloadedMode.getKey());
		Assert.assertNotNull(reloadedMode.getCreationDate());
		Assert.assertNotNull(reloadedMode.getLastModified());
		Assert.assertEquals(savedMode, reloadedMode);
		
		Assert.assertEquals("Assessment in sight", reloadedMode.getName());
		Assert.assertEquals("Assessment description", reloadedMode.getDescription());
		
		Assert.assertEquals(begin, reloadedMode.getBegin());
		Assert.assertEquals(end, reloadedMode.getEnd());
		Assert.assertEquals(15, reloadedMode.getLeadTime());
		
		Assert.assertEquals(Target.course, reloadedMode.getTargetAudience());
		
		Assert.assertTrue(reloadedMode.isRestrictAccessElements());
		Assert.assertEquals("173819739,239472389", reloadedMode.getElementList());
		
		Assert.assertTrue(reloadedMode.isRestrictAccessIps());
		Assert.assertEquals("192.168.1.123", reloadedMode.getIpList());
		
		Assert.assertTrue(reloadedMode.isApplySettingsForCoach());
		
		Assert.assertTrue(reloadedMode.isSafeExamBrowser());
		Assert.assertEquals("785rhqg47368ahfahl", reloadedMode.getSafeExamBrowserKey());
		Assert.assertEquals("Use the SafeExamBrowser", reloadedMode.getSafeExamBrowserHint());
	}
	
	@Test
	public void loadAssessmentModes() {
		RepositoryEntry entry = JunitTestHelper.createAndPersistRepositoryEntry();
		AssessmentMode mode = assessmentModeMgr.createAssessmentMode(entry);
		mode.setName("Assessment to load");
		mode.setBegin(new Date());
		mode.setEnd(new Date());
		mode.setTargetAudience(Target.course);
		AssessmentMode savedMode = assessmentModeMgr.persist(mode);
		dbInstance.commitAndCloseSession();
		Assert.assertNotNull(savedMode);
		
		List<AssessmentMode> assessmentModes = assessmentModeMgr.getAssessmentModeFor(entry);
		Assert.assertNotNull(assessmentModes);
		Assert.assertEquals(1, assessmentModes.size());
		Assert.assertEquals(savedMode, assessmentModes.get(0));
	}
	
	@Test
	public void createAssessmentModeToGroup() {
		Identity author = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-1");
		RepositoryEntry entry = JunitTestHelper.createAndPersistRepositoryEntry();
		BusinessGroup businessGroup = businessGroupService.createBusinessGroup(author, "as_mode_1", "", null, null, null, null, false, false, null);
		
		AssessmentMode mode = createMinimalAssessmentmode(entry);
		mode = assessmentModeMgr.persist(mode);
		dbInstance.commitAndCloseSession();
		Assert.assertNotNull(mode);

		AssessmentModeToGroup modeToGroup = assessmentModeMgr.createAssessmentModeToGroup(mode, businessGroup);
		mode.getGroups().add(modeToGroup);
		AssessmentMode savedMode = assessmentModeMgr.merge(mode, true);
		dbInstance.commitAndCloseSession();
		
		AssessmentMode reloadedMode = assessmentModeMgr.getAssessmentModeById(mode.getKey());
		Assert.assertEquals(mode, reloadedMode);
		Assert.assertEquals(savedMode, reloadedMode);
		Assert.assertNotNull(reloadedMode.getGroups());
		Assert.assertEquals(1, reloadedMode.getGroups().size());
		Assert.assertEquals(modeToGroup, reloadedMode.getGroups().iterator().next());
		dbInstance.commitAndCloseSession();
	}
	
	@Test
	public void createAssessmentModeToArea() {
		Identity author = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-1");
		RepositoryEntry entry = JunitTestHelper.createAndPersistRepositoryEntry();
		BusinessGroup businessGroup = businessGroupService.createBusinessGroup(author, "as_mode_1", "", null, null, null, null, false, false, null);
		BGArea area = areaMgr.createAndPersistBGArea("little area", "My little secret area", entry.getOlatResource());
		areaMgr.addBGToBGArea(businessGroup, area);
		AssessmentMode mode = createMinimalAssessmentmode(entry);
		mode = assessmentModeMgr.persist(mode);
		dbInstance.commitAndCloseSession();
		Assert.assertNotNull(mode);

		AssessmentModeToArea modeToArea = assessmentModeMgr.createAssessmentModeToArea(mode, area);
		mode.getAreas().add(modeToArea);
		AssessmentMode savedMode = assessmentModeMgr.merge(mode, true);
		dbInstance.commitAndCloseSession();
		
		AssessmentMode reloadedMode = assessmentModeMgr.getAssessmentModeById(mode.getKey());
		Assert.assertEquals(mode, reloadedMode);
		Assert.assertEquals(savedMode, reloadedMode);
		Assert.assertNotNull(reloadedMode.getAreas());
		Assert.assertEquals(1, reloadedMode.getAreas().size());
		Assert.assertEquals(modeToArea, reloadedMode.getAreas().iterator().next());
		dbInstance.commitAndCloseSession();
	}
	
	@Test
	public void deleteAssessmentMode() {
		//prepare the setup
		Identity author = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-1");
		RepositoryEntry entry = JunitTestHelper.createAndPersistRepositoryEntry();
		BusinessGroup businessGroup = businessGroupService.createBusinessGroup(author, "as_mode_1", "", null, null, null, null, false, false, null);
		
		AssessmentMode mode = createMinimalAssessmentmode(entry);
		mode = assessmentModeMgr.persist(mode);
		dbInstance.commitAndCloseSession();
		Assert.assertNotNull(mode);

		AssessmentModeToGroup modeToGroup = assessmentModeMgr.createAssessmentModeToGroup(mode, businessGroup);
		mode.getGroups().add(modeToGroup);
		AssessmentMode savedMode = assessmentModeMgr.merge(mode, true);
		dbInstance.commitAndCloseSession();
		
		BusinessGroup businessGroupForArea = businessGroupService.createBusinessGroup(author, "as_mode_1", "", null, null, null, null, false, false, null);
		BGArea area = areaMgr.createAndPersistBGArea("little area", "My little secret area", entry.getOlatResource());
		areaMgr.addBGToBGArea(businessGroupForArea, area);
		dbInstance.commitAndCloseSession();
		AssessmentModeToArea modeToArea = assessmentModeMgr.createAssessmentModeToArea(savedMode, area);
		savedMode.getAreas().add(modeToArea);
		savedMode = assessmentModeMgr.merge(savedMode, true);
		dbInstance.commitAndCloseSession();
		
		//delete
		assessmentModeMgr.delete(savedMode);
		dbInstance.commit();
		//check
		AssessmentMode deletedMode = assessmentModeMgr.getAssessmentModeById(mode.getKey());
		Assert.assertNull(deletedMode);
	}
	
	@Test
	public void loadAssessmentMode_repositoryEntry() {
		RepositoryEntry entry = JunitTestHelper.createAndPersistRepositoryEntry();
		AssessmentMode mode = createMinimalAssessmentmode(entry);
		mode = assessmentModeMgr.persist(mode);
		dbInstance.commitAndCloseSession();
		Assert.assertNotNull(mode);
		
		//check
		List<AssessmentMode> currentModes = assessmentModeMgr.getAssessmentModeFor(entry);
		Assert.assertNotNull(currentModes);
		Assert.assertEquals(1, currentModes.size());
		Assert.assertTrue(currentModes.contains(mode));
	}
	
	@Test
	public void loadCurrentAssessmentModes() {
		RepositoryEntry entry = JunitTestHelper.createAndPersistRepositoryEntry();
		AssessmentMode mode = createMinimalAssessmentmode(entry);
		mode = assessmentModeMgr.persist(mode);
		dbInstance.commitAndCloseSession();
		Assert.assertNotNull(mode);
		
		//check
		Date now = new Date();
		List<AssessmentMode> currentModes = assessmentModeMgr.getAssessmentModes(now);
		Assert.assertNotNull(currentModes);
		Assert.assertFalse(currentModes.isEmpty());
		Assert.assertTrue(currentModes.contains(mode));
	}
	
	/**
	 * Check an assessment linked to a group with one participant
	 * 
	 */
	@Test
	public void loadAssessmentMode_identityInBusinessGroup() {
		RepositoryEntry entry = JunitTestHelper.createAndPersistRepositoryEntry();
		Identity author = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-2");
		Identity participant = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-3");
		Identity coach = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-3");
		BusinessGroup businessGroup = businessGroupService.createBusinessGroup(author, "as-mode-2", "", null, null, null, null, false, false, entry);
		businessGroupRelationDao.addRole(participant, businessGroup, GroupRoles.participant.name());
		businessGroupRelationDao.addRole(coach, businessGroup, GroupRoles.coach.name());
		
		AssessmentMode mode = createMinimalAssessmentmode(entry);
		mode.setTargetAudience(AssessmentMode.Target.courseAndGroups);
		mode.setApplySettingsForCoach(false);
		mode = assessmentModeMgr.persist(mode);
		
		AssessmentModeToGroup modeToGroup = assessmentModeMgr.createAssessmentModeToGroup(mode, businessGroup);
		mode.getGroups().add(modeToGroup);
		mode = assessmentModeMgr.merge(mode, true);
		dbInstance.commitAndCloseSession();
		Assert.assertNotNull(mode);
		
		//check participant
		List<AssessmentMode> currentModes = assessmentModeMgr.getAssessmentModeFor(participant);
		Assert.assertNotNull(currentModes);
		Assert.assertEquals(1, currentModes.size());
		Assert.assertTrue(currentModes.contains(mode));
		
		//check coach
		List<AssessmentMode> currentCoachModes = assessmentModeMgr.getAssessmentModeFor(coach);
		Assert.assertNotNull(currentCoachModes);
		Assert.assertTrue(currentCoachModes.isEmpty());
		
		//check author
		List<AssessmentMode> currentAuthorModes = assessmentModeMgr.getAssessmentModeFor(author);
		Assert.assertNotNull(currentAuthorModes);
		Assert.assertTrue(currentAuthorModes.isEmpty());
	}
	
	
	/**
	 * Check an assessment linked to a group with one participant
	 * 
	 */
	@Test
	public void loadAssessmentMode_identityInBusinessGroup_coach() {
		RepositoryEntry entry = JunitTestHelper.createAndPersistRepositoryEntry();
		Identity author = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-4");
		Identity participant = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-5");
		Identity coach = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-6");
		BusinessGroup businessGroup = businessGroupService.createBusinessGroup(null, "as-mode-3", "", null, null, null, null, false, false, entry);
		businessGroupRelationDao.addRole(participant, businessGroup, GroupRoles.participant.name());
		businessGroupRelationDao.addRole(coach, businessGroup, GroupRoles.coach.name());
		
		AssessmentMode mode = createMinimalAssessmentmode(entry);
		mode.setTargetAudience(AssessmentMode.Target.courseAndGroups);
		mode.setApplySettingsForCoach(true);
		mode = assessmentModeMgr.persist(mode);
		
		AssessmentModeToGroup modeToGroup = assessmentModeMgr.createAssessmentModeToGroup(mode, businessGroup);
		mode.getGroups().add(modeToGroup);
		mode = assessmentModeMgr.merge(mode, true);
		dbInstance.commitAndCloseSession();
		Assert.assertNotNull(mode);
		
		//check participant
		List<AssessmentMode> currentModes = assessmentModeMgr.getAssessmentModeFor(participant);
		Assert.assertNotNull(currentModes);
		Assert.assertEquals(1, currentModes.size());
		Assert.assertTrue(currentModes.contains(mode));
		
		//check coach
		List<AssessmentMode> currentCoachModes = assessmentModeMgr.getAssessmentModeFor(coach);
		Assert.assertNotNull(currentCoachModes);
		Assert.assertEquals(1, currentCoachModes.size());
		Assert.assertTrue(currentCoachModes.contains(mode));
		
		//check author
		List<AssessmentMode> currentAuthorModes = assessmentModeMgr.getAssessmentModeFor(author);
		Assert.assertNotNull(currentAuthorModes);
		Assert.assertTrue(currentAuthorModes.isEmpty());
	}
	
	@Test
	public void loadAssessmentMode_identityInCourse() {
		RepositoryEntry entry = JunitTestHelper.createAndPersistRepositoryEntry();
		Identity author = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-4");
		Identity participant = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-5");
		Identity coach = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-6");
		repositoryEntryRelationDao.addRole(participant, entry, GroupRoles.participant.name());
		repositoryEntryRelationDao.addRole(coach, entry, GroupRoles.coach.name());
		repositoryEntryRelationDao.addRole(author, entry, GroupRoles.owner.name());
		
		AssessmentMode mode = createMinimalAssessmentmode(entry);
		mode.setTargetAudience(AssessmentMode.Target.courseAndGroups);
		mode.setApplySettingsForCoach(false);
		mode = assessmentModeMgr.persist(mode);
		dbInstance.commitAndCloseSession();
		Assert.assertNotNull(mode);
		
		//check participant
		List<AssessmentMode> currentModes = assessmentModeMgr.getAssessmentModeFor(participant);
		Assert.assertNotNull(currentModes);
		Assert.assertEquals(1, currentModes.size());
		Assert.assertTrue(currentModes.contains(mode));
		
		//check coach
		List<AssessmentMode> currentCoachModes = assessmentModeMgr.getAssessmentModeFor(coach);
		Assert.assertNotNull(currentCoachModes);
		Assert.assertTrue(currentCoachModes.isEmpty());
		
		//check author
		List<AssessmentMode> currentAuthorModes = assessmentModeMgr.getAssessmentModeFor(author);
		Assert.assertNotNull(currentAuthorModes);
		Assert.assertTrue(currentAuthorModes.isEmpty());
	}
	
	@Test
	public void loadAssessmentMode_identityInCourse_coach() {
		RepositoryEntry entry = JunitTestHelper.createAndPersistRepositoryEntry();
		Identity author = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-7");
		Identity participant = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-8");
		Identity coach = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-9");
		repositoryEntryRelationDao.addRole(participant, entry, GroupRoles.participant.name());
		repositoryEntryRelationDao.addRole(coach, entry, GroupRoles.coach.name());
		repositoryEntryRelationDao.addRole(author, entry, GroupRoles.owner.name());
		
		AssessmentMode mode = createMinimalAssessmentmode(entry);
		mode.setTargetAudience(AssessmentMode.Target.courseAndGroups);
		mode.setApplySettingsForCoach(true);
		mode = assessmentModeMgr.persist(mode);
		dbInstance.commitAndCloseSession();
		Assert.assertNotNull(mode);
		
		//check participant
		List<AssessmentMode> currentModes = assessmentModeMgr.getAssessmentModeFor(participant);
		Assert.assertNotNull(currentModes);
		Assert.assertEquals(1, currentModes.size());
		Assert.assertTrue(currentModes.contains(mode));
		
		//check coach
		List<AssessmentMode> currentCoachModes = assessmentModeMgr.getAssessmentModeFor(coach);
		Assert.assertNotNull(currentCoachModes);
		Assert.assertEquals(1, currentCoachModes.size());
		Assert.assertTrue(currentCoachModes.contains(mode));
		
		//check author
		List<AssessmentMode> currentAuthorModes = assessmentModeMgr.getAssessmentModeFor(author);
		Assert.assertNotNull(currentAuthorModes);
		Assert.assertTrue(currentAuthorModes.isEmpty());
	}
	
	/**
	 * Check an assessment linked to an area with one participant
	 * 
	 */
	@Test
	public void loadAssessmentMode_identityInArea() {
		RepositoryEntry entry = JunitTestHelper.createAndPersistRepositoryEntry();
		Identity author = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-12");
		Identity participant = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-13");
		Identity coach = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-14");
		BusinessGroup businessGroup = businessGroupService.createBusinessGroup(author, "as-mode-3", "", null, null, null, null, false, false, entry);
		businessGroupRelationDao.addRole(participant, businessGroup, GroupRoles.participant.name());
		businessGroupRelationDao.addRole(coach, businessGroup, GroupRoles.coach.name());
		
		BGArea area = areaMgr.createAndPersistBGArea("area for people", "", entry.getOlatResource());
		areaMgr.addBGToBGArea(businessGroup, area);
		
		AssessmentMode mode = createMinimalAssessmentmode(entry);
		mode.setTargetAudience(AssessmentMode.Target.courseAndGroups);
		mode.setApplySettingsForCoach(false);
		mode = assessmentModeMgr.persist(mode);
		
		AssessmentModeToGroup modeToGroup = assessmentModeMgr.createAssessmentModeToGroup(mode, businessGroup);
		mode.getGroups().add(modeToGroup);
		mode = assessmentModeMgr.merge(mode, true);
		dbInstance.commitAndCloseSession();
		Assert.assertNotNull(mode);
		
		//check participant
		List<AssessmentMode> currentModes = assessmentModeMgr.getAssessmentModeFor(participant);
		Assert.assertNotNull(currentModes);
		Assert.assertEquals(1, currentModes.size());
		Assert.assertTrue(currentModes.contains(mode));
		
		//check coach
		List<AssessmentMode> currentCoachModes = assessmentModeMgr.getAssessmentModeFor(coach);
		Assert.assertNotNull(currentCoachModes);
		Assert.assertTrue(currentCoachModes.isEmpty());
		
		//check author
		List<AssessmentMode> currentAuthorModes = assessmentModeMgr.getAssessmentModeFor(author);
		Assert.assertNotNull(currentAuthorModes);
		Assert.assertTrue(currentAuthorModes.isEmpty());
	}
	
	/**
	 * Check an assessment linked to an area with one participant
	 * 
	 */
	@Test
	public void loadAssessmentMode_identityInArea_coach() {
		RepositoryEntry entry = JunitTestHelper.createAndPersistRepositoryEntry();
		Identity author = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-12");
		Identity participant = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-13");
		Identity coach = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-14");
		BusinessGroup businessGroup = businessGroupService.createBusinessGroup(null, "as-mode-3", "", null, null, null, null, false, false, entry);
		businessGroupRelationDao.addRole(participant, businessGroup, GroupRoles.participant.name());
		businessGroupRelationDao.addRole(coach, businessGroup, GroupRoles.coach.name());
		
		BGArea area = areaMgr.createAndPersistBGArea("area for people", "", entry.getOlatResource());
		areaMgr.addBGToBGArea(businessGroup, area);
		
		AssessmentMode mode = createMinimalAssessmentmode(entry);
		mode.setTargetAudience(AssessmentMode.Target.courseAndGroups);
		mode.setApplySettingsForCoach(true);
		mode = assessmentModeMgr.persist(mode);
		
		AssessmentModeToArea modeToArea = assessmentModeMgr.createAssessmentModeToArea(mode, area);
		mode.getAreas().add(modeToArea);
		mode = assessmentModeMgr.merge(mode, true);
		dbInstance.commitAndCloseSession();
		Assert.assertNotNull(mode);
		
		//check participant
		List<AssessmentMode> currentModes = assessmentModeMgr.getAssessmentModeFor(participant);
		Assert.assertNotNull(currentModes);
		Assert.assertEquals(1, currentModes.size());
		Assert.assertTrue(currentModes.contains(mode));
		
		//check coach
		List<AssessmentMode> currentCoachModes = assessmentModeMgr.getAssessmentModeFor(coach);
		Assert.assertNotNull(currentCoachModes);
		Assert.assertEquals(1, currentCoachModes.size());
		Assert.assertTrue(currentCoachModes.contains(mode));
		
		//check author
		List<AssessmentMode> currentAuthorModes = assessmentModeMgr.getAssessmentModeFor(author);
		Assert.assertNotNull(currentAuthorModes);
		Assert.assertTrue(currentAuthorModes.isEmpty());
	}
	
	@Test
	public void getAssessedIdentities_course_groups() {
		RepositoryEntry entry = JunitTestHelper.createAndPersistRepositoryEntry();
		Identity author = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-15");
		Identity participant1 = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-16");
		Identity coach1 = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-17");
		
		BusinessGroup businessGroup = businessGroupService.createBusinessGroup(null, "as-mode-4", "", null, null, null, null, false, false, entry);
		businessGroupRelationDao.addRole(participant1, businessGroup, GroupRoles.participant.name());
		businessGroupRelationDao.addRole(coach1, businessGroup, GroupRoles.coach.name());
		
		Identity participant2 = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-18");
		Identity coach2 = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-19");
		repositoryEntryRelationDao.addRole(participant2, entry, GroupRoles.participant.name());
		repositoryEntryRelationDao.addRole(coach2, entry, GroupRoles.coach.name());
		repositoryEntryRelationDao.addRole(author, entry, GroupRoles.owner.name());
		
		AssessmentMode mode = createMinimalAssessmentmode(entry);
		mode.setTargetAudience(AssessmentMode.Target.courseAndGroups);
		mode.setApplySettingsForCoach(true);
		mode = assessmentModeMgr.persist(mode);

		AssessmentModeToGroup modeToGroup = assessmentModeMgr.createAssessmentModeToGroup(mode, businessGroup);
		mode.getGroups().add(modeToGroup);
		mode = assessmentModeMgr.merge(mode, true);
		dbInstance.commitAndCloseSession();

		Set<Long> assessedIdentityKeys = assessmentModeMgr.getAssessedIdentityKeys(mode);
		Assert.assertNotNull(assessedIdentityKeys);
		Assert.assertEquals(4, assessedIdentityKeys.size());
		Assert.assertFalse(assessedIdentityKeys.contains(author.getKey()));
		Assert.assertTrue(assessedIdentityKeys.contains(coach1.getKey()));
		Assert.assertTrue(assessedIdentityKeys.contains(participant1.getKey()));
		Assert.assertTrue(assessedIdentityKeys.contains(coach2.getKey()));
		Assert.assertTrue(assessedIdentityKeys.contains(participant2.getKey()));
	}
	
	@Test
	public void getAssessedIdentities_course_areas() {
		RepositoryEntry entry = JunitTestHelper.createAndPersistRepositoryEntry();
		Identity author = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-20");
		Identity participant1 = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-21");
		Identity coach1 = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-22");
		
		BusinessGroup businessGroup = businessGroupService.createBusinessGroup(null, "as-mode-5", "", null, null, null, null, false, false, entry);
		businessGroupRelationDao.addRole(participant1, businessGroup, GroupRoles.participant.name());
		businessGroupRelationDao.addRole(coach1, businessGroup, GroupRoles.coach.name());
		
		Identity participant2 = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-23");
		Identity coach2 = JunitTestHelper.createAndPersistIdentityAsRndUser("as-mode-24");
		repositoryEntryRelationDao.addRole(participant2, entry, GroupRoles.participant.name());
		repositoryEntryRelationDao.addRole(coach2, entry, GroupRoles.coach.name());
		repositoryEntryRelationDao.addRole(author, entry, GroupRoles.owner.name());
		
		AssessmentMode mode = createMinimalAssessmentmode(entry);
		mode.setTargetAudience(AssessmentMode.Target.courseAndGroups);
		mode.setApplySettingsForCoach(true);
		mode = assessmentModeMgr.persist(mode);
		
		BGArea area = areaMgr.createAndPersistBGArea("area for people", "", entry.getOlatResource());
		areaMgr.addBGToBGArea(businessGroup, area);

		AssessmentModeToArea modeToArea = assessmentModeMgr.createAssessmentModeToArea(mode, area);
		mode.getAreas().add(modeToArea);
		mode = assessmentModeMgr.merge(mode, true);
		dbInstance.commitAndCloseSession();
		Assert.assertNotNull(mode);

		Set<Long> assessedIdentityKeys = assessmentModeMgr.getAssessedIdentityKeys(mode);
		Assert.assertNotNull(assessedIdentityKeys);
		Assert.assertEquals(4, assessedIdentityKeys.size());
		Assert.assertFalse(assessedIdentityKeys.contains(author.getKey()));
		Assert.assertTrue(assessedIdentityKeys.contains(coach1.getKey()));
		Assert.assertTrue(assessedIdentityKeys.contains(participant1.getKey()));
		Assert.assertTrue(assessedIdentityKeys.contains(coach2.getKey()));
		Assert.assertTrue(assessedIdentityKeys.contains(participant2.getKey()));
	}
	
	@Test
	public void isIpAllowed_exactMatch() {
		String ipList = "192.168.1.203";

		boolean allowed1 = assessmentModeMgr.isIpAllowed(ipList, "192.168.1.203");
		Assert.assertTrue(allowed1);

		//negative test
		boolean notAllowed1 = assessmentModeMgr.isIpAllowed(ipList, "192.168.1.129");
		Assert.assertFalse(notAllowed1);
		boolean notAllowed2 = assessmentModeMgr.isIpAllowed(ipList, "192.168.1.204");
		Assert.assertFalse(notAllowed2);
		boolean notAllowed3 = assessmentModeMgr.isIpAllowed(ipList, "192.168.100.203");
		Assert.assertFalse(notAllowed3);
		boolean notAllowed4 = assessmentModeMgr.isIpAllowed(ipList, "192.203.203.203");
		Assert.assertFalse(notAllowed4);
	}
	
	@Test
	public void isIpAllowed_pseudoRange() {
		String ipList = "192.168.1.1 - 192.168.1.128";

		boolean allowed1 = assessmentModeMgr.isIpAllowed(ipList, "192.168.1.64");
		Assert.assertTrue(allowed1);

		//negative test
		boolean notAllowed1 = assessmentModeMgr.isIpAllowed(ipList, "192.168.1.129");
		Assert.assertFalse(notAllowed1);
		boolean notAllowed2 = assessmentModeMgr.isIpAllowed(ipList, "192.168.1.204");
		Assert.assertFalse(notAllowed2);
		boolean notAllowed3 = assessmentModeMgr.isIpAllowed(ipList, "192.168.100.64");
		Assert.assertFalse(notAllowed3);
		boolean notAllowed4 = assessmentModeMgr.isIpAllowed(ipList, "212.203.203.64");
		Assert.assertFalse(notAllowed4);
	}
	
	@Test
	public void isIpAllowed_cidr() {
		String ipList = "192.168.100.1/24";

		boolean allowed1 = assessmentModeMgr.isIpAllowed(ipList, "192.168.100.64");
		Assert.assertTrue(allowed1);

		//negative test
		boolean notAllowed1 = assessmentModeMgr.isIpAllowed(ipList, "192.168.99.129");
		Assert.assertFalse(notAllowed1);
		boolean notAllowed2 = assessmentModeMgr.isIpAllowed(ipList, "192.168.101.204");
		Assert.assertFalse(notAllowed2);
		boolean notAllowed3 = assessmentModeMgr.isIpAllowed(ipList, "192.167.100.1");
		Assert.assertFalse(notAllowed3);
		boolean notAllowed4 = assessmentModeMgr.isIpAllowed(ipList, "212.203.203.64");
		Assert.assertFalse(notAllowed4);
	}
	
	@Test
	public void isIpAllowed_all() {
		String ipList = "192.168.1.203\n192.168.30.1 - 192.168.32.128\n192.168.112.1/24";

		boolean allowed1 = assessmentModeMgr.isIpAllowed(ipList, "192.168.1.203");
		Assert.assertTrue(allowed1);
		boolean allowed2 = assessmentModeMgr.isIpAllowed(ipList, "192.168.31.203");
		Assert.assertTrue(allowed2);
		boolean allowed3 = assessmentModeMgr.isIpAllowed(ipList, "192.168.112.203");
		Assert.assertTrue(allowed3);

		//negative test
		boolean notAllowed1 = assessmentModeMgr.isIpAllowed(ipList, "192.168.99.129");
		Assert.assertFalse(notAllowed1);
		boolean notAllowed2 = assessmentModeMgr.isIpAllowed(ipList, "192.168.101.204");
		Assert.assertFalse(notAllowed2);
		boolean notAllowed3 = assessmentModeMgr.isIpAllowed(ipList, "192.167.100.1");
		Assert.assertFalse(notAllowed3);
		boolean notAllowed4 = assessmentModeMgr.isIpAllowed(ipList, "212.203.203.64");
		Assert.assertFalse(notAllowed4);
	}

	private AssessmentMode createMinimalAssessmentmode(RepositoryEntry entry) {
		AssessmentMode mode = assessmentModeMgr.createAssessmentMode(entry);
		mode.setName("Assessment to load");
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.HOUR_OF_DAY, -1);
		mode.setBegin(cal.getTime());
		cal.add(Calendar.HOUR_OF_DAY, 2);
		mode.setEnd(cal.getTime());
		mode.setTargetAudience(Target.course);
		return mode;
	}
	
}
