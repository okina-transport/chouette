package mobi.chouette.exchange.netexprofile.importer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import javax.naming.InitialContext;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.parser.PublicationDeliveryParser;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ActionReporter.FILE_ERROR_CODE;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.*;
import mobi.chouette.model.type.Utils;import mobi.chouette.model.util.NamingUtil;
import mobi.chouette.model.util.Referential;
import org.apache.commons.lang3.StringUtils;

@Log4j
public class NetexLineParserCommand implements Command, Constant {

    public static final String COMMAND = "NetexLineParserCommand";

    @Getter
    @Setter
    private Path path;

    @Override
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;
        Monitor monitor = MonitorFactory.start(COMMAND);

        String fileName = path.getFileName().toString();

        ActionReporter reporter = ActionReporter.Factory.getInstance();
        reporter.addFileReport(context, fileName, IO_TYPE.INPUT);
        context.put(FILE_NAME, fileName);

        try {
            Referential referential = (Referential) context.get(REFERENTIAL);
            if (referential != null) {
                referential.clear(true);
            }

            PublicationDeliveryParser parser = (PublicationDeliveryParser) ParserFactory.create(PublicationDeliveryParser.class.getName());
            parser.parse(context);

            feedLineInfoWithCommonData(context);

			addStats(context, reporter, referential);


            reporter.setFileState(context, fileName, IO_TYPE.INPUT, ActionReporter.FILE_STATE.OK);
            result = SUCCESS;
        } catch (Exception e) {
            log.error("ERROR", e);
            reporter.addFileErrorInReport(context, fileName, FILE_ERROR_CODE.INTERNAL_ERROR, e.toString());
            reporter.setActionError(context, ActionReporter.ERROR_CODE.INVALID_DATA, "Error");
            throw e;
        } finally {
            log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
        }

