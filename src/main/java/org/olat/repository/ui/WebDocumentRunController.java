package org.olat.repository.ui;

import javax.servlet.http.HttpServletRequest;

import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.services.image.Size;
import org.olat.core.commons.services.video.MovieService;
import org.olat.core.dispatcher.mapper.Mapper;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.iframe.IFrameDisplayController;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.gui.util.CSSHelper;
import org.olat.core.helpers.Settings;
import org.olat.core.util.FileUtils;
import org.olat.core.util.vfs.LocalFileImpl;
import org.olat.core.util.vfs.LocalImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSMediaResource;
import org.olat.fileresource.FileResourceManager;
import org.olat.repository.RepositoryEntry;
import org.olat.resource.OLATResource;

/**
 * 
 * Initial date: 02.12.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class WebDocumentRunController extends BasicController {

	public WebDocumentRunController(UserRequest ureq, WindowControl wControl, RepositoryEntry entry) {
		super(ureq, wControl);

		VelocityContainer mainVC = createVelocityContainer("web_content");
		mainVC.contextPut("displayName", entry.getDisplayname());
		putInitialPanel(mainVC);

		LocalFileImpl document = getWebDocument(entry);
		if(document != null) {
			String filename = document.getName();
			mainVC.contextPut("filename", filename);
			String lowerFilename = filename.toLowerCase();
			String cssClass = CSSHelper.createFiletypeIconCssClassFor(lowerFilename);
			mainVC.contextPut("cssClass", cssClass);
			
			String extension = FileUtils.getFileSuffix(filename);
			if("png".equals(extension) || "jpg".equals(extension) || "jpeg".equals(extension) || "gif".equals(extension)) {
				String mediaUrl = registerMapper(ureq, new MediaMapper(document));
				mainVC.contextPut("image", filename);
				mainVC.contextPut("mediaUrl", mediaUrl);
			} else if("mp4".equals(extension) || "m4v".equals(extension) || "mov".equals(extension)) {
				String mediaUrl = registerMapper(ureq, new MediaMapper(document));
				mainVC.contextPut("movie", filename);
				mainVC.contextPut("mediaUrl", Settings.createServerURI() + mediaUrl);
				Size realSize = CoreSpringFactory.getImpl(MovieService.class).getSize(document, extension);
				if(realSize != null) {
					mainVC.contextPut("height", realSize.getHeight());
					mainVC.contextPut("width", realSize.getWidth());
				} else {
					mainVC.contextPut("height", 480);
					mainVC.contextPut("width", 640);
				}
			} else {
				IFrameDisplayController idc = new IFrameDisplayController(ureq, getWindowControl(), document.getParentContainer(), null, null);
				listenTo(idc);	
				idc.setCurrentURI(document.getName());
				mainVC.put("content", idc.getInitialComponent());
			}
		}
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		//
	}
	
	private LocalFileImpl getWebDocument(RepositoryEntry entry) {
		OLATResource resource = entry.getOlatResource();
		VFSContainer fResourceFileroot = FileResourceManager.getInstance()
				.getFileResourceRootImpl(resource);
		
		LocalFileImpl document = null;
		for(VFSItem item:fResourceFileroot.getItems()) {
			if(item instanceof VFSLeaf && item instanceof LocalImpl) {
				LocalFileImpl localItem = (LocalFileImpl)item;
				if(localItem != null && !localItem.getBasefile().isHidden()) {
					document = (LocalFileImpl)item;
				}
			}	
		}
		return document;
	}
	
	private static class MediaMapper implements Mapper {
		
		private final VFSLeaf mediaFile;
		
		public MediaMapper(VFSLeaf mediaFile) {
			this.mediaFile = mediaFile;
		}

		@Override
		public MediaResource handle(String relPath, HttpServletRequest request) {
			return new VFSMediaResource(mediaFile);
		}
	}
}
