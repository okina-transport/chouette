package mobi.chouette.exchange.gtfs.parser;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.gtfs.NetworksNames;
import mobi.chouette.exchange.gtfs.importer.GtfsImportParameters;
import mobi.chouette.exchange.gtfs.model.GtfsAgency;
import mobi.chouette.exchange.gtfs.model.GtfsRoute;
import mobi.chouette.exchange.gtfs.model.importer.*;
import mobi.chouette.exchange.gtfs.validation.Constant;
import mobi.chouette.exchange.gtfs.validation.GtfsValidationReporter;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.importer.Validator;
import mobi.chouette.model.Company;
import mobi.chouette.model.Line;
import mobi.chouette.model.Network;
import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;

import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Log4j
public class GtfsRouteParser implements Parser, Validator, Constant {

    @Getter
    @Setter
    private String gtfsRouteId;


    @Getter
    @Setter
    private Integer position;

    /**
     * Parse the GTFS file routes.txt into a virtual list of GtfsRoute. This
     * list is virtual: (Re-)Parse the list to access a GtfsRoute.
     * <p>
     * Validation rules of type I and II are checked during this step, and
     * results are stored in reports.
     */
    // TODO. Rename this function "parse(Context context)".
    @Override
   public void validate(Context context) throws Throwable {
        GtfsImporter importer = (GtfsImporter) context.get(PARSER);
        GtfsValidationReporter gtfsValidationReporter = (GtfsValidationReporter) context.get(GTFS_REPORTER);
        gtfsValidationReporter.getExceptions().clear();

        // routes.txt
        // log.info("validating routes");
        if (importer.hasRouteImporter()) { // the file "routes.txt" exists ?

            gtfsValidationReporter.reportSuccess(context, GTFS_1_GTFS_Common_1, GTFS_ROUTES_FILE);

            Optional<Index<GtfsRoute>> parserOpt = Optional.empty();
            try {
                parserOpt = Optional.of(importer.getRouteById());
            } catch (GtfsException ex) {
                gtfsValidationReporter.reportError(context, ex, GTFS_ROUTES_FILE);
            } catch (Exception ex) {
                gtfsValidationReporter.throwUnknownError(context, ex, GTFS_ROUTES_FILE);
            }

            final Index<GtfsRoute> parser = parserOpt.orElseThrow(() -> {
                try {
                    return gtfsValidationReporter.throwUnknownError(context, new Exception("Cannot instantiate RouteById class"), GTFS_ROUTES_FILE);
                } catch (Exception e) {
                    // Gérer l'exception ici si nécessaire
                    throw new RuntimeException(e); // Réemballer l'exception en RuntimeException pour la lambda
                }
            });

            gtfsValidationReporter.validateOkCSV(context, GTFS_ROUTES_FILE);

            gtfsValidationReporter.validate(context, GTFS_ROUTES_FILE, parser.getOkTests());
            gtfsValidationReporter.validateUnknownError(context);

            if (!parser.getErrors().isEmpty()) {
                gtfsValidationReporter.reportErrors(context, parser.getErrors(), GTFS_ROUTES_FILE);
                parser.getErrors().clear();
            }

            gtfsValidationReporter.validateOKGeneralSyntax(context, GTFS_ROUTES_FILE);

            if (parser.getLength() == 0) {
                gtfsValidationReporter.reportError(context, new GtfsException(GTFS_ROUTES_FILE, 1, null, GtfsException.ERROR.FILE_WITH_NO_ENTRY, null, null), GTFS_ROUTES_FILE);
            } else {
                gtfsValidationReporter.validate(context, GTFS_ROUTES_FILE, GtfsException.ERROR.FILE_WITH_NO_ENTRY);
            }

            //agency in route.txt
            Set<String> agencyIds = StreamSupport.stream(parser.spliterator(), false).map(GtfsRoute::getAgencyId).collect(Collectors.toSet());
            //agency in agency.txt
            Stream<GtfsAgency> gtfsAgencyStream = StreamSupport.stream(importer.getAgencyById().spliterator(), false);

            GtfsException fatalException = StreamSupport.stream(parser.spliterator(), false)
                    .peek(bean -> parser.setWithValidation(true))
                    .peek(bean -> {
                        try {
                            parser.validate(bean, importer);
                        } catch (Exception ex) {
                            try {
                                throw new RuntimeException(gtfsValidationReporter.throwUnknownError(context, ex, GTFS_ROUTES_FILE));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }).flatMap(bean -> {
                        Set<GtfsException> beanErrors = bean.getErrors();
                        try {
                            gtfsValidationReporter.reportErrors(context, beanErrors, GTFS_ROUTES_FILE);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return beanErrors.stream();
                    }).filter(ex -> this.isFatalException(ex, agencyIds))
                    .findFirst()
                    .orElse(null);

            parser.setWithValidation(false);

            // Validation des IDs d'agence non utilisées
            long count = StreamSupport.stream(importer.getAgencyById().spliterator(), false).count();

            gtfsAgencyStream
                    .filter(bean -> !agencyIds.contains(bean.getAgencyId()))
                    .forEach(bean -> {
                        try {
                            gtfsValidationReporter.reportError(context, new GtfsException(GTFS_AGENCY_FILE, 1, AgencyById.FIELDS.agency_id.name(), GtfsException.ERROR.UNUSED_ID, null, bean.getAgencyId()), GTFS_AGENCY_FILE);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });

            handleFatalException(gtfsValidationReporter, context, fatalException, GTFS_ROUTES_FILE, agencyIds);
        } else {
            gtfsValidationReporter.reportError(context, new GtfsException(GTFS_ROUTES_FILE, 1, null,
                    GtfsException.ERROR.MISSING_FILE, null, null), GTFS_ROUTES_FILE);
        }
    }

    /**
     * Translate every (mobi.chouette.exchange.gtfs.model.)GtfsRoute to a
     * (mobi.chouette.model.)Line.
     * <p>
     * Validation rules of type III are checked at this step.
     */
    // TODO. Rename this function "translate(Context context)" or
    // "produce(Context context)", ...
    @Override
    public void parse(Context context) throws Exception {

        Referential referential = (Referential) context.get(REFERENTIAL);
        GtfsImportParameters configuration = (GtfsImportParameters) context.get(CONFIGURATION);
        GtfsImporter importer = (GtfsImporter) context.get(PARSER);

        Index<GtfsRoute> routes = importer.getRouteById();
        GtfsRoute gtfsRoute = routes.getValue(gtfsRouteId);

        String lineId = AbstractConverter.composeObjectId(configuration, Line.LINE_KEY, gtfsRouteId);
        Line line = ObjectFactory.getLine(referential, lineId);
        line.setPosition(position);
        convert(context, gtfsRoute, line);


        String agencyId = gtfsRoute.getAgencyId();
        if (agencyId == null) {
            agencyId = configuration.getReferentialName();
        }

        String operatorId = AbstractConverter.composeObjectId(configuration,
                Company.OPERATOR_KEY, agencyId + "o");
        Company operator = ObjectFactory.getCompany(referential, operatorId);
        line.setCompany(operator);

        // PTNetwork
        String ptNetworkId = AbstractConverter.composeObjectId(configuration,
                Network.PTNETWORK_KEY, agencyId);
        Network ptNetwork = ObjectFactory.getPTNetwork(referential, ptNetworkId);
        if (ptNetwork.getCompany() == null) {
            String authorityId = AbstractConverter.composeObjectId(configuration,
                    Company.AUTHORITY_KEY, agencyId);
            Company authority = ObjectFactory.getCompany(referential, authorityId);
            ptNetwork.setCompany(authority);
            ptNetwork.setName(authority.getName());
        }

        line.setNetwork(ptNetwork);

        // Route VehicleJourney VehicleJourneyAtStop , JourneyPattern ,StopPoint
        GtfsTripParser gtfsTripParser = (GtfsTripParser) ParserFactory.create(GtfsTripParser.class.getName());
        gtfsTripParser.setGtfsRouteId(gtfsRouteId);
        gtfsTripParser.setPosition(position);
        gtfsTripParser.parse(context);

    }

    protected void convert(Context context, GtfsRoute gtfsRoute, Line line) {
        GtfsImportParameters configuration = (GtfsImportParameters) context.get(CONFIGURATION);

        List incomingLineList = (List) context.get(INCOMING_LINE_LIST);
        incomingLineList.add(line.getObjectId());

        NetworksNames networksNames = new NetworksNames();

        line.setName(AbstractConverter.getNonEmptyTrimedString(gtfsRoute.getRouteLongName()));

        line.setNumber(AbstractConverter.getNonEmptyTrimedString(gtfsRoute.getRouteShortName()));

        line.setPublishedName(AbstractConverter.getNonEmptyTrimedString(gtfsRoute.getRouteLongName()));

        if (line.getName() == null) {
            line.setName(line.getNumber());
        }

        if(networksNames.getTerritorializedSites(configuration.getObjectIdPrefix())){
            line.setTransportModeName(TransportModeNameEnum.Coach);
        }
        else{
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

    // Méthode utilitaire pour vérifier si une exception est fatale
    private boolean isFatalException(GtfsException ex, Set<String> agenciesIds) {
        if(agenciesIds.size() <= 1 && AbstractRouteById.FIELDS.agency_id.name().equals(ex.getField()) && GtfsException.ERROR.UNREFERENCED_ID.equals(ex.getError())) {
            return false;
        }
        return ex != null && ex.isFatal();
    }

    // Méthode utilitaire pour traiter les exceptions fatales
    private void handleFatalException(GtfsValidationReporter reporter, Context context, GtfsException fatalException, String fileName, Set<String> agenciesIds) throws Exception {
        if (fatalException != null && isFatalException(fatalException, agenciesIds))
            reporter.throwUnknownError(context, fatalException, fileName);
    }

    static {
        ParserFactory.register(GtfsRouteParser.class.getName(), new ParserFactory() {
            @Override
            protected Parser create() {
                return new GtfsRouteParser();
            }
        });
    }
}
