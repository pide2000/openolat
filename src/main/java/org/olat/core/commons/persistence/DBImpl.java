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

package org.olat.core.commons.persistence;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.RollbackException;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.jpa.HibernateEntityManager;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.stat.Statistics;
import org.hibernate.type.Type;
import org.olat.core.configuration.Destroyable;
import org.olat.core.id.Persistable;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.DBRuntimeException;
import org.olat.core.logging.LogDelegator;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * A <b>DB </b> is a central place to get a Entity Managers. It acts as a
 * facade to the database, transactions and Queries. The hibernateSession is
 * lazy loaded per thread.
 * 
 * @author Andreas Ch. Kapp
 * @author Christian Guretzki
 */
public class DBImpl extends LogDelegator implements DB, Destroyable {
	private static final int MAX_DB_ACCESS_COUNT = 500;
	private static DBImpl INSTANCE;
	
	private String dbVendor;
	private EntityManagerFactory emf;
	private PlatformTransactionManager txManager;

	private final ThreadLocal<ThreadLocalData> data = new ThreadLocal<ThreadLocalData>();
	// Max value for commit-counter, values over this limit will be logged.
	private static int maxCommitCounter = 10;

	/**
	 * [used by spring]
	 */
	private DBImpl() {
		INSTANCE = this;
	}
	
	protected static DBImpl getInstance() {
		return INSTANCE;
	}
	
	@Override
	public String getDbVendor() {
		return dbVendor;
	}
	/**
	 * [used by spring]
	 * @param dbVendor
	 */
	public void setDbVendor(String dbVendor) {
		this.dbVendor = dbVendor;
	}
    
	public void setEntityManagerFactory(EntityManagerFactory emf) {
		this.emf = emf;
	}
	
	public void setTxManager(PlatformTransactionManager txManager) {
		this.txManager = txManager;
	}

	/**
	 * A <b>ThreadLocalData</b> is used as a central place to store data on a per
	 * thread basis.
	 * 
	 * @author Andreas CH. Kapp
	 * @author Christian Guretzki
	 */
	protected static class ThreadLocalData {

		private boolean error;
		private Exception lastError;
		
		private boolean initialized = false;
		// count number of db access in beginTransaction, used to log warn 'to many db access in one transaction'
		private int accessCounter = 0;
		// count number of commit in db-session, used to log warn 'Call more than one commit in a db-session'
		private int commitCounter = 0;
		
		private ThreadLocalData() {
		// don't let any other class instantiate ThreadLocalData.
		}

		/**
		 * @return true if initialized.
		 */
		protected boolean isInitialized() {
			return initialized;
		}

		protected void setInitialized(boolean b) {
			initialized = b;
		}

		public boolean isError() {
			return error;
		}

		public void setError(boolean error) {
			this.error = error;
		}

		public Exception getLastError() {
			return lastError;
		}

		public void setError(Exception ex) {
			this.lastError = ex;
			this.error = true;
		}

		protected void incrementAccessCounter() {
			this.accessCounter++;
		}
		
		protected int getAccessCounter() {
			return this.accessCounter;
		}
		
		protected void resetAccessCounter() {
			this.accessCounter = 0;
		}	

		protected void incrementCommitCounter() {
			this.commitCounter++;
		}
		
		protected int getCommitCounter() {
			return this.commitCounter;
		}

		protected void resetCommitCounter() {
			this.commitCounter = 0;
		}
	}

	private void setData(ThreadLocalData data) {
		this.data.set(data);
	}

	private ThreadLocalData getData() {
		ThreadLocalData tld = data.get();
		if (tld == null) {
			tld = new ThreadLocalData();
			setData(tld);
		}
		return tld;
	}
	
	@Override
	public EntityManager getCurrentEntityManager() {
		//if spring has already an entity manager in this thread bounded, return it
		EntityManager threadBoundedEm = EntityManagerFactoryUtils.getTransactionalEntityManager(emf);
		if(threadBoundedEm != null && threadBoundedEm.isOpen()) {
			EntityTransaction trx = threadBoundedEm.getTransaction();
			//if not active begin a new one (possibly manual committed)
			if(!trx.isActive()) {
				trx.begin();
			}
			updateDataStatistics(threadBoundedEm, "entityManager");
			return threadBoundedEm;
		}
		EntityManager em = getEntityManager();
		updateDataStatistics(em, "entityManager");
		return em;
	}
	
