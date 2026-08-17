package mobi.chouette.exchange.importer;

import mobi.chouette.common.Context;
import mobi.chouette.dao.LineTranslationDAO;
import mobi.chouette.dao.ProviderDAO;
import mobi.chouette.dao.StopAreaTranslationDAO;
import mobi.chouette.dao.VehicleJourneyTranslationDAO;
import mobi.chouette.exchange.importer.utils.FileUtils;
import mobi.chouette.model.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class GenerateTranslationMappingCsvTest {

    private static final String PROVIDER_CODE = "testds";
    private static final String DATASET = "TESTDS";

    private LineTranslationDAO lineTranslationDAOMock;
    private StopAreaTranslationDAO stopAreaTranslationDAOMock;
    private VehicleJourneyTranslationDAO vehicleJourneyTranslationDAOMock;
    private ProviderDAO providerDAOMock;
    private Path outputDir;
    GenerateTranslationMappingCsv tested;

    @BeforeMethod
    private void beforeMethod() throws IOException {
        lineTranslationDAOMock = Mockito.mock(LineTranslationDAO.class);
        stopAreaTranslationDAOMock = Mockito.mock(StopAreaTranslationDAO.class);
        vehicleJourneyTranslationDAOMock = Mockito.mock(VehicleJourneyTranslationDAO.class);
        providerDAOMock = Mockito.mock(ProviderDAO.class);
        outputDir = Files.createTempDirectory("translationMappingCsvTest");
        tested = new GenerateTranslationMappingCsv(lineTranslationDAOMock, stopAreaTranslationDAOMock,
                vehicleJourneyTranslationDAOMock, providerDAOMock, new FileUtils(), outputDir);

        Provider provider = new Provider();
        provider.setCode(PROVIDER_CODE);
        provider.setName("Test dataset");
        Mockito.when(providerDAOMock.getAllProviders()).thenReturn(Collections.singletonList(provider));

        Mockito.when(lineTranslationDAOMock.findAllNewTransaction()).thenReturn(Collections.emptyList());
        Mockito.when(stopAreaTranslationDAOMock.findAllNewTransaction()).thenReturn(Collections.emptyList());
        Mockito.when(vehicleJourneyTranslationDAOMock.findAllNewTransaction()).thenReturn(Collections.emptyList());
    }

    @AfterMethod
    private void afterMethod() throws IOException {
        Path csvFile = outputDir.resolve(GenerateTranslationMappingCsv.TRANSLATION_MAPPING_CSV);
        Files.deleteIfExists(csvFile);
        Files.deleteIfExists(outputDir);
    }

    @Test
    public void testExecute_recordIdKeyedTranslation_writesResolvedObjectIdAndBlankFieldValue() throws Exception {
        // arrange: record_id-keyed translations, owner FK already resolved at import time
        Line line = new Line();
        line.setObjectId("TESTDS:Line:1");
        LineTranslation lineTranslation = new LineTranslation();
        lineTranslation.setObjectId("TESTDS:Translation:1");
        lineTranslation.setLine(line);
        lineTranslation.setFieldName("name");
        lineTranslation.setLanguage("en");
        lineTranslation.setTranslation("Line one");
        Mockito.when(lineTranslationDAOMock.findAllNewTransaction()).thenReturn(Collections.singletonList(lineTranslation));

        StopArea stopArea = new StopArea();
        stopArea.setObjectId("TESTDS:StopArea:1");
        stopArea.setOriginalStopId("1");
        StopAreaTranslation stopAreaTranslation = new StopAreaTranslation();
        stopAreaTranslation.setObjectId("TESTDS:Translation:2");
        stopAreaTranslation.setStopArea(stopArea);
        stopAreaTranslation.setFieldName("stopName");
        stopAreaTranslation.setLanguage("es");
        stopAreaTranslation.setTranslation("Parada uno");
        Mockito.when(stopAreaTranslationDAOMock.findAllNewTransaction()).thenReturn(Collections.singletonList(stopAreaTranslation));

        VehicleJourney vehicleJourney = new VehicleJourney();
        vehicleJourney.setObjectId("TESTDS:VehicleJourney:1");
        VehicleJourneyTranslation vjTranslation = new VehicleJourneyTranslation();
        vjTranslation.setObjectId("TESTDS:Translation:3");
        vjTranslation.setVehicleJourney(vehicleJourney);
        vjTranslation.setFieldName("publishedJourneyName");
        vjTranslation.setLanguage("de");
        vjTranslation.setTranslation("Fahrt eins");
        Mockito.when(vehicleJourneyTranslationDAOMock.findAllNewTransaction()).thenReturn(Collections.singletonList(vjTranslation));

        // act
        boolean out = tested.execute(new Context());

        // assert
        Assert.assertTrue(out, "should return true");
        List<CSVRecord> records = readCsv();
        Assert.assertEquals(records.size(), 3);

        assertRecord(records.get(0), DATASET, "LINE", "1", "name", "", "en", "Line one");
        assertRecord(records.get(1), DATASET, "STOP", "1", "stopName", "", "es", "Parada uno");
        assertRecord(records.get(2), DATASET, "VEHICLE_JOURNEY", "1", "publishedJourneyName", "", "de", "Fahrt eins");
    }

    @Test
    public void testExecute_fieldValueKeyedTranslation_writesBlankObjectIdAndFieldValue() throws Exception {
        // arrange: legacy field_value-keyed row (no record_id in translations.txt), owner FK left null
        LineTranslation lineTranslation = new LineTranslation();
        lineTranslation.setObjectId("TESTDS:Translation:1");
        lineTranslation.setFieldName("name");
        lineTranslation.setFieldValue("Downtown Express");
        lineTranslation.setLanguage("en");
        lineTranslation.setTranslation("Downtown Express (EN)");
        Mockito.when(lineTranslationDAOMock.findAllNewTransaction()).thenReturn(Collections.singletonList(lineTranslation));

        // act
        boolean out = tested.execute(new Context());

        // assert
        Assert.assertTrue(out, "should return true");
        List<CSVRecord> records = readCsv();
        Assert.assertEquals(records.size(), 1);
        assertRecord(records.get(0), DATASET, "LINE", "", "name", "Downtown Express", "en", "Downtown Express (EN)");
    }

    @Test
    public void testExecute_whenNoProviders_writesHeaderOnly() throws Exception {
        // arrange
        Mockito.when(providerDAOMock.getAllProviders()).thenReturn(Collections.emptyList());

        // act
        boolean out = tested.execute(new Context());

        // assert
        Assert.assertTrue(out, "should return true");
        Assert.assertTrue(readCsv().isEmpty());
    }

    private List<CSVRecord> readCsv() throws IOException {
        Path csvFile = outputDir.resolve(GenerateTranslationMappingCsv.TRANSLATION_MAPPING_CSV);
        try (CSVParser parser = CSVParser.parse(csvFile, java.nio.charset.StandardCharsets.UTF_8,
                CSVFormat.Builder.create().setHeader().setSkipHeaderRecord(true).get())) {
            return parser.getRecords();
        }
    }

    private void assertRecord(CSVRecord csvRecord, String dataset, String objectType, String objectId, String fieldName,
            String fieldValue, String language, String translation) {
        Assert.assertEquals(csvRecord.get("dataset"), dataset);
        Assert.assertEquals(csvRecord.get("object_type"), objectType);
        Assert.assertEquals(csvRecord.get("object_id"), objectId);
        Assert.assertEquals(csvRecord.get("field_name"), fieldName);
        Assert.assertEquals(csvRecord.get("field_value"), fieldValue);
        Assert.assertEquals(csvRecord.get("language"), language);
        Assert.assertEquals(csvRecord.get("translation"), translation);
    }
}
