package multidbsources;

import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;

public class MultiDBSourcesModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(MultiDBSources.class).in(Singleton.class);

		LocalTxnInterceptor txnInterceptor = new LocalTxnInterceptor();
		requestInjection(txnInterceptor);

		// class-level @Transactional
		bindInterceptor(
				annotatedWith(Transactional.class),
				any(),
				txnInterceptor);

		// method-level @Transactional
		bindInterceptor(
				any(),
				annotatedWith(Transactional.class),
				txnInterceptor);
	}

}
