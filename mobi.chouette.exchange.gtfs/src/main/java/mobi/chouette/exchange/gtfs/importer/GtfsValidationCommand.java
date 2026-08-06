package mobi.chouette.exchange.gtfs.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.FileUtil;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.exchange.gtfs.model.fares.GtfsFareV1File;
import mobi.chouette.exchange.gtfs.model.fares.GtfsFareV2File;
import mobi.chouette.exchange.gtfs.model.importer.GtfsException;
import mobi.chouette.exchange.gtfs.parser.*;
import mobi.chouette.exchange.gtfs.validation.GtfsValidationReporter;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ActionReporter.ERROR_CODE;
import mobi.chouette.exchange.report.ActionReporter.FILE_STATE;
import mobi.chouette.exchange.report.IO_TYPE;
import org.apache.commons.lang3.StringUtils;

import javax.naming.InitialContext;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Log4j
public class GtfsValidationCommand implements Command, Constant {

	public static final String COMMAND = "GtfsValidationCommand";

	private static final List<String> processableAllFiles = Arrays.asList(GTFS_AGENCY_FILE, GTFS_STOPS_FILE, GTFS_ROUTES_FILE, GTFS_SHAPES_FILE, GTFS_TRIPS_FILE, GTFS_STOP_TIMES_FILE, GTFS_CALENDAR_FILE, GTFS_CALENDAR_DATES_FILE, GTFS_FREQUENCIES_FILE, GTFS_TRANSFERS_FILE, GTFS_TRANSLATIONS_FILE, GTFS_TRIP_COMPANIES_FILE, GTFS_TRIP_EXTENSIONS_FILE);

	private static final List<String> processableStopAreaFiles = Arrays.asList(GTFS_STOPS_FILE, GTFS_TRANSFERS_FILE, GTFS_TRANSLATIONS_FILE);

	private static final List<String> GTFS_FARES_V2_FILE = Arrays.stream(GtfsFareV2File.values()).map(GtfsFareV2File::getFilename).collect(Collectors.toList());

	private static final List<String> GTFS_FARES_V1_FILE = Arrays.stream(GtfsFareV1File.values()).map(GtfsFareV1File::getFilename).collect(Collectors.toList());

	static {
		CommandFactory.factories.put(GtfsValidationCommand.class.getName(), new DefaultCommandFactory());
	}

