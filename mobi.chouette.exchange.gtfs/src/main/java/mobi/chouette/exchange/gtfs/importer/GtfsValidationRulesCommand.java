package mobi.chouette.exchange.gtfs.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.common.monitor.JamonUtils;
import mobi.chouette.exchange.gtfs.validation.Constant;
import mobi.chouette.exchange.gtfs.validation.GtfsValidationReporter;

import javax.naming.InitialContext;
import java.io.IOException;

@Log4j
public class GtfsValidationRulesCommand implements Command, Constant {

	public static final String COMMAND = "GtfsValidationRulesCommand";

	@Override
	public boolean execute(Context context) throws Exception {
		Monitor monitor = MonitorFactory.start(COMMAND);
		
		GtfsValidationReporter gtfsValidationReporter = (GtfsValidationReporter)context.get(GTFS_REPORTER);
		if (gtfsValidationReporter == null) {
			gtfsValidationReporter = new GtfsValidationReporter(context);
			context.put(GTFS_REPORTER, gtfsValidationReporter);
		}
		
		JamonUtils.logMagenta(log, monitor);
		return SUCCESS;
	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = new GtfsValidationRulesCommand();
			return result;
		}
	}

	static {
		CommandFactory.factories.put(GtfsValidationRulesCommand.class.getName(), new DefaultCommandFactory());
	}

}
