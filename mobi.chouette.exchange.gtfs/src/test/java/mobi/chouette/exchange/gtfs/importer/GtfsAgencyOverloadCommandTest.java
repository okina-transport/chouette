package mobi.chouette.exchange.gtfs.importer;

import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.gtfs.JobDataTest;
import mobi.chouette.exchange.report.ActionReport;
import mobi.chouette.model.Company;
import mobi.chouette.model.type.OrganisationTypeEnum;
import mobi.chouette.model.util.Referential;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static mobi.chouette.common.Constant.REFERENTIAL;
import static mobi.chouette.common.Constant.TARGET_COMPANY_OBJECT_ID;

public class GtfsAgencyOverloadCommandTest {

    // job data
    public static final long ID = 1L;
    public static final String INPUT_FILENAME = "GTFS_EXPORT_COURALIN_STAR.zip";
    public static final String OUTPUT_FILENAME = "";
    public static final String ACTION = "importer";
    public static final String TYPE = "gtfs";
    public static final String OBJECT_ID_PREFIX = "test";
    public static final String PATHNAME = Paths.get("src", "test", "data", "gtfs_agency_overload_command").toString();

    // targetNetwork
    public static final String TARGET_NETWORK = "target_network";

    // folders
    public static final Path FOLDER_INPUT = Paths.get(PATHNAME, "input");

    // files
    public static final Path FILE_AGENCY_TXT = Paths.get(FOLDER_INPUT.toString(), "agency.txt");
    public static final Path FILE_ROUTES_TXT = Paths.get(FOLDER_INPUT.toString(), "routes.txt");
    public static final Path FILE_ORIGINAL_ROUTES_TXT = Paths.get(FOLDER_INPUT.toString(), "original.routes.txt");
    public static final Path FILE_GTFS_ZIP = Paths.get(PATHNAME, INPUT_FILENAME);
    public static final String COMPANY_OBJECT_ID = "test:Authority:666";

    final Company targetCompany;
    GtfsAgencyOverloadCommand tested;

    public GtfsAgencyOverloadCommandTest() {
        targetCompany = new Company();
        targetCompany.setId(1L);
        targetCompany.setOrganisationType(OrganisationTypeEnum.Authority);
        targetCompany.setName(TARGET_NETWORK);
        targetCompany.setObjectId(COMPANY_OBJECT_ID);
        targetCompany.setUrl("https://www.gtfs.io/company/666");
        targetCompany.setActive(true);

        tested = new GtfsAgencyOverloadCommand();
    }

    private static Context getContext(String targetNetwork, String targetCompanyObjectId) {
        Context context = new Context();
        GtfsImportParameters parameters = new GtfsImportParameters();
        parameters.setTargetNetwork(targetNetwork);
        context.put(Constant.CONFIGURATION, parameters);
        JobDataTest jobDataTest = new JobDataTest(ID, INPUT_FILENAME, OUTPUT_FILENAME, ACTION, TYPE, OBJECT_ID_PREFIX, PATHNAME);
        context.put(Constant.JOB_DATA, jobDataTest);
        context.put(Constant.INPUT, PATHNAME);
        context.put(Constant.REPORT, new ActionReport());
        context.put(TARGET_COMPANY_OBJECT_ID, targetCompanyObjectId);
        return context;
    }

    private static Context getContextWithReferential(String targetNetwork, String targetCompanyObjectId, Company targetCompany) {
        Context context = getContext(targetNetwork, targetCompanyObjectId);
        Referential referential = new Referential();
        referential.getSharedCompanies().put(targetCompanyObjectId, targetCompany);
        context.put(REFERENTIAL, referential);
        return context;
    }

    private static List<CSVRecord> parseCsv(File csvFile) throws IOException {
        try (Reader csvFileReader = new FileReader(csvFile)) {
            CSVParser csvParser = CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(false).build().parse(csvFileReader);
            return csvParser.getRecords();
        }
    }

    @AfterMethod
    private void afterMethod() throws IOException {
        // delete input folder and its content after each test
        FileUtils.deleteDirectory(FOLDER_INPUT.toFile());
    }

