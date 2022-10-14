package mobi.chouette.ws;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application extends javax.ws.rs.core.Application implements
		ServletContextListener {

	@Context
	private UriInfo uriInfo;
	
	@Override
	public Set<Class<?>> getClasses() {
		Set<Class<?>> result = new HashSet<Class<?>>();
		result.add(RestService.class);
		result.add(RestAdmin.class);
		result.add(RestStatisticsService.class);
		result.add(RestNetexStopPlaceService.class);
		result.add(HealthResource.class);
		return result;
	}

	@Override
	public Set<Object> getSingletons() {
		Set<Object> result = new HashSet<Object>();
		return result;
	}

	@Override
	public Map<String, Object> getProperties() {
		Map<String, Object> result = new HashMap<String, Object>();
		return result;
	}

}
