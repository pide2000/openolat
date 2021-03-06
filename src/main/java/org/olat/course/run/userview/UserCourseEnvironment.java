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

package org.olat.course.run.userview;

import org.olat.core.id.IdentityEnvironment;
import org.olat.course.condition.interpreter.ConditionInterpreter;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.scoring.ScoreAccounting;
import org.olat.repository.model.RepositoryEntryLifecycle;

/**
 * @author Felix Jost
 *
 */
public interface UserCourseEnvironment {
	/**
	 * @return Returns the courseEnvironment.
	 */
	public CourseEnvironment getCourseEnvironment();
	/**
	 * 
	 * @return returns a view to the course in the editor
	 */
	public CourseEditorEnv getCourseEditorEnv();
	
	public ConditionInterpreter getConditionInterpreter();
	
	public IdentityEnvironment getIdentityEnvironment();
	
	public ScoreAccounting getScoreAccounting();
	

	public boolean isAdmin();
	
	public boolean isCoach();

	public boolean isParticipant();
	
	public boolean isIdentityInCourseGroup(Long groupKey);
	
	public RepositoryEntryLifecycle getLifecycle();
	
	/**
	 * Check if the user has an efficiency statement or a certificate. The method
	 * doesn't check if the efficiency statement or the certificate are configured
	 * for the course. It's a database check only.
	 * 
	 * @param update
	 * @return
	 */
	public boolean hasEfficiencyStatementOrCertificate(boolean update);

}