    @BeforeMethod
    private void beforeMethod() throws IOException {
        // recreate "input" folder and unzip GTFS into it before each test
        if (!FOLDER_INPUT.toFile().mkdir() && !FOLDER_INPUT.toFile().exists() && !FOLDER_INPUT.toFile().isDirectory()) {
            Assert.fail("Error making input directory");
        }
        try (ZipFile gtfsZip = new ZipFile(FILE_GTFS_ZIP.toString())) {
            gtfsZip.extractAll(FOLDER_INPUT.toString());
        }
    }

    @DataProvider
    public Object[][] blankTargetNetwork() {
        return new Object[][]{{null}, {""}, {"            "},};
    }

    @DataProvider
    public Object[][] companiesByName() {
        return new Object[][]{{Collections.singletonList(targetCompany)}};
    }

    @Test(dataProvider = "blankTargetNetwork", expectedExceptions = IllegalArgumentException.class)
    public void testExecute_whenContextTargetNetworkIsBlank_thenThrowsIllegalArgumentException(String targetNetwork) throws Exception {
        // arrange
        Context ctx = getContextWithReferential(targetNetwork, "", null);

        Assert.assertTrue(StringUtils.isBlank(targetNetwork), "targetNetwork should be blank");

        // act
        tested.execute(ctx);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testExecute_whenTargetCompanyIsNotInReferential_thenThrowsRuntimeException() throws Exception {
        // arrange
        Context ctx = getContextWithReferential(TARGET_NETWORK, COMPANY_OBJECT_ID, new Company());

        // act
        tested.execute(ctx);
    }

    @Test(dataProvider = "companiesByName")
    public void testExecute_whenTargetCompanyIsInReferential_thenAgencyTxtIsGeneratedProperly(List<Company> companiesByName) throws Exception {
        // arrange
        Context context = getContextWithReferential(TARGET_NETWORK, COMPANY_OBJECT_ID, targetCompany);

        // act
        tested.execute(context);

        // assert
        String expectedAgencyId = "666";
        List<CSVRecord> agencies = parseCsv(FILE_AGENCY_TXT.toFile());
        Assert.assertEquals(agencies.size(), 1, "there should be 1 agency");
        Assert.assertEquals(agencies.get(0).get("agency_name"), TARGET_NETWORK, "agency.agency_name should be equal to targetNetwork");
        Assert.assertEquals(agencies.get(0).get("agency_id"), expectedAgencyId, "agency.agency_id should be equal to company.object_id");
    }

    @Test(dataProvider = "companiesByName")
    public void testExecute__whenTargetCompanyIsInReferential_thenRouteTxtIsGeneratedProperly(List<Company> companiesByName) throws Exception {
        // arrange
        Context context = getContextWithReferential(TARGET_NETWORK, COMPANY_OBJECT_ID, targetCompany);

        // act
        tested.execute(context);

        // assert
        List<CSVRecord> newRoutes = parseCsv(FILE_ROUTES_TXT.toFile());
        List<CSVRecord> originalRoutes = parseCsv(FILE_ORIGINAL_ROUTES_TXT.toFile());
        String expectedAgencyId = "666";
        Assert.assertEquals(newRoutes.size(), originalRoutes.size(), "there should be the same number of routes");
        for (int i = 0; i < newRoutes.size(); i++) {
            Assert.assertEquals(newRoutes.get(i).get("agency_id"), expectedAgencyId, "route.agency_id should be equal to company.object_id");
            Map<String, String> newRouteMap = newRoutes.get(i).toMap();
            Map<String, String> originalRouteMap = originalRoutes.get(i).toMap();
            for (String key : originalRouteMap.keySet()) {
                if ("agency_id".equals(key)) {
                    continue;
                }
                Assert.assertEquals(newRouteMap.get(key).toLowerCase(), originalRouteMap.get(key).toLowerCase(), "new routes.txt should have same values than original routes.txt (except for agency_id)");
            }
        }
    }

}
