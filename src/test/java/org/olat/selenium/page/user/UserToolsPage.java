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
package org.olat.selenium.page.user;

import java.util.List;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.Graphene;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.olat.selenium.page.LoginPage;
import org.olat.selenium.page.graphene.OOGraphene;
import org.olat.selenium.page.portfolio.PortfolioPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * 
 * Initial date: 20.06.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class UserToolsPage {
	
	public static final By mySettingsClassName = By.className("o_sel_user_tools-mysettings");

	@Drone
	private WebDriver browser;

	@Page
	private UserSettingsPage userSettings;
	
	/**
	 * Check if the user menu is displayed.
	 * 
	 * @return
	 */
	public UserToolsPage assertOnUserTools() {
		By personnalToolsBy = By.className("o_sel_menu_tools");
		List<WebElement> personnalTools = browser.findElements(personnalToolsBy);
		Assert.assertEquals(1, personnalTools.size());
		Assert.assertTrue(personnalTools.get(0).isDisplayed());
		return this;
	}
	
	/**
	 * Open the user menu with the tools.
	 * 
	 * @return The user menu page
	 */
	public UserToolsPage openUserToolsMenu() {
		List<WebElement> mySettingsLinks = browser.findElements(mySettingsClassName);
		if(mySettingsLinks.isEmpty() || !mySettingsLinks.get(0).isDisplayed()) {
			By toolbarCaretBy = By.id("o_sel_navbar_my_menu_caret");
			List<WebElement> toolbarCaretLinks = browser.findElements(toolbarCaretBy);
			Assert.assertFalse(toolbarCaretLinks.isEmpty());
			WebElement toolbarCaretLink = toolbarCaretLinks.get(0);
			Assert.assertNotNull(toolbarCaretLink);
			toolbarCaretLink.click();
			OOGraphene.waitingTransition();
			OOGraphene.waitElement(mySettingsClassName);
		}
		assertOnUserTools();
		return this;
	}
	
	/**
	 * Open the user settings.
	 * 
	 * @return The user sesstings page fragment
	 */
	public UserSettingsPage openMySettings() {
		WebElement mySettingsLink = browser.findElement(mySettingsClassName);
		Assert.assertTrue(mySettingsLink.isDisplayed());
		mySettingsLink.click();
		OOGraphene.waitBusy();
		return userSettings;
	}
	
	public UserSettingsPage openPassword() {
		WebElement passwordLink = browser.findElement(By.className("o_sel_user_tools-mypassword"));
		Assert.assertTrue(passwordLink.isDisplayed());
		passwordLink.click();
		OOGraphene.waitBusy();
		return userSettings;
	}
	
	public PortfolioPage openPortfolio() {
		By linkBy = By.className("o_sel_user_tools-Portfolio");
		WebElement passwordLink = browser.findElement(linkBy);
		Assert.assertTrue(passwordLink.isDisplayed());
		passwordLink.click();
		OOGraphene.waitBusy();
		
		WebElement main = browser.findElement(By.id("o_main"));
		return Graphene.createPageFragment(PortfolioPage.class, main);
	}
	
	/**
	 * Log out and wait until the login form appears
	 */
	public void logout() {
		OOGraphene.closeBlueMessageWindow(browser);
		openUserToolsMenu();

		By logoutBy = By.className("o_logout");
		WebElement logoutLink = browser.findElement(logoutBy);
		Graphene.guardHttp(logoutLink).click();
		OOGraphene.waitElement(LoginPage.loginFormBy);
	}
}