	private Session getSession(EntityManager em) {
		return em.unwrap(HibernateEntityManager.class).getSession();
	}
	
	private boolean unusableTrx(EntityTransaction trx) {
		return trx == null || !trx.isActive() || trx.getRollbackOnly();
	}
	
	private EntityManager getEntityManager() {
		EntityManager txEm = EntityManagerFactoryUtils.getTransactionalEntityManager(emf);
		if(txEm == null) {
			if(txManager != null) {
				DefaultTransactionDefinition def = new DefaultTransactionDefinition();
				txManager.getTransaction(def);
				txEm = EntityManagerFactoryUtils.getTransactionalEntityManager(emf);
			} else {
				txEm = emf.createEntityManager();
			}
		} else if(!txEm.isOpen()) {
			DefaultTransactionDefinition def = new DefaultTransactionDefinition();
			def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			txManager.getTransaction(def);
			txEm = EntityManagerFactoryUtils.getTransactionalEntityManager(emf);
		} else {
			EntityTransaction trx = txEm.getTransaction();
			//if not active begin a new one (possibly manual committed)
			if(!trx.isActive()) {
				trx.begin();
			}
		}
		return txEm;
	}

  private void updateDataStatistics(EntityManager em, Object logObject) {
		/*
  	//OLAT-3621: paranoia check for error state: we need to catch errors at the earliest point possible. OLAT-3621 has a suspected situation
		//           where an earlier transaction failed and didn't clean up nicely. To check this, we introduce error checking in getInstance here
	  
		if (transaction != null && transaction.isActive() && transaction.getRollbackOnly()
				&& !Thread.currentThread().getName().equals("TaskExecutorThread")) {
			INSTANCE.logWarn("beginTransaction: Transaction (still?) in Error state: "+transaction, new Exception("DBImpl begin transaction)"));
		}
		*/
	
  	// increment only non-cachable query 
  	if (logObject instanceof String ) {
  		String query = (String) logObject;
  		query = query.trim();
  		if (   !query.startsWith("select count(poi) from org.olat.basesecurity.SecurityGroupMembershipImpl as sgmsi, org.olat.basesecurity.PolicyImpl as poi,")
  		    && !query.startsWith("select count(grp) from org.olat.group.BusinessGroupImpl as grp")
  		    && !query.startsWith("select count(sgmsi) from  org.olat.basesecurity.SecurityGroupMembershipImpl as sgmsi") ) {
  			// it is no of cached queries
  			getData().incrementAccessCounter();  			
  		}
  	} else {
  		getData().incrementAccessCounter();
  	}

    if (getData().getAccessCounter() > MAX_DB_ACCESS_COUNT) {
    	logWarn("beginTransaction bulk-change, too many db access for one transaction, could be a performance problem (add closeSession/createSession in loop) logObject=" + logObject, null);
    	getData().resetAccessCounter();
    }
  }

	/**
	 * Close the database session.
	 */
	@Override
	public void closeSession() {
		getData().resetAccessCounter();
		// Note: closeSession() now also checks if the connection is open at all
		//  in OLAT-4318 a situation is described where commit() fails and closeSession()
		//  is not called at all. that was due to a call to commit() with a session
		//  that was closed underneath by hibernate (not noticed by DBImpl).
		//  in order to be robust for any similar situation, we check if the 
		//  connection is open, otherwise we shouldn't worry about doing any commit/rollback anyway
		

		//commit
		//getCurrentEntityManager();
		EntityManager s = EntityManagerFactoryUtils.getTransactionalEntityManager(emf);
		if(s != null) {
			EntityTransaction trx = s.getTransaction();
			if(trx.isActive()) {
				try {
					trx.commit();
				} catch (RollbackException ex) {
					//possible if trx setRollbackonly
					logWarn("Close session with transaction set with setRollbackOnly", ex);
				} catch (Exception e) {
					logError("", e);
					trx.rollback();
				}
			}
	
			TransactionSynchronizationManager.clear();
			EntityManagerFactoryUtils.closeEntityManager(s);
			Map<Object,Object> map = TransactionSynchronizationManager.getResourceMap();
			if(map.containsKey(emf)) {
				TransactionSynchronizationManager.unbindResource(emf);
			}
		}
		data.remove();
	}
  
