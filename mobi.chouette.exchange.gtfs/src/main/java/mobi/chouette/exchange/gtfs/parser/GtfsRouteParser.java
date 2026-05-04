package mobi.chouette.exchange.gtfs.parser;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.ObjectIdUtil;
import mobi.chouette.exchange.gtfs.NetworksNames;
import mobi.chouette.exchange.gtfs.importer.GtfsImportParameters;
import mobi.chouette.exchange.gtfs.model.GtfsAgency;
import mobi.chouette.exchange.gtfs.model.GtfsRoute;
import mobi.chouette.exchange.gtfs.model.importer.AbstractRouteById.FIELDS;
import mobi.chouette.exchange.gtfs.model.importer.*;
import mobi.chouette.exchange.gtfs.validation.Constant;
import mobi.chouette.exchange.gtfs.validation.GtfsValidationReporter;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.importer.Validator;
import mobi.chouette.model.*;
import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.util.*;
import java.util.List;

@Slf4j
public class GtfsRouteParser implements Parser, Validator, Constant {

    static {
        ParserFactory.register(GtfsRouteParser.class.getName(), new ParserFactory() {
            @Override
            protected Parser create() {
                return new GtfsRouteParser();
            }
        });
    }

    @Getter
    @Setter
    private String gtfsRouteId;

