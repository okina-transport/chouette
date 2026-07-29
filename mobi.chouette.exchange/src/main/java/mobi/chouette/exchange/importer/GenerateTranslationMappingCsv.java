package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.ObjectIdUtil;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.LineTranslationDAO;
import mobi.chouette.dao.ProviderDAO;
import mobi.chouette.dao.StopAreaTranslationDAO;
import mobi.chouette.dao.VehicleJourneyTranslationDAO;
import mobi.chouette.exchange.importer.utils.FileUtils;
import mobi.chouette.model.*;
import mobi.chouette.persistence.hibernate.ContextHolder;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static mobi.chouette.exchange.importer.utils.ProviderPredicate.isProviderForCsvGeneration;

@Log4j
@Stateless(name = GenerateTranslationMappingCsv.COMMAND)
public class GenerateTranslationMappingCsv implements Command {

    public static final String COMMAND = "GenerateTranslationMappingCsv";
    public static final Path OUTDIR = Paths.get("/opt/jboss/data/referentials/mobiiti_technique/translations/");
    public static final String TRANSLATION_MAPPING_CSV = "translationMapping.csv";
    protected static final String[] CSV_HEADERS = { "dataset", "object_type", "object_id", "field_name", "field_value", "language", "translation" };
    private static final String LINE_OBJECT_TYPE = "LINE";
    private static final String VEHICLE_JOURNEY_OBJECT_TYPE = "VEHICLE_JOURNEY";
    private static final String STOP_OBJECT_TYPE = "STOP";

    @EJB
    LineTranslationDAO lineTranslationDAO;

    @EJB
    StopAreaTranslationDAO stopAreaTranslationDAO;

    @EJB
    VehicleJourneyTranslationDAO vehicleJourneyTranslationDAO;

    @EJB
    ProviderDAO providerDAO;

    @EJB
    FileUtils fileUtils;

    private Path outputDir = OUTDIR;

    public GenerateTranslationMappingCsv() {
    }

    public GenerateTranslationMappingCsv(LineTranslationDAO lineTranslationDAO, StopAreaTranslationDAO stopAreaTranslationDAO,
            VehicleJourneyTranslationDAO vehicleJourneyTranslationDAO, ProviderDAO providerDAO, FileUtils fileUtils,
            Path outputDir) {
        this.lineTranslationDAO = lineTranslationDAO;
        this.stopAreaTranslationDAO = stopAreaTranslationDAO;
        this.vehicleJourneyTranslationDAO = vehicleJourneyTranslationDAO;
        this.providerDAO = providerDAO;
        this.fileUtils = fileUtils;
        this.outputDir = outputDir;
    }

    @Override
    public boolean execute(Context context) throws Exception {
        log.info(String.format("Start generation of %s", TRANSLATION_MAPPING_CSV));
        long startTime = System.currentTimeMillis();
        fileUtils.buildFolderIfNotExist(outputDir);
        String currentContext = ContextHolder.getContext();
        ContextHolder.clear();
        ContextHolder.setContext("admin");
        List<Provider> referentials = providerDAO.getAllProviders()
                .stream()
                .filter(isProviderForCsvGeneration())
                .toList();

        try (BufferedWriter csvWriter = Files.newBufferedWriter(outputDir.resolve(TRANSLATION_MAPPING_CSV),
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
             CSVPrinter csvPrinter = new CSVPrinter(csvWriter,
                     CSVFormat.Builder.create().setHeader(CSV_HEADERS).get())
        ) {
            for (Provider referential : referentials) {
                try {
                    ContextHolder.clear();
                    ContextHolder.setContext(SUPERSPACE_PREFIX + "_" + referential.getCode());
                    String dataset = referential.getCode();

                    for (LineTranslation translation : lineTranslationDAO.findAllNewTransaction()) {
                        printTranslation(csvPrinter, dataset, LINE_OBJECT_TYPE,
                                translation.getLine() != null ? ObjectIdUtil.extractOriginalId(translation.getLine().getObjectId()) : null, translation);
                    }
                    for (StopAreaTranslation translation : stopAreaTranslationDAO.findAllNewTransaction()) {
                        printTranslation(csvPrinter, dataset, STOP_OBJECT_TYPE,
                                translation.getStopArea() != null ? ObjectIdUtil.extractOriginalId(translation.getStopArea().getObjectId()) : null, translation);
                    }
                    for (VehicleJourneyTranslation translation : vehicleJourneyTranslationDAO.findAllNewTransaction()) {
                        printTranslation(csvPrinter, dataset, VEHICLE_JOURNEY_OBJECT_TYPE,
                                translation.getVehicleJourney() != null ? ObjectIdUtil.extractOriginalId(translation.getVehicleJourney().getObjectId()) : null, translation);
                    }
                } catch (Exception e) {
                    log.error("Error while generating translation mapping for provider:" + referential.getName(), e);
                }
            }
            csvPrinter.flush();
            log.info(String.format("Finished generation of %s", TRANSLATION_MAPPING_CSV));
            log.info(String.format("It took %d seconds", (System.currentTimeMillis() - startTime) / 1000));
            return true;
        } catch (IOException e) {
            log.error(String.format("Error generating %s file", TRANSLATION_MAPPING_CSV), e);
            return false;
        } finally {
            ContextHolder.setContext(currentContext);
        }
    }

    /**
     * record_id-keyed rows carry a resolved owner objectId (fieldValue left blank); legacy field_value-keyed
     * rows (no record_id in translations.txt, owner FK left null at import time) carry the raw field_value
     * text instead (objectId left blank) rather than an opportunistically-matched objectId.
     */
    private void printTranslation(CSVPrinter csvPrinter, String datasetCode, String objectType, String objectId,
            Translation translation) throws IOException {
        csvPrinter.printRecord(datasetCode.toUpperCase(), objectType, objectId, translation.getFieldName(),
                translation.getFieldValue(), translation.getLanguage(), translation.getTranslation());
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = null;
            try {
                String name = "java:app/mobi.chouette.exchange/" + COMMAND;
                result = (Command) context.lookup(name);
            } catch (NamingException e) {
                // try another way on test context
                String name = "java:module/" + COMMAND;
                try {
                    result = (Command) context.lookup(name);
                } catch (NamingException e1) {
                    log.error(e);
                }
            }
            return result;
        }
    }

    static {
        CommandFactory.factories.put(GenerateTranslationMappingCsv.class.getName(), new GenerateTranslationMappingCsv.DefaultCommandFactory());
    }
}
