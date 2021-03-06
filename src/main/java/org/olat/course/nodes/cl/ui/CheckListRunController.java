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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olat.core.CoreSpringFactory;
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
import org.olat.core.gui.control.ControllerEventListener;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.dtabs.Activateable2;
import org.olat.core.gui.util.CSSHelper;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.StateEntry;
import org.olat.core.logging.activity.CourseLoggingAction;
import org.olat.core.logging.activity.StringResourceableType;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSMediaResource;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.course.auditing.UserNodeAuditManager;
import org.olat.course.nodes.CheckListCourseNode;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.MSCourseNode;
import org.olat.course.nodes.cl.CheckboxManager;
import org.olat.course.nodes.cl.model.Checkbox;
import org.olat.course.nodes.cl.model.CheckboxList;
import org.olat.course.nodes.cl.model.DBCheck;
import org.olat.course.nodes.cl.model.DBCheckbox;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.scoring.ScoreEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;
import org.olat.util.logging.activity.LoggingResourceable;

/**
 * 
 * Initial date: 04.02.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class CheckListRunController extends FormBasicController implements ControllerEventListener, Activateable2 {
	
	private final Date dueDate;
	private final boolean withScore, withPassed;
	private final Boolean closeAfterDueDate;
	private final CheckboxList checkboxList;
	
	private static final String[] onKeys = new String[]{ "on" };

	private final ModuleConfiguration config;
	private final CheckListCourseNode courseNode;
	private final OLATResourceable courseOres;
	private final UserCourseEnvironment userCourseEnv;
	
	private final CheckboxManager checkboxManager;
	
	/**
	 * Use this constructor to launch the checklist.
	 * 
	 * @param ureq
	 * @param wControl
	 * @param courseNode
	 */
	public CheckListRunController(UserRequest ureq, WindowControl wControl, UserCourseEnvironment userCourseEnv,
			OLATResourceable courseOres, CheckListCourseNode courseNode) {
		super(ureq, wControl, "run", Util.createPackageTranslator(CourseNode.class, ureq.getLocale()));
		
		this.courseNode = courseNode;
		this.courseOres = courseOres;
		this.userCourseEnv = userCourseEnv;
		checkboxManager = CoreSpringFactory.getImpl(CheckboxManager.class);
		
		config = courseNode.getModuleConfiguration();
		CheckboxList configCheckboxList = (CheckboxList)config.get(CheckListCourseNode.CONFIG_KEY_CHECKBOX);
		if(configCheckboxList == null) {
			checkboxList = new CheckboxList();
			checkboxList.setList(Collections.<Checkbox>emptyList());
		} else {
			checkboxList = configCheckboxList;
		}
		closeAfterDueDate = (Boolean)config.get(CheckListCourseNode.CONFIG_KEY_CLOSE_AFTER_DUE_DATE);
		if(closeAfterDueDate != null && closeAfterDueDate.booleanValue()) {
			dueDate = (Date)config.get(CheckListCourseNode.CONFIG_KEY_DUE_DATE);
		} else {
			dueDate = null;
		}
		
		Boolean hasScore = (Boolean)config.get(MSCourseNode.CONFIG_KEY_HAS_SCORE_FIELD);
		withScore = (hasScore == null || hasScore.booleanValue());
		Boolean hasPassed = (Boolean)config.get(MSCourseNode.CONFIG_KEY_HAS_PASSED_FIELD);
		withPassed = (hasPassed == null || hasPassed.booleanValue());

		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		boolean readOnly = isReadOnly();
		if(formLayout instanceof FormLayoutContainer) {
			FormLayoutContainer layoutCont = (FormLayoutContainer)formLayout;
			layoutCont.contextPut("readOnly", new Boolean(readOnly));
			if(dueDate != null) {
				layoutCont.contextPut("dueDate", dueDate);
				if(dueDate.compareTo(new Date()) < 0) {
					layoutCont.contextPut("afterDueDate", Boolean.TRUE);
				}
			}
			layoutCont.contextPut("withScore", new Boolean(withScore));
			
			List<DBCheck> checks = checkboxManager.loadCheck(getIdentity(), courseOres, courseNode.getIdent());
			Map<String, DBCheck> uuidToCheckMap = new HashMap<>();
			for(DBCheck check:checks) {
				uuidToCheckMap.put(check.getCheckbox().getCheckboxId(), check);
			}
			
			List<Checkbox> list = checkboxList.getList();
			List<CheckboxWrapper> wrappers = new ArrayList<>(list.size());
			for(Checkbox checkbox:list) {
				DBCheck check = uuidToCheckMap.get(checkbox.getCheckboxId());
				CheckboxWrapper wrapper = forgeCheckboxWrapper(checkbox, check, readOnly, formLayout);
				layoutCont.add(wrapper.getCheckboxEl());
				wrappers.add(wrapper);
			}
			layoutCont.contextPut("checkboxList", wrappers);
			
			if(withScore || withPassed) {
				layoutCont.contextPut("enableScoreInfo", Boolean.TRUE);
				exposeConfigToVC(layoutCont);
				exposeUserDataToVC(layoutCont);
			} else {
				layoutCont.contextPut("enableScoreInfo", Boolean.FALSE);
			}
		}
	}
	
	private void exposeConfigToVC(FormLayoutContainer layoutCont) {
		layoutCont.contextPut(MSCourseNode.CONFIG_KEY_HAS_SCORE_FIELD, config.get(MSCourseNode.CONFIG_KEY_HAS_SCORE_FIELD));
		layoutCont.contextPut(MSCourseNode.CONFIG_KEY_HAS_PASSED_FIELD, config.get(MSCourseNode.CONFIG_KEY_HAS_PASSED_FIELD));
		layoutCont.contextPut(MSCourseNode.CONFIG_KEY_HAS_COMMENT_FIELD, config.get(MSCourseNode.CONFIG_KEY_HAS_COMMENT_FIELD));
	    String infoTextUser = (String) config.get(MSCourseNode.CONFIG_KEY_INFOTEXT_USER);
	    if(StringHelper.containsNonWhitespace(infoTextUser)) {
	    	layoutCont.contextPut(MSCourseNode.CONFIG_KEY_INFOTEXT_USER, infoTextUser);
	    }
	    layoutCont.contextPut(MSCourseNode.CONFIG_KEY_PASSED_CUT_VALUE, AssessmentHelper.getRoundedScore((Float)config.get(MSCourseNode.CONFIG_KEY_PASSED_CUT_VALUE)));
	    layoutCont.contextPut(MSCourseNode.CONFIG_KEY_SCORE_MIN, AssessmentHelper.getRoundedScore((Float)config.get(MSCourseNode.CONFIG_KEY_SCORE_MIN)));
	    layoutCont.contextPut(MSCourseNode.CONFIG_KEY_SCORE_MAX, AssessmentHelper.getRoundedScore((Float)config.get(MSCourseNode.CONFIG_KEY_SCORE_MAX)));
	}
	
	private void exposeUserDataToVC(FormLayoutContainer layoutCont) {
		ScoreEvaluation scoreEval = courseNode.getUserScoreEvaluation(userCourseEnv);
		layoutCont.contextPut("score", AssessmentHelper.getRoundedScore(scoreEval.getScore()));
		layoutCont.contextPut("hasPassedValue", (scoreEval.getPassed() == null ? Boolean.FALSE : Boolean.TRUE));
		layoutCont.contextPut("passed", scoreEval.getPassed());
		StringBuilder comment = Formatter.stripTabsAndReturns(courseNode.getUserUserComment(userCourseEnv));
		layoutCont.contextPut("comment", StringHelper.xssScan(comment));
		UserNodeAuditManager am = userCourseEnv.getCourseEnvironment().getAuditManager();
		layoutCont.contextPut("log", am.getUserNodeLog(courseNode, userCourseEnv.getIdentityEnvironment().getIdentity()));
	}
	
	private CheckboxWrapper forgeCheckboxWrapper(Checkbox checkbox, DBCheck check, boolean readOnly, FormItemContainer formLayout) {
		String[] values = new String[]{ translate(checkbox.getLabel().i18nKey()) };
		
		boolean canCheck = CheckboxReleaseEnum.userAndCoach.equals(checkbox.getRelease());
		
		String boxId = "box_" + checkbox.getCheckboxId();
		MultipleSelectionElement el = uifactory
				.addCheckboxesHorizontal(boxId, null, formLayout, onKeys, values);
		el.setEnabled(canCheck && !readOnly);
		el.addActionListener(FormEvent.ONCHANGE);

		FormLink downloadLink = null;
		if(StringHelper.containsNonWhitespace(checkbox.getFilename())) {
			String filename = checkbox.getFilename();
			String name = "file_" + checkbox.getCheckboxId();
			downloadLink = uifactory.addFormLink(name, "download", filename, null, formLayout, Link.LINK | Link.NONTRANSLATED);
			String css = CSSHelper.createFiletypeIconCssClassFor(filename);
			downloadLink.setIconLeftCSS("o_icon o_icon-fw " + css);
			((Link)downloadLink.getComponent()).setTarget("_blank");
		}
		
		CheckboxWrapper wrapper = new CheckboxWrapper(checkbox, downloadLink, el);
		el.setUserObject(wrapper);
		if(check != null && check.getChecked() != null && check.getChecked().booleanValue()) {
			el.select(onKeys[0], true);
			wrapper.setDbCheckbox(check.getCheckbox());
		}
		if(downloadLink != null) {
			downloadLink.setUserObject(wrapper);
		}
		
		return wrapper;
	}
	
	private boolean isReadOnly() {
		return (closeAfterDueDate != null && closeAfterDueDate.booleanValue()
				&& dueDate != null && dueDate.before(new Date()));
	}

	@Override
	protected void doDispose() {
		//
	}
	
	@Override
	protected void formOK(UserRequest ureq) {
		//
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(source instanceof MultipleSelectionElement) {
			MultipleSelectionElement boxEl = (MultipleSelectionElement)source;
			CheckboxWrapper wrapper = (CheckboxWrapper)boxEl.getUserObject();
			if(wrapper != null) {
				boolean checked = boxEl.isAtLeastSelected(1);
				doCheck(wrapper, checked);
			}
		} else if(source instanceof FormLink) {
			FormLink link = (FormLink)source;
			if("download".equals(link.getCmd())) {
				CheckboxWrapper wrapper = (CheckboxWrapper)link.getUserObject();
				CourseEnvironment courseEnv = userCourseEnv.getCourseEnvironment();
				VFSContainer container = checkboxManager.getFileContainer(courseEnv, courseNode, wrapper.getCheckbox());
				VFSItem item = container.resolve(wrapper.getCheckbox().getFilename());
				if(item instanceof VFSLeaf) {
					VFSMediaResource rsrc = new VFSMediaResource((VFSLeaf)item);
					rsrc.setDownloadable(true);
					ureq.getDispatchResult().setResultingMediaResource(rsrc);
				}
			}
		}
		super.formInnerEvent(ureq, source, event);
	}
	
	private void doCheck(CheckboxWrapper wrapper, boolean checked) {
		DBCheckbox theOne;
		if(wrapper.getDbCheckbox() == null) {
			String uuid = wrapper.getCheckbox().getCheckboxId();
			theOne = checkboxManager.loadCheckbox(courseOres, courseNode.getIdent(), uuid);
		} else {
			theOne = wrapper.getDbCheckbox();
		}
		
		if(theOne == null) {
			//only warning because this happen in course preview
			logWarn("A checkbox is missing: " + courseOres + " / " + courseNode.getIdent(), null);
		} else {
			Float score;
			if(checked) {
				score = wrapper.getCheckbox().getPoints();
			} else {
				score = 0f;
			}
			checkboxManager.check(theOne, getIdentity(), score, new Boolean(checked));
			//make sure all results is on the database before calculating some scores
			//manager commit already DBFactory.getInstance().commit();
			
			courseNode.updateScoreEvaluation(userCourseEnv, getIdentity());
			
			Checkbox checkbox = wrapper.getCheckbox();
			logUpdateCheck(checkbox.getCheckboxId(), checkbox.getTitle());
		}
		
		exposeUserDataToVC(flc);
	}
	
	private void logUpdateCheck(String checkboxId, String boxTitle) {
		ThreadLocalUserActivityLogger.log(CourseLoggingAction.CHECKLIST_CHECK_UPDATED, getClass(), 
				LoggingResourceable.wrapNonOlatResource(StringResourceableType.checkbox, checkboxId, boxTitle));
	}

	@Override
	public void activate(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		//nothin to do
	}
	
	public static class CheckboxWrapper {
		
		private final Checkbox checkbox;
		private final FormLink downloadLink;
		private final MultipleSelectionElement checkboxEl;
		private DBCheckbox dbCheckbox;
		
		public CheckboxWrapper(Checkbox checkbox, FormLink downloadLink, MultipleSelectionElement checkboxEl) {
			this.checkboxEl = checkboxEl;
			this.downloadLink = downloadLink;
			this.checkbox = checkbox;
		}

		public Checkbox getCheckbox() {
			return checkbox;
		}
		
		/**
		 * This value is lazy loaded and can be null!
		 * @return
		 */
		public DBCheckbox getDbCheckbox() {
			return dbCheckbox;
		}

		public void setDbCheckbox(DBCheckbox dbCheckbox) {
			this.dbCheckbox = dbCheckbox;
		}

		public String getTitle() {
			return checkbox.getTitle();
		}
		
		public boolean isPointsAvailable() {
			return checkbox.getPoints() != null;
		}
		
		public String getPoints() {
			return AssessmentHelper.getRoundedScore(checkbox.getPoints());
		}
		
		public String getDescription() {
			return checkbox.getDescription();
		}
		
		public MultipleSelectionElement getCheckboxEl() {
			return checkboxEl;
		}
		
		public String getCheckboxElName() {
			return checkboxEl.getName();//getComponent().getComponentName();
		}
		
		public String getDownloadName() {
			return downloadLink.getComponent().getComponentName();
		}
	}
}