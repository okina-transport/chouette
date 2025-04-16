package mobi.chouette.exchange.importer.utils;

import org.joda.time.DateTimeZone;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

public class CsvGenerationConstants {

    private CsvGenerationConstants() {
        throw new IllegalStateException("Utility class");
    }

    public static final DateTimeFormatter DTF_HHMMSS = DateTimeFormatter.ofPattern("HHmmss");
    public static final Path TH_SM_CSV_DIRECTORY = Paths.get("/opt/jboss/data/referentials/mobiiti_technique/theoreticalStopMonitoringInfo/");
    public static final String TH_SM_CSV_FILE = "_th_sm.csv";
    public static final DateTimeZone ZONE_ID = DateTimeZone.forID("Europe/Paris");
    public static final String SCHEMA_TECHNIQUE = "technique";
    public static final String PREFIX_SCHEMA_MOBIITI = "mobiiti";
}