	private boolean contains(Object object) {
		EntityManager em = getCurrentEntityManager();
		return em.contains(object);
	}

	/**
	 * Create a DBQuery
	 * 
	 * @param query
	 * @return DBQuery
	 */
	@Override
	public DBQuery createQuery(String query) {
		try {
			EntityManager em = getCurrentEntityManager();
			Query q = getSession(em).createQuery(query);
			return new DBQueryImpl(q);
		} catch (HibernateException he) {
			getData().setError(he);
			throw new DBRuntimeException("Error while creating DBQueryImpl: ", he);
		}
	}

	/**
	 * Delete an object.
	 * 
	 * @param object
	 */
	@Override
	public void deleteObject(Object object) {
		EntityManager em = getCurrentEntityManager();
		EntityTransaction trx = em.getTransaction();
		if (unusableTrx(trx)) { // some program bug
			throw new DBRuntimeException("cannot delete in a transaction that is rolledback or committed " + object);
		}
		try {
			Object relaoded = em.merge(object);
			em.remove(relaoded);
			if (isLogDebugEnabled()) {
				logDebug("delete (trans "+trx.hashCode()+") class "+object.getClass().getName()+" = "+object.toString());	
			}
		} catch (HibernateException e) { // we have some error
			trx.setRollbackOnly();
			getData().setError(e);
			throw new DBRuntimeException("Delete of object failed: " + object, e);
		}
	}

	/**
	 * Deletion query.
	 * 
	 * @param query
	 * @param value
	 * @param type
	 * @return nr of deleted rows
	 */
	@Override
	public int delete(String query, Object value, Type type) {
		int deleted = 0;
		EntityManager em = getCurrentEntityManager();
		EntityTransaction trx = em.getTransaction();
		if (unusableTrx(trx)) { // some program bug
			throw new DBRuntimeException("cannot delete in a transaction that is rolledback or committed " + value);
		}
		try {
			//old: deleted = getSession().delete(query, value, type);
			Session si = getSession(em);
			Query qu = si.createQuery(query);
			qu.setParameter(0, value, type);
			List foundToDel = qu.list();
			int deletionCount = foundToDel.size();
			for (int i = 0; i < deletionCount; i++ ) {
				si.delete( foundToDel.get(i) );
			}
		} catch (HibernateException e) { // we have some error
			trx.setRollbackOnly();
			throw new DBRuntimeException ("Could not delete object: " + value, e);
		}
		return deleted;
	}

	/**
	 * Deletion query.
	 * 
	 * @param query
	 * @param values
	 * @param types
	 * @return nr of deleted rows
	 */
	@Override
	public int delete(String query, Object[] values, Type[] types) {
		EntityManager em = getCurrentEntityManager();
		EntityTransaction trx = em.getTransaction();
		if (unusableTrx(trx)) { // some program bug
			throw new DBRuntimeException("cannot delete in a transaction that is rolledback or committed " + values);
		}
		try {
			//old: deleted = getSession().delete(query, values, types);
			Session si = getSession(em);
			Query qu = si.createQuery(query);
			qu.setParameters(values, types);
			List foundToDel = qu.list();
			int deleted = foundToDel.size();
			for (int i = 0; i < deleted; i++ ) {
				si.delete( foundToDel.get(i) );
			}	
			return deleted;
		} catch (HibernateException e) { // we have some error
			trx.setRollbackOnly();
			throw new DBRuntimeException ("Could not delete object: " + values, e);
		}
	}

