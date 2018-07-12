/*
 * Some portions of this class are copied from Google Guice
 * https://github.com/google/guice/blob/master/extensions/persist/src/com/google/inject/persist/jpa/JpaLocalTxnInterceptor.java
 * under the following license.
 *
 * Copyright (C) 2010 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package multidbsources;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

import ninja.lifecycle.Dispose;
import ninja.lifecycle.Start;
import ninja.utils.NinjaProperties;

@Singleton
public class MultiDBSources {

	final Logger logger = LoggerFactory.getLogger(MultiDBSources.class);

	@Inject
	NinjaProperties ninjaProperties;

	private boolean hasInitializedProviders = false;

	private List<EntityManagerProvider> providers = new ArrayList<>();

	private final ThreadLocal<Boolean> hasStartedTransaction = new ThreadLocal<>();

	boolean hasTransaction() {
		return hasStartedTransaction.get()!=null;
	}

	void beginTransaction() {
		Preconditions.checkState(hasStartedTransaction.get()==null, "Transaction has already started. Please call endTransaction() first.");
		hasStartedTransaction.set(Boolean.TRUE);

		logger.debug("beginTransaction: Beginning database transaction for {} providers...", providers.size());
		for (EntityManagerProvider provider : providers) {
			EntityTransaction txn = provider.get().getTransaction();
			if (!txn.isActive()) {
				logger.debug("beginTransaction: *** Starting db transaction for provider '{}'", provider.getSourceID());
				txn.begin();
			} else {
				logger.debug("beginTransaction: *** Skipping starting db transaction for provider '{}'. It already has an active transaction.", provider.getSourceID());
			}
		}
	}

	void endTransaction(Transactional transactional) {
		endTransaction(transactional, null);
	}

	void endTransaction(Transactional transactional, Exception ex) {
		Preconditions.checkState(hasStartedTransaction.get()!=null, "No transaction. Please call startTransaction() first.");
		hasStartedTransaction.remove();

		logger.debug("endTransaction: Ending db transactions for {} providers with exception '{}'", providers.size(), ex!=null ? ex.toString() : "<null>");
		for (EntityManagerProvider provider : providers) {
			if (provider.isWorking()) {
				EntityTransaction txn = provider.get().getTransaction();

				//commit transaction only if rollback didnt occur
				if (txn.isActive()) {
					if (ex==null) {
						logger.debug("endTransaction: *** Committing transaction for provider '{}'", provider.getSourceID());
						txn.commit();
					} else if (rollbackIfNecessary(provider, transactional, ex, txn)) {
						txn.commit();
					}
				} else {
					logger.debug("endTransaction: *** No active transaction for provider '{}'", provider.getSourceID());
				}
			}
		}
	}

	void registerEntityManagerProvider(EntityManagerProvider provider) {
		if (hasInitializedProviders) {
			throw new IllegalArgumentException(String.format("Cannot register EntityManagerProvider subclass '%s'. Please remember to bind your class in conf/Modules.java.", provider.getClass().getName()));
		}

		providers.add(provider);
		logger.info("registerEntityManagerProvider: Register provider: {}", provider);
	}

	public void beginWorkUnit() {
		logger.debug("beginWorkUnit: Begin work unit");
		for (EntityManagerProvider provider : providers) {
			provider.begin();
		}
	}

	public void endWorkUnit() {
		for (EntityManagerProvider provider : providers) {
			provider.end();
		}
		logger.debug("endWorkUnit: Work unit ended");
	}

	@Start(order = 10)
	public void startService() {
		for (EntityManagerProvider provider : providers) {
			provider.startProvider();
		}
		hasInitializedProviders = true;

		logger.info("startService: MultiDBSources ready");
	}

	@Dispose(order = 10)
	public void stopService() {
		for (EntityManagerProvider provider : providers) {
			provider.stopProvider();
		}
		logger.info("stopService: MultiDBSources terminated");
	}

	// Copied from Google Guice
	private boolean rollbackIfNecessary(EntityManagerProvider provider, Transactional transactional, Exception e, EntityTransaction txn) {
		boolean commit = true;

		//check rollback clauses
		for (Class<? extends Exception> rollBackOn : transactional.rollbackOn()) {

			//if one matched, try to perform a rollback
			if (rollBackOn.isInstance(e)) {
				commit = false;

				//check ignore clauses (supercedes rollback clause)
				for (Class<? extends Exception> exceptOn : transactional.ignore()) {
					//An exception to the rollback clause was found, DON'T rollback
					// (i.e. commit and throw anyway)
					if (exceptOn.isInstance(e)) {
						commit = true;
						break;
					}
				}

				//rollback only if nothing matched the ignore check
				if (!commit) {
					logger.debug("*** rollbackIfNecessary: Rolling back transaction for provider '{}'", provider.getSourceID());
					txn.rollback();
				}
				//otherwise continue to commit

				break;
			}
		}

		return commit;
	}
}
