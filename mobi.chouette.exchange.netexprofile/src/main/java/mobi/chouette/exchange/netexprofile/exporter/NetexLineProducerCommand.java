package mobi.chouette.exchange.netexprofile.exporter;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.TimeUtil;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.common.monitor.JamonUtils;
import mobi.chouette.dao.ConnectionLinkDAO;
import mobi.chouette.exchange.exporter.SharedDataKeys;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.Line;
import mobi.chouette.model.util.NamingUtil;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXParseException;

import javax.naming.InitialContext;
import javax.xml.bind.MarshalException;
import java.io.IOException;
import java.time.LocalDate;

@Log4j
public class NetexLineProducerCommand implements Command, Constant {

    public static final String COMMAND = "NetexLineProducerCommand";

    private ConnectionLinkDAO connectionLinkDao;

    @Override
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;
        Monitor monitor = MonitorFactory.start(COMMAND);
        ActionReporter reporter = ActionReporter.Factory.getInstance();

        try {

            Line line = (Line) context.get(LINE);
            log.info("Processing NeTEx export for line " + line.getObjectId() + " (" + NamingUtil.getName(line) + ')');

            if(line != null && line.getCategoriesForLine() != null && !line.getCategoriesForLine().getName().equalsIgnoreCase("idfm")){
                log.error("Ligne : " + line.getObjectId() + " en catégorie IDFM mais CODIFLIGNE manquant.");
                return SUCCESS;
            }

            if(line != null && StringUtils.isEmpty(line.getCodifligne()) && line.getCategoriesForLine() != null && line.getCategoriesForLine().getName().equalsIgnoreCase("idfm")) {
                reporter.addObjectReport(context, line.getObjectId(), ActionReporter.OBJECT_TYPE.LINE,
                        "Codifligne manquant", ActionReporter.OBJECT_STATE.ERROR, IO_TYPE.OUTPUT);
                reporter.setActionError(context, ActionReporter.ERROR_CODE.NO_DATA_FOUND, "Codifligne manquant");
                return ERROR;
            }
            NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(CONFIGURATION);
            
            
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

            NetexDataCollector collector = new NetexDataCollector(collection, line, startDate, endDate, !configuration.isExportBlocks());
            collector.setConnectionLinkDAO(connectionLinkDao);
            boolean cont = collector.collect();

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
            JamonUtils.logMagenta(log, monitor);
        }

        return result;
    }

    public void setConnectionLinkDao(ConnectionLinkDAO connectionLinkDao) {
        this.connectionLinkDao = connectionLinkDao;
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            return new NetexLineProducerCommand();
        }
    }

    static {
        CommandFactory.factories.put(NetexLineProducerCommand.class.getName(), new NetexLineProducerCommand.DefaultCommandFactory());
    }

}