	/**
	 * Find objects based on query
	 * 
	 * @param query
	 * @param value
	 * @param type
	 * @return List of results.
	 */
	@Override
	public List find(String query, Object value, Type type) {
		EntityManager em = getCurrentEntityManager();
		EntityTransaction trx = em.getTransaction();
		try {
			Query qu = getSession(em).createQuery(query);
			qu.setParameter(0, value, type);
			return qu.list();
		} catch (HibernateException e) {
			trx.setRollbackOnly();
			String msg = "Find failed in transaction. Query: " +  query + " " + e;
			getData().setError(e);
			throw new DBRuntimeException(msg, e);
		}
	}

	/**
	 * Find objects based on query
	 * 
	 * @param query
	 * @param values
	 * @param types
	 * @return List of results.
	 */
	@Override
	public List find(String query, Object[] values, Type[] types) {
		EntityManager em = getCurrentEntityManager();
		try {
			// old: li = getSession().find(query, values, types);
			Query qu = getSession(em).createQuery(query);
			qu.setParameters(values, types);
			return qu.list();
		} catch (HibernateException e) {
			em.getTransaction().setRollbackOnly();
			getData().setError(e);
			throw new DBRuntimeException("Find failed in transaction. Query: " +  query + " " + e, e);
		}
	}

	/**
	 * Find objects based on query
	 * 
	 * @param query
	 * @return List of results.
	 */
	@Override
	public List find(String query) {
		EntityManager em = getCurrentEntityManager();
		try {
			return em.createQuery(query).getResultList();
		} catch (HibernateException e) {
			em.getTransaction().setRollbackOnly();
			getData().setError(e);
			throw new DBRuntimeException("Find in transaction failed: " + query + " " + e, e);
		}
	}

	/**
	 * Find an object.
	 * 
	 * @param theClass
	 * @param key
	 * @return Object, if any found. Null, if non exist. 
	 */
	@Override
	public <U> U findObject(Class<U> theClass, Long key) {
		return getCurrentEntityManager().find(theClass, key);
	}
	
	/**
	 * Load an object.
	 * 
	 * @param theClass
	 * @param key
	 * @return Object.
	 */
	@Override
	public <U> U loadObject(Class<U> theClass, Long key) {
		try {
			return getCurrentEntityManager().find(theClass, key);
		} catch (Exception e) {
			throw new DBRuntimeException("loadObject error: " + theClass + " " + key + " ", e);
		}
	}

	/**
	 * Save an object.
	 * 
	 * @param object
	 */
	@Override
	public void saveObject(Object object) {
		EntityManager em = getCurrentEntityManager();
		EntityTransaction trx = em.getTransaction();
		if (unusableTrx(trx)) { // some program bug
			throw new DBRuntimeException("cannot save in a transaction that is rolledback or committed: " + object);
		}
		try {
			em.persist(object);					
		} catch (Exception e) { // we have some error
			trx.setRollbackOnly();
			getData().setError(e);
			throw new DBRuntimeException("Save failed in transaction. object: " +  object, e);
		}
	}

	/**
	 * Update an object.
	 * 
	 * @param object
	 */
	@Override
	public void updateObject(Object object) {
		EntityManager em = getCurrentEntityManager();
		EntityTransaction trx = em.getTransaction();
		if (unusableTrx(trx)) { // some program bug
			throw new DBRuntimeException("cannot update in a transaction that is rolledback or committed " + object);
		}
		try {
			getSession(em).update(object);								
		} catch (HibernateException e) { // we have some error
			trx.setRollbackOnly();
			getData().setError(e);
			throw new DBRuntimeException("Update object failed in transaction. Query: " +  object, e);
		}
	}

	/**
	 * Get any errors from a previous DB call.
	 * 
	 * @return Exception, if any.
	 */
	public Exception getError() {
		return getData().getLastError();
	}

	/**
	 * @return True if any errors occured in the previous DB call.
	 */
	@Override
	public boolean isError() {
		//EntityTransaction trx = getCurrentEntityManager().getTransaction();
		EntityManager em = EntityManagerFactoryUtils.getTransactionalEntityManager(emf);
		if(em != null && em.isOpen()) {
			EntityTransaction trx = em.getTransaction();
			if (trx != null && trx.isActive()) {
				return trx.getRollbackOnly();
			} 
		}
		return getData() == null ? false : getData().isError();
	}

