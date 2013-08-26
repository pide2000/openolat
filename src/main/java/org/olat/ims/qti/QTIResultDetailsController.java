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

package org.olat.ims.qti;

import java.io.File;
import java.util.List;

import org.dom4j.Document;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.table.DefaultColumnDescriptor;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableEvent;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.id.Identity;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.i18n.I18nModule;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.course.nodes.AssessableCourseNode;
import org.olat.course.run.scoring.ScoreEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.ims.qti.container.AssessmentContext;
import org.olat.ims.qti.process.AssessmentFactory;
import org.olat.ims.qti.process.AssessmentInstance;
import org.olat.ims.qti.process.FilePersister;
import org.olat.ims.qti.process.Persister;
import org.olat.ims.qti.render.LocalizedXSLTransformer;
import org.olat.modules.ModuleConfiguration;
import org.olat.modules.iq.IQManager;
import org.olat.modules.iq.IQRetrievedEvent;
import org.olat.repository.RepositoryEntry;
import org.olat.user.UserManager;

/**
 * Initial Date:  12.01.2005
 *
 * @author Mike Stock
 */
public class QTIResultDetailsController extends BasicController {

	private Long courseResourceableId;
	private String nodeIdent;
	private Identity assessedIdentity;
	private RepositoryEntry repositoryEntry;
	private Persister qtiPersister;
	private String type;

	private final IQManager iqm;
	private final QTIResultManager qrm;
	
	private VelocityContainer main, details;
	private QTIResultTableModel tableModel;
	private TableController tableCtr;
	private DialogBoxController retrieveConfirmationCtr;
	
	private CloseableModalController cmc;
	
	/**
	 * @param courseResourceableId
	 * @param nodeIdent
	 * @param identity
	 * @param re
	 * @param type
	 * @param ureq
	 * @param wControl
	 */
	public QTIResultDetailsController(UserRequest ureq, WindowControl wControl, Long courseResourceableId, String nodeIdent, Identity assessedIdentity,
			RepositoryEntry re, String type) {
		super(ureq, wControl);
		this.courseResourceableId = courseResourceableId;
		this.nodeIdent = nodeIdent;
		this.assessedIdentity = assessedIdentity;
		this.repositoryEntry = re;
		this.type = type;
		this.iqm = IQManager.getInstance();
		this.qrm = QTIResultManager.getInstance();
		
		String resourcePath = courseResourceableId + File.separator + nodeIdent;
		qtiPersister = new FilePersister(assessedIdentity, resourcePath);
		
		init(ureq);
	}
	
