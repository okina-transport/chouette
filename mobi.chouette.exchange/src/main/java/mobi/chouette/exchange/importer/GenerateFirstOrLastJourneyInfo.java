package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.ObjectIdUtil;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.ProviderDAO;
import mobi.chouette.dao.VehicleJourneyDAO;
import mobi.chouette.model.FirstOrLastJourneyInfo;
import mobi.chouette.model.IneoVJMapping;
import mobi.chouette.model.Provider;
import mobi.chouette.persistence.hibernate.ContextHolder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;

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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Log4j
@Stateless(name = GenerateFirstOrLastJourneyInfo.COMMAND)
public class GenerateFirstOrLastJourneyInfo implements Command {

    public static final String COMMAND = "GenerateFirstOrLastJourneyInfo";
    public static final Path OUTDIR = Paths.get("/opt/jboss/data/referentials/mobiiti_technique/vehicleJourneys/");
    public static final String FIRST_OR_LAST_JOURNEY_CSV = "firstOrLastJourney.csv";
    public static final String[] CSV_HEADERS = { "dateyyyyMMdd",  "lineId","vehicleJourneyId", "servicePosition" };
    private static final DateTimeZone ZONE_ID = DateTimeZone.forID("Europe/Paris");
    public static final DateFormat DF_YYYY_MM_DD = new SimpleDateFormat("yyyyMMdd");

    @EJB
    VehicleJourneyDAO vjDAO;

    @EJB
    ProviderDAO providerDAO;


    @Override
    public boolean execute(Context context) throws Exception {
        log.info("Start generation of GenerateFirstOrLastJourneyInfo");

        long startTime = System.currentTimeMillis();
        buildFolderIfNotExist(OUTDIR);
        String currentContext = ContextHolder.getContext();
        ContextHolder.clear();
        ContextHolder.setContext("admin");
        List<Provider> referentials = providerDAO.getAllProviders()
                .stream()
                .filter(prov -> !prov.getCode().startsWith("mobiiti") && !prov.getCode().equals(
                        "technique"))
                .collect(Collectors.toList());


        try (BufferedWriter csvWriter = Files.newBufferedWriter(OUTDIR.resolve(FIRST_OR_LAST_JOURNEY_CSV),
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
        ) {
            CSVPrinter csvPrinter = new CSVPrinter(csvWriter,
                    CSVFormat.Builder.create().setHeader(CSV_HEADERS).build());
            for (Provider referential : referentials) {

                try {
                    ContextHolder.clear();
                    ContextHolder.setContext(SUPERSPACE_PREFIX + "_" + referential.getCode());
                    LocalDate today = LocalDate.now(ZONE_ID);


                    List<FirstOrLastJourneyInfo> todayEntities = vjDAO.getFirstOrLastJourneyData(today);
                    List<FirstOrLastJourneyInfo> tomorrowEntities = vjDAO.getFirstOrLastJourneyData(today.plusDays(1));


                    List<FirstOrLastJourneyInfo> all = new ArrayList<>();
                    if (CollectionUtils.isNotEmpty(todayEntities)) {
                        all.addAll(todayEntities);
                    }
                    if (CollectionUtils.isNotEmpty(tomorrowEntities)) {
                        all.addAll(tomorrowEntities);
                    }

                    for (FirstOrLastJourneyInfo entity : all) {
                        csvPrinter.printRecord(
                                DF_YYYY_MM_DD.format(entity.getDate()),
                                entity.getLineId(),
                                entity.getVehicleJourneyId(),
                                entity.getServicePosition().name()
                        );
                    }
                }catch(Exception e){
                    log.error("Error while generating firstOrLastJourneyInfo for provider:" + referential.getName(), e);
                }
            }
            csvPrinter.flush();
            log.info(String.format("Finished generation of %s", FIRST_OR_LAST_JOURNEY_CSV));
            log.info(String.format("It took %d seconds", (System.currentTimeMillis() - startTime) / 1000));
            return true;
        } catch (IOException e) {
            log.error(String.format("Error generating %s file", FIRST_OR_LAST_JOURNEY_CSV), e);
            return false;
        }

        finally {
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

    static {
        CommandFactory.factories.put(GenerateFirstOrLastJourneyInfo.class.getName(), new GenerateFirstOrLastJourneyInfo.DefaultCommandFactory());
    }
}
