package de.unileipzig.xman.admin.controller;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.List;

import org.olat.admin.user.UserSearchController;
import org.olat.basesecurity.events.SingleIdentityChosenEvent;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.Windows;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.table.Table;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableEvent;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.components.table.TableMultiSelectEvent;
import org.olat.core.gui.components.util.ComponentUtil;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.DefaultController;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.dtabs.DTab;
import org.olat.core.gui.control.generic.dtabs.DTabs;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.gui.control.generic.tool.ToolController;
import org.olat.core.gui.control.generic.tool.ToolFactory;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Util;
import org.olat.core.util.mail.ContactList;
import org.olat.core.util.mail.ContactMessage;
import org.olat.core.util.notifications.ContextualSubscriptionController;
import org.olat.core.util.notifications.PublisherData;
import org.olat.core.util.notifications.SubscriptionContext;
import org.olat.core.util.resource.OresHelper;

import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.modules.co.ContactFormController;
import org.olat.modules.wiki.WikiManager;
import org.olat.modules.wiki.WikiPage;
import org.olat.repository.RepoJumpInHandlerFactory;
import org.olat.repository.RepositoryManager;
import org.olat.resource.OLATResourceManager;
import org.olat.user.HomePageConfigManager;
import org.olat.user.HomePageConfigManagerImpl;
import org.olat.user.UserInfoMainController;

import de.unileipzig.xman.admin.ExamAdminSite;
import de.unileipzig.xman.admin.mail.MailManager;
import de.unileipzig.xman.comment.CommentEntry;
import de.unileipzig.xman.comment.CommentManager;
import de.unileipzig.xman.esf.ElectronicStudentFile;
import de.unileipzig.xman.esf.ElectronicStudentFileManager;
import de.unileipzig.xman.esf.controller.ESFCreateController;
import de.unileipzig.xman.esf.controller.ESFEditController;
import de.unileipzig.xman.esf.controller.ESFLaunchController;
import de.unileipzig.xman.esf.table.ESFTableModel;
import de.unileipzig.xman.exam.Exam;
import de.unileipzig.xman.exam.ExamDBManager;
import de.unileipzig.xman.exam.controllers.ExamLaunchController;
import de.unileipzig.xman.protocol.Protocol;
import de.unileipzig.xman.protocol.tables.ProtocolTableModel;

/**
 * 
 * Description:<br>
 * 
 * 
 * <P>
 * Initial Date: 22.05.2008 <br>
 * 
 * @author gerb
 */
public class ExamAdminESFController extends BasicController {

	private static final String VELOCITY_ROOT = Util
			.getPackageVelocityRoot(ExamAdminSite.class);

	private boolean provideLaunchButton;

	private Translator translator;
	private VelocityContainer mainVC;
	private ToolController toolCtr;

	private TableController esfTableCtr;
	private ESFTableModel esfTableMdl;

	private ESFCreateController createESFController;

	private UserSearchController userSearchCreateESFCtr;
	private UserSearchController userSearchEditESFCtr;

	private ContactFormController contactFormController;

	private CloseableModalController createESFModalCtr;
	private CloseableModalController editESFModalCtr;
	private CloseableModalController sendMailCtr;

	private DialogBoxController deleteDialog;
	private List<ElectronicStudentFile> esfList;

	/**
	 * 
	 * @param ureq
	 * @param wControl
	 * @param provideLaunchButton
	 *            - true, for showing all esf which are NOT validated yet
	 */
	protected ExamAdminESFController(UserRequest ureq, WindowControl wControl,
			boolean provideLaunchButton) {
		super(ureq, wControl);

		this.translator = Util.createPackageTranslator(ExamAdminSite.class,
				ureq.getLocale());
		this.mainVC = new VelocityContainer("examCategories", VELOCITY_ROOT
				+ "/esf.html", translator, this);
		this.provideLaunchButton = provideLaunchButton;

		toolCtr = ToolFactory.createToolController(wControl);
		toolCtr.addControllerListener(this);
		toolCtr.addHeader(translator
				.translate("ExamAdminESFController.tool.header"));
		toolCtr.addLink("action.add", translator
				.translate("ExamAdminESFController.tool.add"));
		toolCtr.addLink("action.search", translator
				.translate("ExamAdminESFController.tool.search"));

		// which esf should be displayed
		buildView(ureq, wControl, provideLaunchButton);

		this.putInitialPanel(mainVC);
	}

