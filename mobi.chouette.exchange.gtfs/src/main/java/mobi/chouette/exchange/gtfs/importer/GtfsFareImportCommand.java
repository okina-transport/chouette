package mobi.chouette.exchange.gtfs.importer;

import com.google.common.net.HttpHeaders;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.utils.TokenServiceBuilder;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import mobi.chouette.exchange.importer.utils.TokenService;

import javax.naming.InitialContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.zip.ZipFile;

import static mobi.chouette.exchange.gtfs.model.fares.GtfsFareV1File.FARE_ATTRIBUTES;
import static mobi.chouette.exchange.gtfs.model.fares.GtfsFareV1File.FARE_RULES;
import static mobi.chouette.exchange.gtfs.model.fares.GtfsFareV2File.FARE_PRODUCTS;

@Log4j
public class GtfsFareImportCommand implements Command, Constant {

    private static final String FARES_BASE_URL = System.getenv("FARES_BASE_URL");
    private static final String FARES_IMPORT_GTFS_PATH = "fares-referential/fares/import/gtfs";
    private static final String FARES_IMPORT_GTFS_PATH_V2 = "fares-referential/fares/v2/import/gtfs";

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
        boolean validFareV1Input;
        boolean validFareV2Input;
        try (ZipFile zipFile = new ZipFile(inputGtfs.toFile())) {
            validFareV1Input = zipFile.getEntry(FARE_ATTRIBUTES.getFilename()) != null
                    && zipFile.getEntry(FARE_RULES.getFilename()) != null;
            validFareV2Input = zipFile.getEntry(FARE_PRODUCTS.getFilename()) != null;
            if (!validFareV1Input && !validFareV2Input) {
                // fare V1 files are optional, so this is not an error
                log.info("GTFS fares files are not present in GTFS archive there is no fare data to import");
                return true;
            }
        } catch (IOException e) {
            log.error("Error reading GTFS file", e);
            return true;
        }

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                        "file",
                        "file.zip",
                        RequestBody.create(MediaType.parse("application/zip"), inputGtfs.toFile())
                )
                .build();
        String agencyId = (String) context.get(TARGET_COMPANY_OBJECT_ID);
        Request.Builder requestBuilder = new Request.Builder().post(requestBody);
        if (validFareV2Input) {
            requestBuilder.url(Objects.requireNonNull(faresBaseUrl.resolve(FARES_IMPORT_GTFS_PATH_V2)));
        } else {
            requestBuilder.url(Objects.requireNonNull(faresBaseUrl.resolve(FARES_IMPORT_GTFS_PATH)));
        }
        TokenService tokenService = TokenServiceBuilder.init().build();
        Request request = requestBuilder
                .addHeader(HEADER_PROVIDER, jobData.getReferential())
                .addHeader(HEADER_FOLDER, folder.getFileName().toString())
                .addHeader(HEADER_AGENCY_ID, StringUtils.trimToEmpty(agencyId))
                .addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.getToken())
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
