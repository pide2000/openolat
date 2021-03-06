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
package org.olat.repository.ui.author;

import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.repository.RepositoryService;

/**
 * 
 * Initial date: 02.10.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class ConfirmDeleteController extends FormBasicController {
	
	private FormLink deleteButton;
	private MultipleSelectionElement acknowledgeEl;
	
	private final boolean notAllDeleteable;
	private final List<AuthoringEntryRow> rows;
	
	public ConfirmDeleteController(UserRequest ureq, WindowControl wControl, List<AuthoringEntryRow> rows, boolean notAllDeleteable) {
		super(ureq, wControl);
		setTranslator(Util.createPackageTranslator(RepositoryService.class, getLocale(), getTranslator()));
		
		this.rows = rows;
		this.notAllDeleteable = notAllDeleteable;
		
		initForm(ureq);
	}

	public List<AuthoringEntryRow> getRows() {
		return rows;
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		if(notAllDeleteable) {
			setFormWarning("dialog.confirm.delete.notAllDeleteable");
		}
		
		StringBuilder message = new StringBuilder();
		for(AuthoringEntryRow row:rows) {
			if(message.length() > 0) message.append(", ");
			message.append(StringHelper.escapeHtml(row.getDisplayname()));
		}
		uifactory.addStaticTextElement("rows", "details.delete.entries", message.toString(), formLayout);
		
		String acknowledge = translate("details.delete.acknowledge.msg");
		acknowledgeEl = uifactory.addCheckboxesHorizontal("confirm", "details.delete.acknowledge", formLayout, new String[]{ "" }, new String[] { acknowledge });
		
		FormLayoutContainer buttonsCont = FormLayoutContainer.createButtonLayout("buttons", getTranslator());
		formLayout.add(buttonsCont);
		deleteButton = uifactory.addFormLink("details.delete", buttonsCont, Link.BUTTON);
		uifactory.addFormCancelButton("cancel", buttonsCont, ureq, getWindowControl());
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		boolean allOk = true;
		
		acknowledgeEl.clearError();
		if(!acknowledgeEl.isAtLeastSelected(1)) {
			acknowledgeEl.setErrorKey("details.delete.acknowledge.error", null);
			allOk &= false;
		}
		
		return allOk & super.validateFormLogic(ureq);
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(deleteButton == source) {
			if(validateFormLogic(ureq)) {
				fireEvent(ureq, Event.DONE_EVENT);
			}
		}
	}
	
	@Override
	protected void formOK(UserRequest ureq) {
		//
	}

	@Override
	protected void formCancelled(UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}
}
