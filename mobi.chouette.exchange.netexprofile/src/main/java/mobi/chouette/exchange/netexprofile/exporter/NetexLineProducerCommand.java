package mobi.chouette.exchange.netexprofile.exporter;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.TimeUtil;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.CompanyTranslationDAO;
import mobi.chouette.dao.ConnectionLinkDAO;
import mobi.chouette.dao.LineTranslationDAO;
import mobi.chouette.dao.NetworkTranslationDAO;
import mobi.chouette.dao.StopAreaTranslationDAO;
import mobi.chouette.dao.VehicleJourneyTranslationDAO;
import mobi.chouette.exchange.exporter.SharedDataKeys;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.CompanyTranslation;
import mobi.chouette.model.Line;
import mobi.chouette.model.LineTranslation;
import mobi.chouette.model.NeptuneIdentifiedObject;
import mobi.chouette.model.NetworkTranslation;
import mobi.chouette.model.StopAreaTranslation;
import mobi.chouette.model.Translation;
import mobi.chouette.model.VehicleJourneyTranslation;
import mobi.chouette.model.util.NamingUtil;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXParseException;

import javax.naming.InitialContext;
import javax.xml.bind.MarshalException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Log4j
public class NetexLineProducerCommand implements Command, Constant {

    public static final String COMMAND = "NetexLineProducerCommand";

    static {
        CommandFactory.factories.put(NetexLineProducerCommand.class.getName(), new NetexLineProducerCommand.DefaultCommandFactory());
    }

    private ConnectionLinkDAO connectionLinkDao;
    private NetworkTranslationDAO networkTranslationDao;
    private CompanyTranslationDAO companyTranslationDao;
    private LineTranslationDAO lineTranslationDao;
    private StopAreaTranslationDAO stopAreaTranslationDao;
    private VehicleJourneyTranslationDAO vehicleJourneyTranslationDao;

    @Override
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;
        Monitor monitor = MonitorFactory.start(COMMAND);
        ActionReporter reporter = ActionReporter.Factory.getInstance();

