# About this demo

This demo offers a custom solution how to support multiple database sources using Ninja web framework. I hope this could be useful to developers who are looking for a solution on accessing more than one databases.

# Features of this demo

- Support multiple database sources using Ninja web framework. Majority of the existing Ninja web framework documentation for database access is still applicable. The solution only needs to define a EntityManagerProvider for each database source and to inject the EntityManagerProviders in your DAO classes.
- Database migration scripts can be defined for each database source to migrate schema changes independently for each database.
- Use HikariCP connection pool in Ninja web framework.

# Setting up the demo

The demo was developed on Ubuntu 16.04 (x64) for Ninja web framework 6.1.0 and will connect to MySQL/Maria databases on localhost. All software (maven, openjdk-8-jdk, mariadb-client and mariadb-server) are installed from Ubuntu official repositories. Please install these software before setting up this demo.

1.  Create the databases and users needed for the demo. You can run the ```sql/databases.sql``` script using your favourite MySQL client as *root* user.
```
$> mysql -u root -p mysql < sql/databases.sql
```

2. Generate the demo package first
```
$> mvn clean package
```

3. Now run the demo in development mode.
```
$> mvn ninja:run
```

Check the *multidb_user* and *multidb_task* databases using your favourite MySQL clients. If the databases are setup correctly, you will see *User* table inside the *multidb_user* database and *Task* table inside the *multidb_task* database as the tables are being created automatically in the two databases, similar to how Ninja framework does the database migration for you.

# Testing the demo

1. On the Ubuntu machine, start the local web browser like Firefox, and access http://localhost:8080/. You will see a simple login page.

2. Fill in any random ID, and click on the Login button. You will be redirected to the Task listing page.

You can continue to test with different account IDs, and create tasks for each account via the web UI. The system will know which database to use for the queries and data updates.

Tasks created via the web UI will be marked as "completed" within a few minutes by the *CompletionWorker* worker service at regular intervals.

# General working of the demo

In the demo's code, all multi-db specific comments are prefixed with "*[multidb]*". You can search/find all these comments and learn about the workings.

- A custom JPA persistence provider is registered at *META-INF/services/javax.persistence.spi.PersistenceProvider*. All the persistence units defined in *META-INF/persistence.xml* must utilize this persistence provider.
- Two **EntityManagerProvider** subclasses, **models.UserEntityManagerProvider** and **models.TaskEntityManagerProvider** are used to represent each of the user and task databases. The overridden *getSourceID()* method will return the source identifier that will be used to identify the data source and for retrieving the necessary database access credentials from the *application.conf* file. The overridden *getModelPackages()* method will return an array of package names that will be parsed for entity classes for the database source.
- The **dao.UserDAO** and **dao.TaskDAO** classes inject the necessary providers for retrieving an **EntityManager** instance during queries and updates.
- The **MultiDBWorkUnitFilter** class is added to the global filters at **conf.Filters** to properly terminate the internal work unit and return the database connections to the pools
- The **schedules.CompletionWorker** class shows how to implement scheduled jobs. Each scheduled job needs to call *MultiDBSources.beginWorkUnit()* and *MultiDBSources.endWorkUnit()* to properly terminate the internal work unit and return the database connections to the pools.

# Customizing your Ninja applications

The following changes need to be made to upgrade your application to support multiple databases.

1. Comment out ```ninja.migration.run``` and ```ninja.jpa.persistence_unit_name``` properties (including those that are prefixed with ```%prod```, ```%dev``` or ```%test```). **This is very important as Ninja web framework's JPA module will interfere with the multi-database workings**.

2. Copy the ```multidbsources``` package in ```src/main/java``` in this demo to your application's code base.

3. Copy the ```META-INF/services/javax.persistence.spi.PersistenceProvider``` file in the demo to your application's code base. Update the persistence units in your ```META-INF/persistence.xml``` to use the **multidbsources.MultiDBPersistenceProvider** provider.

4. For each database source, define the necessary properties for each source in ```application.conf```. Please see the ```application.conf``` in the demo for more details. If you have enabled migration, the database migration scripts must reside in ```db/migration/<sourceID>/``` folder.

5. For each database source, subclass the **EntityManagerProvider** class and override the methods which return the source identifier and the array of package names for the entities. You can look at **models.UserEntityManagerProvider** and **models.TaskEntityManagerProvider** classes for more information.

6. Install the **MultiDBSourcesModule** module and bind all your **EntityManagerProvider** subclasses in **conf.Module** class. Refer to the **Module** class in the demo for more information.

7. Add **MultiDBWorkUnitFilter** class to the global filters at **conf.Filters**.

8. Inject the EntityManagerProvider subclasses in your DAO or controller classes. Any method that uses **EntityManager** instance must retrieve the **EntityManager** instance via the injected attribute. You can see the **UserDAO** and **TaskDAO** class in the demo for more details.

9. Check all scheduled tasks. Any task that accesses the database must call *MultiDBSources.beginWorkUnit()* method before making any database queries/updates, and to end the work unit by calling *MultiDBSources.endWorkUnit()*.

# Other matters

- Do not use @UnitOfWork annotation. It is not supported.
- Be sure to use *@Transactional* annotation from the *com.google.inject.persist* package.
- If there are multiple *@Transactional* annotated methods along the path of execution, the transactions across all databases will only be committed after all the *@Transactional* annotated methods have returned.
- You cannot use foreign key annotations like *@ManyToOne* to reference entity classes across different **EntityManagerProvider** classes. For example, the **Task** class in the demo cannot use such annotations to reference to **User** class.
