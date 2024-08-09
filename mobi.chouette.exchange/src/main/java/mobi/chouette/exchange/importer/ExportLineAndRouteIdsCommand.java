package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.ProviderDAO;
import mobi.chouette.model.Line;
import mobi.chouette.model.Provider;
import mobi.chouette.model.Route;
import mobi.chouette.persistence.hibernate.ContextHolder;
import org.apache.commons.collections.CollectionUtils;
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
import java.util.stream.Collectors;

@Log4j
@Stateless(name = ExportLineAndRouteIdsCommand.COMMAND)
public class ExportLineAndRouteIdsCommand implements Command {

    public static final String COMMAND = "ExportLineAndRouteIdsCommand";
    public static final String LINE_IDS_MAPPING = "lineIdsMapping.csv";
    public static final String ROUTE_IDS_MAPPING = "routeIdsMapping.csv";
    public static final String[] LINES_CSV_HEADERS = { "objectid", "lineNumber" };
    public static final String[] ROUTES_CSV_HEADERS = { "objectid", "lineNumber", "direction" };
    public static final Path LINES_OUTDIR = Paths.get("/opt/jboss/data/referentials/mobiiti_technique/lines/");
    public static final Path ROUTES_OUTDIR = Paths.get("/opt/jboss/data/referentials/mobiiti_technique/routes/");

    @EJB
    LineDAO lineDAO;

    @EJB
    ProviderDAO providerDAO;

    @Override
    public boolean execute(Context context) throws Exception {
        if (!"true".equals(System.getenv("INEO_SIC_ENABLED"))) {
            // just required by SIC ATM
            return true;
        }
        log.info(String.format("Start generation of %s and %s", LINE_IDS_MAPPING, ROUTE_IDS_MAPPING));
        buildFolderIfNotExist(LINES_OUTDIR);
        buildFolderIfNotExist(ROUTES_OUTDIR);
        String currentContext = ContextHolder.getContext();
        ContextHolder.clear();
        ContextHolder.setContext("admin");
        List<Provider> referentials = providerDAO.getAllProviders()
                .stream()
                .filter(prov -> !prov.getCode().startsWith("mobiiti") && !prov.getCode().equals(
                        "technique"))
                .collect(Collectors.toList());
        try (BufferedWriter linesWriter = Files.newBufferedWriter(LINES_OUTDIR.resolve(LINE_IDS_MAPPING),
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
             BufferedWriter routesWriter = Files.newBufferedWriter(ROUTES_OUTDIR.resolve(ROUTE_IDS_MAPPING),
                     StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
        ) {
            CSVPrinter linesCsvPrinter = new CSVPrinter(linesWriter,
                    CSVFormat.Builder.create().setHeader(LINES_CSV_HEADERS).build());
            CSVPrinter routesCsvPrinter = new CSVPrinter(routesWriter,
                    CSVFormat.Builder.create().setHeader(ROUTES_CSV_HEADERS).build());
            for (Provider referential : referentials) {
                ContextHolder.clear();
                ContextHolder.setContext(SUPERSPACE_PREFIX + "_" + referential.getCode());
                List<Line> lines = lineDAO.findNotDeletedInNewTransaction();
                if (CollectionUtils.isEmpty(lines)) {
                    log.warn("No lines found on schema " + ContextHolder.getContext());
                }
                for (Line line : lines) {
                    linesCsvPrinter.printRecord(line.getObjectId(), line.getNumber());
                    if (CollectionUtils.isEmpty(line.getRoutes())) {
                        log.warn("No routes for line " + line.getObjectId() + " on schema " + ContextHolder.getContext());
                        continue;
                    }
                    for (Route route : line.getRoutes()) {
                        if (!route.getSupprime()) {
                            routesCsvPrinter.printRecord(route.getObjectId(), line.getNumber(), route.getDirection().name());
                        }
                    }
                }
            }
            linesCsvPrinter.flush();
            routesCsvPrinter.flush();
            return true;
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

    static {
        CommandFactory.factories.put(ExportLineAndRouteIdsCommand.class.getName(), new ExportLineAndRouteIdsCommand.DefaultCommandFactory());
    }

}
