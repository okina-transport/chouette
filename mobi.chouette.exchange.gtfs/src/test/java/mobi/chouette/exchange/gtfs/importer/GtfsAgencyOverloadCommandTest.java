package mobi.chouette.exchange.gtfs.importer;

import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.ObjectIdUtil;
import mobi.chouette.dao.CompanyDAO;
import mobi.chouette.exchange.gtfs.JobDataTest;
import mobi.chouette.exchange.report.ActionReport;
import mobi.chouette.model.Company;
import mobi.chouette.model.type.OrganisationTypeEnum;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.mockito.Mockito;
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

public class GtfsAgencyOverloadCommandTest {

    // job data
    public static final long ID = 1L;
    public static final String INPUT_FILENAME = "GTFS_EXPORT_COURALIN_STAR.zip";
    public static final String OUTPUT_FILENAME = "";
    public static final String ACTION = "importer";
    public static final String TYPE = "gtfs";
    public static final String REFERENTIAL = "test";
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
    final Company company;
    CompanyDAO companyDAOMock;
    GtfsAgencyOverloadCommand tested;

    public GtfsAgencyOverloadCommandTest() {
        company = new Company();
        company.setId(1L);
        company.setOrganisationType(OrganisationTypeEnum.Authority);
        company.setName(TARGET_NETWORK);
        company.setObjectId("test:Authority:666");
        company.setUrl("https://www.gtfs.io/company/666");
        company.setActive(true);
    }

    private static Context getContext() {
        Context context = new Context();
        GtfsImportParameters parameters = new GtfsImportParameters();
        parameters.setTargetNetwork(TARGET_NETWORK);
        parameters.setObjectIdPrefix(REFERENTIAL);
        parameters.setSplitIdOnDot(false);
        context.put(Constant.CONFIGURATION, parameters);
        JobDataTest jobDataTest = new JobDataTest(ID, INPUT_FILENAME, OUTPUT_FILENAME, ACTION, TYPE, REFERENTIAL, PATHNAME);
        context.put(Constant.JOB_DATA, jobDataTest);
        context.put(Constant.INPUT, PATHNAME);
        context.put(Constant.REPORT, new ActionReport());
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
        // (re)create mock and tested command before each test
        companyDAOMock = Mockito.mock(CompanyDAO.class);
        tested = new GtfsAgencyOverloadCommand(companyDAOMock);

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
        return new Object[][]{{Collections.singletonList(company)}};
    }

    @Test(dataProvider = "blankTargetNetwork", expectedExceptions = IllegalArgumentException.class)
    public void test__when_context_target_network_is_blank__then_throws_illegal_argument_exception(String targetNetwork) throws Exception {
        // arrange
        Context context = new Context();
        GtfsImportParameters parameters = new GtfsImportParameters();
        parameters.setTargetNetwork(targetNetwork);
        context.put(Constant.CONFIGURATION, parameters);

        Assert.assertTrue(StringUtils.isBlank(parameters.getTargetNetwork()), "targetNetwork should be blank");

        // act
        tested.execute(context);
    }

    @Test(dataProvider = "companiesByName")
    public void test__when_company_with_name_equal_to_target_network_exists__then_agency_txt_is_generated_properly(List<Company> companiesByName) throws Exception {
        // arrange
        Context context = getContext();
        Mockito.when(companyDAOMock.findByName(TARGET_NETWORK)).thenReturn(companiesByName);

        // act
        tested.execute(context);

        // assert
        GtfsImportParameters parameters = (GtfsImportParameters) context.get(Constant.CONFIGURATION);
        String expectedAgencyId = ObjectIdUtil.toGtfsId(companiesByName.get(0).getObjectId(), parameters.getObjectIdPrefix(), false);
        List<CSVRecord> agencies = parseCsv(FILE_AGENCY_TXT.toFile());
        Assert.assertEquals(agencies.size(), 1, "there should be 1 agency");
        Assert.assertEquals(agencies.get(0).get("agency_name"), TARGET_NETWORK, "agency.agency_name should be equal to targetNetwork");
        Assert.assertEquals(agencies.get(0).get("agency_id"), expectedAgencyId, "agency.agency_id should be equal to company.object_id");
    }

    @Test(dataProvider = "companiesByName")
    public void test__when_company_with_name_equal_to_target_network_exists__then_routes_txt_is_generated_properly(List<Company> companiesByName) throws Exception {
        // arrange
        Context context = getContext();
        Mockito.when(companyDAOMock.findByName(TARGET_NETWORK)).thenReturn(companiesByName);

        // act
        tested.execute(context);

        // assert
        List<CSVRecord> newRoutes = parseCsv(FILE_ROUTES_TXT.toFile());
        List<CSVRecord> originalRoutes = parseCsv(FILE_ORIGINAL_ROUTES_TXT.toFile());
        GtfsImportParameters parameters = (GtfsImportParameters) context.get(Constant.CONFIGURATION);
        String expectedAgencyId = ObjectIdUtil.toGtfsId(companiesByName.get(0).getObjectId(), parameters.getObjectIdPrefix(), false);
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

    @Test
    public void test__when_no_company_with_name_equal_to_target_network_exists__then_agency_txt_is_generated_properly() throws Exception {
        // arrange
        Context context = getContext();
        Mockito.when(companyDAOMock.findByName(TARGET_NETWORK)).thenReturn(null);

        // act
        tested.execute(context);

        // assert
        List<CSVRecord> agencies = parseCsv(FILE_AGENCY_TXT.toFile());
        Assert.assertEquals(agencies.size(), 1, "there should be 1 agency");
        Assert.assertEquals(agencies.get(0).get("agency_name"), TARGET_NETWORK, "agency.agency_name should be equal to targetNetwork");
    }

    @Test
    public void test__when_no_company_with_name_equal_to_target_network_exists__then_routes_txt_is_generated_properly() throws Exception {
        // arrange
        Context context = getContext();
        Mockito.when(companyDAOMock.findByName(TARGET_NETWORK)).thenReturn(null);

        // act
        tested.execute(context);

        // assert
        List<CSVRecord> newRoutes = parseCsv(FILE_ROUTES_TXT.toFile());
        List<CSVRecord> originalRoutes = parseCsv(FILE_ORIGINAL_ROUTES_TXT.toFile());
        Assert.assertEquals(newRoutes.size(), originalRoutes.size(), "there should be the same number of routes");
        for (int i = 0; i < newRoutes.size(); i++) {
            Map<String, String> newRouteMap = newRoutes.get(i).toMap();
            Map<String, String> originalRouteMap = originalRoutes.get(i).toMap();
            for (String key : originalRouteMap.keySet()) {
                if ("agency_id".equals(key)) {
                    Assert.assertNotEquals(newRouteMap.get(key), originalRouteMap.get(key), "agency_id should be updated");
                } else {
                    Assert.assertEquals(newRouteMap.get(key).toLowerCase(), originalRouteMap.get(key).toLowerCase(), "new routes.txt should have same values than original routes.txt (except for agency_id)");
                }
            }
        }
    }

}
