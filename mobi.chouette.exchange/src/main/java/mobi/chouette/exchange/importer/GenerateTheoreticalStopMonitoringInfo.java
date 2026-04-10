package mobi.chouette.exchange.importer;

import lombok.extern.slf4j.Slf4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.ProviderDAO;
import mobi.chouette.dao.VehicleJourneyDAO;
import mobi.chouette.exchange.importer.utils.FileUtils;
import mobi.chouette.model.Provider;
import mobi.chouette.model.TheoreticalStopMonitoringInfo;
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
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static mobi.chouette.exchange.importer.utils.CsvGenerationConstants.*;
import static mobi.chouette.exchange.importer.utils.ProviderPredicate.isProviderForCsvGeneration;

@Slf4j
@Stateless(name = GenerateTheoreticalStopMonitoringInfo.COMMAND)
public class GenerateTheoreticalStopMonitoringInfo implements Command {

    public static final String COMMAND = "GenerateTheoreticalStopMonitoringInfo";
    private static final String[] CSV_HEADERS_TH_SM_INFO = {
            "dateyyyyMMdd",
            "monitoringRef",
            "stopPointName",
            "monitoredVehicleJourneyRef",
            "lineRef",
            "publishedLineName",
            "directionName",
            "aimedDepartureTime",
            "aimedArrivalTime",
            "originRef",
            "originName",
            "destinationRef",
            "destinationName"
    };

    static {
        CommandFactory.factories.put(GenerateTheoreticalStopMonitoringInfo.class.getName(), new GenerateTheoreticalStopMonitoringInfo.DefaultCommandFactory());
    }

    private final DateTimeFormatter dateFormatYYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    @EJB
    private VehicleJourneyDAO vjDAO;
    @EJB
    private ProviderDAO providerDAO;
    @EJB
    private FileUtils fileUtils;

    @Override
    public boolean execute(Context context) throws Exception {
        log.info("Start generation of theoreticalStopMonitoringInfo");

        long startTime = System.currentTimeMillis();
        fileUtils.buildFolderIfNotExist(TH_SM_CSV_DIRECTORY);
        String currentContext = ContextHolder.getContext();
        ContextHolder.clear();
        ContextHolder.setContext("admin");

        String generateThTRParam = System.getenv("GENERATE_TH_SM_DATASETS");
        List<String> datasetIds = StringUtils.isEmpty(generateThTRParam) ? new ArrayList<>() : Arrays.asList(System.getenv("GENERATE_TH_SM_DATASETS").toLowerCase().split(","));

        List<Provider> referentials = providerDAO.getAllProviders()
                .stream()
                .filter(isProviderForCsvGeneration())
                .filter(provider -> datasetIds.contains(provider.getCode()))
                .collect(Collectors.toList());


        for (Provider referential : referentials) {
            log.info("Launching TH generation for provider : {}",referential.getCode());
            try (BufferedWriter csvWriter = Files.newBufferedWriter(TH_SM_CSV_DIRECTORY.resolve(referential.getCode() + TH_SM_CSV_FILE),
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
            ) {
                CSVPrinter csvPrinter = new CSVPrinter(csvWriter,
                        CSVFormat.Builder.create().setHeader(CSV_HEADERS_TH_SM_INFO).build());
                generateTheoreticalStopMonitoringInfoFileForProvider(referential, csvPrinter);
                csvPrinter.flush();
            } catch (IOException e) {
                log.error("Error generating {} file", referential.getCode() + TH_SM_CSV_FILE, e);
                return false;
            }
            log.info("Launching TH generation for provider : {} completed", referential.getCode());
        }

        log.info("Finished generation of all theoreticalStopMonitoringInfo");
        log.info("It took {} seconds", (System.currentTimeMillis() - startTime) / 1000);
        ContextHolder.setContext(currentContext);
        return true;
    }

    private void generateTheoreticalStopMonitoringInfoFileForProvider(Provider referential, CSVPrinter csvPrinter) {
        try {
            ContextHolder.clear();
            ContextHolder.setContext(SUPERSPACE_PREFIX + "_" + referential.getCode());
            LocalDate today = LocalDate.now(ZONE_ID);


            List<TheoreticalStopMonitoringInfo> yesterdayEntities = vjDAO.getAllTheoreticalStopMonitoringInfoByDate(today.minusDays(1));
            if (CollectionUtils.isNotEmpty(yesterdayEntities)) {
                yesterdayEntities = yesterdayEntities.stream()
                        .filter(e -> e.getDate().equals(today))
                        .collect(Collectors.toList());
            }
            List<TheoreticalStopMonitoringInfo> todayEntities = vjDAO.getAllTheoreticalStopMonitoringInfoByDate(today);
            List<TheoreticalStopMonitoringInfo> tomorrowEntities = vjDAO.getAllTheoreticalStopMonitoringInfoByDate(today.plusDays(1));


            List<TheoreticalStopMonitoringInfo> all = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(yesterdayEntities)) {
                all.addAll(yesterdayEntities);
            }
            if (CollectionUtils.isNotEmpty(todayEntities)) {
                all.addAll(todayEntities);
            }
            if (CollectionUtils.isNotEmpty(tomorrowEntities)) {
                all.addAll(tomorrowEntities);
            }

            for (TheoreticalStopMonitoringInfo entity : all) {
                csvPrinter.printRecord(
                        entity.getDate().format(dateFormatYYYYMMDD),
                        entity.getMonitoringRef(),
                        entity.getStopPointName(),
                        entity.getMonitoredVehicleJourneyRef(),
                        entity.getLineRef(),
                        entity.getPublishedLineName(),
                        entity.getDirectionName(),
                        entity.getAimedDepartureTime().format(DTF_HHMMSS),
                        entity.getAimedArrivalTime().format(DTF_HHMMSS),
                        entity.getOriginRef(),
                        entity.getOriginName(),
                        entity.getDestinationRef(),
                        entity.getDestinationName()
                );
            }
        } catch (Exception e) {
            log.error("Error while generating theoreticalStopMonitoringInfo for provider: {}", referential.getName(), e);
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
                    log.error(e.getMessage());
                }
            }
            return result;
        }
    }
}
