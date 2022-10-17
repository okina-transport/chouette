/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */

package mobi.chouette.exchange.validation;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.common.monitor.JamonUtils;
import mobi.chouette.exchange.validation.checkpoint.*;
import mobi.chouette.exchange.validation.report.ValidationReport;

import javax.naming.InitialContext;
import java.io.IOException;

/**
 *
 */
@Log4j
public class LineValidatorCommand implements Command, Constant 
{
	public static final String COMMAND = "LineValidatorCommand";

	private LineCheckPoints lineCheckPoints = new LineCheckPoints();
	private RouteCheckPoints routeCheckPoints = new RouteCheckPoints();
	private JourneyPatternCheckPoints journeyPatternCheckPoints = new JourneyPatternCheckPoints();
	private VehicleJourneyCheckPoints vehicleJourneyCheckPoints = new VehicleJourneyCheckPoints();
	private DatedServiceJourneyCheckPoints datedServiceJourneyCheckPointsCheckPoints = new DatedServiceJourneyCheckPoints();
	private StopPointCheckPoints stopPointCheckPoints = new StopPointCheckPoints();

	@Override
	public boolean execute(Context context) throws Exception {
		boolean result = ERROR;
		Monitor monitor = MonitorFactory.start(COMMAND);

		ValidationReport report = (ValidationReport) context.get(VALIDATION_REPORT);
		if (report == null)
		{
			context.put(VALIDATION_REPORT, new ValidationReport());
		}
		try {
			lineCheckPoints.validate(context, null);
			routeCheckPoints.validate(context, null);
			journeyPatternCheckPoints.validate(context, null);
			vehicleJourneyCheckPoints.validate(context, null);
			datedServiceJourneyCheckPointsCheckPoints.validate(context, null);
			stopPointCheckPoints.validate(context, null);

			result = SUCCESS;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			JamonUtils.logMagenta(log, monitor);
		}

		return result;
	}



	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = new LineValidatorCommand();
			return result;
		}
	}

	static {
		CommandFactory.factories.put(LineValidatorCommand.class.getName(),
				new DefaultCommandFactory());
	}


}