	private void init(UserRequest ureq) {
		main = createVelocityContainer("qtires");
		details = createVelocityContainer("qtires_details");
		
		TableGuiConfiguration tableConfig = new TableGuiConfiguration();
		tableCtr = new TableController(tableConfig, ureq, getWindowControl(), getTranslator());
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("column.header.date", 0, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("column.header.duration", 1, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("column.header.assesspoints", 2, null, ureq.getLocale()));
		tableCtr.addColumnDescriptor(new QTISelectColumnDescriptor("column.header.action", 3, ureq.getLocale(), getTranslator()));

		List<QTIResultSet> resultSets = qrm.getResultSets(courseResourceableId, nodeIdent, repositoryEntry.getKey(), assessedIdentity);
		tableModel = new QTIResultTableModel(resultSets, qtiPersister, getTranslator());
		tableCtr.setTableDataModel(tableModel);
		listenTo(tableCtr);
		
		main.put("qtirestable", tableCtr.getInitialComponent());
		putInitialPanel(main);
	}
	
	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		if (source == main) {
			if (event.getCommand().equals("close")) {
				fireEvent(ureq, Event.DONE_EVENT);
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(UserRequest ureq, Controller source, Event event) {
		if (source == tableCtr) {
			TableEvent tEvent = (TableEvent)event;
			if (tEvent.getActionId().equals("sel")) {
				QTIResultSet resultSet = tableModel.getObject(tEvent.getRowId());
				
				try {
					Document doc = FilePersister.retreiveResultsReporting(assessedIdentity, type, resultSet.getAssessmentID());
					if (doc == null) {
						showInfo("error.resreporting.na");
						return;
					}
					StringBuilder resultsHTML = LocalizedXSLTransformer.getInstance(ureq.getLocale()).renderResults(doc);
					details.contextPut("reshtml", resultsHTML);
					
					removeAsListenerAndDispose(cmc);
					cmc = new CloseableModalController(getWindowControl(), getTranslator().translate("close"), details);
					listenTo(cmc);
					cmc.activate();
				} catch (Exception e) {
					logError("", e);
					showError("error.resreporting.na");
				}	
			} else if(tEvent.getActionId().equals("ret")) {
				updateTableModel();
				if(tableModel.isTestRunning()) {
					String fullname = UserManager.getInstance().getUserDisplayName(assessedIdentity.getUser());
					String title = translate("retrievetest.confirm.title");
					String text = translate("retrievetest.confirm.text", new String[]{fullname});
					retrieveConfirmationCtr = activateYesNoDialog(ureq, title, text, retrieveConfirmationCtr);
				}
			}
		} else if (source == cmc) {
			updateTableModel();
			removeAsListenerAndDispose(cmc);
			cmc = null;
		} else if (source == retrieveConfirmationCtr) {
			if(DialogBoxUIFactory.isYesEvent(event)) {
				updateTableModel();
				if(tableModel.isTestRunning()) {
					IQRetrievedEvent retrieveEvent = new IQRetrievedEvent(assessedIdentity, courseResourceableId, nodeIdent);
					CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(retrieveEvent, retrieveEvent);
					doRetrieveTest(ureq);
				}
			}
			removeAsListenerAndDispose(retrieveConfirmationCtr);
			retrieveConfirmationCtr = null;
		}
	}
	
	private void updateTableModel() {
		List<QTIResultSet> resultSets = qrm.getResultSets(courseResourceableId, nodeIdent, repositoryEntry.getKey(), assessedIdentity);
		tableModel = new QTIResultTableModel(resultSets, qtiPersister, getTranslator());
		tableCtr.setTableDataModel(tableModel);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	protected void doDispose() {
		//
	}
	
	/**
	 * Retrieve the test: load the course, close the assessment instamce, persist the QTI
	 * result set, pass the score to the course node.
	 * @param ureq
	 */
	protected void doRetrieveTest(UserRequest ureq2) {
		ICourse course = CourseFactory.loadCourse(courseResourceableId);
		AssessableCourseNode testNode = (AssessableCourseNode)course.getRunStructure().getNode(nodeIdent);
		ModuleConfiguration modConfig = testNode.getModuleConfiguration();

		String resourcePathInfo = courseResourceableId + File.separator + nodeIdent;
		AssessmentInstance ai = AssessmentFactory.createAssessmentInstance(assessedIdentity, "", modConfig, false, courseResourceableId, nodeIdent, resourcePathInfo, null);
		//close the test
		ai.close();
		//persist the results
		iqm.persistResults(ai);

		//reporting
		Document docResReporting = iqm.getResultsReporting(ai, assessedIdentity, I18nModule.getDefaultLocale());
		FilePersister.createResultsReporting(docResReporting, assessedIdentity, ai.getFormattedType(), ai.getAssessID());
		
		//olat results
		AssessmentContext ac = ai.getAssessmentContext();
		Float score = new Float(ac.getScore());
		Boolean passed = new Boolean(ac.isPassed());
		ScoreEvaluation sceval = new ScoreEvaluation(score, passed, new Long(ai.getAssessID()));
		UserCourseEnvironment userCourseEnv = AssessmentHelper.createAndInitUserCourseEnvironment(assessedIdentity, course);
		testNode.updateUserScoreEvaluation(sceval, userCourseEnv, assessedIdentity, true);
		
		List<QTIResultSet> resultSets = qrm.getResultSets(courseResourceableId, nodeIdent, repositoryEntry.getKey(), assessedIdentity);
		tableModel.setObjects(resultSets);
		tableCtr.modelChanged();
	}
}