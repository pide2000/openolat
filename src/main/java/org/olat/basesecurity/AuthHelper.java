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

package org.olat.basesecurity;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.olat.basesecurity.manager.GroupDAO;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.fullWebApp.BaseFullWebappController;
import org.olat.core.commons.fullWebApp.BaseFullWebappControllerParts;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.dispatcher.DispatcherModule;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.WindowManager;
import org.olat.core.gui.Windows;
import org.olat.core.gui.components.Window;
import org.olat.core.gui.control.ChiefController;
import org.olat.core.gui.media.RedirectMediaResource;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.render.URLBuilder;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.logging.activity.OlatLoggingAction;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.SessionInfo;
import org.olat.core.util.UserSession;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.i18n.I18nManager;
import org.olat.core.util.i18n.I18nModule;
import org.olat.core.util.session.UserSessionManager;
import org.olat.course.assessment.AssessmentMode;
import org.olat.course.assessment.AssessmentModeManager;
import org.olat.course.assessment.model.TransientAssessmentMode;
import org.olat.login.AuthBFWCParts;
import org.olat.login.GuestBFWCParts;
import org.olat.portfolio.manager.InvitationDAO;
import org.olat.user.UserManager;
import org.olat.util.logging.activity.LoggingResourceable;

/**
 * Description: <br>
 * 
 * @author Felix Jost
 */
public class AuthHelper {
	/**
	 * <code>LOGOUT_PAGE</code>
	 */
	public  static final int LOGIN_OK = 0;
	private static final int LOGIN_FAILED = 1;
	private static final int LOGIN_DENIED = 2;
	public  static final int LOGIN_NOTAVAILABLE = 3;

	private static final int MAX_SESSION_NO_LIMIT = 0;

	
	/** whether or not requests to dmz (except those coming via 'switch-to-node' cluster feature) are 
	 * rejected hence resulting the browser to go to another node.
	 * Note: this is not configurable currently as it's more of a runtime choice to change this to true
	 */
	private static boolean rejectDMZRequests = false;

	private static boolean loginBlocked = false;
	private static int maxSessions = MAX_SESSION_NO_LIMIT;
	
	private static OLog log = Tracing.createLoggerFor(AuthHelper.class);

	/**
	 * Used by DMZDispatcher to do regular logins and by ShibbolethDispatcher
	 * which is somewhat special because logins are handled asynchronuous ->
	 * therefore a dedicated dispatcher is needed which also has to have access to
	 * the doLogin() method.
	 * 
	 * @param identity
	 * @param authProvider
	 * @param ureq
	 * @return True if success, false otherwise.
	 */
	public static int doLogin(Identity identity, String authProvider, UserRequest ureq) {
		int initializeStatus = initializeLogin(identity, authProvider, ureq, false);
		if (initializeStatus != LOGIN_OK) { 
			return initializeStatus; // login not successfull
		}
		
		// do logging
		ThreadLocalUserActivityLogger.log(OlatLoggingAction.OLAT_LOGIN, AuthHelper.class, LoggingResourceable.wrap(identity));

		// brasato:: fix it 
		// successfull login, reregister window
		ChiefController occ;
		if(ureq.getUserSession().getRoles().isGuestOnly()){
			occ = createGuestHome(ureq);
		}else{
			occ = createAuthHome(ureq);
		}
	
		Window currentWindow = occ.getWindow();
		currentWindow.setUriPrefix(WebappHelper.getServletContextPath() + DispatcherModule.PATH_AUTHENTICATED);
		Windows.getWindows(ureq).registerWindow(currentWindow);
	
		// redirect to AuthenticatedDispatcher
		// IMPORTANT: windowID has changed due to re-registering current window -> do not use ureq.getWindowID() to build new URLBuilder.
		URLBuilder ubu = new URLBuilder(WebappHelper.getServletContextPath() + DispatcherModule.PATH_AUTHENTICATED, currentWindow.getInstanceId(), "1", null);	
		StringOutput sout = new StringOutput(30);
		ubu.buildURI(sout, null, null);
		ureq.getDispatchResult().setResultingMediaResource(new RedirectMediaResource(sout.toString()));
				
		return LOGIN_OK;
	}
	
