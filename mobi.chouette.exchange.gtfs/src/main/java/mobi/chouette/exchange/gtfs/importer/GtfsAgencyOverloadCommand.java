package mobi.chouette.exchange.gtfs.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.JobData;
import mobi.chouette.common.ObjectIdUtil;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.CompanyDAO;
import mobi.chouette.exchange.gtfs.exporter.producer.GtfsAgencyProducer;
import mobi.chouette.exchange.gtfs.model.GtfsRoute;
import mobi.chouette.exchange.gtfs.model.exporter.GtfsExporter;
import mobi.chouette.exchange.gtfs.model.importer.FactoryParameters;
import mobi.chouette.exchange.gtfs.model.importer.GtfsImporter;
import mobi.chouette.model.Company;
import mobi.chouette.model.type.OrganisationTypeEnum;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * This command generates new agency.txt/routes.txt files with a different agency/agency_id:
 * <ul>
 *     <li>look for an active company with name {@link GtfsImportParameters#getTargetNetwork} in database, if not found generate a new company</li>
 *     <li>rename agency.txt into {@link GtfsAgencyOverloadCommand#ORIGINAL_AGENCY_TXT} from unzipped GTFS archive</li>
 *     <li>generate a new agency.txt with a single agency named {@link GtfsImportParameters#getTargetNetwork}</li>
 *     <li>parse route.txt</li>
 *     <li>rename routes.txt into {@link GtfsAgencyOverloadCommand#ORIGINAL_ROUTES_TXT}</li>
 *     <li>generate new routes.txt file with new agency_id</li>
 * </ul>
 *  /!\ It must be executed after unzipping input GTFS archive /!\
 */
@Log4j
@Stateless(name = GtfsAgencyOverloadCommand.COMMAND)
public class GtfsAgencyOverloadCommand implements Command {

    public static final String COMMAND = "GtfsAgencyOverloadCommand";

    public static final String AGENCY_TXT = "agency.txt";
    public static final String ORIGINAL_AGENCY_TXT = "original.agency.txt";
    public static final String ROUTES_TXT = "routes.txt";
    public static final String ORIGINAL_ROUTES_TXT = "original.routes.txt";

    public static final String DEFAULT_URL = "https://www.okina.fr";

    public static final String ERROR_GENERATING_AGENCY_TXT_FILE = "Generation of agency.txt file failed";
    public static final String ERROR_GENERATING_ROUTES_TXT_FILE = "Generating of routes.txt file failed";
    public static final String ERROR_RENAMING_AGENCY_TXT_FILE = "Renaming agency.txt file failed";
    public static final String ERROR_RENAMING_ROUTES_TXT_FILE = "Renaming routes.txt file failed";

    static {
        CommandFactory.factories.put(GtfsAgencyOverloadCommand.class.getName(), new GtfsAgencyOverloadCommand.DefaultCommandFactory());
    }

    @EJB
    CompanyDAO companyDAO;

    public GtfsAgencyOverloadCommand() {
    }

    public GtfsAgencyOverloadCommand(CompanyDAO companyDAO) {
        this.companyDAO = companyDAO;
    }

    public boolean execute(Context context) throws Exception {
        GtfsImportParameters parameters = (GtfsImportParameters) context.get(CONFIGURATION);

        if (StringUtils.isBlank(parameters.getTargetNetwork())) {
            log.error("Import parameters useTargetNetwork is true but targetNetwork is blank");
            throw new IllegalArgumentException("Import parameters useTargetNetwork is true but targetNetwork is blank");
        }

        Monitor monitor = MonitorFactory.start(GtfsAgencyOverloadCommand.COMMAND);
        try {
            Optional<Company> optionalCompany = lookForCompanyForCompanyByNameAndActive(parameters.getTargetNetwork());
            Company newCompany = optionalCompany.orElseGet(() -> generateNewCompany(parameters));
            generateNewGtfsFiles(context, newCompany);
        } finally {
            log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
        }
        return SUCCESS;
    }

    private Company generateNewCompany(GtfsImportParameters parameters) {
        Company company = new Company();
        String objectId = ObjectIdUtil.composeObjectId(parameters.isSplitIdOnDot(), parameters.getObjectIdPrefix(), Company.AUTHORITY_KEY, UUID.randomUUID().toString());
        company.setObjectId(objectId);
        company.setName(parameters.getTargetNetwork());
        company.setUrl(DEFAULT_URL);
        return company;
    }

    private Optional<Company> lookForCompanyForCompanyByNameAndActive(String targetNetwork) {
        List<Company> companies = companyDAO.findByName(targetNetwork);
        if (CollectionUtils.isNotEmpty(companies)) {
            // company with same name exists in database
            log.info(String.format("Found company with name %s in database", targetNetwork));
            return Optional.of(companies.stream().filter(c -> c.getOrganisationType() == OrganisationTypeEnum.Authority).findFirst().orElse(companies.get(0)));
        }
        return Optional.empty();
    }

    private void generateNewGtfsFiles(Context context, Company newCompany) {
        GtfsExporter gtfsExporter = null;
        GtfsImportParameters parameters = (GtfsImportParameters) context.get(CONFIGURATION);

        try {
            JobData jobData = (JobData) context.get(JOB_DATA);
            String jobPath = jobData.getPathName();
            Path unzippedGtfsFolder = Paths.get(jobPath, INPUT);
            gtfsExporter = new GtfsExporter(unzippedGtfsFolder.toString());

            generateAgencyTxt(newCompany, unzippedGtfsFolder, gtfsExporter);
            generateRouteTxt(newCompany, unzippedGtfsFolder, gtfsExporter, parameters);
        } finally {
            if (gtfsExporter != null) {
                gtfsExporter.dispose(context);
            }
        }
    }

    /**
     * <ul>
     *     <li>rename agency.txt file from unzipped GTFS archive</li>
     *     <li>generate a new agency.txt with a single agency named {@link GtfsImportParameters#getTargetNetwork}</li>
     * </ul>
     *
     * @param newCompany new company to generate agency.txt with
     * @param unzippedGtfsFolder path to unzipped GTFS folder
     * @param gtfsExporter GTFS exporter
     */
    private void generateAgencyTxt(Company newCompany, Path unzippedGtfsFolder, GtfsExporter gtfsExporter) {
        log.info("Generate new agency.txt file");
        Path agencyTxt = unzippedGtfsFolder.resolve(AGENCY_TXT);
        Path originalAgencyTxt = unzippedGtfsFolder.resolve(ORIGINAL_AGENCY_TXT);
        log.info(String.format("Rename %s into %s", agencyTxt, originalAgencyTxt));
        try {
            Files.move(agencyTxt, originalAgencyTxt, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error(ERROR_RENAMING_AGENCY_TXT_FILE, e);
            throw new RuntimeException(ERROR_RENAMING_AGENCY_TXT_FILE, e);
        }
        log.info("Generate agency.txt file with new agency");
        if (!new GtfsAgencyProducer(gtfsExporter).save(newCompany, "", TimeZone.getDefault(), false)) {
            log.error(ERROR_GENERATING_AGENCY_TXT_FILE);
            throw new RuntimeException(ERROR_GENERATING_AGENCY_TXT_FILE);
        }
    }

    /**
     * <ul>
     *     <li>parse route.txt</li>
     *     <li>update all routes agency_id attribute with new agency_id</li
     *     <li>rename original routes.txt file</li>
     *     <li>generate routes.txt file with new agency_id</li>
     * </ul>
     * @param newCompany new company to generate agency.txt with
     * @param unzippedGtfsFolder path to unzipped GTFS folder
     * @param gtfsExporter GTFS exporter
     * @param parameters GTFS import parameters
     */
    private void generateRouteTxt(Company newCompany, Path unzippedGtfsFolder, GtfsExporter gtfsExporter, GtfsImportParameters parameters) {
        log.info("Generate new route.txt file");
        List<GtfsRoute> updatedGtfsRoutes = new ArrayList<>();
        try {
            FactoryParameters factoryParameters = new FactoryParameters(parameters);
            GtfsImporter importer = new GtfsImporter(unzippedGtfsFolder.toString(), factoryParameters);
            String gtfsAgencyId = ObjectIdUtil.toGtfsId(newCompany.getObjectId(), "", false);
            log.info(String.format("Parse route.txt from GTFS archive %s", unzippedGtfsFolder));
            for (GtfsRoute gtfsRoute : importer.getRouteById()) {
                gtfsRoute.setAgencyId(gtfsAgencyId);
                updatedGtfsRoutes.add(new GtfsRoute(gtfsRoute));
            }
            Path routesTxt = unzippedGtfsFolder.resolve(ROUTES_TXT);
            Path originalRoutesTxt = unzippedGtfsFolder.resolve(ORIGINAL_ROUTES_TXT);
            log.info(String.format("Rename file %s into %s", routesTxt, originalRoutesTxt));
            Files.move(routesTxt, originalRoutesTxt, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error(ERROR_RENAMING_ROUTES_TXT_FILE, e);
            throw new RuntimeException(ERROR_RENAMING_ROUTES_TXT_FILE, e);
        } try {
            log.info("Generate route.txt file with updated agency_id");
            for (GtfsRoute gtfsRoute : updatedGtfsRoutes) {
                gtfsExporter.getRouteExporter().export(gtfsRoute);
            }
        } catch (IOException e) {
            log.error(ERROR_GENERATING_ROUTES_TXT_FILE, e);
            throw new RuntimeException(ERROR_GENERATING_ROUTES_TXT_FILE, e);
        }

    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = null;
            try {
                String name = "java:app/mobi.chouette.exchange.gtfs/" + COMMAND;
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
