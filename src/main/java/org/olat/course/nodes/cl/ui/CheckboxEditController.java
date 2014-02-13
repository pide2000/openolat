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
package org.olat.course.nodes.cl.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FileElement;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.RichTextElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.activity.CourseLoggingAction;
import org.olat.core.logging.activity.ILoggingAction;
import org.olat.core.logging.activity.StringResourceableType;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSManager;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.nodes.CheckListCourseNode;
import org.olat.course.nodes.cl.CheckboxManager;
import org.olat.course.nodes.cl.model.Checkbox;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.util.logging.activity.LoggingResourceable;

/**
 * 
 * Initial date: 06.02.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class CheckboxEditController extends FormBasicController {
	
	private FormLink deleteLink;
	private TextElement titleEl, pointsEl;
	private SingleSelection releaseEl, labelEl;
	private MultipleSelectionElement awardPointEl;
	private RichTextElement descriptionEl;
	private FileElement fileEl;
	
	private final Checkbox checkbox;
	private final boolean newCheckbox;
	private final OLATResourceable courseOres;
	private final CheckListCourseNode courseNode;
	
	private final CheckboxManager checkboxManager;
	
	public CheckboxEditController(UserRequest ureq, WindowControl wControl,
			OLATResourceable courseOres, CheckListCourseNode courseNode,
			Checkbox checkbox, boolean newCheckbox) {
		super(ureq, wControl);
		this.checkbox = checkbox;
		this.courseOres = courseOres;
		this.courseNode = courseNode;
		this.newCheckbox = newCheckbox;
		checkboxManager = CoreSpringFactory.getImpl(CheckboxManager.class);
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		String title = checkbox.getTitle();
		titleEl = uifactory.addTextElement("checkbox.title", "checkbox.title", 255, title, formLayout);
		
		String[] releaseKeys = new String[] {
				CheckboxReleaseEnum.userAndCoach.name(), CheckboxReleaseEnum.coachOnly.name()
		};
		String[] releaseValues = new String[] {
				translate("release.userAndCoach"), translate("release.coachOnly")
		};
		releaseEl = uifactory.addDropdownSingleselect("release", formLayout, releaseKeys, releaseValues, null);
		if(checkbox.getRelease() != null) {
			releaseEl.select(checkbox.getRelease().name(), true);
		}

		String[] labelKeys = new String[CheckboxLabelEnum.values().length];
		String[] labelValues = new String[CheckboxLabelEnum.values().length];
		for(int i=CheckboxLabelEnum.values().length; i-->0; ){
			labelKeys[i] = CheckboxLabelEnum.values()[i].name();
			labelValues[i] = translate(CheckboxLabelEnum.values()[i].i18nKey());
		}
		labelEl = uifactory.addDropdownSingleselect("label", formLayout, labelKeys, labelValues, null);
		if(checkbox.getLabel() != null) {
			labelEl.select(checkbox.getLabel().name(), true);
		}
		
		String[] onKeys = new String[] { "on" };
		String[] onValues = new String[] { translate("award.point.on") };
		awardPointEl = uifactory.addCheckboxesHorizontal("points", formLayout, onKeys, onValues, null);
		if(checkbox.getPoints() != null) {
			awardPointEl.select(onKeys[0], true);
		}
		String points = checkbox.getPoints() == null ? null : Float.toString(checkbox.getPoints().floatValue());
		pointsEl = uifactory.addTextElement("numofpoints", null, 10, points, formLayout);
		pointsEl.setDisplaySize(5);
		
		String desc = checkbox.getDescription();
		descriptionEl = uifactory.addRichTextElementForStringDataMinimalistic("description", "description", desc, 5, -1, formLayout,
				ureq.getUserSession(), getWindowControl());

		fileEl = uifactory.addFileElement("file", formLayout);
		
		FormLayoutContainer buttonsCont = FormLayoutContainer.createButtonLayout("buttons", getTranslator());
		formLayout.add(buttonsCont);
		uifactory.addFormSubmitButton("save", buttonsCont);
		deleteLink = uifactory.addFormLink("delete", buttonsCont, Link.BUTTON);
		uifactory.addFormCancelButton("cancel", buttonsCont, ureq, getWindowControl());
	}

	@Override
	protected void doDispose() {
		//
	}
	
	public Checkbox getCheckbox() {
		return checkbox;
	}

	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		boolean allOk = true;
		
		pointsEl.clearError();
		if(awardPointEl.isAtLeastSelected(1)) {
			try {
				Float.parseFloat(pointsEl.getValue());
			} catch (NumberFormatException e) {
				pointsEl.setErrorKey("form.error.wrongFloat", null);
				allOk &= false;
			}
		}
		
		return allOk & super.validateFormLogic(ureq);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		checkbox.setTitle(titleEl.getValue());
		String releaseKey = releaseEl.getSelectedKey();
		checkbox.setRelease(CheckboxReleaseEnum.valueOf(releaseKey));
		String labelKey = labelEl.getSelectedKey();
		checkbox.setLabel(CheckboxLabelEnum.valueOf(labelKey));
		if(awardPointEl.isAtLeastSelected(1)) {
			Float points = null;
			try {
				points = new Float(Float.parseFloat(pointsEl.getValue()));
			} catch (NumberFormatException e) {
				//check in validation
			}
			checkbox.setPoints(points);	
		} else {
			checkbox.setPoints(null);
		}
		checkbox.setDescription(descriptionEl.getValue());
		
		File uploadedFile = fileEl.getUploadFile();
		if(uploadedFile != null) {
			String filename = fileEl.getUploadFileName();
			checkbox.setFilename(filename);
			
			try {
				ICourse course = CourseFactory.loadCourse(courseOres);
				CourseEnvironment courseEnv = course.getCourseEnvironment();
				VFSContainer container = checkboxManager.getFileContainer(courseEnv, courseNode, checkbox);
				VFSLeaf leaf = container.createChildLeaf(filename);
				InputStream inStream = new FileInputStream(uploadedFile);
				VFSManager.copyContent(inStream, leaf);
			} catch (FileNotFoundException e) {
				logError("", e);
			}
		}
		
		ILoggingAction action = newCheckbox ? CourseLoggingAction.CHECKLIST_CHECKBOX_CREATED : CourseLoggingAction.CHECKLIST_CHECKBOX_UPDATED;
		ThreadLocalUserActivityLogger.log(action, getClass(), LoggingResourceable.wrap(courseNode),
			LoggingResourceable.wrapNonOlatResource(StringResourceableType.checkbox, checkbox.getCheckboxId(), checkbox.getTitle()));

		fireEvent(ureq, Event.CHANGED_EVENT);
	}

	@Override
	protected void formCancelled(UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(deleteLink == source) {
			fireEvent(ureq, new Event("delete"));

			ThreadLocalUserActivityLogger.log(CourseLoggingAction.CHECKLIST_CHECKBOX_DELETED, getClass(), LoggingResourceable.wrap(courseNode),
				LoggingResourceable.wrapNonOlatResource(StringResourceableType.checkbox, checkbox.getCheckboxId(), checkbox.getTitle()));
		}
		super.formInnerEvent(ureq, source, event);
	}
}