        return result;
    }

    /**
     * Feed the current line with information coming from common files.
     * (Common files are parsed before line files. And each time NetexLineParser is called, referential is cleared.
     * To avoid data loss, line information are stored in context and then pulled back to current line in feedLineInfoWithCommonData)
     */
	private void feedLineInfoWithCommonData(Context context) {
		Referential referential = (Referential) context.get(REFERENTIAL);
		if (referential == null) return;

		Set<String> companyIds = new HashSet<>();
		Set<String> networkIds = new HashSet<>();
		Set<String> journeyPatternIds = new HashSet<>();
		Set<String> routeIds = new HashSet<>();
		Set<String> vehicleJourneyIds = new HashSet<>();
		Set<String> connectionLinkIds = new HashSet<>();
		Set<String> timetableIds = new HashSet<>();
		Set<String> accessPointIds = new HashSet<>();
		Set<String> stopAreaIds = new HashSet<>();
		Set<String> stopPointIds = new HashSet<>();

		for (Line currentLine : referential.getLines().values()) {
			String currentLineId = currentLine.getObjectId();

			if (referential.getSharedLines().containsKey(currentLineId)) {
				Line currentLineInfo = referential.getSharedLines().get(currentLineId);

				if (StringUtils.isEmpty(currentLine.getName())) currentLine.setName(currentLineInfo.getName());
				if (StringUtils.isEmpty(currentLine.getNumber())) currentLine.setNumber(currentLineInfo.getNumber());
				if (StringUtils.isEmpty(currentLine.getColor())) currentLine.setColor(currentLineInfo.getColor());
				if (StringUtils.isEmpty(currentLine.getComment())) currentLine.setComment(currentLineInfo.getComment());

				currentLine.setTransportModeName(currentLineInfo.getTransportModeName());

				if (StringUtils.isEmpty(currentLine.getCodifligne())) currentLine.setCodifligne(currentLineInfo.getCodifligne());
				if (StringUtils.isEmpty(currentLine.getPublishedName())) currentLine.setPublishedName(currentLineInfo.getPublishedName());
				if (StringUtils.isEmpty(currentLine.getRegistrationNumber())) currentLine.setRegistrationNumber(currentLineInfo.getRegistrationNumber());
				if (StringUtils.isEmpty(currentLine.getTextColor())) currentLine.setTextColor(currentLineInfo.getTextColor());
				if (StringUtils.isEmpty(currentLine.getUrl())) currentLine.setUrl(currentLineInfo.getUrl());
				if (StringUtils.isEmpty(currentLine.getCreatorId())) currentLine.setCreatorId(currentLineInfo.getCreatorId());

				if (currentLine.getBike() == null) currentLine.setBike(currentLineInfo.getBike());
				if (currentLine.getCategoriesForLine() == null) currentLine.setCategoriesForLine(currentLineInfo.getCategoriesForLine());
				if (currentLine.getCompany() == null) currentLine.setCompany(currentLineInfo.getCompany());
				if (currentLine.getFlexibleLineProperties() == null) currentLine.setFlexibleLineProperties(currentLineInfo.getFlexibleLineProperties());
				if (currentLine.getFlexibleService() == null) currentLine.setFlexibleService(currentLineInfo.getFlexibleService());
				if (currentLine.getFootnotes() == null || currentLine.getFootnotes().isEmpty()) currentLine.setFootnotes(currentLineInfo.getFootnotes());
				if (currentLine.getGroupOfLines() == null || currentLine.getGroupOfLines().isEmpty()) currentLine.setGroupOfLines(currentLineInfo.getGroupOfLines());
				if (currentLine.getIntUserNeeds() == null) currentLine.setIntUserNeeds(currentLineInfo.getIntUserNeeds());
				if (currentLine.getNetwork() == null) currentLine.setNetwork(currentLineInfo.getNetwork());
				if (currentLine.getAccessibilityAssessment() == null) currentLine.setAccessibilityAssessment(currentLineInfo.getAccessibilityAssessment());
				if (currentLine.getPosition() == null) currentLine.setPosition(currentLineInfo.getPosition());
				if (currentLine.getTransportSubModeName() == null) currentLine.setTransportSubModeName(currentLineInfo.getTransportSubModeName());
				if (currentLine.getUserNeeds() == null) currentLine.setUserNeeds(currentLineInfo.getUserNeeds());
				if (currentLine.getTad() == null) currentLine.setTad(currentLineInfo.getTad());
				if (currentLine.getKeyValues() == null || currentLine.getKeyValues().isEmpty()) currentLine.setKeyValues(currentLineInfo.getKeyValues());
			}

			if (currentLine.getNetwork() != null) {
				Network network = currentLine.getNetwork();
				networkIds.add(network.getObjectId());
				if (network.getCompany() != null) {
					companyIds.add(network.getCompany().getObjectId());
				}
			}

			if (currentLine.getRoutes() != null) {
				for (Route route : currentLine.getRoutes()) {
					routeIds.add(route.getObjectId());

					if (route.getJourneyPatterns() != null) {
						for (JourneyPattern jp : route.getJourneyPatterns()) {
							journeyPatternIds.add(jp.getObjectId());

							if (jp.getVehicleJourneys() != null) {
								for (VehicleJourney vj : jp.getVehicleJourneys()) {
									if (vj.getTimetables() != null) {
										for (Timetable tt : vj.getTimetables()) {
											timetableIds.add(tt.getObjectId());
										}
									}
									vehicleJourneyIds.add(vj.getObjectId());
								}
							}
						}
					}

					if (route.getStopPoints() != null) {
						for (StopPoint sp : route.getStopPoints()) {
							stopPointIds.add(sp.getObjectId());
							Optional<StopArea> stopAreaOpt = Utils.getStopAreaFromScheduledStopPoint(sp);

							if (stopAreaOpt.isPresent()){
								StopArea stopArea = stopAreaOpt.get();
								stopAreaIds.add(stopArea.getObjectId());

								if (stopArea.getConnectionStartLinks() != null) {
									for (ConnectionLink cl : stopArea.getConnectionStartLinks()) {
										connectionLinkIds.add(cl.getObjectId());
									}
								}
								if (stopArea.getConnectionEndLinks() != null) {
									for (ConnectionLink cl : stopArea.getConnectionEndLinks()) {
										connectionLinkIds.add(cl.getObjectId());
									}
								}
								if (stopArea.getAccessPoints() != null) {
									for (AccessPoint ap : stopArea.getAccessPoints()) {
										accessPointIds.add(ap.getObjectId());
									}
								}
							}
						}
					}
				}
			}
		}

		Map<String, StopArea> referentialStopArea = referential.getStopAreas();
        Set<String> newStopAreaIds = new HashSet<>(stopAreaIds);
        if (referentialStopArea != null) {
            newStopAreaIds.removeAll(referentialStopArea.keySet());
        }

		Map<String, StopPoint> referentialStopPoint = referential.getStopPoints();

		Set<String> newStopPointIds = new HashSet<>(stopPointIds);
		if (referentialStopPoint != null) {
			newStopPointIds.removeAll(referentialStopPoint.keySet());
		}

		Map<String, Integer> statsMap = new HashMap<>();
		statsMap.put("COMPANY_COUNT", companyIds.size());
		statsMap.put("NETWORK_COUNT", networkIds.size());
		statsMap.put("JOURNEY_PATTERN_COUNT", journeyPatternIds.size());
		statsMap.put("ROUTE_COUNT", routeIds.size());
		statsMap.put("VEHICLE_JOURNEY_COUNT", vehicleJourneyIds.size());
		statsMap.put("CONNECTION_LINK_COUNT", connectionLinkIds.size());
		statsMap.put("TIMETABLE_COUNT", timetableIds.size());
		statsMap.put("ACCESS_POINT_COUNT", accessPointIds.size());
		statsMap.put("STOP_AREA_COUNT", stopAreaIds.size());
		statsMap.put("STOP_AREA_NEW_COUNT", newStopAreaIds.size());
		statsMap.put("STOP_POINT_COUNT", stopPointIds.size());
		statsMap.put("STOP_POINT_NEW_COUNT", newStopPointIds.size());

		context.put("PARSER_STATS_MAP", statsMap);
	}

    private void addStats(Context context, ActionReporter reporter, Referential referential) {
        if (referential != null && !referential.getLines().isEmpty()) {
            Line line = referential.getLines().values().iterator().next();
            Map<String, Integer> stats = (Map<String, Integer>) context.get("PARSER_STATS_MAP");

            if (stats == null) return;

            String lineId = line.getObjectId();
            String lineName = NamingUtil.getName(line);

            reporter.addObjectReport(context, lineId, ActionReporter.OBJECT_TYPE.LINE, lineName, ActionReporter.OBJECT_STATE.OK, IO_TYPE.INPUT);

            reporter.setStatToObjectReport(context, lineId, ActionReporter.OBJECT_TYPE.LINE, ActionReporter.OBJECT_TYPE.LINE, 1);

			reporter.setStatToObjectReport(context, lineId, ActionReporter.OBJECT_TYPE.LINE, ActionReporter.OBJECT_TYPE.COMPANY,
                                stats.getOrDefault("COMPANY_COUNT", 0));

			reporter.setStatToObjectReport(context, lineId, ActionReporter.OBJECT_TYPE.LINE, ActionReporter.OBJECT_TYPE.NETWORK,
                                stats.getOrDefault("NETWORK_COUNT", 0));

			reporter.setStatToObjectReport(context, lineId, ActionReporter.OBJECT_TYPE.LINE, ActionReporter.OBJECT_TYPE.JOURNEY_PATTERN,
                                stats.getOrDefault("JOURNEY_PATTERN_COUNT", 0));

            reporter.setStatToObjectReport(context, lineId, ActionReporter.OBJECT_TYPE.LINE, ActionReporter.OBJECT_TYPE.JOURNEY_PATTERN,
                    stats.getOrDefault("JOURNEY_PATTERN_COUNT", 0));

            reporter.setStatToObjectReport(context, lineId, ActionReporter.OBJECT_TYPE.LINE, ActionReporter.OBJECT_TYPE.ROUTE,
                    stats.getOrDefault("ROUTE_COUNT", 0));

            reporter.setStatToObjectReport(context, lineId, ActionReporter.OBJECT_TYPE.LINE, ActionReporter.OBJECT_TYPE.VEHICLE_JOURNEY,
                    stats.getOrDefault("VEHICLE_JOURNEY_COUNT", 0));

            reporter.setStatToObjectReport(context, lineId, ActionReporter.OBJECT_TYPE.LINE, ActionReporter.OBJECT_TYPE.CONNECTION_LINK,
                    stats.getOrDefault("CONNECTION_LINK_COUNT", 0));

            reporter.setStatToObjectReport(context, lineId, ActionReporter.OBJECT_TYPE.LINE, ActionReporter.OBJECT_TYPE.TIMETABLE,
                    stats.getOrDefault("TIMETABLE_COUNT", 0));

            reporter.setStatToObjectReport(context, lineId, ActionReporter.OBJECT_TYPE.LINE, ActionReporter.OBJECT_TYPE.ACCESS_POINT,
                    stats.getOrDefault("ACCESS_POINT_COUNT", 0));

            reporter.setStatToObjectReport(context, lineId, ActionReporter.OBJECT_TYPE.LINE, ActionReporter.OBJECT_TYPE.STOP_AREA,
                    stats.getOrDefault("STOP_AREA_COUNT", 0));

			reporter.setStatToObjectReport(context, lineId, ActionReporter.OBJECT_TYPE.LINE, ActionReporter.OBJECT_TYPE.STOP_POINT,
                    stats.getOrDefault("STOP_POINT_COUNT", 0));

			reporter.setStatToObjectReport(context, lineId, ActionReporter.OBJECT_TYPE.LINE, ActionReporter.OBJECT_TYPE.STOP_AREA_NEW,
                    stats.getOrDefault("STOP_AREA_NEW_COUNT", 0));

            reporter.setStatToObjectReport(context, lineId, ActionReporter.OBJECT_TYPE.LINE, ActionReporter.OBJECT_TYPE.STOP_POINT_NEW,
                    stats.getOrDefault("STOP_POINT_NEW_COUNT", 0));
        }
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = new NetexLineParserCommand();
            return result;
        }
    }

    static {
        CommandFactory.factories.put(NetexLineParserCommand.class.getName(),
                new DefaultCommandFactory());
    }
}
