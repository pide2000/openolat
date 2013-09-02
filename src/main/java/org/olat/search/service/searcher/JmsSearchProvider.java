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

package org.olat.search.service.searcher;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.lucene.queryparser.classic.ParseException;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.taskExecutor.TaskExecutorManager;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.search.QueryException;
import org.olat.search.SearchResults;
import org.olat.search.SearchService;
import org.olat.search.ServiceNotAvailableException;

/**
 * 
 * Description:<br>
 * This is a server side search proxy - delegates the search to the searchWorker.
 * 
 * <P>
 * Initial Date:  02.06.2008 <br>
 * @author Lavinia Dumitrescu
 */
public class JmsSearchProvider implements MessageListener {
	
	private OLog log_ = Tracing.createLoggerFor(this.getClass());
	private SearchService searchService;
	private ConnectionFactory connectionFactory_;
	private Connection connection_;
	private Queue searchQueue_;
	private Session session_;
	private MessageConsumer consumer_;
	private LinkedList<Session> sessions_ = new LinkedList<Session>();
	private long receiveTimeout = 60000;
	private TaskExecutorManager taskExecutorManager;
	
	/**
	 * [used by spring]
	 *
	 */
	public JmsSearchProvider() {
		//default constructor
	}
	
	public void setConnectionFactory(ConnectionFactory conFac) {
		connectionFactory_ = conFac;
	}

	public void setSearchQueue(Queue searchQueue) {
		this.searchQueue_ = searchQueue;
	}

	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	public void setTaskExecutorManager(TaskExecutorManager taskExecutorManager) {
		this.taskExecutorManager = taskExecutorManager;
	}

	/**
	 * Delegates execution to the searchService.
	 * @see org.olat.search.service.searcher.OLATSearcher#doSearch(java.lang.String, org.olat.core.id.Identity, org.olat.core.id.Roles, boolean)
	 */
	public SearchResults doSearch(String queryString, List<String> condQueries, Identity identity, Roles roles, int firstResult, int maxResults, boolean doHighlighting) throws ServiceNotAvailableException, ParseException, QueryException {
		if (searchService == null) throw new AssertException("searchService in ClusteredSearchProvider is null, please check the search configuration!");
		return searchService.doSearch(queryString, condQueries, identity, roles, firstResult, maxResults, doHighlighting);		
	}
	
	

	public Set<String> spellCheck(String query) throws ServiceNotAvailableException {
		if (searchService == null) throw new AssertException("searchService in ClusteredSearchProvider is null, please check the search configuration!");
		return searchService.spellCheck(query);
	}

	/**
	 * 
	 * @see org.olat.search.service.searcher.OLATSearcher#getQueryCount()
	 */
	public long getQueryCount() {		
		if (searchService == null) throw new AssertException("searchService in ClusteredSearchProvider is null, please check the search configuration!");
		return searchService.getQueryCount();
	}

	/**
	 * 
	 * @see org.olat.search.service.searcher.OLATSearcher#stop()
	 */
	public void stop() {
		if (searchService == null) throw new AssertException("searchService in ClusteredSearchProvider is null, please check the search configuration!");
		searchService.stop();
		try {
			session_.close();
			connection_.close();
			log_.info("ClusteredSearchProvider stopped");
		} catch (JMSException e) {
			log_.warn("Exception in stop ClusteredSearchProvider, ",e);
		}
	}	
	
