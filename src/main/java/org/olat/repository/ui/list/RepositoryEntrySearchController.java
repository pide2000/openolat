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
package org.olat.repository.ui.list;

import java.util.Collections;
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.FormSubmit;
import org.olat.core.gui.components.form.flexible.impl.elements.table.ExtendedFlexiTableSearchController;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.Util;
import org.olat.repository.RepositoryManager;

/**
 * 
 * Initial date: 02.05.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class RepositoryEntrySearchController extends FormBasicController implements ExtendedFlexiTableSearchController {

	private TextElement id; // only for admins
	private TextElement text;
	private TextElement author;
	private FormSubmit searchButton;
	private MultipleSelectionElement membershipMandatoryEl;
	
	private boolean cancelAllowed;
	private boolean enabled = true;
	
	public RepositoryEntrySearchController(UserRequest ureq, WindowControl wControl, boolean cancelAllowed) {
		super(ureq, wControl, "search");
		setTranslator(Util.createPackageTranslator(RepositoryManager.class, getLocale(), getTranslator()));
		this.cancelAllowed = cancelAllowed;
		initForm(ureq);
	}

	public RepositoryEntrySearchController(UserRequest ureq, WindowControl wControl, boolean cancelAllowed, Form form) {
		super(ureq, wControl, LAYOUT_CUSTOM, "search", form);
		setTranslator(Util.createPackageTranslator(RepositoryManager.class, getLocale(), getTranslator()));
		this.cancelAllowed = cancelAllowed;
		initForm(ureq);
	}
	
	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		FormLayoutContainer leftContainer = FormLayoutContainer.createDefaultFormLayout("left_1", getTranslator());
		leftContainer.setRootForm(mainForm);
		formLayout.add(leftContainer);

		text = uifactory.addTextElement("cif_displayname", "cif.displayname", 255, "", leftContainer);
		text.setElementCssClass("o_sel_repo_search_displayname");
		text.setFocus(true);
		
		author = uifactory.addTextElement("cif_author", "cif.author", 255, "", leftContainer);
		author.setElementCssClass("o_sel_repo_search_author");

		FormLayoutContainer rightContainer = FormLayoutContainer.createDefaultFormLayout("right_1", getTranslator());
		rightContainer.setRootForm(mainForm);
		formLayout.add(rightContainer);
		
		id = uifactory.addTextElement("cif_id", "cif.id", 128, "", rightContainer);
		id.setElementCssClass("o_sel_repo_search_id");
		
		membershipMandatoryEl = uifactory.addCheckboxesHorizontal("cif_my", "cif.membership.mandatory", rightContainer, new String[]{ "my" }, new String[]{ "" });
		membershipMandatoryEl.select("my", true);
		
		FormLayoutContainer buttonLayout = FormLayoutContainer.createButtonLayout("button_layout", getTranslator());
		formLayout.add(buttonLayout);
		searchButton = uifactory.addFormSubmitButton("search", buttonLayout);
		searchButton.setElementCssClass("o_sel_repo_search_button");
		if(cancelAllowed) {
			uifactory.addFormCancelButton("quick.search", buttonLayout, ureq, getWindowControl());
		}
	}
	
	public void update(SearchEvent se) {
		id.setValue(se.getId());
		text.setValue(se.getDisplayname());
		author.setValue(se.getAuthor());
		membershipMandatoryEl.select("my", se.isMembershipMandatory());
	}

	@Override
	protected void doDispose() {
		//
	}

	@Override
	public List<String> getConditionalQueries() {
		return Collections.emptyList();
	}

	/**
	 * @return Return value of ID field.
	 */
	public String getId() {
		return id.getValue();
	}

	/**
	 * @return Display name filed value.
	 */
	public String getDisplayName() {
		return text.getValue();
	}

	/**
	 * @return Author field value.
	 */
	public String getAuthor() {
		return author.getValue();
	}
	
	public boolean isMembershipMandatory() {
		return membershipMandatoryEl.isAtLeastSelected(1);
	}

	
	@Override
	public void setEnabled(boolean enable) {
		this.enabled = enable;
	}
	
	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		if(!enabled) return true;
		
		if (text.isEmpty() && author.isEmpty() && (id != null && id.isEmpty()))	{
			showWarning("cif.error.allempty", null);
			return false;
		}
		return true;
	}

	@Override
	protected void formOK(UserRequest ureq) {
		if(enabled) {
			fireSearchEvent(ureq);
		}
	}
	
	@Override
	protected void formCancelled(UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}
	
	@Override
	protected void formInnerEvent (UserRequest ureq, FormItem source, FormEvent event) {
		if(enabled) {
			if (source == searchButton) {
				fireSearchEvent(ureq);
			}
		}
	}
	
	private void fireSearchEvent(UserRequest ureq) {
		SearchEvent e = new SearchEvent();
		e.setId(getId());
		e.setAuthor(getAuthor());
		e.setDisplayname(getDisplayName());
		e.setMembershipMandatory(isMembershipMandatory());
		fireEvent(ureq, e);
	}
}