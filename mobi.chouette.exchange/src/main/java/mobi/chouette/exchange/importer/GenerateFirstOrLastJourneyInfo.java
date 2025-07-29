package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.ProviderDAO;
import mobi.chouette.dao.VehicleJourneyDAO;
import mobi.chouette.exchange.importer.utils.FileUtils;
import mobi.chouette.model.FirstOrLastJourneyInfo;
import mobi.chouette.model.Provider;
import mobi.chouette.persistence.hibernate.ContextHolder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;import org.joda.time.DateTimeZone;
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
import java.util.HashMap;import java.util.List;
import java.util.Map;import java.util.stream.Collectors;

import static mobi.chouette.exchange.importer.utils.ProviderPredicate.isProviderForCsvGeneration;

@Log4j
@Stateless(name = GenerateFirstOrLastJourneyInfo.COMMAND)
public class GenerateFirstOrLastJourneyInfo implements Command {

    public static final String COMMAND = "GenerateFirstOrLastJourneyInfo";
    public static final Path OUTDIR = Paths.get("/opt/jboss/data/referentials/mobiiti_technique/vehicleJourneys/");
    public static final String FIRST_OR_LAST_JOURNEY_CSV = "firstOrLastJourney.csv";
    protected static final String[] CSV_HEADERS = { "dateyyyyMMdd",  "lineId","vehicleJourneyId", "servicePosition" };
    private static final DateTimeZone ZONE_ID = DateTimeZone.forID("Europe/Paris");
    private final DateFormat dateFormatyyyyMMdd = new SimpleDateFormat("yyyyMMdd");

    @EJB
    VehicleJourneyDAO vjDAO;

    @EJB
    ProviderDAO providerDAO;

    @EJB
    FileUtils fileUtils;


    @Override
    public boolean execute(Context context) throws Exception {
        log.info("Start generation of GenerateFirstOrLastJourneyInfo");

        long startTime = System.currentTimeMillis();
        fileUtils.buildFolderIfNotExist(OUTDIR);
        String currentContext = ContextHolder.getContext();
        ContextHolder.clear();
        ContextHolder.setContext("admin");
        List<Provider> referentials = providerDAO.getAllProviders()
                .stream()
                .filter(isProviderForCsvGeneration())
                .collect(Collectors.toList());


        try (BufferedWriter csvWriter = Files.newBufferedWriter(OUTDIR.resolve(FIRST_OR_LAST_JOURNEY_CSV),
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
        ) {
            CSVPrinter csvPrinter = new CSVPrinter(csvWriter,
                    CSVFormat.Builder.create().setHeader(CSV_HEADERS).build());

			List<Provider> providers = providerDAO.getAllProviders();
			Map<String, String> netexPrefixMap = new HashMap<>();

			List<Provider> filteredProviders = providers.stream()
									.filter(prov -> prov.getCode().startsWith("mobiiti") && !prov.getCode().equals("technique"))
									.collect(Collectors.toList());

			for (Provider provider : filteredProviders) {
				if (StringUtils.isNotEmpty(provider.getPrefixNetex())){
					netexPrefixMap.put(provider.getCode().replace("mobiiti_","").toUpperCase(), provider.getPrefixNetex().toUpperCase());
				}

			}

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
						String upperReferentialCode = referential.getCode().toUpperCase();
						String vjIdToWrite = (!netexPrefixMap.isEmpty() && netexPrefixMap.containsKey(upperReferentialCode)) ? entity.getVehicleJourneyId().replace(upperReferentialCode + ":", netexPrefixMap.get(upperReferentialCode) + ":") : entity.getVehicleJourneyId();
						String lineIdToWrite = (!netexPrefixMap.isEmpty() && netexPrefixMap.containsKey(upperReferentialCode)) ? entity.getLineId().replace(upperReferentialCode + ":", netexPrefixMap.get(upperReferentialCode) + ":") : entity.getLineId();

                        csvPrinter.printRecord(
                                dateFormatyyyyMMdd.format(entity.getDate()),
                                lineIdToWrite,
                                vjIdToWrite.replace(COLON_REPLACEMENT_CODE, "-").replace("|", "_") + ":LOC",
                                entity.getServicePosition().name()
                        );
                    }
                } catch(Exception e){
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
