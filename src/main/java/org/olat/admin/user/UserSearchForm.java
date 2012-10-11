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
package org.olat.admin.user;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.user.UserManager;
import org.olat.user.propertyhandlers.EmailProperty;
import org.olat.user.propertyhandlers.UserPropertyHandler;


/**
 * <pre>
 *
 * Initial Date:  Jul 29, 2003
 *
 * @author gnaegi
 * 
 * Comment:  
 * The user search form
 * </pre>
 */
public class UserSearchForm extends FormBasicController {
	
	private final boolean isAdmin, cancelButton;
	private FormLink searchButton;
	
	protected TextElement login;
	protected List<UserPropertyHandler> userPropertyHandlers;
	protected Map <String,FormItem>propFormItems;
	
	/**
	 * @param name
	 * @param cancelbutton
	 * @param isAdmin if true, no field must be filled in at all, otherwise
	 *          validation takes place
	 */
	public UserSearchForm(UserRequest ureq, WindowControl wControl, boolean isAdmin, boolean cancelButton) {
		super(ureq, wControl);
		
		this.isAdmin = isAdmin;
		this.cancelButton = cancelButton;
	
		initForm(ureq);
	}
	
	public UserSearchForm(UserRequest ureq, WindowControl wControl, boolean isAdmin, boolean cancelButton, Form rootForm) {
		super(ureq, wControl, LAYOUT_DEFAULT, null, rootForm);
		
		this.isAdmin = isAdmin;
		this.cancelButton = cancelButton;
	
		initForm(ureq);
	}
	
	@Override
	public boolean validateFormLogic (UserRequest ureq) {
		// override for admins
		if (isAdmin) return true;
		
		boolean filled = !login.isEmpty();
		StringBuffer  full = new StringBuffer(login.getValue().trim());  
		FormItem lastFormElement = login;
		
		// DO NOT validate each user field => see OLAT-3324
		// this are custom fields in a Search Form
		// the same validation logic can not be applied
		// i.e. email must be searchable and not about getting an error like
		// "this e-mail exists already"
		for (UserPropertyHandler userPropertyHandler : userPropertyHandlers) {
			FormItem ui = propFormItems.get(userPropertyHandler.getName());
			String uiValue = userPropertyHandler.getStringValue(ui);
			// add value for later non-empty search check
			if (StringHelper.containsNonWhitespace(uiValue)) {
				full.append(uiValue.trim());
				filled = true;
			}else{
				//its an empty field
				filled = filled || false;
			}

			lastFormElement = ui;
		}

		// Don't allow searches with * or %  or @ chars only (wild cards). We don't want
		// users to get a complete list of all OLAT users this easily.
		String fullString = full.toString();
		boolean onlyStar= fullString.matches("^[\\*\\s@\\%]*$");

		if (!filled || onlyStar) {
			// set the error message
			lastFormElement.setErrorKey("error.search.form.notempty", null);
			return false;
		}
		if ( fullString.contains("**") ) {
			lastFormElement.setErrorKey("error.search.form.no.wildcard.dublicates", null);
			return false;
		}		
		int MIN_LENGTH = 4;
		if ( fullString.length() < MIN_LENGTH ) {
			lastFormElement.setErrorKey("error.search.form.to.short", null);
			return false;
		}
		
		return true;
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		login = uifactory.addTextElement("login", "search.form.login", 128, "", formLayout);

		UserManager um = UserManager.getInstance();
		Translator tr = Util.createPackageTranslator(
				UserPropertyHandler.class,
				getLocale(), 
				getTranslator()
		);
		
		userPropertyHandlers = um.getUserPropertyHandlersFor(
				getClass().getCanonicalName(), isAdmin
		);
		
		propFormItems = new HashMap<String,FormItem>();
		for (UserPropertyHandler userPropertyHandler : userPropertyHandlers) {
			if (userPropertyHandler == null) continue;
			
			FormItem fi = userPropertyHandler.addFormItem(
					getLocale(), null, getClass().getCanonicalName(), false, formLayout
			);
			fi.setTranslator(tr);
			
			// DO NOT validate email field => see OLAT-3324, OO-155, OO-222
			if (userPropertyHandler instanceof EmailProperty && fi instanceof TextElement) {
				TextElement textElement = (TextElement)fi;
				textElement.setItemValidatorProvider(null);
			}

			propFormItems.put(userPropertyHandler.getName(), fi);
		}
		
		FormLayoutContainer buttonGroupLayout = FormLayoutContainer.createButtonLayout("buttonGroupLayout", getTranslator());
		formLayout.add(buttonGroupLayout);

		// Don't use submit button, form should not be marked as dirty since this is
		// not a configuration form but only a search form (OLAT-5626)
		searchButton = uifactory.addFormLink("submit.search", buttonGroupLayout, Link.BUTTON);
		searchButton.addActionListener(this, FormEvent.ONCLICK);
		if (cancelButton) {
			uifactory.addFormCancelButton("cancel", buttonGroupLayout, ureq, getWindowControl());
		}
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formInnerEvent(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.form.flexible.FormItem,
	 *      org.olat.core.gui.components.form.flexible.impl.FormEvent)
	 */
	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if (source == searchButton) {
			source.getRootForm().submit(ureq);			
		}
	}
	
	@Override
	protected void formOK(UserRequest ureq) {
		fireEvent (ureq, Event.DONE_EVENT);
	}
	
	@Override
	protected void formCancelled(UserRequest ureq) {
		fireEvent (ureq, Event.CANCELLED_EVENT);
	}
	
	@Override
	protected void doDispose() {
		//
	}
}