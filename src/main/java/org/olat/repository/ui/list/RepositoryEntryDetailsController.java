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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.olat.NewControllerFactory;
import org.olat.admin.restapi.RestapiAdminController;
import org.olat.basesecurity.GroupRoles;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.services.commentAndRating.CommentAndRatingDefaultSecurityCallback;
import org.olat.core.commons.services.commentAndRating.CommentAndRatingSecurityCallback;
import org.olat.core.commons.services.commentAndRating.manager.UserRatingsDAO;
import org.olat.core.commons.services.commentAndRating.ui.UserCommentsController;
import org.olat.core.commons.services.mark.Mark;
import org.olat.core.commons.services.mark.MarkManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.image.ImageComponent;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.rating.RatingFormEvent;
import org.olat.core.gui.components.rating.RatingWithAverageFormItem;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.helpers.Settings;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.filter.FilterFactory;
import org.olat.core.util.mail.MailPackage;
import org.olat.core.util.mail.MailerResult;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSContainerMapper;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.course.CorruptedCourseException;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.course.assessment.EfficiencyStatementManager;
import org.olat.course.assessment.UserEfficiencyStatement;
import org.olat.course.assessment.manager.UserCourseInformationsManager;
import org.olat.course.run.RunMainController;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupService;
import org.olat.group.model.SearchBusinessGroupParams;
import org.olat.login.LoginModule;
import org.olat.repository.CatalogEntry;
import org.olat.repository.LeavingStatusList;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryRef;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryModule;
import org.olat.repository.RepositoryService;
import org.olat.repository.handlers.RepositoryHandler;
import org.olat.repository.handlers.RepositoryHandlerFactory;
import org.olat.repository.manager.CatalogManager;
import org.olat.repository.model.RepositoryEntryStatistics;
import org.olat.repository.ui.PriceMethod;
import org.olat.repository.ui.RepositoyUIFactory;
import org.olat.resource.accesscontrol.ACService;
import org.olat.resource.accesscontrol.AccessControlModule;
import org.olat.resource.accesscontrol.AccessResult;
import org.olat.resource.accesscontrol.method.AccessMethodHandler;
import org.olat.resource.accesscontrol.model.AccessMethod;
import org.olat.resource.accesscontrol.model.OfferAccess;
import org.olat.resource.accesscontrol.model.Price;
import org.olat.resource.accesscontrol.ui.PriceFormat;
import org.olat.resource.references.ReferenceManager;
import org.olat.user.UserManager;
import org.olat.util.logging.activity.LoggingResourceable;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 25.03.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class RepositoryEntryDetailsController extends FormBasicController {
	
	protected FormLink markLink, commentsLink, startLink, leaveLink;
	private RatingWithAverageFormItem ratingEl;
	
	private CloseableModalController cmc;
	private DialogBoxController leaveDialogBox;
	private UserCommentsController commentsCtrl;
	
	protected RepositoryEntry entry;
	protected RepositoryEntryRow row;
	private Integer index;

	@Autowired
	private LoginModule loginModule;
	@Autowired
	protected UserRatingsDAO userRatingsDao;
	@Autowired
	protected UserManager userManager;
	@Autowired
	protected ACService acService;
	@Autowired
	protected AccessControlModule acModule;
	@Autowired
	protected MarkManager markManager;
	@Autowired
	protected CatalogManager catalogManager;
	@Autowired
	protected RepositoryModule repositoryModule;
	@Autowired
	protected RepositoryManager repositoryManager;
	@Autowired
	protected RepositoryService repositoryService;
	@Autowired
	protected BusinessGroupService businessGroupService;
	@Autowired
	protected EfficiencyStatementManager effManager;
	@Autowired
	protected UserCourseInformationsManager userCourseInfosManager;
	@Autowired
	protected CoordinatorManager coordinatorManager;
	@Autowired
	protected ReferenceManager referenceManager;
	
	private String baseUrl;
	private final boolean guestOnly;
	
	public RepositoryEntryDetailsController(UserRequest ureq, WindowControl wControl, RepositoryEntryRow row) {
		this(ureq, wControl);
		this.row = row;
		entry = repositoryService.loadByKey(row.getKey());
		initForm(ureq);
	}
	
	public RepositoryEntryDetailsController(UserRequest ureq, WindowControl wControl, RepositoryEntryRef ref) {
		this(ureq, wControl);
		entry = repositoryService.loadByKey(ref.getKey());
		initForm(ureq);
	}
	
	public RepositoryEntryDetailsController(UserRequest ureq, WindowControl wControl, RepositoryEntry entry) {
		this(ureq, wControl);
		this.entry = entry;
		initForm(ureq);
	}
	
	private RepositoryEntryDetailsController(UserRequest ureq, WindowControl wControl) {
		super(ureq, wControl, Util.getPackageVelocityRoot(RepositoryEntryDetailsController.class) + "/details.html");
		setTranslator(Util.createPackageTranslator(RepositoryService.class, getLocale(), getTranslator()));
		setTranslator(Util.createPackageTranslator(RestapiAdminController.class, getLocale(), getTranslator()));
		guestOnly = ureq.getUserSession().getRoles().isGuestOnly();

		OLATResourceable ores = OresHelper.createOLATResourceableType("MyCoursesSite");
		ThreadLocalUserActivityLogger.addLoggingResourceInfo(LoggingResourceable.wrapBusinessPath(ores));
	}
	
	public Integer getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}

	private void setText(String text, String key, FormLayoutContainer layoutCont) {
		if(!StringHelper.containsNonWhitespace(text)) return;
		text = StringHelper.xssScan(text);
		if(baseUrl != null) {
			text = FilterFactory.getBaseURLToMediaRelativeURLFilter(baseUrl).filter(text);
		}
		text = Formatter.formatLatexFormulas(text);
		layoutCont.contextPut(key, text);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		int cmpcount = 0;
		if(formLayout instanceof FormLayoutContainer) {
			FormLayoutContainer layoutCont = (FormLayoutContainer)formLayout;
			layoutCont.contextPut("v", entry);
			layoutCont.contextPut("guestOnly", new Boolean(guestOnly));
			String cssClass = RepositoyUIFactory.getIconCssClass(entry);
			layoutCont.contextPut("cssClass", cssClass);
			
			RepositoryHandler handler = RepositoryHandlerFactory.getInstance().getRepositoryHandler(entry);
			VFSContainer mediaContainer = handler.getMediaContainer(entry);
			if(mediaContainer != null) {
				baseUrl = registerMapper(ureq, new VFSContainerMapper(mediaContainer.getParentContainer()));
			}
			
			setText(entry.getDescription(), "description", layoutCont);
			setText(entry.getRequirements(), "requirements", layoutCont);
			setText(entry.getObjectives(), "objectives", layoutCont);
			setText(entry.getCredits(), "credits", layoutCont);

			//thumbnail and movie
			VFSLeaf movie = repositoryService.getIntroductionMovie(entry);
			VFSLeaf image = repositoryService.getIntroductionImage(entry);
			if(image != null || movie != null) {
				ImageComponent ic = new ImageComponent(ureq.getUserSession(), "thumbnail");
				if(movie != null) {
					ic.setMedia(movie);
					ic.setMaxWithAndHeightToFitWithin(500, 300);
					// add poster image
					if (image != null) {
						ic.setPoster(image);
					}
				} else {
					ic.setMedia(image);
					ic.setMaxWithAndHeightToFitWithin(500, 300);
				}
				layoutCont.put("thumbnail", ic);
			}
			
			//categories
			if(repositoryModule.isCatalogEnabled()) {
				List<CatalogEntry> categories = catalogManager.getCatalogEntriesReferencing(entry);
				List<String> categoriesLink = new ArrayList<>(categories.size());
				for(CatalogEntry category:categories) {
					String id = "cat_" + ++cmpcount;
					String title = category.getParent().getName();
					FormLink catLink = uifactory.addFormLink(id, "category", title, null, layoutCont, Link.LINK | Link.NONTRANSLATED);
					catLink.setIconLeftCSS("o_icon o_icon-fw o_icon_catalog");
					catLink.setUserObject(category.getKey());
					categoriesLink.add(id);
				}
				layoutCont.contextPut("categories", categoriesLink);
			}
			
			if(!guestOnly) {
				boolean marked;
				if(row == null) {
					marked = markManager.isMarked(entry, getIdentity(), null);
				} else {
					marked = row.isMarked();
				}
				markLink = uifactory.addFormLink("mark", "mark", marked ? "details.bookmark.remove" : "details.bookmark", null, layoutCont, Link.LINK);
				markLink.setElementCssClass("o_bookmark");
				markLink.setIconLeftCSS(marked ? Mark.MARK_CSS_LARGE : Mark.MARK_ADD_CSS_LARGE);
			}
			
			RepositoryEntryStatistics statistics = entry.getStatistics();
			if(repositoryModule.isRatingEnabled()) {
				Integer myRating;
				if(row == null) {
					myRating = userRatingsDao.getRatingValue(getIdentity(), entry, null);
				} else {
					myRating = row.getMyRating();
				}
				
				Double averageRating = statistics.getRating();
				long numOfRatings = statistics.getNumOfRatings();
				float ratingValue = myRating == null ? 0f : myRating.floatValue();
				float averageRatingValue = averageRating == null ? 0f : averageRating.floatValue();
				ratingEl = new RatingWithAverageFormItem("rating", ratingValue, averageRatingValue, 5, numOfRatings);
				ratingEl.setEnabled(!guestOnly);
				layoutCont.add("rating", ratingEl);
			}
			
			if(repositoryModule.isCommentEnabled()) {
				long numOfComments = statistics.getNumOfComments();
				String title = "(" + numOfComments + ")";
				commentsLink = uifactory.addFormLink("comments", "comments", title, null, layoutCont, Link.NONTRANSLATED);
				commentsLink.setCustomEnabledLinkCSS("o_comments");
				String css = numOfComments > 0 ? "o_icon o_icon_comments o_icon-lg" : "o_icon o_icon_comments_none o_icon-lg";
				commentsLink.setIconLeftCSS(css);
			}
			
			//load memberships
			List<String> memberRoles = repositoryService.getRoles(getIdentity(), entry);
            List<Long> authorKeys = repositoryService.getAuthors(entry);
            boolean isAuthor = false;
            boolean isMember = memberRoles.contains(GroupRoles.owner.name())
            		|| memberRoles.contains(GroupRoles.coach.name())
            		|| memberRoles.contains(GroupRoles.participant.name());
			if (isMember) {
				isAuthor = authorKeys.contains(getIdentity().getKey());
				layoutCont.contextPut("isEntryAuthor", new Boolean(isAuthor));
			}
			// push roles to velocity as well
            Roles roles = ureq.getUserSession().getRoles();
			layoutCont.contextPut("roles", roles);

			if(memberRoles.contains(GroupRoles.participant.name()) && repositoryService.isParticipantAllowedToLeave(entry)) {
				leaveLink = uifactory.addFormLink("sign.out", "leave", "sign.out", null, formLayout, Link.LINK);
				leaveLink.setIconLeftCSS("o_icon o_icon_sign_out");
			}

			//access control
			String accessI18n = null;
			List<PriceMethod> types = new ArrayList<PriceMethod>();
			if (entry.isMembersOnly()) {
				// members only
				if(isMember) {
					String linkText = translate("start.with.type", translate(entry.getOlatResource().getResourceableTypeName()));
					startLink = uifactory.addFormLink("start", "start", linkText, null, layoutCont, Link.BUTTON + Link.NONTRANSLATED);
					startLink.setElementCssClass("o_start btn-block");
					startLink.setIconRightCSS("o_icon o_icon_start o_icon-lg");
					startLink.setPrimary(true);

				}
				accessI18n = translate("cif.access.membersonly");
			} else {
				AccessResult acResult = acService.isAccessible(entry, getIdentity(), isMember, false);
				if(acResult.isAccessible()) {
					String linkText = translate("start.with.type", translate(entry.getOlatResource().getResourceableTypeName()));
					startLink = uifactory.addFormLink("start", "start", linkText, null, layoutCont, Link.BUTTON + Link.NONTRANSLATED);
					startLink.setElementCssClass("o_start btn-block");
				} else if (acResult.getAvailableMethods().size() > 0) {
					for(OfferAccess access:acResult.getAvailableMethods()) {
						AccessMethod method = access.getMethod();
						String type = (method.getMethodCssClass() + "_icon").intern();
						Price p = access.getOffer().getPrice();
						String price = p == null || p.isEmpty() ? "" : PriceFormat.fullFormat(p);
						AccessMethodHandler amh = acModule.getAccessMethodHandler(method.getType());
						String displayName = amh.getMethodName(getLocale());
						types.add(new PriceMethod(price, type, displayName));
					}
					String linkText = guestOnly ? translate("start.with.type", translate(entry.getOlatResource().getResourceableTypeName())) 
							: translate("book.with.type", translate(entry.getOlatResource().getResourceableTypeName()));
					startLink = uifactory.addFormLink("start", "start", linkText, null, layoutCont, Link.BUTTON + Link.NONTRANSLATED);
					startLink.setCustomEnabledLinkCSS("btn btn-success"); // custom style
					startLink.setElementCssClass("o_book btn-block");
					if(guestOnly) {
						if(entry.getAccess() == RepositoryEntry.ACC_USERS_GUESTS) {
							startLink.setVisible(true);
						} else {
							startLink.setVisible(false);
						}
					} else {
						startLink.setVisible(true);
					}
				} else {
					String linkText = translate("start.with.type", translate(entry.getOlatResource().getResourceableTypeName()));
					startLink = uifactory.addFormLink("start", "start", linkText, null, layoutCont, Link.BUTTON + Link.NONTRANSLATED);
					//startLink.setEnabled(false);
					startLink.setElementCssClass("o_start btn-block");
					startLink.setVisible(!guestOnly);
				}
				startLink.setIconRightCSS("o_icon o_icon_start o_icon-lg");
				startLink.setPrimary(true);
				
				switch (entry.getAccess()) {
					case 0: accessI18n = "ERROR";
						break;
					case 1: accessI18n = translate("cif.access.owners");			
						break;
					case 2: accessI18n = translate("cif.access.owners_authors");
						break;
					case 3: accessI18n = translate("cif.access.users");
						break;
					case 4: accessI18n = translate("cif.access.users_guests");
						break;
				}
			}
			layoutCont.contextPut("accessI18n", accessI18n);
			
			if(!types.isEmpty()) {
				layoutCont.contextPut("ac", types);
			}
			
			if(isMember) {
				//show the list of groups
				SearchBusinessGroupParams params = new SearchBusinessGroupParams(getIdentity(), true, true);
				List<BusinessGroup> groups = businessGroupService.findBusinessGroups(params, entry, 0, -1);
				List<String> groupLinkNames = new ArrayList<>(groups.size());
				for(BusinessGroup group:groups) {
					String groupLinkName = "grp_" + ++cmpcount;
					FormLink link = uifactory.addFormLink(groupLinkName, "group", group.getName(), null, layoutCont, Link.LINK | Link.NONTRANSLATED);
					link.setIconLeftCSS("o_icon o_icon-fw o_icon_group");
					link.setUserObject(group.getKey());
					groupLinkNames.add(groupLinkName);
				}
				layoutCont.contextPut("groups", groupLinkNames);
			}
			
			boolean passed = false;
			boolean failed = false;
			String score = null;
			if(row != null) {
				passed = row.isPassed();
				failed = row.isFailed();
				score = row.getScore();
			} else {
				UserEfficiencyStatement statement = effManager.getUserEfficiencyStatementLight(entry.getKey(), getIdentity());
				if(statement != null) {
					Boolean p = statement.getPassed();
					if(p != null) {
						passed = p.booleanValue();
						failed = !p.booleanValue();
					}
					
					Float scoreVal = statement.getScore();
					if(scoreVal != null) {
						score = AssessmentHelper.getRoundedScore(scoreVal);
					}
				}
			}
			layoutCont.contextPut("passed", passed);
			layoutCont.contextPut("failed", failed);
			layoutCont.contextPut("score", score);
			
			Long courseResId = entry.getOlatResource().getResourceableId();
			Date recentLaunch = userCourseInfosManager.getRecentLaunchDate(courseResId, getIdentity());
			layoutCont.contextPut("recentLaunch", recentLaunch);
			
			// show how many users are currently using this resource
            String numUsers;
            OLATResourceable ores = entry.getOlatResource();
            int cnt = 0;
            OLATResourceable courseRunOres = OresHelper.createOLATResourceableInstance(RunMainController.ORES_TYPE_COURSE_RUN, courseResId);
            if (ores != null) cnt = coordinatorManager.getCoordinator().getEventBus().getListeningIdentityCntFor(courseRunOres);
            numUsers = String.valueOf(cnt);
            layoutCont.contextPut("numUsers", numUsers);
            
            // Where is it in use
            if(isAuthor || roles.isOLATAdmin() || roles.isInstitutionalResourceManager()) {
	            String referenceDetails = referenceManager.getReferencesToSummary(entry.getOlatResource(), getLocale());
	            if (referenceDetails != null) {
	            	layoutCont.contextPut("referenceDetails", referenceDetails);
	            }
            }
            
            // Link to bookmark entry
            String url = Settings.getServerContextPathURI() + "/url/RepositoryEntry/" + entry.getKey();
            layoutCont.contextPut("extlink", url);
            Boolean guestAllowed = (entry.getAccess() >= RepositoryEntry.ACC_USERS_GUESTS && loginModule.isGuestLoginLinksEnabled())
            		? Boolean.TRUE : Boolean.FALSE;
            layoutCont.contextPut("isGuestAllowed", guestAllowed);

            //Owners
            List<String> authorLinkNames = new ArrayList<String>(authorKeys.size());
    		Map<Long,String> authorNames = userManager.getUserDisplayNamesByKey(authorKeys);
    		int counter = 0;
    		for(Map.Entry<Long, String> author:authorNames.entrySet()) {
    			Long authorKey = author.getKey();
    			String authorName = author.getValue();
    			
	    		FormLink authorLink = uifactory.addFormLink("owner-" + ++counter, "owner", authorName, null, formLayout, Link.NONTRANSLATED | Link.LINK);
	    		authorLink.setUserObject(authorKey);
	    		authorLinkNames.add(authorLink.getComponent().getComponentName());
    		}
    		layoutCont.contextPut("authorlinknames", authorLinkNames);
		}
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
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(commentsCtrl == source) {
			if(event == Event.CANCELLED_EVENT) {
				updateComments(commentsCtrl.getNumOfComments());
				cmc.deactivate();
				cleanUp();
			}
		} else if(cmc == source) {
			if(commentsCtrl != null) {
				updateComments(commentsCtrl.getNumOfComments());
			}
			cleanUp();
		} else if(leaveDialogBox == source) {
			if (DialogBoxUIFactory.isYesEvent(event) || DialogBoxUIFactory.isOkEvent(event)) {
				doLeave();
				fireEvent(ureq, new LeavingEvent());
			}
		}
		super.event(ureq, source, event);
	}
	
	private void updateComments(int numOfComments) {
		String title = "(" + numOfComments + ")";
		commentsLink.setI18nKey(title);
		String css = numOfComments > 0 ? "o_icon o_icon_comments o_icon-lg" : "o_icon o_icon_comments_none o_icon-lg";
		commentsLink.setIconLeftCSS(css);
	}
	
	private void cleanUp() {
		removeAsListenerAndDispose(commentsCtrl);
		removeAsListenerAndDispose(cmc);
		commentsCtrl = null;
		cmc = null;
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(source instanceof FormLink) {
			FormLink link = (FormLink)source;
			String cmd = link.getCmd();
			if("category".equals(cmd)) {
				Long categoryKey = (Long)link.getUserObject();
				doOpenCategory(ureq, categoryKey);
			} else if("mark".equals(cmd)) {
				boolean marked = doMark();
				markLink.setI18nKey(marked ? "details.bookmark.remove" : "details.bookmark");
				markLink.setIconLeftCSS(marked ? Mark.MARK_CSS_LARGE : Mark.MARK_ADD_CSS_LARGE);

			} else if("comments".equals(cmd)) {
				doOpenComments(ureq);
			} else if("start".equals(cmd)) {
				doStart(ureq);
			} else if("group".equals(cmd)) {
				Long groupKey = (Long)link.getUserObject();
				doOpenGroup(ureq, groupKey);
			} else if("owner".equals(cmd)) {
				Long ownerKey = (Long)link.getUserObject();
				doOpenVisitCard(ureq, ownerKey);
			} else if("leave".equals(cmd)) {
				doConfirmLeave(ureq);
			}
		} else if(ratingEl == source && event instanceof RatingFormEvent) {
			RatingFormEvent ratingEvent = (RatingFormEvent)event;
			doRating(ratingEvent.getRating());
		}
		super.formInnerEvent(ureq, source, event);
	}
	
	protected void doConfirmLeave(UserRequest ureq) {
		String reName = StringHelper.escapeHtml(entry.getDisplayname());
		String title = translate("sign.out");
		String text = translate("sign.out.dialog.text", reName);
		leaveDialogBox = activateYesNoDialog(ureq, title, text, leaveDialogBox);
	}
	
	protected void doLeave() {
		MailerResult result = new MailerResult();
		MailPackage reMailing = new MailPackage(result, getWindowControl().getBusinessControl().getAsString(), true);
		LeavingStatusList status = new LeavingStatusList();
		//leave course
		repositoryManager.leave(getIdentity(), entry, status, reMailing);
		//leave groups
		businessGroupService.leave(getIdentity(), entry, status, reMailing);
		DBFactory.getInstance().commit();//make sur all changes are committed
		
		if(status.isWarningManagedGroup() || status.isWarningManagedCourse()) {
			showWarning("sign.out.warning.managed");
		} else if(status.isWarningGroupWithMultipleResources()) {
			showWarning("sign.out.warning.mutiple.resources");
		} else {
			showInfo("sign.out.success", new String[]{ entry.getDisplayname() });
		}
	}
	
	protected void doStart(UserRequest ureq) {
		try {
			String businessPath = "[RepositoryEntry:" + entry.getKey() + "]";
			NewControllerFactory.getInstance().launch(businessPath, ureq, getWindowControl());
		} catch (CorruptedCourseException e) {
			logError("Course corrupted: " + entry.getKey() + " (" + entry.getOlatResource().getResourceableId() + ")", e);
			showError("cif.error.corrupted");
		}
	}
	
	protected void doOpenCategory(UserRequest ureq, Long categoryKey) {
		String businessPath = "[CatalogEntry:" + categoryKey + "]";
		NewControllerFactory.getInstance().launch(businessPath, ureq, getWindowControl());
	}
	
	protected void doOpenGroup(UserRequest ureq, Long groupKey) {
		String businessPath = "[BusinessGroup:" + groupKey + "]";
		NewControllerFactory.getInstance().launch(businessPath, ureq, getWindowControl());
	}
	
	protected void doOpenVisitCard(UserRequest ureq, Long ownerKey) {
		String businessPath = "[HomePage:" + ownerKey + "]";
		NewControllerFactory.getInstance().launch(businessPath, ureq, getWindowControl());
	}
	
	protected boolean doMark() {
		OLATResourceable item = OresHelper.clone(entry);
		if(markManager.isMarked(item, getIdentity(), null)) {
			markManager.removeMark(item, getIdentity(), null);
			return false;
		} else {
			String businessPath = "[RepositoryEntry:" + item.getResourceableId() + "]";
			markManager.setMark(item, getIdentity(), null, businessPath);
			return true;
		}
	}
	
	private void doRating(float rating) {
		userRatingsDao.updateRating(getIdentity(), entry, null, Math.round(rating));
	}
	
	protected void doOpenComments(UserRequest ureq) {
		if(commentsCtrl != null) return;
		
		boolean anonym = ureq.getUserSession().getRoles().isGuestOnly();
		CommentAndRatingSecurityCallback secCallback = new CommentAndRatingDefaultSecurityCallback(getIdentity(), false, anonym);
		OLATResourceable ores = OresHelper.createOLATResourceableInstance("RepositoryEntry", entry.getKey());
		commentsCtrl = new UserCommentsController(ureq, getWindowControl(), ores, null, secCallback);
		listenTo(commentsCtrl);
		cmc = new CloseableModalController(getWindowControl(), "close", commentsCtrl.getInitialComponent(), true, translate("comments"));
		listenTo(cmc);
		cmc.activate();
	}
}
