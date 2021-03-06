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
package org.olat.course.certificate;

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.olat.basesecurity.IdentityRef;
import org.olat.core.commons.services.notifications.PublisherData;
import org.olat.core.commons.services.notifications.SubscriptionContext;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.mail.MailerResult;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.course.ICourse;
import org.olat.course.certificate.model.CertificateInfos;
import org.olat.group.BusinessGroup;
import org.olat.repository.RepositoryEntry;
import org.olat.resource.OLATResource;

/**
 * 
 * Initial date: 20.10.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public interface CertificatesManager {

	public static final String ORES_CERTIFICATE =  OresHelper.calculateTypeName(CertificatesManager.class);
	public static final OLATResourceable ORES_CERTIFICATE_EVENT =  OresHelper.createOLATResourceableInstance("Certificate", 0l);
	
	public boolean isHTMLTemplateAllowed();
	
	//notifications
	public SubscriptionContext getSubscriptionContext(ICourse course);
	
	public PublisherData getPublisherData(ICourse course, String businessPath);
	
	public void markPublisherNews(Identity ident, ICourse course);
	
	//templates management
	public List<CertificateTemplate> getTemplates();
	
	/**
	 * Add a new template
	 * @param name The filename of the template
	 * @param file The file which is / or contains the template
	 * @param publicTemplate True if the tempalte is accessible system-wide
	 * @return
	 */
	public CertificateTemplate addTemplate(String name, File file, String format, String orientation, boolean publicTemplate);
	
	/**
	 * Update the template files
	 * @param template
	 * @param name
	 * @param file
	 * @return
	 */
	public CertificateTemplate updateTemplate(CertificateTemplate template, String name, File file, String format, String orientation);
	
	/**
	 * Delete the template in the file system and in the database
	 * @param template
	 */
	public void deleteTemplate(CertificateTemplate template);
	
	public CertificateTemplate getTemplateById(Long key);

	public File getTemplateFile(CertificateTemplate template);
	
	public VFSLeaf getTemplateLeaf(CertificateTemplate template);
	
	public InputStream getDefaultTemplate();
	
	//certificate
	public Certificate getCertificateById(Long key);
	
	public Certificate getCertificateByUuid(String uuid);
	
	public CertificateLight getCertificateLightById(Long key);
	
	public VFSLeaf getCertificateLeaf(Certificate certificate);
	
	/**
	 * Return the last certificates of the user.
	 * @param identity
	 * @return A list of certificates
	 */
	public List<CertificateLight> getLastCertificates(IdentityRef identity);
	
	/**
	 * Return the last certificates of all users f the specified course.
	 * @param resourceKey The resource primary key of the course.
	 * @return A list of certificates
	 */
	public List<CertificateLight> getLastCertificates(OLATResource resourceKey);
	
	/**
	 * Return the last certificates of all users and all courses linked
	 * to this group.
	 * @param businessGroup
	 * @return A list of certificates
	 */
	public List<CertificateLight> getLastCertificates(BusinessGroup businessGroup);
	
	public List<Certificate> getCertificatesForNotifications(Identity identity, RepositoryEntry entry, Date lastNews);

	
	public boolean hasCertificate(IdentityRef identity, Long resourceKey);
	
	public Certificate getLastCertificate(IdentityRef identity, Long resourceKey);
	
	public List<Certificate> getCertificates(IdentityRef identity, OLATResource resource);
	
	public boolean isRecertificationAllowed(Identity identity, RepositoryEntry entry);
	
	public File previewCertificate(CertificateTemplate template, RepositoryEntry entry, Locale locale);

	public Certificate uploadCertificate(Identity identity, Date creationDate, OLATResource resource, File certificateFile);
	
	public Certificate uploadStandaloneCertificate(Identity identity, Date creationDate, String courseTitle, Long resourceKey, File certificateFile);
	
	public void generateCertificates(List<CertificateInfos> identities, RepositoryEntry entry, CertificateTemplate template, MailerResult result);

	public Certificate generateCertificate(CertificateInfos identity, RepositoryEntry entry, CertificateTemplate template, MailerResult result);
	
	public void deleteCertificate(Certificate certificate);

}