	private boolean hasTransaction() {
		EntityManager em = EntityManagerFactoryUtils.getTransactionalEntityManager(emf);
		if(em != null && em.isOpen()) {
			EntityTransaction trx = em.getTransaction();
			return trx != null && trx.isActive();
		}
		return false;
	}

	/**
	 * see DB.loadObject(Persistable persistable, boolean forceReloadFromDB)
	 * 
	 * @param persistable
	 * @return the loaded object
	 */
	@Override
	public Persistable loadObject(Persistable persistable) {
		return loadObject(persistable, false);
	}

	/**
	 * loads an object if needed. this makes sense if you have an object which had
	 * been generated in a previous hibernate session AND you need to access a Set
	 * or a attribute which was defined as a proxy.
	 * 
	 * @param persistable the object which needs to be reloaded
	 * @param forceReloadFromDB if true, force a reload from the db (e.g. to catch
	 *          up to an object commited by another thread which is still in this
	 *          thread's session cache
	 * @return the loaded Object
	 */
	@Override
	public Persistable loadObject(Persistable persistable, boolean forceReloadFromDB) {
		if (persistable == null) throw new AssertException("persistable must not be null");

		EntityManager em = getCurrentEntityManager();
		Class<? extends Persistable> theClass = persistable.getClass();
		if (forceReloadFromDB) {
			// we want to reload it from the database.
			// there are 3 scenarios possible:
			// a) the object is not yet in the hibernate cache
			// b) the object is in the hibernate cache
			// c) the object is detached and there is an object with the same id in the hibernate cache
			
			if (contains(persistable)) {
				// case b - then we can use evict and load
				evict(em, persistable, getData());
				return loadObject(theClass, persistable.getKey());
			} else {
				// case a or c - unfortunatelly we can't distinguish these two cases
				// and session.refresh(Object) doesn't work.
				// the only scenario that works is load/evict/load
				Persistable attachedObj = loadObject(theClass, persistable.getKey());
				evict(em, attachedObj, getData());
				return loadObject(theClass, persistable.getKey());
			}
		} else if (!contains(persistable)) { 
			// forceReloadFromDB is false - hence it is OK to take it from the cache if it would be there
			// now this object directly is not in the cache, but it's possible that the object is detached
			// and there is an object with the same id in the hibernate cache.
			// therefore the following loadObject can either return it from the cache or load it from the DB
			return loadObject(theClass, persistable.getKey());
		} else { 
			// nothing to do, return the same object
			return persistable;
		}
	}
	
	private void evict(EntityManager em, Object object, ThreadLocalData localData) {
		try {
			getSession(em).evict(object);			
		} catch (Exception e) {
			localData.setError(e);
			throw new DBRuntimeException("Error in evict() Object from Database. ", e);
		}
	}

	@Override
	public void commitAndCloseSession() {
		try {
			commit();
		} finally {
			try{
				// double check: is the transaction still open? if yes, is it not rolled-back? if yes, do a rollback now!
				if (hasTransaction() && isError()) {
					getLogger().error("commitAndCloseSession: commit seems to have failed, transaction still open. Doing a rollback!", new Exception("commitAndCloseSession"));
					rollback();
				}
			} finally {
				closeSession();
			}
		}
	}
	
	@Override
	public void rollbackAndCloseSession() {
		try {
			rollback();
		} finally {
			closeSession();
		}
	}

