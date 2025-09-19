package mobi.chouette.service;

import lombok.extern.log4j.Log4j;
import mobi.chouette.core.ChouetteRuntimeException;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Singleton(name = PostgresDumpService.BEAN_NAME)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Log4j
public class PostgresDumpService {

    public static final String BEAN_NAME = "PostgresDumpService";

    public void exportSQLDumpForSchema(String dumpFilename, String pgSchema) throws IOException, InterruptedException {
        log.info("Exporting SQL DUMP for schema " + pgSchema + " into " + dumpFilename);

        String connectionUrl = getConnectionUrl();
        log.debug("connectionUrl: " + connectionUrl);

        ProcessBuilder dumpCommand = new ProcessBuilder(
                "pg_dump",
                connectionUrl,
                "-n", pgSchema,
                "-F", "p",
                "-f", dumpFilename
        );

        try {
            Process dump = dumpCommand.start();
            BufferedReader r = new BufferedReader(
                    new InputStreamReader(dump.getErrorStream()));

            String line = r.readLine();
            while (line != null) {
                System.err.println(line);
                line = r.readLine();
            }
            r.close();
            dump.waitFor();

            if (dump.exitValue() != 0) {
                throw new ChouetteRuntimeException("Impossible de réaliser le dump de la filiale: " + pgSchema) {
                    @Override
                    public String getPrefix() {
                        return null;
                    }

                    @Override
                    public String getCode() {
                        return null;
                    }
                };
            }

        } catch (IOException | InterruptedException e) {
            log.error("Error while generating DUMP for schema " + pgSchema);
            throw e;
        }
    }

    public void importSQLDump(String dumpFilename) throws IOException, InterruptedException {
        log.info("Importing SQL DUMP from " + dumpFilename);

        String connectionUrl = getConnectionUrl();
        log.debug("connectionUrl: " + connectionUrl);

        ProcessBuilder restoreCommand = new ProcessBuilder(
                "psql",
                "--single-transaction",
                "-d", connectionUrl,
                "-f", dumpFilename
        );

        restoreCommand.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        restoreCommand.redirectErrorStream(true);

        try {
            Process restore = restoreCommand.start();
            BufferedReader r = new BufferedReader(
                    new InputStreamReader(restore.getErrorStream()));

            String line = r.readLine();
            while (line != null) {
                log.error(line);
                line = r.readLine();
            }
            r.close();
            restore.waitFor();
            if (restore.exitValue() != 0) {
                throw new ChouetteRuntimeException("Erreur lors de l'import du dump " + dumpFilename) {
                    @Override
                    public String getPrefix() {
                        return null;
                    }

                    @Override
                    public String getCode() {
                        return null;
                    }
                };
            } else {
                log.info("Successfully imported SQL DUMP from " + dumpFilename);
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error while importing DUMP");
            throw e;
        }

    }

    public void deleteDumpFile(String dumpFilename) throws IOException, InterruptedException {
        log.info("Deleting dump file : " + dumpFilename);
        ProcessBuilder deleteCommand = new ProcessBuilder(
                "rm", dumpFilename
        );

        deleteCommand.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        deleteCommand.redirectErrorStream(true);

        try {
            Process delete = deleteCommand.start();
            BufferedReader r = new BufferedReader(
                    new InputStreamReader(delete.getErrorStream()));

            String line = r.readLine();
            while (line != null) {
                log.error(line);
                line = r.readLine();
            }
            r.close();
            delete.waitFor();
            if (delete.exitValue() != 0) {
                log.info("Error deleting DUMP file " + dumpFilename);
            } else {
                log.info("Successfully deleted DUMP file " + dumpFilename);
            }

        } catch (IOException | InterruptedException e) {
            log.error("Error deleting DUMP file " + dumpFilename);
            throw e;
        }
    }

    protected String getConnectionUrl() {
        return String.format(
                "postgresql://%s:%s@%s:%s/%s",
                System.getenv("CHOUETTE_DB_USER"),
                System.getenv("CHOUETTE_DB_PASSWORD"),
                System.getenv("CHOUETTE_DB_HOST"),
                System.getenv("CHOUETTE_DB_PORT"),
                System.getenv("CHOUETTE_DB_NAME")
        );
    }

}