    /**
     * Parse the GTFS file routes.txt into a virtual list of GtfsRoute. This
     * list is virtual: (Re-)Parse the list to access a GtfsRoute.
     * <p>
     * Validation rules of type I and II are checked during this step, and
     * results are stored in reports.
     */
    @Override
    public void validate(Context context) throws Exception {
        GtfsImporter importer = (GtfsImporter) context.get(PARSER);
        GtfsValidationReporter gtfsValidationReporter = (GtfsValidationReporter) context.get(GTFS_REPORTER);
        Set<String> agencyIds = new HashSet<>();
        gtfsValidationReporter.getExceptions().clear();
        Set<String> targetRoute = (Set<String>) context.get(GTFS_TARGET_ROUTE_ID);
        Set<String> unmatchedRouteIds = new HashSet<>(targetRoute.size());
        unmatchedRouteIds.addAll(targetRoute);

        // routes.txt
        if (importer.hasRouteImporter()) { // the file "routes.txt" exists ?
            gtfsValidationReporter.reportSuccess(context, GTFS_1_GTFS_Common_1, GTFS_ROUTES_FILE);

            Index<GtfsRoute> parser = null;
            try { // Read and check the header line of the file "routes.txt"
                parser = importer.getRouteById();
            } catch (Exception ex) {
                if (ex instanceof GtfsException) {
                    gtfsValidationReporter.reportError(context, (GtfsException) ex, GTFS_ROUTES_FILE);
                } else {
                    gtfsValidationReporter.throwUnknownError(context, ex, GTFS_ROUTES_FILE);
                }
            }

            gtfsValidationReporter.validateOkCSV(context, GTFS_ROUTES_FILE);

            if (parser == null) { // importer.getRouteById() fails for any other
                // reason
                gtfsValidationReporter.throwUnknownError(context, new Exception("Cannot instantiate RouteById class"),
                        GTFS_ROUTES_FILE);
            } else {
                gtfsValidationReporter.validate(context, GTFS_ROUTES_FILE, parser.getOkTests());
                gtfsValidationReporter.validateUnknownError(context);
            }

            if (!parser.getErrors().isEmpty()) {
                gtfsValidationReporter.reportErrors(context, parser.getErrors(), GTFS_ROUTES_FILE);
                parser.getErrors().clear();
            }

            gtfsValidationReporter.validateOKGeneralSyntax(context, GTFS_ROUTES_FILE);

            if (parser.getLength() == 0) {
                gtfsValidationReporter.reportError(context, new GtfsException(GTFS_ROUTES_FILE, 1, null,
                        GtfsException.ERROR.FILE_WITH_NO_ENTRY, null, null), GTFS_ROUTES_FILE);
            } else {
                gtfsValidationReporter.validate(context, GTFS_ROUTES_FILE, GtfsException.ERROR.FILE_WITH_NO_ENTRY);
            }

            GtfsException fatalException = null;
            parser.setWithValidation(true);
            Map<String, String> routeNamesMap = new HashMap<>();
            Map<String, GtfsRoute> parsedRoutes = new LinkedHashMap<>();
            Map<String, GtfsRouteNamePosition> parsedRoutesNameIndex = new HashMap<>();
            for (GtfsRoute bean : parser) {
                try {
                    parser.validate(bean, importer);
                } catch (Exception ex) {
                    if (ex instanceof GtfsException) {
                        gtfsValidationReporter.reportError(context, bean.getRouteId(), (GtfsException) ex,
                                GTFS_ROUTES_FILE);
                    } else {
                        gtfsValidationReporter.throwUnknownError(context, ex, GTFS_ROUTES_FILE);
                    }
                }
                if (!targetRoute.isEmpty() && targetRoute.contains(bean.getRouteId())) {
                    unmatchedRouteIds.remove(bean.getRouteId());
                }
                parsedRoutesNameIndex.put(bean.getRouteId(), new GtfsRouteNamePosition(parser.getIndex(FIELDS.route_short_name.name()), parser.getIndex(FIELDS.route_long_name.name()), parser.getPath()));
                parsedRoutes.put(bean.getRouteId(), bean.copy());
            }
            if (!unmatchedRouteIds.isEmpty()) {
                GtfsException gtfsException = new GtfsException("routes.txt", 1, "route_id", GtfsException.ERROR.UNMATCHED_TARGET_ROUTE_ID, GTFS_TARGET_ROUTE_ID, String.join(",", unmatchedRouteIds));
                gtfsValidationReporter.reportError(context, gtfsException, "routes.txt");
                context.put(GTFS_UNMATCHED_TARGET_ROUTE_ID, unmatchedRouteIds);
                if (unmatchedRouteIds.size() == targetRoute.size()) {
                    context.put(GTFS_TARGET_ROUTE_ID, new HashSet<>(0));
                    targetRoute.clear();
                }
            }
            GtfsRouteNamePosition gtfsRouteNamePosition;
            for (Map.Entry<String, GtfsRoute> parsedRouteEntry : parsedRoutes.entrySet()) {
                if (targetRoute.isEmpty() || targetRoute.contains(parsedRouteEntry.getKey())) {
                    GtfsRoute bean = parsedRouteEntry.getValue();
                    setAgencyInfo(bean, agencyIds);
                    gtfsRouteNamePosition = parsedRoutesNameIndex.get(parsedRouteEntry.getKey());
                    if (gtfsRouteNamePosition != null) {
                        setRouteInfo(bean, routeNamesMap, gtfsRouteNamePosition.getPath(), gtfsRouteNamePosition.getRouteLongNameIndex(), gtfsRouteNamePosition.getRouteLongNameIndex());
                    } else {
                        setRouteInfo(bean, routeNamesMap, parser.getPath(), -1, -1);
                    }

                    for (GtfsException ex : bean.getErrors()) {
                        if (ex.isFatal())
                            fatalException = ex;
                    }
                    gtfsValidationReporter.reportErrors(context, bean.getRouteId(), bean.getErrors(), GTFS_ROUTES_FILE);
                    gtfsValidationReporter.validate(context, GTFS_ROUTES_FILE, bean.getOkTests());
                } else {
                    log.info("Target route option is enabled - skipping route {}", parsedRouteEntry.getKey());
                }

            }
            parser.setWithValidation(false);
            int i = 1;
            boolean unsuedId = true;
            for (GtfsAgency bean : importer.getAgencyById()) {
                if (agencyIds.add(bean.getAgencyId())) {
                    unsuedId = false;
                    gtfsValidationReporter.reportError(context,
                            new GtfsException(GTFS_AGENCY_FILE, i, AgencyById.FIELDS.agency_id.name(),
                                    GtfsException.ERROR.UNUSED_ID, null, bean.getAgencyId()), GTFS_AGENCY_FILE);
                }
                i++;
            }
            if (unsuedId)
                gtfsValidationReporter.validate(context, GTFS_ROUTES_FILE, GtfsException.ERROR.UNUSED_ID);
            if (fatalException != null)
                throw fatalException;
        } else {
            gtfsValidationReporter.reportError(context, new GtfsException(GTFS_ROUTES_FILE, 1, null,
                    GtfsException.ERROR.MISSING_FILE, null, null), GTFS_ROUTES_FILE);
        }
    }