	/**
	 * 
	 * @param identity
	 * @param authProvider
	 * @param ureq
	 * @param Is login via REST API?
	 * @return
	 */
	public static int doHeadlessLogin(Identity identity, String authProvider, UserRequest ureq, boolean rest) {
		int initializeStatus = initializeLogin(identity, authProvider, ureq, rest);
		if (initializeStatus != LOGIN_OK) { 
			return initializeStatus; // login not successful
		}
		// Set session info to reflect the REST headless login 
		UserSession usess = ureq.getUserSession();
		usess.getSessionInfo().setREST(true);
		usess.getIdentityEnvironment().getAttributes().put("isrest", "true");
		//
		ThreadLocalUserActivityLogger.log(OlatLoggingAction.OLAT_LOGIN, AuthHelper.class, LoggingResourceable.wrap(identity));
		return LOGIN_OK;
	}

	/**
	 * Create a base chief controller for the current anonymous user request
	 * and initialize the first screen after login. Note, the user request 
	 * must be authenticated, but as an anonymous user and not a known user.
	 * 
	 * @param ureq The authenticated user request. 
	 * @return The chief controller
	 */
	private static ChiefController createGuestHome(UserRequest ureq) {
		if (!ureq.getUserSession().isAuthenticated()) throw new AssertException("not authenticated!");
		
		BaseFullWebappControllerParts guestSitesAndNav = new GuestBFWCParts();
		ChiefController cc = new BaseFullWebappController(ureq, guestSitesAndNav);
		Windows.getWindows(ureq.getUserSession()).setChiefController(cc);
		log.debug("set session-attribute 'AUTHCHIEFCONTROLLER'");
		return cc;
	}

	/**
	 * Create a base chief controller for the current authenticated user request
	 * and initialize the first screen after login.
	 * 
	 * @param ureq The authenticated user request. 
	 * @return The chief controller
	 */
	public static ChiefController createAuthHome(UserRequest ureq) {
		if (!ureq.getUserSession().isAuthenticated()) throw new AssertException("not authenticated!");
		
		BaseFullWebappControllerParts authSitesAndNav = new AuthBFWCParts();
		ChiefController cc = new BaseFullWebappController(ureq, authSitesAndNav);
		Windows.getWindows(ureq.getUserSession()).setChiefController(cc);
		log.debug("set session-attribute 'AUTHCHIEFCONTROLLER'");
		return cc;
	}

	/**
	 * Logs in as anonymous user using the given language key. If the current
	 * installation does not support this language, the systems default language
	 * is used instead
	 * 
	 * @param ureq The user request
	 * @param lang The language of the anonymous user or null if system default should be used
	 * @return true if login was successful, false otherwise
	 */
	public static int doAnonymousLogin(UserRequest ureq, Locale locale) {
		Collection<String> supportedLanguages = I18nModule.getEnabledLanguageKeys();
		if ( locale == null || ! supportedLanguages.contains(locale.toString()) ) {
			locale = I18nModule.getDefaultLocale();
		} 
		Identity guestIdent = BaseSecurityManager.getInstance().getAndUpdateAnonymousUserForLanguage(locale);
		int loginStatus = doLogin(guestIdent, BaseSecurityModule.getDefaultAuthProviderIdentifier(), ureq);
		return loginStatus;
	}
	
	public static int doInvitationLogin(String invitationToken, UserRequest ureq, Locale locale) {
		InvitationDAO invitationDao = CoreSpringFactory.getImpl(InvitationDAO.class);
		boolean hasPolicies = invitationDao.hasInvitations(invitationToken, new Date());
		if(!hasPolicies) {
			return LOGIN_DENIED;
		}
		
		UserManager um = UserManager.getInstance();
		BaseSecurity securityManager = BaseSecurityManager.getInstance();
		GroupDAO groupDao = CoreSpringFactory.getImpl(GroupDAO.class);
		Invitation invitation = invitationDao.findInvitation(invitationToken);
		if(invitation == null) {
			return LOGIN_DENIED;
		}
		
		//check if identity exists
		Identity identity = um.findIdentityByEmail(invitation.getMail());
		if(identity != null) {
			SecurityGroup allUsers = securityManager.findSecurityGroupByName(Constants.GROUP_OLATUSERS);
			if(securityManager.isIdentityInSecurityGroup(identity, allUsers)) {
				//already a normal olat user, cannot be invited
				return LOGIN_DENIED;
			} else {
				//fxdiff FXOLAT-151: add eventually the identity to the security group
				if(!groupDao.hasRole(invitation.getBaseGroup(), identity, GroupRoles.invitee.name())) {
					groupDao.addMembership(invitation.getBaseGroup(), identity, GroupRoles.invitee.name());
					DBFactory.getInstance().commit();
				}

				int result = doLogin(identity, BaseSecurityModule.getDefaultAuthProviderIdentifier(), ureq);
				//fxdiff FXOLAT-151: double check: problem with the DB, invitee is not marked has such
				if(ureq.getUserSession().getRoles().isInvitee()) {
					return result;
				}
				return LOGIN_DENIED;
			}
		}
		
		Collection<String> supportedLanguages = I18nModule.getEnabledLanguageKeys();
		if ( locale == null || ! supportedLanguages.contains(locale.toString()) ) {
			locale = I18nModule.getDefaultLocale();
		} 
		
		//invitation ok -> create a temporary user
		Identity invitee = invitationDao.createIdentityFrom(invitation, locale);
		return doLogin(invitee, BaseSecurityModule.getDefaultAuthProviderIdentifier(), ureq);
	}

