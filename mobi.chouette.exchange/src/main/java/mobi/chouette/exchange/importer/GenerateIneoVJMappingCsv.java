package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.ObjectIdUtil;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.ProviderDAO;
import mobi.chouette.dao.VehicleJourneyDAO;
import mobi.chouette.model.IneoVJMapping;
import mobi.chouette.model.Provider;
import mobi.chouette.persistence.hibernate.ContextHolder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;

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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static mobi.chouette.exchange.importer.utils.CsvGenerationConstants.DTF_HHMMSS;
import static mobi.chouette.exchange.importer.utils.ProviderPredicate.isProviderForCsvGeneration;

@Log4j
@Stateless(name = GenerateIneoVJMappingCsv.COMMAND)
public class GenerateIneoVJMappingCsv implements Command {

    public static final String COMMAND = "GenerateIneoVJMappingCsv";
    public static final String INEO_VJ_MAPPING_CSV = "vehicleJourneyMapping.csv";
    public static final Path OUTDIR = Paths.get("/opt/jboss/data/referentials/mobiiti_technique/ineo/");
    protected static final String[] CSV_HEADERS = {"dateyyyyMMdd", "timeHHmmss", "lineNumber",
            "routeDirection", "originalStopId", "originalParentStopId", "vehicleJourneyId", "position", "datasetId"};
    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Paris");

    static {
        CommandFactory.factories.put(GenerateIneoVJMappingCsv.class.getName(), new GenerateIneoVJMappingCsv.DefaultCommandFactory());
    }

    private final DateTimeFormatter dateFormatYYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    @EJB
    VehicleJourneyDAO vjDAO;
    @EJB
    ProviderDAO providerDAO;

    @Override
    public boolean execute(Context context) throws Exception {
        if (!"true".equals(System.getenv("INEO_GENERATE_VJ_MAPPING"))) {
            return true;
        }
        log.info(String.format("Start generation of %s", INEO_VJ_MAPPING_CSV));
        long startTime = System.currentTimeMillis();
        buildFolderIfNotExist(OUTDIR);
        String currentContext = ContextHolder.getContext();
        ContextHolder.clear();
        ContextHolder.setContext("admin");
        List<Provider> referentials = providerDAO.getAllProviders()
                .stream()
                .filter(isProviderForCsvGeneration())
                .toList();
        try (BufferedWriter csvWriter = Files.newBufferedWriter(OUTDIR.resolve(INEO_VJ_MAPPING_CSV),
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
             CSVPrinter csvPrinter = new CSVPrinter(csvWriter,
                     CSVFormat.Builder.create().setHeader(CSV_HEADERS).get())
        ) {

            for (Provider referential : referentials) {
                ContextHolder.clear();
                ContextHolder.setContext(SUPERSPACE_PREFIX + "_" + referential.getCode());
                LocalDate today = LocalDate.now(ZONE_ID);
                List<IneoVJMapping> yesterdayEntities = vjDAO.getIneoVJMappingData(today.minusDays(1));
                if (CollectionUtils.isNotEmpty(yesterdayEntities)) {
                    // certain passages associés aux courses de la veille peuvent avoir lieu le jour J
                    // ex: bus qui commence sa course à 23h30 et la termine le lendemain à 1h du matin
                    yesterdayEntities = yesterdayEntities.stream()
                            .filter(e -> e.getDate().equals(today))
                            .collect(Collectors.toList());
                }
                List<IneoVJMapping> todayEntities = vjDAO.getIneoVJMappingData(today);
                List<IneoVJMapping> tomorrowEntities = vjDAO.getIneoVJMappingData(today.plusDays(1));
                List<IneoVJMapping> all = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(yesterdayEntities)) {
                    all.addAll(yesterdayEntities);
                }
                if (CollectionUtils.isNotEmpty(todayEntities)) {
                    all.addAll(todayEntities);
                }
                if (CollectionUtils.isNotEmpty(tomorrowEntities)) {
                    all.addAll(tomorrowEntities);
                }

                for (IneoVJMapping entity : all) {
                    csvPrinter.printRecord(
                            entity.getDate().format(dateFormatYYYYMMDD),
                            entity.getTime().format(DTF_HHMMSS),
                            entity.getLineNumber(),
                            entity.getRouteDirection(),
                            StringUtils.trimToEmpty(entity.getOriginalStopId()),
                            StringUtils.trimToEmpty(entity.getOriginalParentStopId()),
                            // extract original id because there is only one AO for SEM in order to reduce size of output file
                            ObjectIdUtil.extractOriginalId(entity.getVehicleJourneyObjectId()),
                            entity.getPosition(),
                            referential.getCode().toUpperCase()

                    );
                }
            }
            csvPrinter.flush();
            log.info(String.format("Finished generation of %s", INEO_VJ_MAPPING_CSV));
            log.info(String.format("It took %d seconds", (System.currentTimeMillis() - startTime) / 1000));
            return true;
        } catch (IOException e) {
            log.error(String.format("Error generating %s file", INEO_VJ_MAPPING_CSV), e);
            return false;
        } finally {
            ContextHolder.setContext(currentContext);
        }
    }

    public void buildFolderIfNotExist(Path folder) throws IOException {
        if (!Files.exists(folder) && !folder.toFile().mkdirs()) {
            throw new IOException("Error creating directory " + folder.toAbsolutePath());
        }
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

}
