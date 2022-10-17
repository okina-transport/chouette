/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */

package mobi.chouette.exchange.gtfs.exporter;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.TimeUtil;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.common.monitor.JamonUtils;
import mobi.chouette.dao.ConnectionLinkDAO;
import mobi.chouette.exchange.exporter.ExportableData;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.exchange.gtfs.exporter.producer.GtfsRouteProducer;
import mobi.chouette.exchange.gtfs.exporter.producer.GtfsServiceProducer;
import mobi.chouette.exchange.gtfs.exporter.producer.GtfsShapeProducer;
import mobi.chouette.exchange.gtfs.exporter.producer.GtfsTripProducer;
import mobi.chouette.exchange.gtfs.model.exporter.GtfsExporter;
import mobi.chouette.exchange.gtfs.parameters.IdParameters;
import mobi.chouette.exchange.metadata.Metadata;
import mobi.chouette.exchange.metadata.NeptuneObjectPresenter;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_STATE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_TYPE;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.*;
import mobi.chouette.model.util.NamingUtil;

import javax.naming.InitialContext;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

/**
 *
 */
@Log4j
public class GtfsLineProducerCommand implements Command, Constant {
	public static final String COMMAND = "GtfsLineProducerCommand";

	private ConnectionLinkDAO connectionLinkDao;

	@Override
	public boolean execute(Context context) throws Exception {
		boolean result = ERROR;
		Monitor monitor = MonitorFactory.start(COMMAND);
		ActionReporter reporter = ActionReporter.Factory.getInstance();

		try {

			Line line = (Line) context.get(LINE);
			GtfsExportParameters configuration = (GtfsExportParameters) context.get(CONFIGURATION);

			ExportableData collection = (ExportableData) context.get(EXPORTABLE_DATA);
			if (collection == null) {
				collection = new ExportableData();
                List<ScheduledStopPoint> scheduledStopPoints = (List<ScheduledStopPoint>) context.get(SCHEDULED_STOP_POINTS);
				collection.getScheduledStopPoints().addAll(scheduledStopPoints);
				context.put(EXPORTABLE_DATA, collection);
			}
			reporter.addObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, NamingUtil.getName(line),
					OBJECT_STATE.OK, IO_TYPE.OUTPUT);
			if (line.getCompany() == null && line.getNetwork() == null) {
				log.info("Ignoring line without company or network: " + line.getObjectId());
				reporter.addErrorToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE,
						ActionReporter.ERROR_CODE.INVALID_FORMAT, "no company for this line");
				return SUCCESS;
			}

			LocalDate startDate = null;
			if (configuration.getStartDate() != null) {
				startDate = TimeUtil.toLocalDate(configuration.getStartDate());
			}

			LocalDate endDate = null;
			if (configuration.getEndDate() != null) {
				endDate = TimeUtil.toLocalDate(configuration.getEndDate());
			}

			GtfsDataCollector collector = new GtfsDataCollector(collection, line, startDate, endDate);
			collector.setConnectionLinkDAO(connectionLinkDao);

			boolean cont = collector.collect();
			reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.LINE, 0);
			reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.JOURNEY_PATTERN,
					collection.getJourneyPatterns().size());
			reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.ROUTE, collection
					.getRoutes().size());
			reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.VEHICLE_JOURNEY,
					collection.getVehicleJourneys().size());
			reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.INTERCHANGE,
					collection.getInterchanges().size());

			if (cont) {
				context.put(EXPORTABLE_DATA, collection);

				saveLine(context, line);
				reporter.setStatToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE, OBJECT_TYPE.LINE, 1);
				result = SUCCESS;
			} else {
				reporter.addErrorToObjectReport(context, line.getObjectId(), OBJECT_TYPE.LINE,
						ActionReporter.ERROR_CODE.NO_DATA_ON_PERIOD, "no data on period");
				result = SUCCESS; // else export will stop here
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			JamonUtils.logMagenta(log, monitor);
		}

		return result;
	}

	private boolean saveLine(Context context, Line line) {
		Metadata metadata = (Metadata) context.get(METADATA);
		GtfsExporter exporter = (GtfsExporter) context.get(GTFS_EXPORTER);
		GtfsServiceProducer calendarProducer = new GtfsServiceProducer(exporter);
		GtfsTripProducer tripProducer = new GtfsTripProducer(exporter);
		GtfsRouteProducer routeProducer = new GtfsRouteProducer(exporter);
		GtfsShapeProducer shapeProducer = new GtfsShapeProducer(exporter);

		GtfsExportParameters configuration = (GtfsExportParameters) context.get(CONFIGURATION);
		String prefix = configuration.getObjectIdPrefix();
		ExportableData collection = (ExportableData) context.get(EXPORTABLE_DATA);
		Map<String, List<Timetable>> timetables = collection.getTimetableMap();
		Set<JourneyPattern> jps = new HashSet<JourneyPattern>();

		boolean hasLine = false;
		boolean hasVj = false;
		// utiliser la collection
		if (!collection.getVehicleJourneys().isEmpty()) {
			for (VehicleJourney vj : collection.getVehicleJourneys()) {

				String tmKey = calendarProducer.key(vj.getTimetables(), prefix, configuration.isKeepOriginalId());
				if (vj.hasTimetables() && vj.isNeitherCancelledNorReplaced()) {
					String timeTableServiceId = calendarProducer.key(vj.getTimetables(), prefix, configuration.isKeepOriginalId());
					if (timeTableServiceId != null) {
						IdParameters idParams = new IdParameters(configuration.getStopIdPrefix(), configuration.getIdFormat(), configuration.getIdSuffix(), configuration.getLineIdPrefix(), configuration.getCommercialPointIdPrefix());

						if (tripProducer.save(vj, tmKey, prefix, configuration.isKeepOriginalId(), idParams)) {
							hasVj = true;
							jps.add(vj.getJourneyPattern());
							// TODO : Check merge entur : Le if du dessous est supprimé dans Entur. Doit-on le garder ?
							if (!timetables.containsKey(tmKey)) {
								timetables.put(tmKey, new ArrayList<>(vj.getTimetables()));
							}
						}
					}

				} // vj loop
				for (JourneyPattern jp : jps) {
					shapeProducer.save(jp, prefix, configuration.isKeepOriginalId());
				}
				if (hasVj) {
					IdParameters idParams = new IdParameters(configuration.getStopIdPrefix(), configuration.getIdFormat(), configuration.getIdSuffix(), configuration.getLineIdPrefix(), configuration.getCommercialPointIdPrefix());
					routeProducer.save(line, prefix, configuration.isKeepOriginalId(), configuration.isUseTpegHvt(), idParams);
					hasLine = true;
					if (metadata != null) {
						metadata.getResources().add(
								new Metadata.Resource(NeptuneObjectPresenter.getName(line.getNetwork()),
										NeptuneObjectPresenter.getName(line)));
					}
				}
			}
		}

		return hasLine;
	}

	public void setConnectionLinkDao(ConnectionLinkDAO connectionLinkDao) {
		this.connectionLinkDao = connectionLinkDao;
	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = new GtfsLineProducerCommand();
			return result;
		}
	}

	static {
		CommandFactory.factories.put(GtfsLineProducerCommand.class.getName(), new DefaultCommandFactory());
	}

}