	/**
	 * ONLY for authentication provider OLAT Authenticate Identity and do the
	 * necessary work. Returns true if successfull, false otherwise.
	 * 
	 * @param identity
	 * @param authProvider
	 * @param ureq
	 * @return boolean
	 */
	private static int initializeLogin(Identity identity, String authProvider, UserRequest ureq, boolean rest) {
		// continue only if user has login permission.
		if (identity == null) return LOGIN_FAILED;
		//test if a user may not logon, since he/she is in the PERMISSION_LOGON
		if (!BaseSecurityManager.getInstance().isIdentityVisible(identity.getName())) {
			log.audit("was denied login");
			return LOGIN_DENIED;			
		}
		UserSessionManager sessionManager = CoreSpringFactory.getImpl(UserSessionManager.class);
		// if the user sending the cookie did not log out and we are logging in
		// again, then we need to make sure everything is cleaned up. we cleanup in all cases.
		UserSession usess = ureq.getUserSession();
		// prepare for a new user: clear all the instance vars of the userSession
		// note: does not invalidate the session, since it is reused
		sessionManager.signOffAndClear(usess);
		// init the UserSession for the new User
		// we can set the identity and finish the log in process
		usess.setIdentity(identity);
		setRolesFor(identity, usess);

		// check if loginDenied or maxSession (only for non-admin)
		if ( (loginBlocked && !usess.getRoles().isOLATAdmin())
				|| ( ((maxSessions != MAX_SESSION_NO_LIMIT) && (sessionManager.getUserSessionsCnt() >= maxSessions)) && !usess.getRoles().isOLATAdmin() ) ) {
			log.audit("Login was blocked for username=" + usess.getIdentity().getName() + ", loginBlocked=" + loginBlocked + " NbrOfSessions=" + sessionManager.getUserSessionsCnt());
			sessionManager.signOffAndClear(usess);
			return LOGIN_NOTAVAILABLE;
		}
		
		//need to block the all things for assessment?
		if(usess.getRoles() != null && usess.getRoles().isOLATAdmin()) {
			usess.setAssessmentModes(Collections.<TransientAssessmentMode>emptyList());
		} else {
			AssessmentModeManager assessmentManager = CoreSpringFactory.getImpl(AssessmentModeManager.class);
			List<AssessmentMode> modes = assessmentManager.getAssessmentModeFor(identity);
			if(modes.isEmpty()) {
				usess.setAssessmentModes(Collections.<TransientAssessmentMode>emptyList());
			} else {
				usess.setAssessmentModes(TransientAssessmentMode.create(modes));
			}
		}
		
		//set the language
		usess.setLocale( I18nManager.getInstance().getLocaleOrDefault(identity.getUser().getPreferences().getLanguage()) );
		// update fontsize in users session globalsettings
		Windows.getWindows(ureq).getWindowManager().setFontSize(Integer.parseInt(identity.getUser().getPreferences().getFontsize() ));		
		// calculate session info and attach it to the user session
		setSessionInfoFor(identity, authProvider, ureq, rest);
		//confirm signedOn
		sessionManager.signOn(usess);
		// set users web delivery mode
		setAjaxModeFor(ureq);
		// update web delivery mode in session info
		usess.getSessionInfo().setWebModeFromUreq(ureq);
		return LOGIN_OK;
	}