        try {

            Line line = (Line) context.get(LINE);
            log.info("processing line " + NamingUtil.getName(line));

            if (line != null && line.getCategoriesForLine() != null && !line.getCategoriesForLine().getName().equalsIgnoreCase("idfm")) {
                log.error("Ligne : " + line.getObjectId() + " en catégorie IDFM mais CODIFLIGNE manquant.");
                return SUCCESS;
            }

            if (line != null && StringUtils.isEmpty(line.getCodifligne()) && line.getCategoriesForLine() != null && line.getCategoriesForLine().getName().equalsIgnoreCase("idfm")) {
                reporter.addObjectReport(context, line.getObjectId(), ActionReporter.OBJECT_TYPE.LINE,
                        "Codifligne manquant", ActionReporter.OBJECT_STATE.ERROR, IO_TYPE.OUTPUT);
                reporter.setActionError(context, ActionReporter.ERROR_CODE.NO_DATA_FOUND, "Codifligne manquant");
                return ERROR;
            }

            NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(CONFIGURATION);

            if (!context.containsKey(NETWORK_FIELD_VALUE_TRANSLATIONS)) {
                // translations.txt rows with no record_id are keyed by field_value instead of an owner FK:
                // record_id-keyed rows are already reachable from the owning entity itself (entity.getTranslations()),
                // so only the ownerless field_value-keyed rows need indexing here, resolved lazily below, per
                // exported line, by matching fieldValue against the untranslated raw text of each entity actually
                // being exported. Kept as one map per owner type (rather than merged together) so that e.g. a Line
                // "comment" field_value candidate can never be mismatched onto a Network/StopArea/VehicleJourney
                // "comment" - the owner type is the table it was queried from, not a loose string discriminator.
                Map<String, List<Translation>> networkFieldValueTranslations = new HashMap<>();
                Map<String, List<Translation>> companyFieldValueTranslations = new HashMap<>();
                Map<String, List<Translation>> lineFieldValueTranslations = new HashMap<>();
                Map<String, List<Translation>> stopAreaFieldValueTranslations = new HashMap<>();
                Map<String, List<Translation>> vehicleJourneyFieldValueTranslations = new HashMap<>();

                indexFieldValueTranslations(networkTranslationDao.findAll(), NetworkTranslation::getNetwork, networkFieldValueTranslations);
                indexFieldValueTranslations(companyTranslationDao.findAll(), CompanyTranslation::getCompany, companyFieldValueTranslations);
                indexFieldValueTranslations(lineTranslationDao.findAll(), LineTranslation::getLine, lineFieldValueTranslations);
                indexFieldValueTranslations(stopAreaTranslationDao.findAll(), StopAreaTranslation::getStopArea, stopAreaFieldValueTranslations);
                indexFieldValueTranslations(vehicleJourneyTranslationDao.findAll(), VehicleJourneyTranslation::getVehicleJourney, vehicleJourneyFieldValueTranslations);

                context.put(NETWORK_FIELD_VALUE_TRANSLATIONS, networkFieldValueTranslations);
                context.put(COMPANY_FIELD_VALUE_TRANSLATIONS, companyFieldValueTranslations);
                context.put(LINE_FIELD_VALUE_TRANSLATIONS, lineFieldValueTranslations);
                context.put(STOP_AREA_FIELD_VALUE_TRANSLATIONS, stopAreaFieldValueTranslations);
                context.put(VEHICLE_JOURNEY_FIELD_VALUE_TRANSLATIONS, vehicleJourneyFieldValueTranslations);
            }

            ExportableData collection = (ExportableData) context.get(EXPORTABLE_DATA);
            if (collection == null) {
                collection = new ExportableData();
                context.put(EXPORTABLE_DATA, collection);
            } else {
                collection.clear();
            }

            ExportableNetexData exportableNetexData = (ExportableNetexData) context.get(EXPORTABLE_NETEX_DATA);
            if (exportableNetexData == null) {
                exportableNetexData = new ExportableNetexData();
                context.put(EXPORTABLE_NETEX_DATA, exportableNetexData);
            } else {
                exportableNetexData.clear();
            }

            // TODO This is currently only used to count number of elements exported
            SharedDataKeys sharedData = (SharedDataKeys) context.get(SHARED_DATA_KEYS);
            if (sharedData == null) {
                sharedData = new SharedDataKeys();
                context.put(SHARED_DATA_KEYS, sharedData);
            }

            LocalDate startDate = null;
            if (configuration.getStartDate() != null) {
                startDate = TimeUtil.toLocalDate(configuration.getStartDate());
            }

            LocalDate endDate = null;
            if (configuration.getEndDate() != null) {
                endDate = TimeUtil.toLocalDate(configuration.getEndDate());
            }

            NetexDataCollector collector = new NetexDataCollector();
            collector.setConnectionLinkDAO(connectionLinkDao);
            boolean cont = (collector.collect(collection, line, startDate, endDate, configuration.isExportGeneratedMissingQuays()));

            reporter.addObjectReport(context, line.getObjectId(), ActionReporter.OBJECT_TYPE.LINE, NamingUtil.getName(line), ActionReporter.OBJECT_STATE.OK, IO_TYPE.OUTPUT);
            reporter.setStatToObjectReport(context, line.getObjectId(), ActionReporter.OBJECT_TYPE.LINE, ActionReporter.OBJECT_TYPE.LINE, 0);
            reporter.setStatToObjectReport(context, line.getObjectId(), ActionReporter.OBJECT_TYPE.LINE, ActionReporter.OBJECT_TYPE.JOURNEY_PATTERN, collection.getJourneyPatterns().size());
            reporter.setStatToObjectReport(context, line.getObjectId(), ActionReporter.OBJECT_TYPE.LINE, ActionReporter.OBJECT_TYPE.ROUTE, collection.getRoutes().size());
            reporter.setStatToObjectReport(context, line.getObjectId(), ActionReporter.OBJECT_TYPE.LINE, ActionReporter.OBJECT_TYPE.VEHICLE_JOURNEY, collection.getVehicleJourneys().size());
            reporter.setStatToObjectReport(context, line.getObjectId(), ActionReporter.OBJECT_TYPE.LINE, ActionReporter.OBJECT_TYPE.CONNECTION_LINK, collection.getConnectionLinks().size());
            reporter.setStatToObjectReport(context, line.getObjectId(), ActionReporter.OBJECT_TYPE.LINE, ActionReporter.OBJECT_TYPE.TIMETABLE, collection.getTimetables().size());
            reporter.setStatToObjectReport(context, line.getObjectId(), ActionReporter.OBJECT_TYPE.LINE, ActionReporter.OBJECT_TYPE.ACCESS_POINT, collection.getAccessPoints().size());
            reporter.setStatToObjectReport(context, line.getObjectId(), ActionReporter.OBJECT_TYPE.LINE, ActionReporter.OBJECT_TYPE.STOP_AREA, collection.getStopAreas().size());


            if (cont) {
                try {
                    // TODO changer d'un data producer à l'autre pour changer de PROFIL IDFM Norvégien
//                    NetexLineDataProducer producer = new NetexLineDataProducer();
                    NetexLineDataFranceProducer producer = new NetexLineDataFranceProducer();
                    producer.produce(context);

                    reporter.setStatToObjectReport(context, line.getObjectId(), ActionReporter.OBJECT_TYPE.LINE, ActionReporter.OBJECT_TYPE.LINE, 1);
                    reporter.addObjectReport(context, "merged", ActionReporter.OBJECT_TYPE.NETWORK, "networks", ActionReporter.OBJECT_STATE.OK, IO_TYPE.OUTPUT);
                    reporter.setStatToObjectReport(context, "merged", ActionReporter.OBJECT_TYPE.NETWORK, ActionReporter.OBJECT_TYPE.NETWORK, sharedData.getNetworkIds().size());
                    reporter.addObjectReport(context, "merged", ActionReporter.OBJECT_TYPE.COMPANY, "companies", ActionReporter.OBJECT_STATE.OK, IO_TYPE.OUTPUT);
                    reporter.setStatToObjectReport(context, "merged", ActionReporter.OBJECT_TYPE.COMPANY, ActionReporter.OBJECT_TYPE.COMPANY, sharedData.getCompanyIds().size());
                    reporter.addObjectReport(context, "merged", ActionReporter.OBJECT_TYPE.CONNECTION_LINK, "connection links", ActionReporter.OBJECT_STATE.OK, IO_TYPE.OUTPUT);
                    reporter.setStatToObjectReport(context, "merged", ActionReporter.OBJECT_TYPE.CONNECTION_LINK, ActionReporter.OBJECT_TYPE.CONNECTION_LINK, sharedData.getConnectionLinkIds().size());
                    reporter.addObjectReport(context, "merged", ActionReporter.OBJECT_TYPE.ACCESS_POINT, "access points", ActionReporter.OBJECT_STATE.OK, IO_TYPE.OUTPUT);
                    reporter.setStatToObjectReport(context, "merged", ActionReporter.OBJECT_TYPE.ACCESS_POINT, ActionReporter.OBJECT_TYPE.ACCESS_POINT, sharedData.getAccessPointIds().size());
                    reporter.addObjectReport(context, "merged", ActionReporter.OBJECT_TYPE.STOP_AREA, "stop areas", ActionReporter.OBJECT_STATE.OK, IO_TYPE.OUTPUT);
                    reporter.setStatToObjectReport(context, "merged", ActionReporter.OBJECT_TYPE.STOP_AREA, ActionReporter.OBJECT_TYPE.STOP_AREA, sharedData.getStopAreaIds().size());
                    reporter.addObjectReport(context, "merged", ActionReporter.OBJECT_TYPE.TIMETABLE, "calendars", ActionReporter.OBJECT_STATE.OK, IO_TYPE.OUTPUT);
                    reporter.setStatToObjectReport(context, "merged", ActionReporter.OBJECT_TYPE.TIMETABLE, ActionReporter.OBJECT_TYPE.TIMETABLE, sharedData.getTimetableIds().size());
                    result = SUCCESS;
                } catch (MarshalException e) {
                    if (e.getCause() != null && e.getCause() instanceof SAXParseException) {
                        log.error(e.getCause().getMessage());
                        reporter.addErrorToObjectReport(context, line.getObjectId(), ActionReporter.OBJECT_TYPE.LINE,
                                ActionReporter.ERROR_CODE.INVALID_FORMAT, e.getCause().getMessage());
                    } else {
                        log.error(e.getMessage());
                        reporter.addErrorToObjectReport(context, line.getObjectId(), ActionReporter.OBJECT_TYPE.LINE,
                                ActionReporter.ERROR_CODE.INVALID_FORMAT, e.getMessage());
                    }
                }
            } else {
                reporter.addErrorToObjectReport(context, line.getObjectId(), ActionReporter.OBJECT_TYPE.LINE,
                        ActionReporter.ERROR_CODE.NO_DATA_ON_PERIOD, "no data on period");
                result = ERROR;
            }
        } finally {
            log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
        }

