package mobi.chouette.exchange.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.AgencyDAO;
import mobi.chouette.dao.FareAttributeDAO;
import mobi.chouette.exchange.importer.updater.FareAttributeOptimiser;
import mobi.chouette.exchange.importer.updater.FareAttributeUpdater;
import mobi.chouette.exchange.importer.updater.Updater;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ActionReporter.ERROR_CODE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_STATE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_TYPE;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.FareAttribute;
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
@Stateless(name = FareAttributeRegisterCommand.COMMAND)
public class FareAttributeRegisterCommand implements Command {

	public static final String COMMAND = "FareAttributeRegisterCommand";

	static {
		CommandFactory.factories.put(FareAttributeRegisterCommand.class.getName(), new DefaultCommandFactory());
	}

	@EJB
	private FareAttributeOptimiser optimiser;

	@EJB
	private FareAttributeDAO fareAttributeDAO;

	@EJB
	private AgencyDAO agencyDAO;

	@EJB(beanName = FareAttributeUpdater.BEAN_NAME)
	private Updater<FareAttribute> fareAttributeUpdater;

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
		Map<String, FareAttribute> newValue = referential.getFareAttributes();
		for (FareAttribute fareAttribute : newValue.values()) {
			log.info("Register FareAttribute : " + fareAttribute.getObjectId());
			try {
				optimiser.initialize(cache, referential);
				FareAttribute oldValue = cache.getFareAttributes().get(fareAttribute.getObjectId());
				fareAttributeUpdater.update(context, oldValue, fareAttribute);
				fareAttributeDAO.create(oldValue);
				fareAttributeDAO.flush();

				result = SUCCESS;
			} catch (Exception ex) {
				log.error(ex.getMessage());
				ActionReporter reporter = ActionReporter.Factory.getInstance();
				reporter.addObjectReport(context, fareAttribute.getObjectId(), OBJECT_TYPE.FARE_ATTRIBUTE, NamingUtil.getName(fareAttribute), OBJECT_STATE.ERROR, IO_TYPE.INPUT);
				if (ex.getCause() != null) {
					Throwable e = ex.getCause();
					while (e.getCause() != null) {
						log.error(e.getMessage());
						e = e.getCause();
					}
					if (e instanceof SQLException) {
						e = ((SQLException) e).getNextException();
						reporter.addErrorToObjectReport(context, fareAttribute.getObjectId(), OBJECT_TYPE.FARE_ATTRIBUTE, ERROR_CODE.WRITE_ERROR, e.getMessage());

					} else {
						reporter.addErrorToObjectReport(context, fareAttribute.getObjectId(), OBJECT_TYPE.FARE_ATTRIBUTE, ERROR_CODE.INTERNAL_ERROR, e.getMessage());
					}
				} else {
					reporter.addErrorToObjectReport(context, fareAttribute.getObjectId(), OBJECT_TYPE.FARE_ATTRIBUTE, ERROR_CODE.INTERNAL_ERROR, ex.getMessage());
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
