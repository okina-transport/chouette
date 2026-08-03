package mobi.chouette.exchange.gtfs.parser;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.gtfs.importer.GtfsImportParameters;
import mobi.chouette.exchange.gtfs.model.importer.FactoryParameters;
import mobi.chouette.exchange.gtfs.model.importer.GtfsImporter;
import mobi.chouette.model.StopAreaTranslation;
import mobi.chouette.model.util.Referential;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;

import static mobi.chouette.common.Constant.*;

public class GtfsTranslationParserTest {

    private Context context;
    private Referential referential;

    @BeforeMethod
    public void beforeEachTest() throws Exception {
        File dir = Files.createTempDirectory("gtfs-translations-test").toFile();
        dir.deleteOnExit();

        writeFile(dir, "stops.txt",
                "stop_id,stop_name,stop_lat,stop_lon,location_type\n" +
                        "S1,Stop One,48.0,-1.0,\n" +
                        "ST1,Station One,48.1,-1.1,1\n");

        writeFile(dir, "translations.txt",
                "table_name,field_name,language,translation,record_id,record_sub_id,field_value\n" +
                        "stops,stop_name,en,Stop One EN,S1,,\n" +
                        "stops,stop_name,en,Station One EN,ST1,,\n" +
                        "routes,route_long_name,en,Route One EN,R1,,\n" +
                        "routes,route_short_name,en,R1 EN,R1,,\n" +
                        "trips,trip_headsign,en,Headsign EN,T1,,\n" +
                        "agency,agency_name,en,Agency EN,A1,,\n" +
                        "feed_info,feed_publisher_name,en,Legacy Feed EN,,,Original Feed Name\n" +
                        "levels,level_name,en,Level One EN,L1,,\n" +
                        "stops,stop_name,en,Stop Two EN,,,Stop Two\n");

        GtfsImportParameters configuration = new GtfsImportParameters();
        configuration.setObjectIdPrefix("GtfsTest");
        configuration.setSplitIdOnDot(false);

        GtfsImporter importer = new GtfsImporter(dir.getAbsolutePath(), new FactoryParameters(configuration));

        referential = new Referential();
        context = new Context();
        context.put(PARSER, importer);
        context.put(REFERENTIAL, referential);
        context.put(CONFIGURATION, configuration);
    }

    private void writeFile(File dir, String name, String content) throws Exception {
        try (FileWriter writer = new FileWriter(new File(dir, name))) {
            writer.write(content);
        }
    }