        return result;
    }

    /**
     * Indexes one owner type's translation rows that carry no owner FK (legacy field_value-keyed
     * translations.txt rows): rows with a resolved owner are already reachable via entity.getTranslations()
     * and are skipped here.
     */
    private <T extends Translation> void indexFieldValueTranslations(List<T> allTranslations,
                                                                      Function<T, ? extends NeptuneIdentifiedObject> ownerGetter,
                                                                      Map<String, List<Translation>> fieldValueKeyedTranslations) {
        for (T translation : allTranslations) {
            if (ownerGetter.apply(translation) == null && translation.getFieldValue() != null) {
                fieldValueKeyedTranslations.computeIfAbsent(translation.getFieldValue(), k -> new ArrayList<>()).add(translation);
            }
        }
    }

    public void setConnectionLinkDao(ConnectionLinkDAO connectionLinkDao) {
        this.connectionLinkDao = connectionLinkDao;
    }

    public void setNetworkTranslationDao(NetworkTranslationDAO networkTranslationDao) {
        this.networkTranslationDao = networkTranslationDao;
    }

    public void setCompanyTranslationDao(CompanyTranslationDAO companyTranslationDao) {
        this.companyTranslationDao = companyTranslationDao;
    }

    public void setLineTranslationDao(LineTranslationDAO lineTranslationDao) {
        this.lineTranslationDao = lineTranslationDao;
    }

    public void setStopAreaTranslationDao(StopAreaTranslationDAO stopAreaTranslationDao) {
        this.stopAreaTranslationDao = stopAreaTranslationDao;
    }

    public void setVehicleJourneyTranslationDao(VehicleJourneyTranslationDAO vehicleJourneyTranslationDao) {
        this.vehicleJourneyTranslationDao = vehicleJourneyTranslationDao;
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            return new NetexLineProducerCommand();
        }
    }

}
