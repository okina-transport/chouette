package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.ProviderDAO;
import mobi.chouette.dao.VehicleJourneyDAO;
import mobi.chouette.model.IneoToVjRef;
import mobi.chouette.model.Provider;
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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Log4j
@Stateless(name = GenerateIneoToVjrefMappingCsv.COMMAND)
public class GenerateIneoToVjrefMappingCsv implements Command {

    public static final String COMMAND = "GenerateIneoToVjrefMappingCsv";
    public static final String INEO_VJREF_MAPPING = "ineoVjrefMapping.csv";
    public static final String[] CSV_HEADERS = { "dateyyyyMMdd", "timeHHmmss", "stopAreaObjectId", "lineNumber",
            "routeDirection", "vehicleJourneyObjectId" };
    public static final Path OUTDIR = Paths.get("/opt/jboss/data/referentials/mobiiti_technique/ineo/");
    public static final DateFormat DF_YYYY_MM_DD = new SimpleDateFormat("yyyy-MM-dd");
    public static final DateTimeFormatter DTF_HHMMSS = DateTimeFormatter.ofPattern("HHmmss");

    @EJB
    VehicleJourneyDAO vjDAO;

    @EJB
    ProviderDAO providerDAO;

    @Override
    public boolean execute(Context context) throws Exception {
        log.info(String.format("Start generation of %s", INEO_VJREF_MAPPING));
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
        try (BufferedWriter csvWriter = Files.newBufferedWriter(OUTDIR.resolve(INEO_VJREF_MAPPING),
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
        ) {
            CSVPrinter csvPrinter = new CSVPrinter(csvWriter,
                    CSVFormat.Builder.create().setHeader(CSV_HEADERS).build());
            for (Provider referential : referentials) {
                ContextHolder.clear();
                ContextHolder.setContext(SUPERSPACE_PREFIX + "_" + referential.getCode());
                List<IneoToVjRef> entities = vjDAO.getIneoToVjRefs();
                for (IneoToVjRef entity : entities) {
                    csvPrinter.printRecord(
                            DF_YYYY_MM_DD.format(entity.getDate()),
                            entity.getTime().format(DTF_HHMMSS),
                            entity.getStopAreaObjectId(),
                            entity.getLineNumber(),
                            entity.getRouteDirection(),
                            entity.getVehicleJourneyObjectId()
                    );
                }
            }
            csvPrinter.flush();
            log.info(String.format("Finished generation of %s", INEO_VJREF_MAPPING));
            log.info(String.format("It took %d seconds", (System.currentTimeMillis() - startTime) / 1000));
            return true;
        } catch (IOException e) {
            log.error(String.format("Error generating %s file", INEO_VJREF_MAPPING), e);
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
        CommandFactory.factories.put(GenerateIneoToVjrefMappingCsv.class.getName(), new GenerateIneoToVjrefMappingCsv.DefaultCommandFactory());
    }

}
