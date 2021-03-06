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
* <p>
*/
package org.olat.repository.handlers;

import java.io.File;
import java.util.Locale;
import java.util.Properties;

import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.fullWebApp.LayoutMain3ColsController;
import org.olat.core.commons.modules.glossary.GlossaryItemManager;
import org.olat.core.commons.modules.glossary.GlossaryMainController;
import org.olat.core.commons.modules.glossary.GlossaryRuntimeController;
import org.olat.core.commons.modules.glossary.GlossarySecurityCallback;
import org.olat.core.commons.modules.glossary.GlossarySecurityCallbackImpl;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.stack.TooledStackedPanel;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.layout.MainLayoutController;
import org.olat.core.gui.control.generic.wizard.StepsMainRunController;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.logging.AssertException;
import org.olat.core.util.FileUtils;
import org.olat.core.util.Util;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.LockResult;
import org.olat.core.util.resource.OLATResourceableJustBeforeDeletedEvent;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.course.assessment.AssessmentMode;
import org.olat.fileresource.FileResourceManager;
import org.olat.fileresource.types.FileResource;
import org.olat.fileresource.types.GlossaryResource;
import org.olat.fileresource.types.ResourceEvaluation;
import org.olat.modules.glossary.GlossaryManager;
import org.olat.repository.ErrorList;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryService;
import org.olat.repository.model.RepositoryEntrySecurity;
import org.olat.repository.ui.RepositoryEntryRuntimeController.RuntimeControllerCreator;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;
import org.olat.resource.references.ReferenceManager;


/**
 * Description:<br>
 * 
 * <P>
 * Initial Date: Dec 04 2006 <br>
 * @author Florian Gnägi, frentix GmbH, http://www.frentix.com
 */
public class GlossaryHandler implements RepositoryHandler {

	
	public static final String PROCESS_CREATENEW = "cn";
	public static final String PROCESS_UPLOAD = "pu";

	@Override
	public boolean isCreate() {
		return true;
	}
	
	@Override
	public RepositoryEntry createResource(Identity initialAuthor, String displayname, String description, Object createObject, Locale locale) {
		RepositoryService repositoryService = CoreSpringFactory.getImpl(RepositoryService.class);
		GlossaryResource glossaryResource = GlossaryManager.getInstance().createGlossary();
		OLATResource resource = OLATResourceManager.getInstance().findOrPersistResourceable(glossaryResource);
		RepositoryEntry re = repositoryService.create(initialAuthor, null, "", displayname, description, resource, RepositoryEntry.ACC_OWNERS);
		DBFactory.getInstance().commit();
		return re;
	}
	
	@Override
	public String getCreateLabelI18nKey() {
		return "new.glossary";
	}
	
	@Override
	public boolean isPostCreateWizardAvailable() {
		return false;
	}

	@Override
	public ResourceEvaluation acceptImport(File file, String filename) {
		return GlossaryResource.evaluate(file, filename);
	}
	
	@Override
	public RepositoryEntry importResource(Identity initialAuthor, String initialAuthorAlt, String displayname, String description,
			boolean withReferences, Locale locale, File file, String filename) {
		RepositoryService repositoryService = CoreSpringFactory.getImpl(RepositoryService.class);
		GlossaryResource glossaryResource = GlossaryManager.getInstance().createGlossary();
		OLATResource resource = OLATResourceManager.getInstance().findOrPersistResourceable(glossaryResource);
		//copy resources
		File glossyPath = GlossaryManager.getInstance().getGlossaryRootFolder(glossaryResource).getBasefile();
		FileResource.copyResource(file, filename, glossyPath);
		RepositoryEntry re = repositoryService.create(initialAuthor, null, "", displayname, description, resource, RepositoryEntry.ACC_OWNERS);
		DBFactory.getInstance().commit();
		return re;
	}
	
	@Override
	public RepositoryEntry copy(RepositoryEntry source, RepositoryEntry target) {
		OLATResource sourceResource = source.getOlatResource();
		OLATResource targetResource = target.getOlatResource();
		File sourceFileroot = FileResourceManager.getInstance().getFileResourceRootImpl(sourceResource).getBasefile();
		File targetFileroot = FileResourceManager.getInstance().getFileResourceRootImpl(targetResource).getBasefile();
		FileUtils.copyDirContentsToDir(sourceFileroot, targetFileroot, false, "copy");
		return target;
	}

	@Override
	public String getSupportedType() {
		return GlossaryResource.TYPE_NAME;
	}

	@Override
	public boolean supportsDownload() {
		return true;
	}

	@Override
	public EditionSupport supportsEdit(OLATResourceable resource) {
		return EditionSupport.embedded;
	}
	
