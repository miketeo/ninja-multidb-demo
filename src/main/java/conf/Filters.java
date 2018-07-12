
package conf;

import java.util.List;

import multidbsources.MultiDBWorkUnitFilter;
import ninja.Filter;

public class Filters implements ninja.application.ApplicationFilters {

	@Override
	public void addFilters(List<Class<? extends Filter>> filters) {
		// [multidb]: Add MultiDBWorkUnitFilter here
		// Note that the filters applied here will be invoked for all routes, including the webJARs and any static assets.
		// If this imposes a strain on your system, you can define the filter with @FilterWith at each controller class.
		filters.add(MultiDBWorkUnitFilter.class);
	}
}
