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

package org.olat.repository.handlers;

import java.io.File;
import java.util.Locale;

import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.util.Util;
import org.olat.core.util.ZipUtil;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.resource.OLATResourceableJustBeforeDeletedEvent;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.fileresource.FileResourceManager;
import org.olat.repository.ErrorList;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.resource.references.ReferenceManager;

/**
 * Common super class for all file-based handlers.
 *
 * @author Christian Guretzki
 */
public abstract class FileHandler implements RepositoryHandler {

	public FileHandler() {
		//
	}

	@Override
	public VFSContainer getMediaContainer(RepositoryEntry repoEntry) {
		return FileResourceManager.getInstance()
				.getFileResourceMedia(repoEntry.getOlatResource());
	}

	@Override
	public boolean cleanupOnDelete(OLATResourceable res) {
		// notify all current users of this resource (content packaging file resource) that it will be deleted now.
		CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(new OLATResourceableJustBeforeDeletedEvent(res), res);
		FileResourceManager.getInstance().deleteFileResource(res);
		return true;
	}

	@Override
	public boolean readyToDelete(OLATResourceable res, Identity identity, Roles roles, Locale locale, ErrorList errors) {
		String referencesSummary = ReferenceManager.getInstance().getReferencesToSummary(res, locale);
		if (referencesSummary != null) {
			Translator translator = Util.createPackageTranslator(RepositoryManager.class, locale);
			errors.setError(translator.translate("details.delete.error.references",
					new String[] { referencesSummary }));
			return false;
		}
		return true;
	}

	@Override
	public String archive(Identity archiveOnBehalfOf, String archivFilePath, RepositoryEntry repoEntry) {
		String exportFileName = getDeletedFilePrefix() + repoEntry.getOlatResource().getResourceableId() + ".zip";
		String fullFilePath = archivFilePath + File.separator + exportFileName;
		File rootFile = FileResourceManager.getInstance().getFileResourceRoot(repoEntry.getOlatResource());
		ZipUtil.zipAll(rootFile, new File(fullFilePath), false);
		return exportFileName;
	}
	
	protected abstract String getDeletedFilePrefix();
}