    private void setRouteInfo(GtfsRoute bean, Map<String, String> routeNamesMap, String path, int indexRouteLongName, int indexRouteShortName) {
        if (bean.getRouteShortName() != null && bean.getRouteLongName() != null) {
            String key = bean.getRouteShortName() + "\n" + bean.getRouteLongName();
            String reverseKey = bean.getRouteLongName() + "\n" + bean.getRouteShortName();
            if (routeNamesMap.containsKey(reverseKey)) {
                bean.getErrors().add(
                        new GtfsException(path, bean.getId(), indexRouteLongName, FIELDS.route_long_name.name(),
                                GtfsException.ERROR.INVERSE_DUPLICATE_ROUTE_NAMES, bean.getRouteId(),
                                routeNamesMap.get(reverseKey)));

            } else {
                bean.getOkTests().add(GtfsException.ERROR.INVERSE_DUPLICATE_ROUTE_NAMES);
            }
            if (routeNamesMap.containsKey(key)) {
                bean.getErrors().add(
                        new GtfsException(path, bean.getId(), indexRouteShortName, FIELDS.route_short_name.name(),
                                GtfsException.ERROR.DUPLICATE_ROUTE_NAMES, bean.getRouteId(), routeNamesMap
                                .get(key)));

            } else {
                bean.getOkTests().add(GtfsException.ERROR.DUPLICATE_ROUTE_NAMES);
                routeNamesMap.put(key, bean.getRouteId());
            }
        }
    }

    private void setAgencyInfo(GtfsRoute bean, Set<String> agencyIds) {
        if (bean.getAgencyId() != null) {
            agencyIds.add(bean.getAgencyId());
        } else {
            agencyIds.add(GtfsAgency.DEFAULT_ID);
        }
    }

    /**
     * Translate every (mobi.chouette.exchange.gtfs.model.)GtfsRoute to a
     * (mobi.chouette.model.)Line.
     * <p>
     * Validation rules of type III are checked at this step.
     */
    @Override
    public void parse(Context context) throws Exception {

        Referential referential = (Referential) context.get(REFERENTIAL);
        GtfsImportParameters configuration = (GtfsImportParameters) context.get(CONFIGURATION);
        GtfsImporter importer = (GtfsImporter) context.get(PARSER);

        Index<GtfsRoute> routes = importer.getRouteById();
        GtfsRoute gtfsRoute = routes.getValue(gtfsRouteId);

        String lineId = ObjectIdUtil.composeObjectId(configuration.isSplitIdOnDot(), configuration.getObjectIdPrefix(), Line.LINE_KEY, gtfsRouteId);
        Line line = ObjectFactory.getLine(referential, lineId);

        convert(context, gtfsRoute, line);

        String agencyId = null;

        if (context.get(TARGET_COMPANY_OBJECT_ID) != null) {
            agencyId = StringUtils.chop(ObjectIdUtil.extractOriginalId((String) context.get(TARGET_COMPANY_OBJECT_ID)));
        } else {
            agencyId = gtfsRoute.getAgencyId();
            if (agencyId == null) {
                agencyId = configuration.getReferentialName();
            }
        }

        String operatorId = ObjectIdUtil.composeObjectId(configuration.isSplitIdOnDot(), configuration.getObjectIdPrefix(),
                Company.OPERATOR_KEY, agencyId + "o");
        Company operator = ObjectFactory.getCompany(referential, operatorId);
        line.setCompany(operator);

        // PTNetwork
        String ptNetworkId;
        if (context.get(TARGET_NETWORK_OBJECT_ID) != null) {
            ptNetworkId = (String) context.get(TARGET_NETWORK_OBJECT_ID);
        } else {
            ptNetworkId = ObjectIdUtil.composeObjectId(configuration.isSplitIdOnDot(), configuration.getObjectIdPrefix(),
                    Network.PTNETWORK_KEY, agencyId);
        }
        Network ptNetwork = ObjectFactory.getPTNetwork(referential, ptNetworkId);
        if (ptNetwork.getCompany() == null) {
            String authorityId = ObjectIdUtil.composeObjectId(configuration.isSplitIdOnDot(), configuration.getObjectIdPrefix(),
                    Company.AUTHORITY_KEY, agencyId);
            Company authority = ObjectFactory.getCompany(referential, authorityId);
            ptNetwork.setCompany(authority);
            ptNetwork.setName(authority.getName());
        }

        line.setNetwork(ptNetwork);

        // Route VehicleJourney VehicleJourneyAtStop , JourneyPattern ,StopPoint
        GtfsTripParser gtfsTripParser = (GtfsTripParser) ParserFactory.create(GtfsTripParser.class.getName());
        gtfsTripParser.setGtfsRouteId(gtfsRouteId);
        gtfsTripParser.parse(context);

        if (StringUtils.isNotBlank(configuration.getExternalRefField()) || StringUtils.isNotBlank(configuration.getDriverControllerCodeField())) {
            for (JourneyPattern journeyPattern : referential.getJourneyPatterns().values()) {
                if (StringUtils.isNotBlank(configuration.getExternalRefField())) {
                    copyKeyValueFromVJ(journeyPattern, EXTERNAL_REF);
                }
                if (StringUtils.isNotBlank(configuration.getDriverControllerCodeField())) {
                    copyKeyValueFromVJ(journeyPattern, DRIVER_CONTROLLER_CODE);
                }
            }
        }

        referential.getSharedLines().put(line.getObjectId(), line);

    }

