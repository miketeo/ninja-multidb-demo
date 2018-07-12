package models;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import multidbsources.EntityManagerProvider;
import multidbsources.MultiDBSources;

@Singleton  // [multidb]: This annotation is required; otherwise, multi-instances of the provider will be created
public class UserEntityManagerProvider extends EntityManagerProvider {

	@Inject
	public UserEntityManagerProvider(MultiDBSources multiDBSources) {
		super(multiDBSources);
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
	@Override
	public String getSourceID() {
		return "userdb";
	}

	/**
	 * [multidb]:
	 * Defines the list of packages that will be scanned for classes containing the @Entity annotation.
	 * The scanned classes will be included as the model entities for the entity manager provider.
	 */
	@Override
	public String[] getModelPackages() {
		return new String[] { "models.userdb" };
	}
}
