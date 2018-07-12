
package conf;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import models.TaskEntityManagerProvider;
import models.UserEntityManagerProvider;
import multidbsources.MultiDBSourcesModule;
import schedules.CompletionWorker;

@Singleton
public class Module extends AbstractModule {

	@Override
	protected void configure() {
		// [MultiDB]: Install the MultiDBSourcesModule first
		install(new MultiDBSourcesModule());

		// [MultiDB]: Then, bind your EntityManagerProvider subclasses here
		bind(UserEntityManagerProvider.class).in(Singleton.class);
		bind(TaskEntityManagerProvider.class).in(Singleton.class);

		// [MultiDB]: Finally, bind the rest of your services
		bind(CompletionWorker.class);
	}

}
