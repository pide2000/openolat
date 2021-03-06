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
package org.olat.course.condition;

import java.util.Collection;
import java.util.UUID;

import junit.framework.Assert;

import org.junit.Test;
import org.olat.core.id.Identity;
import org.olat.core.id.IdentityEnvironment;
import org.olat.core.id.Roles;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.condition.interpreter.ConditionInterpreter;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.course.run.userview.UserCourseEnvironmentImpl;
import org.olat.repository.RepositoryEntry;
import org.olat.test.JunitTestHelper;
import org.olat.test.OlatTestCase;

/**
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class ConditionTest extends OlatTestCase {
	
	@Test
	public void simpleExpresion() throws Exception {
		UserCourseEnvironment uce = getUserDemoCourseEnvironment();
		ConditionInterpreter interpreter = new ConditionInterpreter(uce);
		String condition = "now >= date(\"03.07.2012 08:26\")";
		boolean result = interpreter.evaluateCondition(condition);
		Assert.assertTrue(result);
		
		Collection<Object> tokens = interpreter.getParsedTokens(condition);
		Assert.assertNotNull(tokens);
		Assert.assertFalse(tokens.isEmpty());
	}
	
	@Test
	public void complexExpression() throws Exception {
		UserCourseEnvironment uce = getUserDemoCourseEnvironment();
		ConditionInterpreter interpreter = new ConditionInterpreter(uce);

		String condition = "(((inLearningGroup(\"Rule1Group1\") | inLearningGroup(\"Rule1Group2\"))|inLearningArea(\"Rule1Area1\")))";
		boolean result = interpreter.evaluateCondition(condition);
		Assert.assertFalse(result);
	}
	
	@Test
	public void syntaxProposal() throws Exception {
		UserCourseEnvironment uce = getUserDemoCourseEnvironment();
		ConditionInterpreter interpreter = new ConditionInterpreter(uce);

		String condition = "inLearningGroup(\"16872486<Rule1Group1>\")";
		boolean result = interpreter.evaluateCondition(condition);
		Assert.assertFalse(result);
		
		Collection<Object> tokens = interpreter.getParsedTokens(condition);
		Assert.assertNotNull(tokens);
		Assert.assertFalse(tokens.isEmpty());
		
		for(Object token:tokens) {
			System.out.println(token.getClass().getName());
		}
	}
	
	private UserCourseEnvironment getUserDemoCourseEnvironment() {
		Identity author = JunitTestHelper.createAndPersistIdentityAsUser("junit_auth-" + UUID.randomUUID().toString());
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser("condition");
		Roles roles = new Roles(false, false, false, false, false, false, false);
		RepositoryEntry re = JunitTestHelper.deployDemoCourse(author);
		ICourse course = CourseFactory.loadCourse(re.getOlatResource());
		IdentityEnvironment identityEnv = new IdentityEnvironment(id, roles);
		UserCourseEnvironment uce = new UserCourseEnvironmentImpl(identityEnv, course.getCourseEnvironment());
		return uce;
	}
}
