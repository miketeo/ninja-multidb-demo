package multidbsources;

import com.google.inject.Inject;

import ninja.Context;
import ninja.Filter;
import ninja.FilterChain;
import ninja.Result;

public class MultiDBWorkUnitFilter implements Filter {

	@Inject
	MultiDBSources multiDBSources;

	@Override
	public Result filter(FilterChain chain, Context context) {
		multiDBSources.beginWorkUnit();
		try {
			return chain.next(context);
		} finally {
			multiDBSources.endWorkUnit();
		}
	}

}