	/**
	 * [used by spring]
	 * 
	 * @see org.olat.search.service.searcher.OLATSearcherProxy#setSearchService(org.olat.search.service.searcher.OLATSearcher)
	 */
	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;					
	}
	
	public void springInit() throws JMSException {
		connection_ = connectionFactory_.createConnection();
		session_ = connection_.createSession(false, Session.AUTO_ACKNOWLEDGE);
		consumer_ = session_.createConsumer(searchQueue_);
		consumer_.setMessageListener(this);
		connection_.start();
		log_.info("ClusteredSearchProvider JMS started");
	}
		
	public void onMessage(Message message) {
		if ( log_.isDebug() ) {
			log_.debug("onMessage, message=" + message);
		}
		try{
			long sentTimestamp = message.getJMSTimestamp();
			long currentTimestamp = System.currentTimeMillis();
			// check if received message is not too old because in case of overload we could have old search-messages
			if ( (currentTimestamp - sentTimestamp) < receiveTimeout ) { 
				final String correlationID = message.getJMSCorrelationID();
				final Destination replyTo = message.getJMSReplyTo();
				if (message instanceof ObjectMessage) {
					ObjectMessage objectMessage = (ObjectMessage) message;
					final SearchRequest searchRequest = (SearchRequest) objectMessage.getObject();
					taskExecutorManager.execute(new Runnable() {
		
						public void run() {
							onSearchMessage(searchRequest, correlationID, replyTo);
						}
						
					});
				} else if (message instanceof TextMessage) {				
					TextMessage testMessage = (TextMessage)message;
					final String spellText = testMessage.getText();
					taskExecutorManager.execute(new Runnable() {
		
						public void run() {
							onSpellMessage(spellText, correlationID, replyTo);
						}
						
					});
				}
			} else {
				// JMS message is too old, discard it (do nothing)
				log_.warn("JMS message was too old, discard message,  timeout=" + receiveTimeout + "ms , received time=" + (currentTimestamp - sentTimestamp) + "ms");
			}
		} catch(JMSException e) {
			log_.error("error when receiving jms messages", e);
			return; //signal search not available
		} catch (Error err) {
			log_.warn("Error in onMessage, ",err);
			// OLAT-3973: don't throw exceptions here
		} catch (RuntimeException runEx) {
			log_.warn("RuntimeException in onMessage, ",runEx);
			// OLAT-3973: don't throw exceptions here
		}
	}
	
	private synchronized Session acquireSession() throws JMSException {
		if (sessions_.size()==0) {
			return connection_.createSession(false, Session.AUTO_ACKNOWLEDGE);
		} else {
			return sessions_.getFirst();
		}
	}
	
	private synchronized void releaseSession(Session session) {
		if (session==null) {
			return;
		}
		sessions_.addLast(session);
	}

	void onSearchMessage(SearchRequest searchRequest, String correlationID, Destination replyTo) {
		if ( log_.isDebug() ) {
			log_.debug("onSearchMessage, correlationID=" + correlationID + " , replyTo=" + replyTo + " , searchRequest=" + searchRequest);
		}
		Session session = null;
		try{
			Identity identity = BaseSecurityManager.getInstance().loadIdentityByKey(searchRequest.getIdentityId());
	
			SearchResults searchResults = this.doSearch(searchRequest.getQueryString(), searchRequest.getCondQueries(), identity, searchRequest.getRoles(),
					searchRequest.getFirstResult(), searchRequest.getMaxResults(), searchRequest.isDoHighlighting());
			if (log_.isDebug()) {
				log_.debug("searchResults: " + searchResults.getLength());
			}
			if (searchResults != null) {
				session = acquireSession();
				Message responseMessage = session.createObjectMessage(searchResults);
				responseMessage.setJMSCorrelationID(correlationID);
				responseMessage.setStringProperty(SearchClientProxy.JMS_RESPONSE_STATUS_PROPERTY_NAME, SearchClientProxy.JMS_RESPONSE_STATUS_OK);
				MessageProducer producer = session.createProducer(replyTo);
				if ( log_.isDebug() ) {
					log_.debug("onSearchMessage, send ResponseMessage=" + responseMessage + " to replyTo=" + replyTo);
				}
				producer.send(responseMessage);
				producer.close();
				return;
			} else {
				log_.info("onSearchMessage, no searchResults (searchResults=null)");
			}
		} catch (JMSException e) {
			log_.error("error when receiving jms messages", e);
			return; //signal search not available
			// do not throw exceptions here throw new OLATRuntimeException();
		} catch (ServiceNotAvailableException sex) {
			sendErrorResponse(SearchClientProxy.JMS_RESPONSE_STATUS_SERVICE_NOT_AVAILABLE_EXCEPTION, correlationID, replyTo);
		} catch (ParseException pex) {
			sendErrorResponse(SearchClientProxy.JMS_RESPONSE_STATUS_PARSE_EXCEPTION, correlationID, replyTo);
		} catch (QueryException qex) {
			sendErrorResponse(SearchClientProxy.JMS_RESPONSE_STATUS_QUERY_EXCEPTION, correlationID, replyTo);
		}	catch (Throwable th) {
			log_.error("error at ClusteredSearchProvider.receive()", th);
			return;// signal search not available
			// do not throw exceptions throw new OLATRuntimeException();
		} finally{
			releaseSession(session);
			DBFactory.getInstance().commitAndCloseSession();
		}
	}
	
	private void sendErrorResponse(String jmsResponseStatus, String correlationID, Destination replyTo) {
		Session session = null;
		try {
			session = acquireSession();
			Message responseMessage = session.createObjectMessage();
			responseMessage.setJMSCorrelationID(correlationID);
			responseMessage.setStringProperty(SearchClientProxy.JMS_RESPONSE_STATUS_PROPERTY_NAME, jmsResponseStatus);
			MessageProducer producer = session.createProducer(replyTo);
			if ( log_.isDebug() ) {
				log_.debug("onSearchMessage, send ResponseMessage=" + responseMessage + " to replyTo=" + replyTo);
			}
			producer.send(responseMessage);
			producer.close();
			return;
			
		} catch (JMSException e) {
			log_.error("error when receiving jms messages", e);
			return; //signal search not available
		} finally{
			releaseSession(session);
		}
	}

	void onSpellMessage(String spellText, String correlationID, Destination replyTo) {			
		Session session = null;
		try {
			Set<String> spellStrings = this.spellCheck(spellText);
			if(spellStrings!=null) {
				ArrayList<String> spellStringList = new ArrayList<String>(spellStrings);
				session = acquireSession();
				Message responseMessage = session.createObjectMessage(spellStringList);
				responseMessage.setJMSCorrelationID(correlationID);
				MessageProducer producer = session.createProducer(replyTo);
				producer.send(responseMessage);
				producer.close();
				return;
			}
			return; // signal search not available
		} catch (JMSException e) {
			log_.error("error when receiving jms messages", e);
			return; //signal search not available
			// do not throw exceptions here throw new OLATRuntimeException();
		} catch (Throwable th) {
			log_.error("error at ClusteredSearchProvider.receive()", th);
			return;// signal search not available
			// do not throw exceptions throw new OLATRuntimeException();
		} finally{
			releaseSession(session);
			DBFactory.getInstance().commitAndCloseSession();
		}
	}


}