	/**
	 * Build the ESFTableDataModel
	 * 
	 * @param ureq
	 *            - the UserRequest
	 * @param wControl
	 *            - the WindowControl
	 * @param showValidatedOrNonValidated
	 *            - if true, the tablemodel will not provide the clickable
	 *            (launching) institutionalidentifier
	 */
	private void buildView(UserRequest ureq, WindowControl wControl,
			boolean showNonValidated) {

		List<ElectronicStudentFile> esfList = ElectronicStudentFileManager
				.getInstance().retrieveESFByValidation(showNonValidated);

		TableGuiConfiguration esfTableConfig = new TableGuiConfiguration();
		esfTableConfig.setMultiSelect(true);
		esfTableConfig.setColumnMovingOffered(true);
		esfTableConfig.setDownloadOffered(true);
		esfTableConfig.setPageingEnabled(true);
		esfTableConfig
				.setTableEmptyMessage(this.translator
						.translate(showNonValidated ? "ExamAdminESFController.nonValidated.emptyTableMessage"
								: "ExamAdminESFController.validated.emptyTableMessage"));
		esfTableConfig.setShowAllLinkEnabled(true);
		esfTableConfig.setPreferencesOffered(true, "pref");
		esfTableCtr = new TableController(esfTableConfig, ureq, wControl,
				translator);
		esfTableCtr.setMultiSelect(true);
		// validated esf don't need to be validateable
		if (showNonValidated)
			esfTableCtr.addMultiSelectAction("ExamAdminESFController.validate",
					ESFTableModel.COMMAND_VALIDATE);
		else
			esfTableCtr.addMultiSelectAction(
					"ExamAdminESFController.invalidate",
					ESFTableModel.COMMAND_INVALIDATE);
		esfTableCtr.addMultiSelectAction("ExamAdminESFController.delete",
				ESFTableModel.COMMAND_DELETE);
		esfTableCtr.addMultiSelectAction("ExamAdminESFController.sendMail",
				ESFTableModel.COMMAND_SENDMAIL);
		esfTableMdl = new ESFTableModel(translator.getLocale(), esfList,
				showNonValidated);
		esfTableMdl.setTable(esfTableCtr);
		esfTableCtr.setTableDataModel(esfTableMdl);
		if (showNonValidated) {
			// 6 because multiselection counts as 1 (5 is the numbor of the
			// column of the last modified field)
			esfTableCtr.setSortColumn(5 + 1, true);
		} else {
			esfTableCtr.setSortColumn(2, true);
		}
		// neu, Controller muss logischerweise zum Listener geaddet werden damit
		// das Event abgefangen werden kann
		esfTableCtr.addControllerListener(this);

		mainVC.put("esfTable", esfTableCtr.getInitialComponent());
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose()
	 */
	protected void doDispose() {

		this.createESFController = null;
		this.esfTableCtr = null;
		this.esfTableMdl = null;
		this.mainVC = null;
		this.toolCtr = null;
		this.translator = null;
		this.userSearchCreateESFCtr = null;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.Component,
	 *      org.olat.core.gui.control.Event)
	 */
	public void event(UserRequest ureq, Component source, Event event) {

	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.Controller,
	 *      org.olat.core.gui.control.Event)
	 */
	public void event(UserRequest ureq, Controller ctr, Event event) {

		// the toolController
		if (ctr == toolCtr) {

			// Add new student file
			if (event.getCommand().equals("action.add")) {

				// create new user search dialog
				this.userSearchCreateESFCtr = new UserSearchController(ureq,
						this.getWindowControl(), true);
				this.userSearchCreateESFCtr.addControllerListener(this);

				// make it a modal dialog
				createESFModalCtr = new CloseableModalController(
						getWindowControl(), translator.translate("close"),
						userSearchCreateESFCtr.getInitialComponent());
				this.listenTo(createESFModalCtr);
				createESFModalCtr.activate();

				// this method is not deprecated but not really readable
				// getWindowControl().pushAsModalDialog(ComponentUtil.createTitledComponent("ESFCreateForm.name",
				// null, translator, usc.getInitialComponent()));
			}

			// search for esf of a student
			if (event.getCommand().equals("action.search")) {

				// create new user search dialog
				this.userSearchEditESFCtr = new UserSearchController(ureq, this
						.getWindowControl(), true);
				this.userSearchEditESFCtr.addControllerListener(this);

				// make it a modal dialog
				editESFModalCtr = new CloseableModalController(
						getWindowControl(), translator.translate("close"),
						userSearchEditESFCtr.getInitialComponent());
				this.listenTo(editESFModalCtr);
				editESFModalCtr.activate();

				// this method is not deprecated but not really readable
				// getWindowControl().pushAsModalDialog(ComponentUtil.createTitledComponent("ESFCreateForm.name",
				// null, translator, uscEditESFCtr.getInitialComponent()));
			}
		}

		/******************************* xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx ***************************************/

		// the ESFCreateController
		if (ctr == createESFController) {

			if (event == Event.CANCELLED_EVENT) {

				this.getWindowControl().pop();
			}

			if (event == Event.DONE_EVENT
					|| event.getCommand().equals(
							ESFCreateController.VALIDATE_EVENT)) {

				this.getWindowControl().pop();
				this.getWindowControl().pop();
				this.buildView(ureq, this.getWindowControl(),
						this.provideLaunchButton);
			}
		}

		if (ctr == editESFModalCtr) {

			if (event == Event.DONE_EVENT) {

				this.getWindowControl().pop();
			}
		}

		/******************************* the user search controller for creating esf ***************************************/

		// the usersearchcontroller
		if (ctr == userSearchCreateESFCtr) {

			// one identity was choosen
			if (event instanceof SingleIdentityChosenEvent) {

				SingleIdentityChosenEvent uce = (SingleIdentityChosenEvent) event;
				Identity identity = uce.getChosenIdentity();

				// to prefill the input form, the identity is given to the
				// constructor
				this.createESFController = new ESFCreateController(
						ureq,
						this.getWindowControl(),
						this.translator,
						identity,
						translator
								.translate("ExamAdminESFController.findIdentitySearchForm"),
						ESFLaunchController.VALIDATE_ESF);
				this.createESFController.addControllerListener(this);

				// not deprecated but not really readable
				// getWindowControl().pushAsModalDialog(ComponentUtil.createTitledComponent("ESFCreateForm.name",
				// null, translator, esfController.getInitialComponent()));#

				createESFModalCtr = new CloseableModalController(
						getWindowControl(), translator.translate("close"),
						createESFController.getInitialComponent());
				this.listenTo(createESFModalCtr);
				createESFModalCtr.activate();
			}

			if (event == Event.CANCELLED_EVENT) {

				this.getWindowControl().pop();
			}

		}

		/******************************* the user search controller for editing/searching esf ***************************************/

		// the exam office has choosen one somebody to search
		if (ctr == userSearchEditESFCtr) {

			// one identity was choosen
			if (event instanceof SingleIdentityChosenEvent) {

				// find the choosen identity
				SingleIdentityChosenEvent uce = (SingleIdentityChosenEvent) event;
				Identity identity = uce.getChosenIdentity();

				// load the esf for the identity
				ElectronicStudentFile esf = ElectronicStudentFileManager
						.getInstance().retrieveESFByIdentity(identity);
				if (esf != null) {

					OLATResourceable ores = OLATResourceManager.getInstance()
							.findResourceable(esf.getResourceableId(),
									esf.getResourceableTypeName());

					this.getWindowControl().pop();

					// add the esf in a dtab
					DTabs dts = (DTabs) Windows.getWindows(ureq)
							.getWindow(ureq).getAttribute("DTabs");
					DTab dt = dts.getDTab(ores);
					if (dt == null) {
						// does not yet exist -> create and add
						dt = dts.createDTab(ores, esf.getIdentity().getName());
						if (dt == null)
							return;
						ESFEditController esfEditCtr = new ESFEditController(
								ureq, dt.getWindowControl(), esf);
						dt.setController(esfEditCtr);
						dts.addDTab(dt);
					}
					dts.activate(ureq, dt, null);
				} else
					this
							.getWindowControl()
							.setInfo(
									translator
											.translate("ExamAdminESFController.noESFFoundForIdentity"));
			}

			if (event == Event.CANCELLED_EVENT) {

				this.getWindowControl().pop();
			}

			if (event == Event.DONE_EVENT) {

				this.getWindowControl().pop();
			}
		}

		/************************* the table controller ***************************************/

		// the table Controller
		if (ctr == esfTableCtr) {

			if (event.getCommand().equals(Table.COMMANDLINK_ROWACTION_CLICKED)) {

				TableEvent te = (TableEvent) event;
				String actionID = te.getActionId();

				// somebody wants to open an esf
				if (actionID.equals(ESFTableModel.COMMAND_OPEN)) {

					ElectronicStudentFile esf = esfTableMdl.getEntryAt(te
							.getRowId());
					OLATResourceable ores = OLATResourceManager.getInstance()
							.findResourceable(esf.getResourceableId(),
									ElectronicStudentFile.ORES_TYPE_NAME);

					// add the esf in a dtab
					DTabs dts = (DTabs) Windows.getWindows(ureq)
							.getWindow(ureq).getAttribute("DTabs");
					DTab dt = dts.getDTab(ores);
					if (dt == null) {
						// does not yet exist -> create and add
						dt = dts.createDTab(ores, esf.getIdentity().getName());
						if (dt == null)
							return;
						ESFEditController esfLaunchCtr = new ESFEditController(
								ureq, dt.getWindowControl(), esf);
						dt.setController(esfLaunchCtr);
						dts.addDTab(dt);
					}
					dts.activate(ureq, dt, null);
				}
			}

			// multiple identities were choosen
			if (event.getCommand().equals(Table.COMMAND_MULTISELECT)) {

				TableMultiSelectEvent tmse = (TableMultiSelectEvent) event;

				// somebody wants to validate the choosen identities
				if (tmse.getAction().equals(ESFTableModel.COMMAND_VALIDATE)) {

					// get all esf for the choosen identities
					List<ElectronicStudentFile> esfList = this.esfTableMdl
							.getObjects(tmse.getSelection());

					if (esfList.size() == 0) {

						this
								.getWindowControl()
								.setWarning(
										translator
												.translate("ExamAdminESFController.nobodySelected"));
					} else {
						// set validated to true for all selected identities,
						// and update them in the db
						for (ElectronicStudentFile esf : esfList) {

							esf.setValidated(true);

							// add comment with information about the validator

							// the comment
							String[] comment = { ureq.getIdentity().getName(),
									new Date().toString() };
							String entry = translator
									.translate(
											"ExamAdminESFController.validate.setComment",
											comment);

							CommentEntry commentEntry = CommentManager
									.getInstance().createCommentEntry();
							commentEntry.setAuthor(ureq.getIdentity());
							commentEntry.setComment(entry);

							// add the commentEntry
							esf.addCommentEntry(commentEntry);

							esf.setValidator(ureq.getIdentity());
							ElectronicStudentFileManager.getInstance()
									.updateElectronicStundentFile(esf);

							// send Email to students
							MailManager
									.getInstance()
									.sendEmail(
											this.translator
													.translate("ExamAdminESFController.informStudents.subject"),
											this.translator
													.translate(
															"ExamAdminESFController.informStudents.body",
															new String[] { esf
																	.getIdentity()
																	.getUser()
																	.getProperty(
																			UserConstants.FIRSTNAME,
																			null) }),

											esf.getIdentity());
						}
					}
					this.buildView(ureq, this.getWindowControl(),
							this.provideLaunchButton);
				}

				// somebody wants to invalidate the choosen identities
				if (tmse.getAction().equals(ESFTableModel.COMMAND_INVALIDATE)) {

					// get all esf for the choosen identities
					List<ElectronicStudentFile> esfList = this.esfTableMdl
							.getObjects(tmse.getSelection());

					if (esfList.size() == 0) {

						this
								.getWindowControl()
								.setWarning(
										translator
												.translate("ExamAdminESFController.nobodySelected"));
					} else {
						// set validated to true for all selected identities,
						// and update them in the db
						for (ElectronicStudentFile esf : esfList) {

							// refresh esf
							ElectronicStudentFile refreshedESF = ElectronicStudentFileManager
									.getInstance().retrieveESFByIdentity(
											esf.getIdentity());

							refreshedESF.setValidated(false);

							// add comment with information about the validator

							// the comment
							String[] comment = { ureq.getIdentity().getName(),
									new Date().toString() };
							String entry = translator
									.translate(
											"ExamAdminESFController.invalidate.setComment",
											comment);

							CommentEntry commentEntry = CommentManager
									.getInstance().createCommentEntry();
							commentEntry.setAuthor(ureq.getIdentity());
							commentEntry.setComment(entry);

							// add the commentEntry
							refreshedESF.addCommentEntry(commentEntry);

							ElectronicStudentFileManager.getInstance()
									.updateElectronicStundentFile(refreshedESF);
						}
					}
					this.buildView(ureq, this.getWindowControl(),
							this.provideLaunchButton);
				}

				if (tmse.getAction().equals(ESFTableModel.COMMAND_DELETE)) {

					// get all selected esf's and save them in a field cause we
					// need later (deleteDialog)
					esfList = this.esfTableMdl.getObjects(tmse.getSelection());

					if (esfList.size() == 0) {

						this
								.getWindowControl()
								.setWarning(
										translator
												.translate("ExamAdminESFController.nobodySelected"));
					} else {

						deleteDialog = DialogBoxUIFactory
								.createOkCancelDialog(
										ureq,
										this.getWindowControl(),
										translator
												.translate("ExamAdminESFController.deleteESF.title"),
										translator
												.translate("ExamAdminESFController.deleteESF.text"));

						deleteDialog.addControllerListener(this);
						deleteDialog.activate();
					}

				}

				// someone wants to send students an email
				if (tmse.getAction().equals(ESFTableModel.COMMAND_SENDMAIL)) {

					this.sendMailsToSelectedStudents(this.esfTableMdl
							.getObjects(tmse.getSelection()), ureq);
				}
			}
		}

		if (ctr == deleteDialog) {

			if (DialogBoxUIFactory.isOkEvent(event)) {

				for (ElectronicStudentFile esf : esfList) {

					// refresh possible modifications on the esf
					ElectronicStudentFile tempESF = ElectronicStudentFileManager
							.getInstance().retrieveESFByIdentity(
									esf.getIdentity());
					ElectronicStudentFileManager.getInstance()
							.removeElectronicStudentFile(tempESF);
				}
				this.buildView(ureq, this.getWindowControl(),
						this.provideLaunchButton);
			}
		}

		// remove the modal controller
		if (ctr == contactFormController) {

			if (event == Event.DONE_EVENT || event == Event.CANCELLED_EVENT
					|| event == Event.FAILED_EVENT) {

				this.getWindowControl().pop();
			}
		}
	}

	/**
	 * @return the toolController of this Controller
	 */
	public ToolController getToolController() {

		return this.toolCtr;
	}

	/**
	 * 
	 * 
	 * @param objects
	 * @param ureq
	 */
	private void sendMailsToSelectedStudents(
			List<ElectronicStudentFile> objects, UserRequest ureq) {

		List<ElectronicStudentFile> esfList = (List<ElectronicStudentFile>) objects;

		if (esfList.size() >= 1) {

			// add the user as sender
			ContactMessage contactMsg = new ContactMessage(ureq.getIdentity());

			// create the recipients list
			ContactList emailList = new ContactList(translator
					.translate("ExamAdminESFController.sendMails.recipients"));

			// add selected students to to-list of email
			for (ElectronicStudentFile esf : esfList) {

				emailList.add(esf.getIdentity());
			}

			contactMsg.addEmailTo(emailList);

			contactFormController = new ContactFormController(ureq,
					getWindowControl(), true, true, false, false, contactMsg);
			listenTo(contactFormController);

			sendMailCtr = new CloseableModalController(getWindowControl(),
					translator.translate("close"), contactFormController
							.getInitialComponent());
			this.listenTo(sendMailCtr);
			sendMailCtr.activate();
		} else {

			this
					.getWindowControl()
					.setInfo(
							translator
									.translate("ExamAdminESFController.sendEmail.nothingSelected"));
		}
	}
}