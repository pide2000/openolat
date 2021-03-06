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
package org.olat.selenium.page.repository;

import java.util.List;

import org.junit.Assert;
import org.olat.selenium.page.graphene.OOGraphene;
import org.olat.selenium.page.portfolio.ArtefactWizardPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * To drive the page of feed, blog and podcast
 * 
 * Initial date: 24.06.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class FeedPage {
	
	public static final By feedBy = By.className("o_feed");

	public static final By newExternalFeedBy = By.className("o_feed");
	

	private WebDriver browser;
	
	public FeedPage(WebDriver browser) {
		this.browser = browser;
	}
	
	
	public static FeedPage getFeedPage(WebDriver browser) {
		OOGraphene.waitElement(feedBy);
		return new FeedPage(browser);
	}
	
	/**
	 * Check that the post is visible
	 * @param title
	 * @return
	 */
	public FeedPage assertOnBlogPost(String title) {
		//assert on post
		boolean found = false;
		By postTitleBy = By.cssSelector(".o_post h3.o_title>a>span");
		List<WebElement> postTitleEls = browser.findElements(postTitleBy);
		for(WebElement postTitleEl:postTitleEls) {
			if(postTitleEl.getText().contains(title)) {
				found = true;
			}
		}
		Assert.assertTrue(found);
		return this;
	}
	
	/**
	 * Configure the podcast with an external feed.
	 * @param url
	 * @return
	 */
	public FeedPage newExternalPodcast(String url) {
		//click the button to create an external feed
		By lastButton = By.xpath("//div[contains(@class,'o_podcast_no_episodes')]//a[contains(@href,'feed.make.external')]");
		return newExternalFeed(lastButton, url);
	}
	
	/**
	 * Create a new external blog.
	 * @param url
	 * @return
	 */
	public FeedPage newExternalBlog(String url) {
		//click the button to create an external feed
		By lastButton = By.xpath("//div[contains(@class,'o_blog_no_posts')]//a[contains(@href,'feed.make.external')]");
		return newExternalFeed(lastButton, url);
	}
	
	private FeedPage newExternalFeed(By configureExternalButton, String url) {
		browser.findElement(configureExternalButton).click();
		OOGraphene.waitBusy();
		//fill the URL input field
		By urlField = By.xpath("(//div[contains(@class,'modal-body')]//form//input[@type='text'])[2]");
		WebElement urlEl = browser.findElement(urlField);
		urlEl.sendKeys(url);
		
		//write something in description
		OOGraphene.tinymce("...", browser);
		
		//save the settings
		By saveButton = By.xpath("//div[contains(@class,'modal-body')]//form//button[contains(@class,'btn-primary')]");
		browser.findElement(saveButton).click();
		OOGraphene.waitBusy();
		return this;
	}
	
	public FeedPage newBlog() {
		//click the button to create a feed
		By feedButton = By.xpath("//div[contains(@class,'o_blog_no_posts')]//a[contains(@href,'feed.make.internal')]");
		browser.findElement(feedButton).click();
		OOGraphene.waitBusy();
		return this;
	}
	
	public FeedPage addPost() {
		By newItemButton = By.className("o_sel_feed_item_new");
		browser.findElement(newItemButton).click();
		OOGraphene.waitBusy();
		return this;
	}
	
	public FeedPage fillPostForm(String title, String summary, String content) {
		By titleBy = By.cssSelector("div.o_sel_blog_title input[type='text']");
		browser.findElement(titleBy).sendKeys(title);
		
		OOGraphene.tinymce(summary, browser);
		
		OOGraphene.tinymce(content, browser);
		
		return this;
	}
	
	public FeedPage publishPost() {
		By publishButton = By.cssSelector(".o_sel_blog_form button.btn-primary");
		browser.findElement(publishButton).click();
		OOGraphene.waitBusy();
		return this;
	}
	
	/**
	 * Add the thread to my artefacts
	 * 
	 */
	public ArtefactWizardPage addAsArtfeact() {
		By addAsArtefactBy = By.className("o_eportfolio_add");
		WebElement addAsArtefactButton = browser.findElement(addAsArtefactBy);
		addAsArtefactButton.click();
		OOGraphene.waitBusy();
		return ArtefactWizardPage.getWizard(browser);
	}
	
	/**
	 * Click the first month in the pager
	 * @return
	 */
	public FeedPage clickFirstMonthOfPager() {
		By monthBy = By.cssSelector("div.o_year_navigation ul.o_month>li.o_month>a.o_month");
		List<WebElement> monthLinks = browser.findElements(monthBy);
		Assert.assertFalse(monthLinks.isEmpty());
		monthLinks.get(0).click();
		OOGraphene.waitBusy();
		return this;
	}
}