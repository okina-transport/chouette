package mobi.chouette.exchange.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.FareRuleDAO;
import mobi.chouette.dao.RouteDAO;
import mobi.chouette.exchange.importer.updater.FareRuleOptimiser;
import mobi.chouette.exchange.importer.updater.FareRuleUpdater;
import mobi.chouette.exchange.importer.updater.RouteUpdater;
import mobi.chouette.exchange.importer.updater.Updater;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ActionReporter.ERROR_CODE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_STATE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_TYPE;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.FareRule;
import mobi.chouette.model.Route;
import mobi.chouette.model.util.NamingUtil;
import mobi.chouette.model.util.Referential;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.sql.SQLException;

@Log4j
@Stateless(name = FareRuleRegisterCommand.COMMAND)
public class FareRuleRegisterCommand implements Command {

	public static final String COMMAND = "FareRuleRegisterCommand";

	static {
		CommandFactory.factories.put(FareRuleRegisterCommand.class.getName(), new DefaultCommandFactory());
	}

	@EJB
	private FareRuleOptimiser optimiser;

	@EJB
	private FareRuleDAO fareRuleDAO;

	@EJB
	private RouteDAO routeDAO;

	@EJB(beanName = FareRuleUpdater.BEAN_NAME)
	private Updater<FareRule> fareRuleUpdater;

	@EJB(beanName = RouteUpdater.BEAN_NAME)
	private Updater<Route> routeUpdater;

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public boolean execute(Context context) throws Exception {

		boolean result = ERROR;
		Monitor monitor = MonitorFactory.start(COMMAND);

		if (!context.containsKey(OPTIMIZED)) {
			context.put(OPTIMIZED, Boolean.TRUE);
		}
		Referential cache = new Referential();
		context.put(CACHE, cache);

		Referential referential = (Referential) context.get(REFERENTIAL);

		// Use property based enabling of stop place updater, but allow disabling if property exist in context
		FareRule newValue = referential.getFareRules().values().iterator().next();
		context.put(CURRENT_FARE_RULE_ID, newValue.getObjectId());

		log.info("register fare rule : " + newValue.getObjectId());
		try {
			optimiser.initialize(cache, referential);

			FareRule oldValue = cache.getFareRules().get(newValue.getObjectId());
			Route oldRouteValue = cache.getRoutes().get(newValue.getRoute().getObjectId());

			Route findedRoute = routeDAO.findByObjectId(oldRouteValue.getObjectId());

			if (oldValue.getId() == null && findedRoute == null) {
				routeDAO.create(oldRouteValue);
			} else if (findedRoute != null) {
				routeUpdater.update(context, findedRoute, oldRouteValue);
			}

			Route findedRouteAfterSave = routeDAO.findByObjectId(oldRouteValue.getObjectId());
			oldValue.setRoute(findedRouteAfterSave);

			if (oldValue.getId() == null) {
				fareRuleDAO.create(oldValue);
			} else {
				fareRuleUpdater.update(context, oldValue, newValue);
			}
			fareRuleDAO.flush();

			result = SUCCESS;
		} catch (Exception ex) {
			log.error(ex.getMessage());
			ActionReporter reporter = ActionReporter.Factory.getInstance();
			reporter.addObjectReport(context, newValue.getObjectId(),
					OBJECT_TYPE.FARE_RULE, NamingUtil.getName(newValue), OBJECT_STATE.ERROR, IO_TYPE.INPUT);
			if (ex.getCause() != null) {
				Throwable e = ex.getCause();
				while (e.getCause() != null) {
					log.error(e.getMessage());
					e = e.getCause();
				}
				if (e instanceof SQLException) {
					e = ((SQLException) e).getNextException();
					reporter.addErrorToObjectReport(context, newValue.getObjectId(), OBJECT_TYPE.FARE_RULE, ERROR_CODE.WRITE_ERROR, e.getMessage());

				} else {
					reporter.addErrorToObjectReport(context, newValue.getObjectId(), OBJECT_TYPE.FARE_RULE, ERROR_CODE.INTERNAL_ERROR, e.getMessage());
				}
			} else {
				reporter.addErrorToObjectReport(context, newValue.getObjectId(), OBJECT_TYPE.FARE_RULE, ERROR_CODE.INTERNAL_ERROR, ex.getMessage());
			}
			throw ex;
		} finally {
			log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
		}
		return result;
	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = null;
			try {
				String name = "java:app/mobi.chouette.exchange/" + COMMAND;
				result = (Command) context.lookup(name);
			} catch (NamingException e) {
				// try another way on test context
				String name = "java:module/" + COMMAND;
				try {
					result = (Command) context.lookup(name);
				} catch (NamingException e1) {
					log.error(e);
				}
			}
			return result;
		}
	}
}