    private void copyKeyValueFromVJ(JourneyPattern journeyPattern, String key) {
        if (CollectionUtils.isEmpty(journeyPattern.getVehicleJourneys())) {
            return;
        }
        if (CollectionUtils.isEmpty(journeyPattern.getVehicleJourneys().get(0).getKeyValues())) {
            return;
        }
        for (KeyValue keyValue : journeyPattern.getVehicleJourneys().get(0).getKeyValues()) {
            if (keyValue.getKey().equals(key)) {
                KeyValue copy = new KeyValue();
                copy.setKey(keyValue.getKey());
                copy.setValue(keyValue.getValue());
                copy.setTypeOfKey(keyValue.getTypeOfKey());
                journeyPattern.getKeyValues().add(copy);
            }
        }
    }

    protected void convert(Context context, GtfsRoute gtfsRoute, Line line) {
        GtfsImportParameters configuration = (GtfsImportParameters) context.get(CONFIGURATION);

        List incomingLineList = (List) context.get(INCOMING_LINE_LIST);
        incomingLineList.add(line.getObjectId());

        NetworksNames networksNames = new NetworksNames();

        line.setName(StringUtils.trimToNull(gtfsRoute.getRouteLongName()));

        line.setNumber(StringUtils.trimToNull(gtfsRoute.getRouteShortName()));

        line.setPublishedName(StringUtils.trimToNull(gtfsRoute.getRouteLongName()));

        if (line.getName() == null) {
            line.setName(line.getNumber());
        }

        if (networksNames.getTerritorializedSites(configuration.getObjectIdPrefix())) {
            line.setTransportModeName(TransportModeNameEnum.Coach);
        } else {
            line.setTransportModeName(gtfsRoute.getRouteType().getTransportMode());
        }
        line.setTransportSubModeName(gtfsRoute.getRouteType().getSubMode());

        String[] token = line.getObjectId().split(":");
        line.setRegistrationNumber(token[2]);
        line.setComment(gtfsRoute.getRouteDesc());
        line.setColor(toHexa(gtfsRoute.getRouteColor()));
        line.setTextColor(toHexa(gtfsRoute.getRouteTextColor()));
        line.setUrl(AbstractConverter.toString(gtfsRoute.getRouteUrl()));
        line.setFilled(true);
        if (configuration.isRouteSortOrder()) {
            line.setPosition(gtfsRoute.getPosition());
        }

        KeyValue keyValue = new KeyValue();
        keyValue.setKey(EXTERNAL_REF);
        keyValue.setValue(getGtfsRouteId());
        keyValue.setTypeOfKey("ALTERNATIVE_IDENTIFIER");
        line.getKeyValues().add(keyValue);
    }

    private String toHexa(Color color) {
        if (color == null)
            return null;
        String result = Integer.toHexString(color.getRGB());
        if (result.length() == 8)
            result = result.substring(2);
        while (result.length() < 6)
            result = "0" + result;
        return result;
    }
}
