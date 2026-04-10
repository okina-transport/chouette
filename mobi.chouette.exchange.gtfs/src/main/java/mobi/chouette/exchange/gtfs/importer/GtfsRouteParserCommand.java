package mobi.chouette.exchange.gtfs.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.exchange.gtfs.parser.GtfsAgencyParser;
import mobi.chouette.exchange.gtfs.parser.GtfsCalendarParser;
import mobi.chouette.exchange.gtfs.parser.GtfsRouteParser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_STATE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_TYPE;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.*;
import mobi.chouette.model.type.Utils;
import mobi.chouette.model.util.NamingUtil;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import java.time.LocalDate;

import javax.naming.InitialContext;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Log4j
public class GtfsRouteParserCommand implements Command, Constant {

	public static final String COMMAND = "GtfsRouteParserCommand";

	@Getter
	@Setter
	private String gtfsRouteId;

	@Override
	public boolean execute(Context context) throws Exception {
		boolean result = ERROR;

		Monitor monitor = MonitorFactory.start(COMMAND);

		try {
			Referential referential = (Referential) context.get(REFERENTIAL);
			if (referential != null) {
				referential.clear(true);
			}

			GtfsImportParameters configuration = (GtfsImportParameters) context.get(CONFIGURATION);

			ActionReporter reporter = ActionReporter.Factory.getInstance();

			// PTNetwork
			if (referential.getSharedPTNetworks().isEmpty()) {
				createPTNetwork(referential, configuration);
				reporter.addObjectReport(context, "merged", OBJECT_TYPE.NETWORK, "networks", OBJECT_STATE.OK,
						IO_TYPE.INPUT);
				reporter.setStatToObjectReport(context, "merged", OBJECT_TYPE.NETWORK, OBJECT_TYPE.NETWORK,
						referential.getSharedPTNetworks().size());
			}

			// Company
			if (referential.getSharedCompanies().isEmpty()) {
				GtfsAgencyParser gtfsAgencyParser = (GtfsAgencyParser) ParserFactory.create(GtfsAgencyParser.class
						.getName());
				gtfsAgencyParser.parse(context);
				reporter.addObjectReport(context, "merged", OBJECT_TYPE.COMPANY, "companies", OBJECT_STATE.OK,
						IO_TYPE.INPUT);
				reporter.setStatToObjectReport(context, "merged", OBJECT_TYPE.COMPANY, OBJECT_TYPE.COMPANY,
						referential.getSharedCompanies().size());
			}
			
			// Timetable
			if (referential.getSharedTimetables().isEmpty()) {
				GtfsCalendarParser gtfsCalendarParser = (GtfsCalendarParser) ParserFactory
						.create(GtfsCalendarParser.class.getName());
				gtfsCalendarParser.parse(context);
				reporter.addObjectReport(context, "merged", OBJECT_TYPE.TIMETABLE, "time tables", OBJECT_STATE.OK,
						IO_TYPE.INPUT);
				reporter.setStatToObjectReport(context, "merged", OBJECT_TYPE.TIMETABLE, OBJECT_TYPE.TIMETABLE,
						referential.getSharedTimetables().size());
			}

			// Line
			GtfsRouteParser gtfsRouteParser = (GtfsRouteParser) ParserFactory.create(GtfsRouteParser.class.getName());
			gtfsRouteParser.setGtfsRouteId(gtfsRouteId);
			gtfsRouteParser.parse(context);

			addStats(context, referential);
			result = SUCCESS;
		} catch (Exception e) {
			log.error("error : ", e);
			throw e;
		}

		log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
		return result;
	}

	private Network createPTNetwork(Referential referential, GtfsImportParameters configuration) {
		String prefix = configuration.getObjectIdPrefix();
		String ptNetworkId = prefix + ":" + Network.PTNETWORK_KEY + ":" + prefix;
		Network ptNetwork = ObjectFactory.getPTNetwork(referential, ptNetworkId);
		ptNetwork.setVersionDate(LocalDate.now());
		ptNetwork.setName(prefix);
		ptNetwork.setRegistrationNumber(prefix);
		ptNetwork.setSourceName("GTFS");
		return ptNetwork;
	}

	private void addStats(Context context, Referential referential) {
		ActionReporter reporter = ActionReporter.Factory.getInstance();

		Line line = referential.getLines().values().iterator().next();

		Set<String> stopAreaIds = new HashSet<>();
		if (line.getRoutes() != null) {
			for (Route route : line.getRoutes()) {
				if (route.getStopPoints() != null) {
					for (StopPoint sp : route.getStopPoints()) {
						Optional<StopArea> stopAreaOpt = Utils.getStopAreaFromScheduledStopPoint(sp);
						stopAreaOpt.ifPresent(stopArea->stopAreaIds.add(stopArea.getObjectId()));
					}
				}
			}
		}

		Map<String, StopArea> referentialStopArea = referential.getStopAreas();
		Set<String> newStopAreaIds = new HashSet<>(stopAreaIds);
		if (referentialStopArea != null) {
			newStopAreaIds.removeAll(referentialStopArea.keySet());
		}

		reporter.addObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, NamingUtil.getName(line),
				OBJECT_STATE.OK, IO_TYPE.INPUT);
		reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.LINE, 1);
		reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.JOURNEY_PATTERN,
				referential.getJourneyPatterns().size());
		reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.ROUTE, referential
				.getRoutes().size());
		reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.VEHICLE_JOURNEY,
				referential.getVehicleJourneys().size());
		reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.INTERCHANGE,
				referential.getInterchanges().size());
		reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.STOP_AREA,
				referential.getStopAreas().size());
		reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.STOP_AREA_NEW,
				newStopAreaIds.size());

	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = new GtfsRouteParserCommand();
			return result;
		}
	}

	static {
		CommandFactory.factories.put(GtfsRouteParserCommand.class.getName(), new DefaultCommandFactory());
	}
}