	/**
	 * Call this to commit a transaction opened by beginTransaction().
	 */
	@Override
	public void commit() {
		if (isLogDebugEnabled()) logDebug("commit start...", null);
		try {
			if (hasTransaction() && !isError()) {
				if (isLogDebugEnabled()) logDebug("has Transaction and is in Transaction => commit", null);
				getData().incrementCommitCounter();
				if ( isLogDebugEnabled() ) {
					if ((maxCommitCounter != 0) && (getData().getCommitCounter() > maxCommitCounter) ) {
						logInfo("Call too many commit in a db-session, commitCounter=" + getData().getCommitCounter() +"; could be a performance problem" , null);
					}
				}
				
				EntityTransaction trx = getCurrentEntityManager().getTransaction();
				if(trx != null) {
					trx.commit();
				}

				if (isLogDebugEnabled()) logDebug("Commit DONE hasTransaction()=" + hasTransaction(), null);
			} else if(hasTransaction() && isError()) {
				EntityTransaction trx = getCurrentEntityManager().getTransaction();
				if(trx != null && trx.isActive()) {
					throw new DBRuntimeException("Try to commit a transaction in error status");
				}
			} else {
				if (isLogDebugEnabled()) logDebug("Call commit without starting transaction", null );
			}
		} catch (Error er) {
			logError("Uncaught Error in DBImpl.commit.", er);
			throw er;
		} catch (Exception e) {
			// Filter Exception form async TaskExecutorThread, there are exception allowed
			if (!Thread.currentThread().getName().equals("TaskExecutorThread")) {
				logWarn("Caught Exception in DBImpl.commit.", e);
			}
			// Error when trying to commit
			try {
				if (hasTransaction()) {
					TransactionStatus status = txManager.getTransaction(null);
					txManager.rollback(status);
					
					EntityTransaction trx = getCurrentEntityManager().getTransaction();
					if(trx != null && trx.isActive()) {
						if(trx.getRollbackOnly()) {
							try {
								trx.commit();
							} catch (RollbackException e1) {
								//we wait for this exception
							}
						} else {
							trx.rollback();
						}
					}
				}
			} catch (Error er) {
				logError("Uncaught Error in DBImpl.commit.catch(Exception).", er);
				throw er;
			} catch (Exception ex) {
				logWarn("Could not rollback transaction after commit!", ex);
				throw new DBRuntimeException("rollback after commit failed", e);
			}
			throw new DBRuntimeException("commit failed, rollback transaction", e);
		}
	}
	
	/**
	 * Call this to rollback current changes.
	 */
	@Override
	public void rollback() {
		if (isLogDebugEnabled()) logDebug("rollback start...", null);
		try {
			// see closeSession() and OLAT-4318: more robustness with commit/rollback/close, therefore
			// we check if the connection is open at this stage at all

			TransactionStatus status = txManager.getTransaction(null);
			txManager.rollback(status);
			
			EntityTransaction trx = getCurrentEntityManager().getTransaction();
			if(trx != null && trx.isActive()) {
				if(trx.getRollbackOnly()) {
					try {
						trx.commit();
					} catch (RollbackException e) {
						//we wait for this exception
					}
				} else {
					trx.rollback();
				}
			}

		} catch (Exception ex) {
			logWarn("Could not rollback transaction!",ex);
			throw new DBRuntimeException("rollback failed", ex);
		}		
	}

	/**
	 * Statistics must be enabled first, when you want to use it. 
	 * @return Return Hibernates statistics object.
	 */
	@Override
	public Statistics getStatistics() {
		if(emf instanceof HibernateEntityManagerFactory) {
			return ((HibernateEntityManagerFactory)emf).getSessionFactory().getStatistics();
		}
 		return null;
   }
	
	public Object getCache() {
		/*if(emf instanceof HibernateEntityManagerFactory) {
			return ((HibernateEntityManagerFactory)emf).getSessionFactory().getCache().();
		}*/
		return null;
	}

	/**
	 * @see org.olat.core.commons.persistence.DB#intermediateCommit()
	 */
	@Override
	public void intermediateCommit() {
		commit();
		closeSession();
	}

	@Override
	public void destroy() {
		//clean up registered drivers to prevent messages like
		// The web application [/olat] registered the JBDC driver [com.mysql.Driver] but failed to unregister...
		Enumeration<Driver> registeredDrivers = DriverManager.getDrivers();
		while(registeredDrivers.hasMoreElements()) {
			try {
				DriverManager.deregisterDriver(registeredDrivers.nextElement());
			} catch (SQLException e) {
				logError("Could not unregister database driver.", e);
			}
		}
	}
}
