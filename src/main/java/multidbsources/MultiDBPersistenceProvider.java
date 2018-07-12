package multidbsources;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.PersistenceException;
import javax.persistence.spi.PersistenceProvider;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.ProviderChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.reflect.ClassPath;

public class MultiDBPersistenceProvider extends HibernatePersistenceProvider implements PersistenceProvider {

	Logger logger = LoggerFactory.getLogger(MultiDBPersistenceProvider.class);

	@Override
	protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilder(PersistenceUnitDescriptor persistenceUnitDescriptor, Map integration, ClassLoader providedClassLoader) {
		if (persistenceUnitDescriptor instanceof ParsedPersistenceXmlDescriptor) {
			ParsedPersistenceXmlDescriptor desc = (ParsedPersistenceXmlDescriptor) persistenceUnitDescriptor;

			try {
				ClassPath cp = ClassPath.from(providedClassLoader!=null ? providedClassLoader : getClass().getClassLoader());
				String[] packages = (String[])integration.get("multidbsources.packages");
				for (String packageName : packages) {
					Set<ClassPath.ClassInfo> infos = cp.getTopLevelClasses(packageName);
					for (ClassPath.ClassInfo info : infos) {
						Class<?> klass = info.load();
						if (klass.getAnnotation(Entity.class)!=null) {
							desc.addClasses(info.getName());
						}
					}
				}
				desc.setExcludeUnlistedClasses(true);

				logger.info("Model classes for persistence unit '{}': {}", desc.getName(), desc.getManagedClassNames());
			} catch (IOException ex) {
				throw new PersistenceException("Cannot scan classes", ex);
			}
		}

		return super.getEntityManagerFactoryBuilder(persistenceUnitDescriptor, integration, providedClassLoader);
	}

	@Override
	protected EntityManagerFactoryBuilder getEntityManagerFactoryBuilderOrNull(String persistenceUnitName, Map properties, ClassLoader providedClassLoader) {
		logger.debug("Attempting to obtain correct EntityManagerFactoryBuilder for persistenceUnitName: {}", persistenceUnitName);

		final Map integration = wrap(properties);
		final List<ParsedPersistenceXmlDescriptor> units;
		try {
			units = PersistenceXmlParser.locatePersistenceUnits( integration );
		}
		catch (Exception e) {
			logger.debug( "Unable to locate persistence units", e );
			throw new PersistenceException("Unable to locate persistence units", e);
		}

		logger.debug("Located and parsed {} persistence units; checking each", units.size());

		if (persistenceUnitName == null && units.size() > 1) {
			// no persistence-unit name to look for was given and we found multiple persistence-units
			throw new PersistenceException("No name provided and multiple persistence units found");
		}

		for (ParsedPersistenceXmlDescriptor persistenceUnit : units) {
			logger.debug(
					"Checking persistence-unit [name={}, explicit-provider={}] against incoming persistence unit name [{}]",
					persistenceUnit.getName(),
					persistenceUnit.getProviderClassName(),
					persistenceUnitName
					);

			final boolean matches = persistenceUnitName == null || persistenceUnit.getName().equals(persistenceUnitName);
			if (!matches) {
				logger.debug("Excluding from consideration due to name mis-match");
				continue;
			}

			// See if we (Hibernate) are the persistence provider

			String extractRequestedProviderName = ProviderChecker.extractRequestedProviderName(persistenceUnit, integration);

			if (!ProviderChecker.isProvider(persistenceUnit, properties) && !(this.getClass().getName().equals(extractRequestedProviderName))) {
				logger.debug("Excluding from consideration due to provider mis-match");
				continue;
			}

			return getEntityManagerFactoryBuilder(persistenceUnit, integration, providedClassLoader);
		}

		logger.debug("Found no matching persistence units");
		return null;
	}
}