	/**
	 * Persists the given user and creates an identity for it
	 * 
	 * @param loginName
	 * @param pwd null: no OLAT authentication is generated. If not null, the password will be 
	 * encrypted and and an OLAT authentication is generated.
	 * @param newUser unpersisted user
	 * @return Identity
	 */
	private static Identity createAndPersistIdentityAndUser(String loginName, String externalId, String pwd, User newUser) {
		Identity ident = null;
		if (pwd == null) {
			// when no password is used the provider must be set to null to not generate
			// an OLAT authentication token. See method doku.
			ident = BaseSecurityManager.getInstance().createAndPersistIdentityAndUser(loginName, externalId, newUser, null, null);
 		} else {
			ident = BaseSecurityManager.getInstance().createAndPersistIdentityAndUser(loginName, externalId, newUser,
			BaseSecurityModule.getDefaultAuthProviderIdentifier(), loginName, pwd);
		}
		// TODO: Tracing message
		return ident;
	}

	/**
	 * Persists the given user, creates an identity for it and adds the user to
	 * the users system group
	 * 
	 * @param loginName
	 * @param pwd null: no OLAT authentication is generated. If not null, the password will be 
	 * encrypted and and an OLAT authentication is generated.
	 * @param newUser unpersisted users
	 * @return Identity
	 */
	public static Identity createAndPersistIdentityAndUserWithUserGroup(String loginName, String externalId, String pwd,  User newUser) {
		Identity ident = createAndPersistIdentityAndUser(loginName, externalId, pwd, newUser);
		// Add user to system users group
		BaseSecurity securityManager = BaseSecurityManager.getInstance();
		SecurityGroup olatuserGroup = securityManager.findSecurityGroupByName(Constants.GROUP_OLATUSERS);
		securityManager.addIdentityToSecurityGroup(ident, olatuserGroup);
		return ident;
	}
	
	/**
	 * Persists the given user, creates an identity for it and adds the user to
	 * the users system group, create an authentication for an external provider
	 * 
	 * @param loginName
	 * @param provider
	 * @param authusername
	 * @param newUser
	 * @return
	 */
	public static Identity createAndPersistIdentityAndUserWithUserGroup(String loginName, String externalId, String provider, String authusername, User newUser) {
		BaseSecurity securityManager = BaseSecurityManager.getInstance();
		Identity ident = securityManager.createAndPersistIdentityAndUser(loginName, externalId, newUser, provider, authusername, null);
		// Add user to system users group
		SecurityGroup olatuserGroup = securityManager.findSecurityGroupByName(Constants.GROUP_OLATUSERS);
		securityManager.addIdentityToSecurityGroup(ident, olatuserGroup);
		return ident;
	}

	/**
	 * This is a convenience method to log out. IMPORTANT: This method initiates a
	 * redirect and RETURN. Make sure you return the call hierarchy gracefully.
	 * Most of all, don't touch HttpServletRequest or the Session after you call
	 * this method.
	 * 
	 * @param ureq
	 */
	public static void doLogout(UserRequest ureq) {
		if(ureq == null) return;

		boolean wasGuest = false;
		UserSession usess = ureq.getUserSession();
		if(usess != null && usess.getRoles() != null) {
			wasGuest = ureq.getUserSession().getRoles().isGuestOnly();
		}
		
		String lang = I18nManager.getInstance().getLocaleKey(ureq.getLocale());
		HttpSession session = ureq.getHttpReq().getSession(false);
		// next line fires a valueunbound event to UserSession, which does some
		// stuff on logout
		if (session != null) {
			try{
				session.invalidate();
				deleteShibsessionCookie(ureq);
			} catch(IllegalStateException ise) {
				// thrown when session already invalidated. fine. ignore.
			}
		}

		// redirect to logout page in dmz realm, set info that DMZ is shown because of logout
		// if it was a guest user, do not set logout=true. The parameter must be evaluated
		// by the implementation of the AuthenticationProvider.
		String setWarning = wasGuest ? "" : "&logout=true";
		ureq.getDispatchResult().setResultingMediaResource(
				new RedirectMediaResource(WebappHelper.getServletContextPath() + "/dmz/?lang=" + lang + setWarning));
	}