	@Override
	public boolean execute(Context context) throws Exception {
		boolean result = ERROR;

		Monitor monitor = MonitorFactory.start(COMMAND);

		ActionReporter reporter = ActionReporter.Factory.getInstance();

		JobData jobData = (JobData) context.get(JOB_DATA);
		// check ignored files
		Path path = Paths.get(jobData.getPathName(), INPUT);

		if ("exporter".equals(jobData.getAction()) || "globalExport".equals(jobData.getAction())) {
			// on exports, files are copied to tmp directory before validation, to avoid .nfs file creations
			path = FileUtil.getTmpPath(path);
		}

		List<Path> list = FileUtil.listFiles(path, "*");

		GtfsImportParameters parameters = (GtfsImportParameters) context.get(CONFIGURATION);
		boolean all = !(parameters.getReferencesType().equalsIgnoreCase("stop_area"));

		boolean importFareFiles = parameters.isImportFareFiles();

		Set<String> targetRouteId = new HashSet<>();
		if (StringUtils.isNotBlank(parameters.getImportTargetRoutes())) {
			String[] routeIds = parameters.getImportTargetRoutes().split(",");
            targetRouteId.addAll(Arrays.asList(routeIds));
		}
		context.put(GTFS_TARGET_ROUTE_ID, targetRouteId);

		List<String> processableFiles = new ArrayList<>(processableAllFiles);
		if (importFareFiles) {
			processableFiles.addAll(GTFS_FARES_V1_FILE);
			processableFiles.addAll(GTFS_FARES_V2_FILE);
		}
		if (!all) {
			processableFiles = processableStopAreaFiles;
		}

		GtfsValidationReporter gtfsValidationReporter = (GtfsValidationReporter) context.get(GTFS_REPORTER);
		for (Path fileName : list) {
			if (!processableFiles.contains(fileName.getFileName().toString())) {
				reporter.setFileState(context, fileName.getFileName().toString(), IO_TYPE.INPUT, FILE_STATE.IGNORED);
				gtfsValidationReporter.reportError(context, new GtfsException(fileName.getFileName().toString(), 1, null, GtfsException.ERROR.UNUSED_FILE, null, null), fileName.getFileName().toString());
			} else {
				// TODO : implement a new status : UNCHECKED
				reporter.setFileState(context, fileName.getFileName().toString(), IO_TYPE.INPUT, FILE_STATE.IGNORED);
			}
		}

		try {
			if (all) {
				// agency.txt
				GtfsAgencyParser agencyParser = (GtfsAgencyParser) ParserFactory.create(GtfsAgencyParser.class.getName());
				agencyParser.validate(context);

				// routes.txt
				GtfsRouteParser routeParser = (GtfsRouteParser) ParserFactory.create(GtfsRouteParser.class.getName());
				routeParser.validate(context);
			}

			if (importFareFiles) {
				// fare_attributes.txt
				GtfsFareAttributeParser gtfsFareAttributeParser = (GtfsFareAttributeParser) ParserFactory.create(GtfsFareAttributeParser.class.getName());
				gtfsFareAttributeParser.validate(context);

				// fare_rules.txt
				GtfsFareRuleParser gtfsFareRuleParser = (GtfsFareRuleParser) ParserFactory.create(GtfsFareRuleParser.class.getName());
				gtfsFareRuleParser.validate(context);

				GtfsFareMediaParser gtfsFareMediaParser = (GtfsFareMediaParser) ParserFactory.create(GtfsFareMediaParser.class.getName());
				gtfsFareMediaParser.validate(context);

				GtfsFareRiderCategoriesParser riderCategoriesParser = (GtfsFareRiderCategoriesParser) ParserFactory.create(GtfsFareRiderCategoriesParser.class.getName());
				riderCategoriesParser.validate(context);

				GtfsFareProductsParser fareProductsParser = (GtfsFareProductsParser) ParserFactory.create(GtfsFareProductsParser.class.getName());
				fareProductsParser.validate(context);

				GtfsFareLegRulesParser gtfsFareLegRulesParser = (GtfsFareLegRulesParser) ParserFactory.create(GtfsFareLegRulesParser.class.getName());
				gtfsFareLegRulesParser.validate(context);

				if (!parameters.isParseConnectionLinks()) {
					GtfsTransferParser transferParser = (GtfsTransferParser) ParserFactory.create(GtfsTransferParser.class.getName());
					transferParser.validate(context);
				}
			}

			// stops.txt
			GtfsStopParser stopParser = (GtfsStopParser) ParserFactory.create(GtfsStopParser.class.getName());
			stopParser.validate(context);

			if (all) {
				// calendar.txt & calendar_dates.txt
				GtfsCalendarParser calendarParser = (GtfsCalendarParser) ParserFactory.create(GtfsCalendarParser.class.getName());
				calendarParser.validate(context);

				// shapes.txt, trips.txt, stop_times.txt & frequencies.txt
				GtfsTripParser tripParser = (GtfsTripParser) ParserFactory.create(GtfsTripParser.class.getName());
				tripParser.validate(context);

				Map<String, List<Map<String, String>>> duplicateTripStructure = (Map<String, List<Map<String, String>>>) context.get("duplicateTripStructure");
				if (duplicateTripStructure != null) {
					calendarParser.compareServiceIdsAndGroupTripsToAnnomalyDetection(context, duplicateTripStructure);
				}
			}

			// transfers.txt
			if (parameters.isParseConnectionLinks()) {
				GtfsTransferParser transferParser = (GtfsTransferParser) ParserFactory.create(GtfsTransferParser.class.getName());
				transferParser.validate(context);
			}

			// translations.txt
			GtfsTranslationParser translationParser = (GtfsTranslationParser) ParserFactory.create(GtfsTranslationParser.class.getName());
			translationParser.validate(context);

			// companies.txt
			GtfsTripCompanyParser tripCompanyParser = (GtfsTripCompanyParser) ParserFactory.create(GtfsTripCompanyParser.class.getName());
			tripCompanyParser.validate(context);

			// trip_extensions.txt
			GtfsTripExtensionParser tripExtensionParser = (GtfsTripExtensionParser) ParserFactory.create(GtfsTripExtensionParser.class.getName());
			tripExtensionParser.validate(context);

			result = SUCCESS;
		} catch (GtfsException e) {
			// log.error(e,e);
			if (e.getError().equals(GtfsException.ERROR.SYSTEM))
				throw e;
			else
				reporter.setActionError(context, ERROR_CODE.INVALID_DATA, e.getError().name() + " " + e.getPath());

		} catch (Exception e) {
			if (e instanceof RuntimeException)
				log.error(e, e);
			throw e;
		} finally {
			log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
		}

		return result;
	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = new GtfsValidationCommand();
			return result;
		}
	}
}
