package multidbsources;

import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import ninja.utils.NinjaProperties;

@Singleton
public abstract class EntityManagerProvider implements Provider<EntityManager> {

	private final ThreadLocal<EntityManager> entityManager = new ThreadLocal<>();

	private EntityManagerFactory entityManagerFactory;

	Logger logger = LoggerFactory.getLogger(EntityManagerProvider.class);

	@Inject
	NinjaProperties ninjaProperties;

	public EntityManagerProvider(MultiDBSources multiDBSources) {
		multiDBSources.registerEntityManagerProvider(this);
	}

	/**
	 * [multidb]:
	 * Defines part of the multidb access property name in the application.conf used to define the database configuration for this entity manager provider.
	 *
	 * If the method returns "abc", then the application.conf must define
	 * - multidbsources.abc.persistence_unit_name to define the persistence unit name in the persistence.xml for this database
	 * - multidbsources.abc.url to define the JDBC URL for the database URL
	 * - multidbsources.abc.user and multidbsources.abc.password to define the access credentials for the database.
	 *
	 * The sourceID also determines the path component which the Flyway migration scripts are defined for the database.
	 */
	public abstract String getSourceID();

	/**
	 * [multidb]:
	 * Defines the list of packages that will be scanned for classes containing the @Entity annotation.
	 * The scanned classes will be included as the model entities for the entity manager provider.
	 */
	public abstract String[] getModelPackages();

	@Override
	public EntityManager get() {
		if (!isWorking()) {
			begin();
		}

		return entityManager.get();
	}

	public boolean isWorking() {
		return entityManager.get() != null;
	}

	void begin() {
		Preconditions.checkState(entityManager.get()==null, "Work unit has already begin. Looks like you have called UnitOfWork.begin() twice without a balancing call to end() in-between.");
		entityManager.set(entityManagerFactory.createEntityManager());
	}

	void end() {
		EntityManager em = entityManager.get();
		if (em!=null) {
			try {
				em.close();
			} finally {
				entityManager.remove();
			}
		}
	}

	void startProvider() {
		String dataSourceID = getSourceID();
		String ninjaPropertiesPrefix = "multidbsources." + dataSourceID;
		String persistenceUnitName = ninjaProperties.getOrDie(ninjaPropertiesPrefix + ".persistence_unit_name");
		String dbURL = ninjaProperties.get(ninjaPropertiesPrefix + ".url");
		String dbUser = ninjaProperties.get(ninjaPropertiesPrefix + ".user");
		String dbPassword = ninjaProperties.get(ninjaPropertiesPrefix + ".password");

		// Perform migration
		if (ninjaProperties.getBooleanWithDefault(ninjaPropertiesPrefix + ".migration.run", Boolean.FALSE)) {
			Flyway flyway = new Flyway();
			flyway.setDataSource(dbURL, dbUser, dbPassword);
			flyway.setLocations("classpath:db/migration/" + dataSourceID + "/");
			flyway.migrate();
		}

		// Instantiate EntityManagerFactory instance
		Properties jpaProperties = new Properties();
		jpaProperties.put("multidbsources.packages", getModelPackages());
		jpaProperties.put("hibernate.ejb.entitymanager_factory_name", dataSourceID);
		jpaProperties.put("hibernate.hikari.poolName", dataSourceID);
		if (dbURL!=null) {
			jpaProperties.put("hibernate.connection.url", dbURL);
		}
		if (dbUser!=null) {
			jpaProperties.put("hibernate.connection.username", dbUser);
		}
		if (dbPassword!=null) {
			jpaProperties.put("hibernate.connection.password", dbPassword);
		}
		entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnitName, jpaProperties);
	}

	void stopProvider() {
		if (entityManagerFactory!=null) {
			try {
				entityManagerFactory.close();
			} catch (Exception e) {
				// Ignore
			}
			entityManagerFactory = null;
		}
	}
}
