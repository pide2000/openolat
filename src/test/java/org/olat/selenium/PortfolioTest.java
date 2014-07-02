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
package org.olat.selenium;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.InitialPage;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.olat.selenium.page.LoginPage;
import org.olat.selenium.page.NavigationPage;
import org.olat.selenium.page.course.CourseEditorPageFragment;
import org.olat.selenium.page.course.CoursePageFragment;
import org.olat.selenium.page.forum.ForumPage;
import org.olat.selenium.page.graphene.OOGraphene;
import org.olat.selenium.page.portfolio.ArtefactWizardPage;
import org.olat.selenium.page.portfolio.PortfolioPage;
import org.olat.selenium.page.repository.AuthoringEnvPage;
import org.olat.selenium.page.repository.AuthoringEnvPage.ResourceType;
import org.olat.selenium.page.user.UserToolsPage;
import org.olat.selenium.page.wiki.WikiPage;
import org.olat.test.ArquillianDeployments;
import org.olat.test.rest.UserRestClient;
import org.olat.user.restapi.UserVO;
import org.openqa.selenium.WebDriver;


@RunWith(Arquillian.class)
public class PortfolioTest {
	
	@Deployment(testable = false)
	public static WebArchive createDeployment() {
		return ArquillianDeployments.createDeployment();
	}

	@Drone
	private WebDriver browser;
	@ArquillianResource
	private URL deploymentUrl;

	@Page
	private UserToolsPage userTools;
	@Page
	private NavigationPage navBar;

	/**
	 * Create a course with a forum, publish it.
	 * Create a map.
	 * Post in the forum, collect the artefact, bind it to the map.
	 * Check the map and the artefact.
	 * 
	 * @param loginPage
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test
	@RunAsClient
	public void collectForumArtefactInCourse(@InitialPage LoginPage loginPage)
	throws IOException, URISyntaxException {
		UserVO author = new UserRestClient(deploymentUrl).createAuthor();
		loginPage
			.loginAs(author.getLogin(), author.getPassword())
			.resume();
		
		//open the portfolio
		PortfolioPage portfolio = userTools
				.openUserToolsMenu()
				.openPortfolio();
		
		//create a map
		String mapTitle = "Map-Forum-" + UUID.randomUUID();
		String pageTitle = "Page-Forum-" + UUID.randomUUID();
		String structureElementTitle = "Struct-Forum-" + UUID.randomUUID();
		portfolio
			.openMyMaps()
			.createMap(mapTitle, "Hello forum")
			.openEditor()
			.selectMapInEditor(mapTitle)
			.selectFirstPageInEditor()
			.setPage(pageTitle, "With a little description")
			.createStructureElement(structureElementTitle, "Structure description");
		
		
		String courseTitle = "Collect-Forum-" + UUID.randomUUID();
		String forumTitle = ("Forum-" + UUID.randomUUID()).substring(0, 24);
		//go to authoring, create a course with a forum
		navBar
			.openAuthoringEnvironment()
			.openCreateDropDown()
			.clickCreate(ResourceType.course)
			.fillCreateForm(courseTitle)
			.clickToolbarBack()
			.edit();
		
		//open course editor
		CourseEditorPageFragment courseEditor = CourseEditorPageFragment.getEditor(browser);
		courseEditor
			.createNode("fo")
			.nodeTitle(forumTitle)
			.publish()
			.quickPublish();
		
		navBar.backToTheTop();
		
		CoursePageFragment course = CoursePageFragment.getCourse(browser);
		course
			.clickTree()
			.selectWithTitle(forumTitle);
		
		String threadTitle = "Very interessant thread";
		ForumPage forum = ForumPage.getForumPage(browser);
		ArtefactWizardPage artefactWizard = forum
			.createThread(threadTitle, "With a lot of content")
			.addAsArtfeact();
		
		artefactWizard
			.next()
			.tags("Forum", "Thread", "Miscellanous")
			.next()
			.selectMap(mapTitle, pageTitle, structureElementTitle)
			.finish();

		OOGraphene.closeBlueMessageWindow(browser);
		
		//open the portfolio
		portfolio = userTools
			.openUserToolsMenu()
			.openPortfolio()
			.openMyMaps()
			.openMap(mapTitle)
			.selectStructureInTOC(structureElementTitle);
		
		portfolio.assertArtefact(threadTitle);
	}
	
	/**
	 * Create a wiki, create a new page.
	 * Create a map.
	 * Collect the artefact, bind it to the map.
	 * Check the map and the artefact.
	 * 
	 * @param loginPage
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test
	@RunAsClient
	public void collectWikiArtefactInWikiResource(@InitialPage LoginPage loginPage)
	throws IOException, URISyntaxException {
		UserVO author = new UserRestClient(deploymentUrl).createAuthor();
		loginPage
			.loginAs(author.getLogin(), author.getPassword())
			.resume();
		
		//open the portfolio
		PortfolioPage portfolio = userTools
			.openUserToolsMenu()
			.openPortfolio();
				
		//create a map
		String mapTitle = "Map-Wiki-" + UUID.randomUUID();
		String pageTitle = "Page-Wiki-" + UUID.randomUUID();
		String structureElementTitle = "Struct-Wiki-" + UUID.randomUUID();
		portfolio
			.openMyMaps()
			.createMap(mapTitle, "Hello wiki")
			.openEditor()
			.selectMapInEditor(mapTitle)
			.selectFirstPageInEditor()
			.setPage(pageTitle, "With a little description about wiki and such tools")
			.createStructureElement(structureElementTitle, "Structure description");

		//go to authoring
		AuthoringEnvPage authoringEnv = navBar
			.assertOnNavigationPage()
			.openAuthoringEnvironment();
				
		String title = "EP-Wiki-" + UUID.randomUUID();
		//create a wiki and launch it
		authoringEnv
			.openCreateDropDown()
			.clickCreate(ResourceType.wiki)
			.fillCreateForm(title).assertOnGeneralTab()
			.clickToolbarBack()
			.assertOnTitle(title)
			.launch();
		
		//create a page in the wiki
		String page = "LMS-" + UUID.randomUUID();
		String content = "Learning Management System";
		WikiPage wiki = WikiPage.getWiki(browser);

		//create page and add it as artefact to portfolio
		ArtefactWizardPage artefactWizard = wiki
				.createPage(page, content)
				.addAsArtfeact();
			
		artefactWizard
			.next()
			.tags("Wiki", "Thread", "Miscellanous")
			.next()
			.selectMap(mapTitle, pageTitle, structureElementTitle)
			.finish();

		OOGraphene.closeBlueMessageWindow(browser);
		
		//open the portfolio
		portfolio = userTools
			.openUserToolsMenu()
			.openPortfolio()
			.openMyMaps()
			.openMap(mapTitle)
			.selectStructureInTOC(structureElementTitle);
		
		portfolio.assertArtefact(page);
	}

}