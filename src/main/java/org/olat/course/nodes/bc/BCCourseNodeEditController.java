/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.
*/

package org.olat.course.nodes.bc;

import org.olat.admin.quota.QuotaConstants;
import org.olat.core.commons.modules.bc.FolderRunController;
import org.olat.core.commons.modules.bc.vfs.OlatNamedContainerImpl;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.tabbedpane.TabbedPane;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.ControllerEventListener;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.tabbable.ActivateableTabbableDefaultController;
import org.olat.core.util.vfs.Quota;
import org.olat.core.util.vfs.QuotaManager;
import org.olat.core.util.vfs.callbacks.FullAccessWithQuotaCallback;
import org.olat.core.util.vfs.callbacks.VFSSecurityCallback;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.course.condition.Condition;
import org.olat.course.condition.ConditionEditController;
import org.olat.course.editor.NodeEditController;
import org.olat.course.nodes.BCCourseNode;
import org.olat.course.run.userview.UserCourseEnvironment;

/**
 * Initial Date: Apr 28, 2004
 * 
 * @author gnaegi
 */
public class BCCourseNodeEditController extends ActivateableTabbableDefaultController implements ControllerEventListener {

	public static final String PANE_TAB_FOLDER = "pane.tab.folder";
	public static final String PANE_TAB_ACCESSIBILITY = "pane.tab.accessibility";
	static final String[] paneKeys = { PANE_TAB_FOLDER, PANE_TAB_ACCESSIBILITY };
	
	private ICourse course;//o_clusterOK by guido: inside course editor its save to have a reference to the course
	private BCCourseNode bcNode;
	private VelocityContainer accessabiliryContent, folderContent;

	private ConditionEditController uploaderCondContr, downloaderCondContr;
	private Controller quotaContr;
	private TabbedPane myTabbedPane;
	private Link vfButton;

	/**
	 * Constructor for a folder course building block editor controller
	 * 
	 * @param bcNode
	 * @param course
	 * @param ureq
	 * @param wControl
	 */
	public BCCourseNodeEditController(BCCourseNode bcNode, ICourse course, UserRequest ureq, WindowControl wControl,
			UserCourseEnvironment euce) {
		super(ureq,wControl);
		//o_clusterOK by guido: inside course editor its save to have a reference to the course
		this.course = course;
		this.bcNode = bcNode;
		myTabbedPane = null;
				
		accessabiliryContent = this.createVelocityContainer("edit"); 

		// Uploader precondition
		Condition uploadCondition = bcNode.getPreConditionUploaders();
		uploaderCondContr = new ConditionEditController(ureq, getWindowControl(),
				uploadCondition, AssessmentHelper
						.getAssessableNodes(course.getEditorTreeModel(), bcNode), euce);		
		this.listenTo(uploaderCondContr);
		accessabiliryContent.put("uploadCondition", uploaderCondContr.getInitialComponent());

		// Uploader precondition
		Condition downloadCondition = bcNode.getPreConditionDownloaders();
		downloaderCondContr = new ConditionEditController(ureq, getWindowControl(),
				downloadCondition, AssessmentHelper
						.getAssessableNodes(course.getEditorTreeModel(), bcNode), euce);		
		this.listenTo(downloaderCondContr);
		accessabiliryContent.put("downloadCondition", downloaderCondContr.getInitialComponent());
		
		folderContent = createVelocityContainer("folder");
		vfButton = LinkFactory.createButton("folder.view", folderContent, this);
		
		if ((ureq.getUserSession().getRoles().isOLATAdmin()) | ((ureq.getUserSession().getRoles().isInstitutionalResourceManager()))) {
			String relPath = BCCourseNode.getFoldernodePathRelToFolderBase(course.getCourseEnvironment(), bcNode);
			quotaContr = QuotaManager.getInstance().getQuotaEditorInstance(ureq, wControl, relPath, false);
			folderContent.put("quota", quotaContr.getInitialComponent());
			folderContent.contextPut("editQuota", Boolean.TRUE);
		} else {
			folderContent.contextPut("editQuota", Boolean.FALSE);
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	public void event(UserRequest ureq, Component source, Event event) {
		if (source == vfButton){
			OlatNamedContainerImpl namedContainer = BCCourseNode.getNodeFolderContainer(bcNode, course.getCourseEnvironment());
			Quota quota = QuotaManager.getInstance().getCustomQuota(namedContainer.getRelPath());
			if (quota == null) {
				Quota defQuota = QuotaManager.getInstance().getDefaultQuota(QuotaConstants.IDENTIFIER_DEFAULT_NODES);
				quota = QuotaManager.getInstance().createQuota(namedContainer.getRelPath(), defQuota.getQuotaKB(), defQuota.getUlLimitKB());
			}
			VFSSecurityCallback secCallback = new FullAccessWithQuotaCallback(quota);
			namedContainer.setLocalSecurityCallback(secCallback);
			CloseableModalController cmc = new CloseableModalController(getWindowControl(), translate("close"),
					new FolderRunController(namedContainer, false, ureq, getWindowControl()).getInitialComponent());
			cmc.activate();
			return;
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	public void event(UserRequest urequest, Controller source, Event event) {
		if (source == uploaderCondContr) {
			if (event == Event.CHANGED_EVENT) {
				Condition cond = uploaderCondContr.getCondition();
				bcNode.setPreConditionUploaders(cond);
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		} else if (source == downloaderCondContr) {
			if (event == Event.CHANGED_EVENT) {
				Condition cond = downloaderCondContr.getCondition();
				bcNode.setPreConditionDownloaders(cond);
				fireEvent(urequest, NodeEditController.NODECONFIG_CHANGED_EVENT);
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.generic.tabbable.TabbableDefaultController#addTabs(org.olat.core.gui.components.TabbedPane)
	 */
	public void addTabs(TabbedPane tabbedPane) {
		myTabbedPane = tabbedPane;
		tabbedPane.addTab(translate(PANE_TAB_ACCESSIBILITY), accessabiliryContent);
		tabbedPane.addTab(translate(PANE_TAB_FOLDER), folderContent);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	protected void doDispose() {
    //child controllers registered with listenTo() get disposed in BasicController
		if (quotaContr != null) {
			quotaContr.dispose();
			quotaContr = null;
		}
	}
	
	public String[] getPaneKeys() {
		return paneKeys;
	}

	public TabbedPane getTabbedPane() {
		return myTabbedPane;
	}

}