	private static void deleteShibsessionCookie(UserRequest ureq) {
    //	try to delete the "shibsession" cookie for this ureq, if any found
		Cookie[] cookies = ureq.getHttpReq().getCookies();
		Cookie cookie = null;
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {						
				/*if(log.isDebug()) {
					log.info("found cookie with name: " + cookies[i].getName() + " and value: " + cookies[i].getValue());
				}*/
				if (cookies[i].getName().indexOf("shibsession")!=-1) { //contains "shibsession"
					cookie = cookies[i];
					break;
				}
			}
		}
		if(cookie!=null) {
			//A zero value causes the cookie to be deleted.
			cookie.setMaxAge(0);
			//cookie.setMaxAge(-1); //TODO: LD: check this out as well
			cookie.setPath("/");
			ureq.getHttpResp().addCookie(cookie);					
			if(log.isDebug()) {
				log.info("AuthHelper - shibsession cookie deleted");
			}					
		}
	}

	/**
	 * Set AJAX / Web 2.0 based on User GUI-Preferences and configuration.
	 * If the "ajax feature" checkbox in the user settings is enabled, turn on ajax (do not care about which browser)
	 * @param ureq
	 */
	private static void setAjaxModeFor(UserRequest ureq) {
		Boolean ajaxOn = (Boolean) ureq.getUserSession().getGuiPreferences().get(WindowManager.class, "ajax-beta-on");
		//if user does not have an gui preference it will be only enabled if globally on and browser is capable
		if (ajaxOn != null) {
			Windows.getWindows(ureq).getWindowManager().setAjaxEnabled(ajaxOn.booleanValue());
		} else {			
			// enable ajax if olat configured and browser matching
			Windows.getWindows(ureq).getWindowManager().setAjaxWanted(ureq, true);
		}		
	}

	/**
	 * Build session info
	 * @param identity
	 * @param authProvider
	 * @param ureq
	 */
	public static void setSessionInfoFor(Identity identity, String authProvider, UserRequest ureq, boolean rest) {
		HttpSession session = ureq.getHttpReq().getSession();
		SessionInfo sinfo = new SessionInfo(identity.getKey(), identity.getName(), session);
		sinfo.setFirstname(identity.getUser().getProperty(UserConstants.FIRSTNAME, ureq.getLocale()));
		sinfo.setLastname(identity.getUser().getProperty(UserConstants.LASTNAME, ureq.getLocale()));
		sinfo.setFromIP(ureq.getHttpReq().getRemoteAddr());
		sinfo.setFromFQN(ureq.getHttpReq().getRemoteAddr());
		try {
			InetAddress[] iaddr = InetAddress.getAllByName(ureq.getHttpReq().getRemoteAddr());
			if (iaddr.length > 0) sinfo.setFromFQN(iaddr[0].getHostName());
		} catch (UnknownHostException e) {
			//       ok, already set IP as FQDN
		}
		sinfo.setAuthProvider(authProvider);
		sinfo.setUserAgent(ureq.getHttpReq().getHeader("User-Agent"));
		sinfo.setSecure(ureq.getHttpReq().isSecure());
		sinfo.setLastClickTime();
		sinfo.setREST(rest);
		// set session info for this session
		UserSession usess = ureq.getUserSession();
		usess.setSessionInfo(sinfo);
		// For Usertracking, let the User object know about some desired/specified infos from the sessioninfo
		Map<String,String> sessionInfoForUsertracking = new HashMap<String, String>();
		sessionInfoForUsertracking.put("language", usess.getLocale().toString());
		sessionInfoForUsertracking.put("authprovider", authProvider);
		sessionInfoForUsertracking.put("iswebdav", String.valueOf(sinfo.isWebDAV()));
		sessionInfoForUsertracking.put("isrest", String.valueOf(sinfo.isREST()));
		usess.getIdentityEnvironment().setAttributes(sessionInfoForUsertracking);
		
	}

	/**
	 * Set the roles (admin, author, guest)
	 * @param identity
	 * @param usess
	 */
	private static void setRolesFor(Identity identity, UserSession usess) {
		Roles roles = BaseSecurityManager.getInstance().getRoles(identity);
		usess.setRoles(roles);
	}
	
	public static void setLoginBlocked(boolean newLoginBlocked) {
		loginBlocked = newLoginBlocked;
	}
	
	public static boolean isLoginBlocked() {
		return loginBlocked;
	}
	
	public static void setRejectDMZRequests(boolean newRejectDMZRequests) {
		rejectDMZRequests = newRejectDMZRequests;
	}
	
	public static boolean isRejectDMZRequests() {
		return rejectDMZRequests;
	}
	
	public static void setMaxSessions(int maxSession) {
		maxSessions  = maxSession;
	}

	public static int getMaxSessions() {
		return maxSessions;
	}
	
}