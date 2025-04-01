package mobi.chouette.exchange.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.FareRuleDAO;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.exchange.importer.updater.FareRuleOptimiser;
import mobi.chouette.exchange.importer.updater.FareRuleUpdater;
import mobi.chouette.exchange.importer.updater.LineUpdater;
import mobi.chouette.exchange.importer.updater.Updater;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ActionReporter.ERROR_CODE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_STATE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_TYPE;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.FareRule;
import mobi.chouette.model.Line;
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
	private LineDAO lineDAO;

	@EJB(beanName = LineUpdater.BEAN_NAME)
	private Updater<Line> lineUpdater;

	@EJB(beanName = FareRuleUpdater.BEAN_NAME)
	private Updater<FareRule> fareRuleUpdater;

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
		Map<String, FareRule> newValue = referential.getFareRules();
		for (FareRule fareRule : newValue.values()) {
			log.info("Register FareRule : " + fareRule.getObjectId());
			try {
				optimiser.initialize(cache, referential);

				FareRule oldValue = cache.getFareRules().get(fareRule.getObjectId());
				for (Line item : fareRule.getLines()) {
					Line line = cache.getLines().get(item.getObjectId());

					if (line == null) {
						lineDAO.create(item);
						oldValue.addLine(item);
					} else {
						Line findedLine = lineDAO.findByObjectId(line.getObjectId());
						if (findedLine == null) {
							lineDAO.create(line);
							oldValue.addLine(line);
						} else {
							oldValue.addLine(findedLine);
						}
					}
				}

				FareRule findedFareRule = fareRuleDAO.findByObjectId(oldValue.getObjectId());
				if (findedFareRule == null) {
					fareRuleDAO.create(oldValue);
				} else {
					fareRuleUpdater.update(context, findedFareRule, oldValue);
				}
				fareRuleDAO.flush();

				result = SUCCESS;
			} catch (Exception ex) {
				log.error(ex.getMessage());
				ActionReporter reporter = ActionReporter.Factory.getInstance();
				reporter.addObjectReport(context, fareRule.getObjectId(), OBJECT_TYPE.FARE_RULE, NamingUtil.getName(fareRule), OBJECT_STATE.ERROR, IO_TYPE.INPUT);
				if (ex.getCause() != null) {
					Throwable e = ex.getCause();
					while (e.getCause() != null) {
						log.error(e.getMessage());
						e = e.getCause();
					}
					if (e instanceof SQLException) {
						e = ((SQLException) e).getNextException();
						reporter.addErrorToObjectReport(context, fareRule.getObjectId(), OBJECT_TYPE.FARE_RULE, ERROR_CODE.WRITE_ERROR, e.getMessage());

					} else {
						reporter.addErrorToObjectReport(context, fareRule.getObjectId(), OBJECT_TYPE.FARE_RULE, ERROR_CODE.INTERNAL_ERROR, e.getMessage());
					}
				} else {
					reporter.addErrorToObjectReport(context, fareRule.getObjectId(), OBJECT_TYPE.FARE_RULE, ERROR_CODE.INTERNAL_ERROR, ex.getMessage());
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
