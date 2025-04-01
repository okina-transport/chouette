package mobi.chouette.exchange.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.dao.TransfersDAO;
import mobi.chouette.exchange.importer.updater.RouteUpdater;
import mobi.chouette.exchange.importer.updater.TransfersOptimiser;
import mobi.chouette.exchange.importer.updater.TransfersUpdater;
import mobi.chouette.exchange.importer.updater.Updater;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ActionReporter.ERROR_CODE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_STATE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_TYPE;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.Line;
import mobi.chouette.model.Route;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.Transfers;
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
import java.util.Map;

@Log4j
@Stateless(name = TransfersRegisterCommand.COMMAND)
public class TransfersRegisterCommand implements Command {

	public static final String COMMAND = "TransfersRegisterCommand";

	static {
		CommandFactory.factories.put(TransfersRegisterCommand.class.getName(), new DefaultCommandFactory());
	}

	@EJB
	private TransfersOptimiser optimiser;

	@EJB
	private TransfersDAO transfersDAO;

	@EJB
	private LineDAO lineDAO;

	@EJB
	private StopAreaDAO stopAreaDAO;

	@EJB(beanName = RouteUpdater.BEAN_NAME)
	private Updater<Route> routeUpdater;

	@EJB(beanName = TransfersUpdater.BEAN_NAME)
	private Updater<Transfers> transfersUpdater;

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
		Map<String, Transfers> newValue = referential.getTransfers();
		for (Transfers transfers : newValue.values()) {
			log.info("Register Transfers : " + transfers.getObjectId());
			try {
				optimiser.initialize(cache, referential);

				Transfers oldValue = cache.getTransfers().get(transfers.getObjectId());

				if (transfers.getFromLine() != null) {
					Line oldFromRouteValue = cache.getLines().get(transfers.getFromLine().getObjectId());
					Line findedFromRoute = lineDAO.findByObjectId(oldFromRouteValue.getObjectId());
					if (oldValue.getId() == null && findedFromRoute == null) {
						lineDAO.create(oldFromRouteValue);
						oldValue.setFromLine(oldFromRouteValue);
					} else {
						oldValue.setFromLine(oldFromRouteValue);
					}
				}

				if (transfers.getToLine() != null) {
					Line oldToRouteValue = cache.getLines().get(transfers.getToLine().getObjectId());
					Line findedToRoute = lineDAO.findByObjectId(oldToRouteValue.getObjectId());
					if (oldValue.getId() == null && findedToRoute == null) {
						lineDAO.create(oldToRouteValue);
						oldValue.setToLine(oldToRouteValue);
					} else {
						oldValue.setToLine(oldToRouteValue);
					}
				}

				if (transfers.getFromStop() != null) {
					StopArea oldFromStopAreaValue = cache.getStopAreas().get(transfers.getFromStop().getObjectId());
					StopArea findedFromStopArea = stopAreaDAO.findByOriginalId(oldFromStopAreaValue.getObjectId().split(":")[2]).get(0);
					if (findedFromStopArea == null) {
						stopAreaDAO.create(oldFromStopAreaValue);
						oldValue.setFromStop(oldFromStopAreaValue);
					} else {
						oldValue.setFromStop(findedFromStopArea);
					}
				}

				if (transfers.getFromStop() != null) {
					StopArea oldToStopAreaValue = cache.getStopAreas().get(transfers.getToStop().getObjectId());
					StopArea findedToStopArea = stopAreaDAO.findByOriginalId(oldToStopAreaValue.getObjectId().split(":")[2]).get(0);
					if (findedToStopArea == null) {
						stopAreaDAO.create(oldToStopAreaValue);
						oldValue.setToStop(oldToStopAreaValue);
					} else {
						oldValue.setToStop(findedToStopArea);
					}
				}

				Transfers findedTransfers = transfersDAO.findByObjectId(oldValue.getObjectId());
				if (findedTransfers == null) {
					transfersDAO.create(oldValue);
				} else {
					transfersUpdater.update(context, findedTransfers, oldValue);
				}
				transfersDAO.flush();

				result = SUCCESS;
			} catch (Exception ex) {
				log.error(ex.getMessage());
				ActionReporter reporter = ActionReporter.Factory.getInstance();
				reporter.addObjectReport(context, transfers.getObjectId(), OBJECT_TYPE.FARE_RULE, NamingUtil.getName(transfers), OBJECT_STATE.ERROR, IO_TYPE.INPUT);
				if (ex.getCause() != null) {
					Throwable e = ex.getCause();
					while (e.getCause() != null) {
						log.error(e.getMessage());
						e = e.getCause();
					}
					if (e instanceof SQLException) {
						e = ((SQLException) e).getNextException();
						reporter.addErrorToObjectReport(context, transfers.getObjectId(), OBJECT_TYPE.FARE_RULE, ERROR_CODE.WRITE_ERROR, e.getMessage());

					} else {
						reporter.addErrorToObjectReport(context, transfers.getObjectId(), OBJECT_TYPE.FARE_RULE, ERROR_CODE.INTERNAL_ERROR, e.getMessage());
					}
				} else {
					reporter.addErrorToObjectReport(context, transfers.getObjectId(), OBJECT_TYPE.FARE_RULE, ERROR_CODE.INTERNAL_ERROR, ex.getMessage());
				}
				throw ex;
			} finally {
				log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
			}
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
