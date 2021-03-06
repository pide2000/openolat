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
package org.olat.modules.coach;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.GroupRoles;
import org.olat.basesecurity.IdentityRef;
import org.olat.core.commons.persistence.DB;
import org.olat.core.id.Identity;
import org.olat.course.ICourse;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupService;
import org.olat.group.manager.BusinessGroupRelationDAO;
import org.olat.modules.coach.model.CourseStatEntry;
import org.olat.modules.coach.model.EfficiencyStatementEntry;
import org.olat.modules.coach.model.GroupStatEntry;
import org.olat.modules.coach.model.StudentStatEntry;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryService;
import org.olat.restapi.repository.course.CoursesWebService;
import org.olat.test.JunitTestHelper;
import org.olat.test.OlatTestCase;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class CoachingServiceTest extends OlatTestCase {
	
	private static boolean isInitialized = false;
	
	private static int NUM_OF_COURSES = 5;
	private static int NUM_OF_STUDENTS = 15;
	
	private static Identity author;
	private static Identity coach10, coach11, coach12, coach13;
	private static Identity coach20, coach21, coach22, coach23, coach24, coach25, coach26;
	
	private static Identity student10;
	
	private static RepositoryEntry course10;

	private static Map<Long,List<Long>> coachToCourseMap = new ConcurrentHashMap<Long,List<Long>>();
	private static Map<Identity,List<RepositoryEntry>> studentToCourseMap = new ConcurrentHashMap<Identity,List<RepositoryEntry>>();
	private static Map<Long,List<Long>> coachToGroupCourseMap = new ConcurrentHashMap<Long,List<Long>>();
	
	@Autowired
	private DB dbInstance;
	@Autowired
	private CoachingService coachingService;
	@Autowired
	private RepositoryManager repositoryManager;
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private BaseSecurity securityManager;
	@Autowired
	private BusinessGroupRelationDAO businessGroupRelationDao;
	@Autowired
	private BusinessGroupService businessGroupService;
	

	
	@Before
	public void setUp() throws Exception {
		if(isInitialized) return;
		
		//author
		author = JunitTestHelper.createAndPersistIdentityAsAuthor("author_" + getUUID());
		//r1 set of coach
		coach10 = JunitTestHelper.createAndPersistIdentityAsRndUser("coach-10");
		coach11 = JunitTestHelper.createAndPersistIdentityAsRndUser("coach-11");
		coach12 = JunitTestHelper.createAndPersistIdentityAsRndUser("coach-12");
		coach13 = JunitTestHelper.createAndPersistIdentityAsRndUser("coach-13");
		//r2 set of coach
		coach20 = JunitTestHelper.createAndPersistIdentityAsRndUser("coach-20");
		coach21 = JunitTestHelper.createAndPersistIdentityAsRndUser("coach-21");
		coach22 = JunitTestHelper.createAndPersistIdentityAsRndUser("coach-22");
		coach23 = JunitTestHelper.createAndPersistIdentityAsRndUser("coach-23");
		coach24 = JunitTestHelper.createAndPersistIdentityAsRndUser("coach-24");
		coach25 = JunitTestHelper.createAndPersistIdentityAsRndUser("coach-25");
		coach26 = JunitTestHelper.createAndPersistIdentityAsRndUser("coach-26");
		
		List<Identity> students = new ArrayList<Identity>();
		//r1 set of student
		for(int i=0; i<NUM_OF_STUDENTS; i++) {
			Identity student = JunitTestHelper.createAndPersistIdentityAsRndUser("student-" + i);
			students.add(student);
			if(i == 0) {
				student10 = student;
			}
		}
		
		int qCount = 0;

		//create courses with members
		for(int i=0; i<NUM_OF_COURSES; i++) {
			ICourse course = CoursesWebService.createEmptyCourse(author, "Coaching - " + i, "Coaching - " + i, null);
			RepositoryEntry re = repositoryManager.lookupRepositoryEntry(course, false);
			if(i == 0) {
				course10 = re;
			}
			
			int r1 = (i % 4);
			int r2 = (i % 7);
			
			switch(r1) {
				case 0: addCoachToCourse(coach10, re); break;
				case 1: addCoachToCourse(coach11, re); break;
				case 2: addCoachToCourse(coach12, re); break;
				case 3: addCoachToCourse(coach13, re); break;
			}
			
			switch(r2) {
				case 0: addCoachToCourse(coach20, re); break;
				case 1: addCoachToCourse(coach21, re); break;
				case 2: addCoachToCourse(coach22, re); break;
				case 3: addCoachToCourse(coach23, re); break;
				case 4: addCoachToCourse(coach24, re); break;
				case 5: addCoachToCourse(coach25, re); break;
				case 6: addCoachToCourse(coach26, re); break;
			}
			assertNotNull(re);
			
			List<Identity> newStudents = reservoirSample(students, NUM_OF_STUDENTS / 2);
			for(Identity newStudent:newStudents) {
				addStudentToCourse(newStudent, re);
				if(qCount++ % 20 == 0) {
					dbInstance.intermediateCommit();
				}
			}
			dbInstance.intermediateCommit();
		}
		
		//create courses with members
		for(int i=0; i<NUM_OF_COURSES; i++) {
			ICourse course = CoursesWebService.createEmptyCourse(author, "Coaching - " + i, "Coaching - " + i, null);
			RepositoryEntry re = repositoryManager.lookupRepositoryEntry(course, false);
			// create groups without waiting list
			BusinessGroup g1 = businessGroupService.createBusinessGroup(author, "coach-g1", null, new Integer(0), new Integer(10), false, false, re);
			BusinessGroup g2 = businessGroupService.createBusinessGroup(author, "coach-g2", null, new Integer(0), new Integer(10), false, false, re);
	    
			//permission to see owners and participants
			businessGroupService.updateDisplayMembers(g1, false, false, false, false, false, false, false);
			businessGroupService.updateDisplayMembers(g2, true, true, false, false, false, false, false);
	    
			// coach
			addCoachToGroupCourse(coach10, g1, re);
			addCoachToGroupCourse(coach11, g1, re);
			addCoachToGroupCourse(coach12, g2, re);
	    
			//students
			List<Identity> newStudents = reservoirSample(students, NUM_OF_STUDENTS / 2);
			for(Iterator<Identity> it=newStudents.iterator(); it.hasNext(); ) {
				Identity newStudent = it.next();
				addStudentToGroupCourse(newStudent, g1, re);
				it.remove();
				if(qCount++ % 20 == 0) {
					dbInstance.intermediateCommit();
				}
				if(rnd.nextDouble() > 0.8d) {
					break;
				}
			}

			// members g2
			for(Iterator<Identity> it=newStudents.iterator(); it.hasNext(); ) {
				addStudentToGroupCourse(it.next(), g2, re);
				if(qCount++ % 20 == 0) {
					dbInstance.intermediateCommit();
				}
			}

			dbInstance.intermediateCommit();
		}
		
		isInitialized = true;
	}
	
	private void addCoachToCourse(Identity coach, RepositoryEntry re) {
		Long coachKey = coach.getKey();
		
		repositoryService.addRole(coach, re, GroupRoles.coach.name());
		if(!coachToCourseMap.containsKey(coachKey)) {
			coachToCourseMap.put(coachKey, new ArrayList<Long>());
		}
		coachToCourseMap.get(coachKey).add(re.getKey());
	}
	
	private void addCoachToGroupCourse(Identity coach, BusinessGroup group, RepositoryEntry re) {
		Long coachKey = coach.getKey();
		
		businessGroupRelationDao.addRole(coach, group, GroupRoles.coach.name());
		if(!coachToCourseMap.containsKey(coachKey)) {
			coachToCourseMap.put(coachKey, new ArrayList<Long>());
		}
		coachToCourseMap.get(coachKey).add(re.getKey());
		
		//GROUPS
		if(!coachToGroupCourseMap.containsKey(coachKey)) {
			coachToGroupCourseMap.put(coachKey, new ArrayList<Long>());
		}
		coachToGroupCourseMap.get(coachKey).add(re.getKey());
	}
	
	private void addStudentToCourse(Identity student, RepositoryEntry re) {
		repositoryService.addRole(student, re, GroupRoles.participant.name());
		if(!studentToCourseMap.containsKey(student)) {
			studentToCourseMap.put(student, new ArrayList<RepositoryEntry>());
		}
		studentToCourseMap.get(student).add(re);
	}
	
	private void addStudentToGroupCourse(Identity student, BusinessGroup group, RepositoryEntry re) {
		businessGroupRelationDao.addRole(student, group, GroupRoles.participant.name());
		if(!studentToCourseMap.containsKey(student)) {
			studentToCourseMap.put(student, new ArrayList<RepositoryEntry>());
		}
		studentToCourseMap.get(student).add(re);
	}
	
	private Random rnd = new Random();
	
	public List<Identity> reservoirSample(Iterable<Identity> items, int m) {   
		List<Identity> res = new ArrayList<Identity>(m);   
		int count = 0;   
		for(Identity item : items){       
			count++;
			if (count <= m)           
				res.add(item);       
			else{  
				int r = rnd.nextInt(count);
				if (r < m)               
					res.set(r, item);       
			}   
		}   
		return res;
	}
	
	@Test
	public void testManagers() {
		assertNotNull(coachingService);
		assertNotNull(repositoryManager);
		assertNotNull(securityManager);
		assertNotNull(businessGroupService);
	}
	
	@Test
	public void getStudentsStatistics() {
		List<StudentStatEntry> statEntries = coachingService.getStudentsStatistics(coach10);
		assertNotNull(statEntries);
	}
	
	@Test
	public void getCoursesStatistics() {
		List<CourseStatEntry> statEntries = coachingService.getCoursesStatistics(coach10);
		assertNotNull(statEntries);
		List<Long> myCourses = coachToCourseMap.get(coach10.getKey());
		assertNotNull(myCourses);
		assertEquals(myCourses.size(), statEntries.size());
	}
	
	@Test
	public void getGroupsStatistics() {
		List<GroupStatEntry> statEntries = coachingService.getGroupsStatistics(coach10);
		assertNotNull(statEntries);
		List<Long> myCourses = coachToGroupCourseMap.get(coach10.getKey());
		assertNotNull(myCourses);
		
		assertEquals(myCourses.size(), statEntries.size());
	}
	
	@Test
	public void getCourse() {
		List<Long> myCourses = coachToCourseMap.get(coach10.getKey());
		assertNotNull(myCourses);

		List<EfficiencyStatementEntry> statEntries = coachingService.getCourse(coach10, course10);
		assertNotNull(statEntries);
		assertFalse(statEntries.isEmpty());
		assertTrue(myCourses.contains(course10.getKey()));

		for(EfficiencyStatementEntry statEntry:statEntries) {
			assertNotNull(statEntry.getCourse());
			assertEquals(course10.getKey(), statEntry.getCourse().getKey());
		}
	}
	
	@Test
	public void getStudentsCourses() {
		List<RepositoryEntry> courses = coachingService.getStudentsCourses(coach10, student10, 0, -1);
		assertNotNull(courses);
		
		List<Long> myCourses = coachToCourseMap.get(coach10.getKey());
		assertNotNull(myCourses);
	}
	
	@Test
	public void getUserCourses() {
		List<RepositoryEntry> courses = coachingService.getUserCourses(student10, 0, -1);
		Assert.assertNotNull(courses);
		Assert.assertEquals(studentToCourseMap.get(student10).size(), courses.size());
	}
	
	@Test
	public void getUsersStatistics() {
		List<IdentityRef> identities = Collections.<IdentityRef>singletonList(student10);
		List<StudentStatEntry> statEntries = coachingService.getUsersStatistics(identities);
		Assert.assertNotNull(statEntries);
		Assert.assertEquals(1, statEntries.size());
		
		StudentStatEntry statEntry = statEntries.get(0);
		Assert.assertEquals(student10.getKey(), statEntry.getStudentKey());
		Assert.assertEquals(studentToCourseMap.get(student10).size(), statEntry.getCountRepo());
	}
	
	private String getUUID() {
		return UUID.randomUUID().toString().replace("-", "");
	}

}
