package mobi.chouette.exchange.gtfs.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import okhttp3.*;
import org.apache.commons.lang.StringUtils;

import javax.naming.InitialContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.zip.ZipFile;

import static mobi.chouette.exchange.gtfs.Constant.GTFS_FARE_ATTRIBUTES_FILE;
import static mobi.chouette.exchange.gtfs.Constant.GTFS_FARE_RULES_FILE;

@Log4j
public class GtfsFareImportCommand implements Command, Constant {

    private static final String FARES_BASE_URL = System.getenv("FARES_BASE_URL");
    private static final String FARES_IMPORT_GTFS_PATH = "fares-referential/fares/import/gtfs";

    private static final String HEADER_PROVIDER = "provider";
    private static final String HEADER_FOLDER = "folder";
    private static final String HEADER_AGENCY_ID = "agencyId";

    static {
        CommandFactory.factories.put(GtfsFareImportCommand.class.getName(),
                new GtfsFareImportCommand.DefaultCommandFactory());
    }

    @Override
    public boolean execute(Context context) throws Exception {
        if (StringUtils.isEmpty(FARES_BASE_URL)) {
            log.warn("FARES_BASE_URL not set, skip import");
            return true;
        }
        HttpUrl faresBaseUrl = HttpUrl.parse(FARES_BASE_URL);
        if (faresBaseUrl == null) {
            log.error("Error parsing FARES_BASE_URL " + FARES_BASE_URL + ", skip import");
            return true;
        }
        JobData jobData = (JobData) context.get(JOB_DATA);
        Path folder = Paths.get(jobData.getPathName());
        Path inputGtfs = folder.resolve(jobData.getInputFilename());
        if (!Files.exists(inputGtfs)) {
            log.info(String.format("GTFS file '%s' does not exist, skip fares import", inputGtfs));
            return true;
        }
        try (ZipFile zipFile = new ZipFile(inputGtfs.toFile())) {
            if (zipFile.getEntry(GTFS_FARE_ATTRIBUTES_FILE) == null || zipFile.getEntry(GTFS_FARE_RULES_FILE) == null) {
                // fare V1 files are optional, so this is not an error
                log.info(String.format("%s and/or %s is not present in GTFS archive there is no fare data to import", GTFS_FARE_ATTRIBUTES_FILE, GTFS_FARE_RULES_FILE));
                return true;
            }
        } catch (IOException e) {
            log.error("Error reading GTFS file", e);
            return true;
        }

        RequestBody requestBody = new MultipartBody.Builder()
                .addFormDataPart(
                        "file",
                        "file",
                        RequestBody.create(inputGtfs.toFile(), MediaType.parse("multipart/form-data"))
                )
                .build();
        String agencyId = (String) context.get(TARGET_COMPANY_OBJECT_ID);
        Request request = new Request.Builder().post(requestBody)
                .url(Objects.requireNonNull(faresBaseUrl.resolve(FARES_IMPORT_GTFS_PATH)))
                .addHeader(HEADER_PROVIDER, jobData.getReferential())
                .addHeader(HEADER_FOLDER, folder.getFileName().toString())
                .addHeader(HEADER_AGENCY_ID, StringUtils.trimToEmpty(agencyId))
                .build();
        OkHttpClient client = new OkHttpClient();
        log.info("Send GTFS to fares GTFS import");
        try (Response response = client.newCall(request).execute()) {
            int status = response.code();
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.error("Error starting GTFS fares import job (resp HTTP status: " + status + ") (resp body: " + responseBody +
                        ")");
            } else {
                log.info("GTFS fares import job started successfully");
            }
        } catch (IOException e) {
            log.error("Error starting GTFS fares import job", e);
        }
        return true;
    }

    public static class DefaultCommandFactory extends CommandFactory {
        @Override
        protected Command create(InitialContext context) throws IOException {
            return new GtfsFareImportCommand();
        }
    }

}