	@Override
	public VFSContainer getMediaContainer(RepositoryEntry repoEntry) {
		return FileResourceManager.getInstance()
				.getFileResourceMedia(repoEntry.getOlatResource());
	}

	@Override
	public StepsMainRunController createWizardController(OLATResourceable res, UserRequest ureq, WindowControl wControl) {
		throw new AssertException("Trying to get wizard where no creation wizard is provided for this type.");
	}

	/**
	 * @param ureq
	 * @param wControl
	 * @param res
	 * @param initialViewIdentifier
	 * @return Controller
	 */
	@Override
	public MainLayoutController createLaunchController(RepositoryEntry re, RepositoryEntrySecurity reSecurity, UserRequest ureq, WindowControl wControl) {
		return new GlossaryRuntimeController(ureq, wControl, re, reSecurity,
			new RuntimeControllerCreator() {
				@Override
				public Controller create(UserRequest uureq, WindowControl wwControl, TooledStackedPanel toolbarPanel,
						RepositoryEntry entry, RepositoryEntrySecurity security, AssessmentMode assessmentMode) {
					VFSContainer glossaryFolder = GlossaryManager.getInstance().getGlossaryRootFolder(entry.getOlatResource());

					Properties glossProps = GlossaryItemManager.getInstance().getGlossaryConfig(glossaryFolder);
					boolean editableByUser = "true".equals(glossProps.getProperty(GlossaryItemManager.EDIT_USERS));
					boolean owner = security.isOwner();
					
					GlossarySecurityCallback secCallback;
					if (uureq.getUserSession().getRoles().isGuestOnly()) {
						secCallback = new GlossarySecurityCallbackImpl();				
					} else {
						secCallback = new GlossarySecurityCallbackImpl(false, owner, editableByUser, uureq.getIdentity().getKey());
					}
					return new GlossaryMainController(wwControl, uureq, glossaryFolder, entry.getOlatResource(), secCallback, false);	
				}
			});
	}

	@Override
	public MediaResource getAsMediaResource(OLATResourceable res, boolean backwardsCompatible) {
		return GlossaryManager.getInstance().getAsMediaResource(res);
	}

	@Override
	public Controller createEditorController(RepositoryEntry re, UserRequest ureq, WindowControl wControl, TooledStackedPanel toolbar) {
		VFSContainer glossaryFolder = GlossaryManager.getInstance().getGlossaryRootFolder(re.getOlatResource());

		Properties glossProps = GlossaryItemManager.getInstance().getGlossaryConfig(glossaryFolder);
		boolean editableByUser = "true".equals(glossProps.getProperty(GlossaryItemManager.EDIT_USERS));
		GlossarySecurityCallback secCallback;
		if (ureq.getUserSession().getRoles().isGuestOnly()) {
			secCallback = new GlossarySecurityCallbackImpl();				
		} else {
			secCallback = new GlossarySecurityCallbackImpl(true, true, editableByUser, ureq.getIdentity().getKey());
		}
		GlossaryMainController gctr = new GlossaryMainController(wControl, ureq, glossaryFolder, re.getOlatResource(), secCallback, false);
		// use on column layout
		LayoutMain3ColsController layoutCtr = new LayoutMain3ColsController(ureq, wControl, gctr);
		layoutCtr.addDisposableChildController(gctr); // dispose content on layout dispose
		return layoutCtr;
	}

	@Override
	public boolean cleanupOnDelete(OLATResourceable res) {
		// FIXME fg
		// do not need to notify all current users of this resource, since the only
		// way to access this resource
		// FIXME:fj:c to be perfect, still need to notify
		// repositorydetailscontroller and searchresultcontroller....
		CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(new OLATResourceableJustBeforeDeletedEvent(res), res);
		GlossaryManager.getInstance().deleteGlossary(res);
		return true;
	}

	@Override
	public boolean readyToDelete(OLATResourceable res, Identity identity, Roles roles, Locale locale, ErrorList errors) {
		ReferenceManager refM = ReferenceManager.getInstance();
		String referencesSummary = refM.getReferencesToSummary(res, locale);
		if (referencesSummary != null) {
			Translator translator = Util.createPackageTranslator(RepositoryManager.class, locale);
			errors.setError(translator.translate("details.delete.error.references", new String[] { referencesSummary }));
			return false;
		}
		return true;
	}

	@Override
	public String archive(Identity archiveOnBehalfOf, String archivFilePath, RepositoryEntry repoEntry) {
		return GlossaryManager.getInstance().archive(archivFilePath, repoEntry);
	}

	@Override
	public LockResult acquireLock(OLATResourceable ores, Identity identity) {
    //nothing to do
		return null;
	}

	@Override
	public void releaseLock(LockResult lockResult) {
		//nothing to do since nothing locked
	}

	@Override
	public boolean isLocked(OLATResourceable ores) {
		return false;
	}
}
