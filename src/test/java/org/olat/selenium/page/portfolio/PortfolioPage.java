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
package org.olat.selenium.page.portfolio;

import java.util.List;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.Assert;
import org.olat.selenium.page.graphene.OOGraphene;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Drive the portfolio map age, view and editor
 * 
 * 
 * Initial date: 01.07.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class PortfolioPage {
	
	@Drone
	private WebDriver browser;
	
	/**
	 * Open the segment view at "My maps"
	 * 
	 * @return
	 */
	public PortfolioPage openMyMaps() {
		By myMapsBy = By.className("o_sel_ep_my_maps");
		WebElement myMapsLink = browser.findElement(myMapsBy);
		Assert.assertTrue(myMapsLink.isDisplayed());
		myMapsLink.click();
		OOGraphene.waitBusy();
		return this;
	}
	
	/**
	 * Open the "My artefacts" segment
	 * @return
	 */
	public PortfolioPage openMyArtefacts() {
		By myMapsBy = By.className("o_sel_ep_my_artfeacts");
		WebElement myMapsLink = browser.findElement(myMapsBy);
		Assert.assertTrue(myMapsLink.isDisplayed());
		myMapsLink.click();
		OOGraphene.waitBusy();
		return this;
	}
	
	/**
	 * Start the wizard to create a text artefact.
	 * 
	 * @return
	 */
	public ArtefactWizardPage createTextArtefact() {
		By addTextArtefactBy = By.className("o_sel_add_text_artfeact");
		OOGraphene.waitElement(addTextArtefactBy);
		WebElement addTextArtefactLink = browser.findElement(addTextArtefactBy);
		addTextArtefactLink.click();
		OOGraphene.waitBusy();
		
		return ArtefactWizardPage.getWizard(browser);
	}
	
	/**
	 * Create a learning journal or live blog
	 * @return
	 */
	public ArtefactWizardPage createLearningJournal() {
		By addJournalArtefactBy = By.className("o_sel_add_liveblog_artfeact");
		OOGraphene.waitElement(addJournalArtefactBy);
		WebElement addJournalArtefactLink = browser.findElement(addJournalArtefactBy);
		addJournalArtefactLink.click();
		OOGraphene.waitBusy();
		
		return ArtefactWizardPage.getWizard(browser);
	}
	
	public ArtefactWizardPage createFileArtefact() {
		By addJournalArtefactBy = By.className("o_sel_add_upload_artfeact");
		OOGraphene.waitElement(addJournalArtefactBy);
		WebElement addJournalArtefactLink = browser.findElement(addJournalArtefactBy);
		addJournalArtefactLink.click();
		OOGraphene.waitBusy();
		
		return ArtefactWizardPage.getWizard(browser);
	}
	
	/**
	 * Click the link to add an artefact to the map
	 * 
	 * @return
	 */
	public PortfolioPage linkArtefact() {
		By linkArtefactBy = By.cssSelector("a.o_eportfolio_add_link.o_eportfolio_link");
		WebElement linkArtefactLink = browser.findElement(linkArtefactBy);
		linkArtefactLink.click();
		OOGraphene.waitBusy();
		return this;
	}
	
	/**
	 * The button to create an artefact
	 * @return
	 */
	public PortfolioPage addArtefact() {
		By addAsArtefactBy = By.className("o_sel_add_artfeact");
		WebElement addAsArtefactButton = browser.findElement(addAsArtefactBy);
		addAsArtefactButton.click();
		OOGraphene.waitBusy();
		return this;
	}
	
	/**
	 * Create a map (type default) with the specified title.
	 * @param title
	 * @param description
	 * @return
	 */
	public PortfolioPage createMap(String title, String description) {
		//open sub menu
		By createMapBy = By.className("o_sel_create_map");
		WebElement createMapLink = browser.findElement(createMapBy);
		Assert.assertTrue(createMapLink.isDisplayed());
		createMapLink.click();
		
		//create default map
		By createDefaultMapBy = By.className("o_sel_create_default_map");
		OOGraphene.waitElement(createDefaultMapBy);
		WebElement createDefaultMapLink = browser.findElement(createDefaultMapBy);
		Assert.assertTrue(createDefaultMapLink.isDisplayed());
		createDefaultMapLink.click();
		OOGraphene.waitBusy();
		
		//title
		By titleBy = By.xpath("//div[contains(@class,'o_sel_add_map_window')]//form//input[@type='text']");
		WebElement titleEl = browser.findElement(titleBy);
		titleEl.sendKeys(title);
		
		//description
		OOGraphene.tinymce(description, browser);
		
		//ok
		By saveBy = By.xpath("(//div[contains(@class,'o_sel_add_map_window')]//form//button)[last()]");
		WebElement saveButton = browser.findElement(saveBy);
		Assert.assertTrue(saveButton.isDisplayed());
		saveButton.click();
		OOGraphene.waitElement(By.className("o_eportfolio_map"));
		return this;
	}
	
	/**
	 * Open the editor from the map
	 * 
	 * @return
	 */
	public PortfolioPage openEditor() {
		By editorMarkerBy = By.className("o_eportfolio_edit");
		List<WebElement> markers = browser.findElements(editorMarkerBy);
		if(markers.isEmpty()) {
			By editorBy = By.className("o_sel_ep_edit_map");
			WebElement editorButton = browser.findElement(editorBy);
			Assert.assertTrue(editorButton.isDisplayed());
			editorButton.click();
			OOGraphene.waitBusy();
		}
		return this;
	}
	
	/**
	 * Open the editor of a template
	 * @return
	 */
	public PortfolioPage openResourceEditor() {
		By toolsMenu = By.cssSelector("ul.o_sel_repository_tools");
		if(!browser.findElement(toolsMenu).isDisplayed()) {
			openToolsMenu();
		}
		By editTemplateBy = By.className("o_sel_ep_edit_map");
		browser.findElement(editTemplateBy).click();
		OOGraphene.waitBusy();
		return this;
	}
	
	/**
	 * Open the tools drop-down
	 * @return
	 */
	public PortfolioPage openToolsMenu() {
		By toolsMenuCaretBy = By.cssSelector("a.o_sel_repository_tools");
		browser.findElement(toolsMenuCaretBy).click();
		By toolsMenuBy = By.cssSelector("ul.o_sel_repository_tools");
		OOGraphene.waitElement(toolsMenuBy);
		return this;
	}
	
	public PortfolioPage openMap(String mapTitle) {
		By mapBy = By.cssSelector("div.o_eportfolio_maps div.o_map-default");
		By headingBy = By.cssSelector("div.panel-heading h4");
		By openBy = By.className("o_sel_ep_open_map");
		
		List<WebElement> mapEls = browser.findElements(mapBy);
		WebElement mapEl = null;
		for(WebElement el:mapEls) {
			WebElement headingEl = el.findElement(headingBy);
			if(headingEl.getText().contains(mapTitle)) {
				mapEl = el;
			}
		}
		Assert.assertNotNull(mapEl);
		WebElement openLink = mapEl.findElement(openBy);
		openLink.click();
		OOGraphene.waitBusy();	
		return this;
	}
	
	/**
	 * Select and click the map node in the TOC
	 * of the editor.
	 * 
	 * @param mapTitle
	 * @return
	 */
	public PortfolioPage selectMapInEditor(String mapTitle) {
		By mapNodeBy = By.cssSelector("div.o_ep_toc_editor span.o_tree_level_label_open.o_tree_l1>a");
		WebElement selectedNode = null;
		List<WebElement> level1Nodes = browser.findElements(mapNodeBy);
		for(WebElement level1Node:level1Nodes) {
			if(level1Node.getText().contains(mapTitle)) {
				selectedNode = level1Node;
			}
		}
		Assert.assertNotNull(selectedNode);
		selectedNode.click();
		OOGraphene.waitBusy();
		return this;
	}
	
	public PortfolioPage selectStructureInTOC(String structureElementTitle) {
		WebElement selectedStructLink = null;
		By structBy = By.cssSelector("li.level2.type_struct>a");
		List<WebElement> structLinkEls = browser.findElements(structBy);
		for(WebElement el:structLinkEls) {
			if(el.getText().contains(structureElementTitle)) {
				selectedStructLink = el;
			}
		}
		Assert.assertNotNull(selectedStructLink);
		selectedStructLink.click();
		OOGraphene.waitBusy();
		return this;
	}
	
	/**
	 * Find and click the first page in the TOC of the
	 * editor.
	 * 
	 * @return
	 */
	public PortfolioPage selectFirstPageInEditor() {
		By pageNodeBy = By.cssSelector("div.o_ep_toc_editor span.o_tree_level_label_leaf.o_tree_l2>a");
		List<WebElement> level2Nodes = browser.findElements(pageNodeBy);
		Assert.assertFalse(level2Nodes.isEmpty());
		level2Nodes.get(0).click();
		OOGraphene.waitBusy();
		return this;
	}
	
	public PortfolioPage assertArtefact(String artefactTitle) {
		By artefactBy = By.cssSelector(".o_artefact h3");
		List<WebElement> artefactEls = browser.findElements(artefactBy);
		Assert.assertFalse(artefactEls.isEmpty());
		WebElement artefactEl = null;
		for(WebElement el:artefactEls) {
			if(el.getText().contains(artefactTitle)) {
				artefactEl = el;
			}
		}
		Assert.assertNotNull(artefactEl);	
		return this;
	}
	
	/**
	 * Set title and description of a page
	 * 
	 * @param name
	 * @param description
	 * @return
	 */
	public PortfolioPage setPage(String name, String description) {
		return fillElementForm(name, description);
	}
	
	/**
	 * Create a structure element and set its title
	 * and description.
	 * 
	 * @param title
	 * @param description
	 * @return
	 */
	public PortfolioPage createStructureElement(String title, String description) {
		By newStructureBy = By.cssSelector("div.o_ep_toc_editor a.o_eportfolio_add_link.o_ep_struct_icon");
		WebElement newStructureButton = browser.findElement(newStructureBy);
		newStructureButton.click();
		OOGraphene.waitBusy();
		
		//fill the form
		return fillElementForm(title, description);
	}
	
	/**
	 * Fill the standard form for portfolio element.
	 * 
	 * @param name
	 * @param description
	 * @return
	 */
	private PortfolioPage fillElementForm(String name, String description) {
		//title
		By nameBy = By.cssSelector("form div.o_ep_struct_editor input[type='text']");
		WebElement nameEl = browser.findElement(nameBy);
		nameEl.clear();
		nameEl.sendKeys(name);
		
		//description
		OOGraphene.tinymce(description, browser);
		
		By saveBy = By.cssSelector("form div.o_ep_struct_editor button.btn-primary");
		WebElement saveButton = browser.findElement(saveBy);
		saveButton.click();
		OOGraphene.waitBusy();
		return this;
	}
}
