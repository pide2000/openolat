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
package org.olat.selenium.page.course;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jcodec.common.Assert;
import org.olat.selenium.page.OOGraphene;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;

/**
 * 
 * Page fragment to control the publish process.
 * 
 * 
 * Initial date: 20.06.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class PublisherPageFragment {
	
	public static final By nextBy = By.className("o_wizard_button_next");
	public static final By finishBy = By.className("o_wizard_button_finish");
	public static final By selectAccessBy = By.cssSelector("div.o_sel_course_publish_wizard select");
	public static final By selectCatalogYesNoBy = By.cssSelector("div.o_sel_course_publish_wizard select");
	
	@Drone
	private WebDriver browser;
	
	@FindBy(className="o_sel_course_publish_wizard")
	private WebElement publishBody;
	
	public PublisherPageFragment assertOnPublisher() {
		Assert.assertTrue(publishBody.isDisplayed());
		return this;
	}
	
	public void quickPublish() {
		assertOnPublisher()
			.next()
			.selectAccess(Access.guests)
			.next()
			.selectCatalog(false)
			.next() // -> no problem found
			.finish();
	}
	
	public PublisherPageFragment next() {
		WebElement next = browser.findElement(nextBy);
		Assert.assertTrue(next.isDisplayed());
		Assert.assertTrue(next.isEnabled());
		next.click();
		OOGraphene.waitBusy();
		return this;
	}
	
	public PublisherPageFragment finish() {
		WebElement finish = browser.findElement(finishBy);
		Assert.assertTrue(finish.isDisplayed());
		Assert.assertTrue(finish.isEnabled());
		finish.click();
		OOGraphene.waitBusy();
		return this;
	}
	
	public PublisherPageFragment selectAccess(Access access) {
		WebElement select = browser.findElement(selectAccessBy);
		new Select(select).selectByValue(access.getValue());
		return this;
	}
	
	public PublisherPageFragment selectCatalog(boolean access) {
		WebElement select = browser.findElement(selectCatalogYesNoBy);
		new Select(select).selectByValue(access ? "yes" : "no");
		return this;
	}
	
	public enum Access {
		owner("1"),
		authors("2"),
		users("3"),
		guests("4"),
		membersOnly("membersonly");

		private final String value;
		
		private Access(String value) {
			this.value = value;
		}
		
		public String getValue() {
			return value;
		}
	}
}