    @Test
    public void parseResolvesEachTableToItsChouetteOwnerEntity() throws Exception {
        new GtfsTranslationParser().parse(context);

        // 2 stops + 3 routes + 1 trip + 3 agencies (network + authority + operator)
        // + 1 field_value-keyed stop = 10
        // the feed_info/levels rows have no NeTEx-exportable counterpart, so no *Translation entity
        // is ever created for them - there is no generic bucket left to fall into.
        int total = countAll(referential.getStopAreaTranslationsByObjectId(), referential.getStopAreaTranslationsByFieldValue())
                + countAll(referential.getLineTranslationsByObjectId(), referential.getLineTranslationsByFieldValue())
                + countAll(referential.getVehicleJourneyTranslationsByObjectId(), referential.getVehicleJourneyTranslationsByFieldValue())
                + countAll(referential.getNetworkTranslationsByObjectId(), referential.getNetworkTranslationsByFieldValue())
                + countAll(referential.getCompanyTranslationsByObjectId(), referential.getCompanyTranslationsByFieldValue());
        Assert.assertEquals(total, 10);

        assertStopAreaTranslation("GtfsTest:Quay:S1", "stopName", "Stop One EN");
        assertStopAreaTranslation("GtfsTest:StopPlace:ST1", "stopName", "Station One EN");
        assertLineTranslation("GtfsTest:Line:R1", "name", "Route One EN");
        assertLineTranslation("GtfsTest:Line:R1", "publishedName", "Route One EN");
        assertLineTranslation("GtfsTest:Line:R1", "number", "R1 EN");
        assertVehicleJourneyTranslation("GtfsTest:VehicleJourney:T1", "publishedJourneyName", "Headsign EN");
        assertNetworkTranslation("GtfsTest:Network:A1", "name", "Agency EN");
        assertCompanyTranslation("GtfsTest:Authority:A1", "name", "Agency EN");
        assertCompanyTranslation("GtfsTest:Operator:A1o", "name", "Agency EN");

        StopAreaTranslation fieldValueKeyed = referential.getStopAreaTranslationsByFieldValue().stream()
                .filter(t -> "Stop Two EN".equals(t.getTranslation()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected a translation row for the field_value-keyed stop"));
        Assert.assertNull(fieldValueKeyed.getStopArea(), "field_value-keyed row has no resolvable owner");
        Assert.assertEquals(fieldValueKeyed.getFieldName(), "stopName");
        Assert.assertEquals(fieldValueKeyed.getFieldValue(), "Stop Two");
    }

    private <T> int countAll(java.util.Map<String, java.util.List<T>> byObjectId, java.util.List<T> byFieldValue) {
        return byObjectId.values().stream().mapToInt(java.util.List::size).sum() + byFieldValue.size();
    }

    private void assertStopAreaTranslation(String expectedOwnerObjectId, String fieldName, String expectedTranslation) {
        boolean found = referential.getStopAreaTranslationsByObjectId().getOrDefault(expectedOwnerObjectId, java.util.List.of()).stream().anyMatch(t ->
                fieldName.equals(t.getFieldName()) && "en".equals(t.getLanguage()) && expectedTranslation.equals(t.getTranslation()));
        Assert.assertTrue(found, "expected a StopArea translation targeting " + expectedOwnerObjectId + " (" + fieldName + ")");
    }

    private void assertLineTranslation(String expectedOwnerObjectId, String fieldName, String expectedTranslation) {
        boolean found = referential.getLineTranslationsByObjectId().getOrDefault(expectedOwnerObjectId, java.util.List.of()).stream().anyMatch(t ->
                fieldName.equals(t.getFieldName()) && "en".equals(t.getLanguage()) && expectedTranslation.equals(t.getTranslation()));
        Assert.assertTrue(found, "expected a Line translation targeting " + expectedOwnerObjectId + " (" + fieldName + ")");
    }

    private void assertVehicleJourneyTranslation(String expectedOwnerObjectId, String fieldName, String expectedTranslation) {
        boolean found = referential.getVehicleJourneyTranslationsByObjectId().getOrDefault(expectedOwnerObjectId, java.util.List.of()).stream().anyMatch(t ->
                fieldName.equals(t.getFieldName()) && "en".equals(t.getLanguage()) && expectedTranslation.equals(t.getTranslation()));
        Assert.assertTrue(found, "expected a VehicleJourney translation targeting " + expectedOwnerObjectId + " (" + fieldName + ")");
    }

    private void assertNetworkTranslation(String expectedOwnerObjectId, String fieldName, String expectedTranslation) {
        boolean found = referential.getNetworkTranslationsByObjectId().getOrDefault(expectedOwnerObjectId, java.util.List.of()).stream().anyMatch(t ->
                fieldName.equals(t.getFieldName()) && "en".equals(t.getLanguage()) && expectedTranslation.equals(t.getTranslation()));
        Assert.assertTrue(found, "expected a Network translation targeting " + expectedOwnerObjectId + " (" + fieldName + ")");
    }

    private void assertCompanyTranslation(String expectedOwnerObjectId, String fieldName, String expectedTranslation) {
        boolean found = referential.getCompanyTranslationsByObjectId().getOrDefault(expectedOwnerObjectId, java.util.List.of()).stream().anyMatch(t ->
                fieldName.equals(t.getFieldName()) && "en".equals(t.getLanguage()) && expectedTranslation.equals(t.getTranslation()));
        Assert.assertTrue(found, "expected a Company translation targeting " + expectedOwnerObjectId + " (" + fieldName + ")");
    }

}
