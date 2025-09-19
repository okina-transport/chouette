package mobi.chouette.service;

import lombok.extern.log4j.Log4j;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.ReferentialDAO;
import mobi.chouette.dao.StorageDAO;
import mobi.chouette.model.Storage;
import mobi.chouette.persistence.hibernate.ContextHolder;
import org.joda.time.Instant;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static mobi.chouette.common.Constant.SUPERSPACE_PREFIX;


@Singleton(name = StorageService.BEAN_NAME)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Log4j
public class StorageService {

    public static final String BEAN_NAME = "StorageService";
    private static final Path STORAGE_DUMPS = Paths.get("/opt/jboss/data/referentials/mobiiti_technique/storage/");

    @EJB
    private StorageDAO storageDAO;
    @EJB
    private ReferentialDAO referentialDAO;
    @EJB
    private PostgresDumpService postgresDumpService;
    @EJB
    private LineDAO lineDAO;

    /**
     * Store a referential. Steps:
     * Before:
     *  <ul>
     *      <li>create storage dumps directory (and parent directories) if it does not exist</li>
     *      <li>generate DUMP for production referential</li>
     *      <li>production schema = mobiiti_toto (must exist)</li>
     *      <li>storage schema = null | storage_toto</li>
     *  </ul>
     * @param validationReferential validation referential
     * @param productionReferential production referential to store
     */
    public void storeReferential(String validationReferential, String productionReferential) throws IOException, InterruptedException {
        log.info("Store referential " + productionReferential);
        String currentContext = ContextHolder.getContext();
        try {
            if (!STORAGE_DUMPS.toFile().exists() && !STORAGE_DUMPS.toFile().mkdirs()) {
                throw new RuntimeException("Could not create storage dumps directory " + STORAGE_DUMPS);
            }
            Path dumpFilepath = STORAGE_DUMPS.resolve(productionReferential);
            ContextHolder.setContext(productionReferential);
            if (!lineDAO.checkHasAnyLineInNewTransaction()) {
                // if referential has never been transferred, do not store referential
                log.info("No lines in referential " + ContextHolder.getContext() + ", abort storage");
                return;
            }
            postgresDumpService.exportSQLDumpForSchema(dumpFilepath.toAbsolutePath().toString(), productionReferential);
            ContextHolder.setContext(validationReferential);
            Storage storage = new Storage();
            storage.setStoredAt(Instant.now());
            storageDAO.create(storage);
            log.info("Stored referential " + productionReferential + " successfully");
        } finally {
            ContextHolder.setContext(currentContext);
        }
    }

    /**
     * Restore a referential. Steps:
     * <ul>
     * <li>rename {productionSchema} schema into 'tmp_{productionSchema}'</li>
     * <li>import stored production DUMP into {productionSchema}</li>
     * <li>rename {productionSchema} into {validationSchema}</li>
     * <li>rename 'tmp_{productionSchema}' into {productionSchema}</li>
     * <li>delete DUMP file</li>
     * </ul>
     * @param validationReferential validation referential to restore to
     */
    public void restoreReferential(String validationReferential) throws ServiceException {
        log.info("Restore referential " + validationReferential);
        String productionReferential =  SUPERSPACE_PREFIX + "_" + validationReferential;
        Path dumpFilepath = STORAGE_DUMPS.resolve(productionReferential);
        try {
            String tmpProductionReferential = "tmp_"  + productionReferential;
            referentialDAO.dropSchemaIfExists(tmpProductionReferential);
            referentialDAO.renameSchema(productionReferential, tmpProductionReferential);
            try {
                // DUMP import will re-create productionReferential
                postgresDumpService.importSQLDump(dumpFilepath.toString());
            } catch (IOException e) {
                log.error("Error while importing SQL DUMP from " + dumpFilepath, e);
                referentialDAO.dropSchemaIfExists(productionReferential);
                referentialDAO.renameSchema(tmpProductionReferential, productionReferential);
                throw e;
            }
            referentialDAO.dropSchemaIfExists(validationReferential);
            referentialDAO.renameSchema(productionReferential, validationReferential);
            referentialDAO.renameSchema(tmpProductionReferential, productionReferential);
            ContextHolder.setContext(validationReferential);
            Optional<Storage> latestStorage =
                    storageDAO.findByMaxStoredAtAndRestoredAtNull();
            if (latestStorage.isPresent()) {
                latestStorage.get().setRestoredAt(Instant.now());
                storageDAO.update(latestStorage.get());
            } else {
                // there is no storage record in DB (shall not happen)
                // use dump file last modification date as stored at date
                Storage storage = new Storage();
                storage.setStoredAt(new Instant().withMillis(dumpFilepath.toFile().lastModified()));
                storage.setRestoredAt(Instant.now());
                storageDAO.create(storage);
            }
        } catch (Exception e) {
            log.error("Error restoring referential " + validationReferential, e);
            throw new ServiceException(ServiceExceptionCode.INTERNAL_ERROR, "Error restoring referential " + validationReferential, e);
        }
        try {
            postgresDumpService.deleteDumpFile(dumpFilepath.toString());
        } catch (Exception e) {
            // pass - error logged by service
        }
        log.info("Restored referential " + validationReferential + " successfully");
    }

    public void deleteStorageForReferential(String validationReferential) throws ServiceException {
        log.info("Delete storage for referential " + validationReferential);
        String currentContext = ContextHolder.getContext();
        try {
            ContextHolder.setContext(validationReferential);
            Path dumpFilepath = STORAGE_DUMPS.resolve(validationReferential);
            try {
                postgresDumpService.deleteDumpFile(dumpFilepath.toString());
            } catch (Exception e) {
                // pass - error logged by service
            }
            storageDAO.truncate();
            log.info("Deleted storage for referential " + validationReferential);
        } finally {
            ContextHolder.setContext(currentContext);
        }
    }

    /**
     * Get the latest storage time for a referential.
     *
     * @param referential referential to get last storage time for
     * @return storage time for a referential
     */
    public Optional<Instant> getLatestStorageTime(String referential) throws ServiceException {
        if (referentialDAO.checkSchemaExists(referential)) {
            ContextHolder.setContext(referential);
            return storageDAO.findByMaxStoredAtAndRestoredAtNull().map(Storage::getStoredAt);
        } else {
            throw new ServiceException(ServiceExceptionCode.INVALID_REQUEST, String.format("Schema %s does not " +
                    "exist", referential));
        }
    }

    /**
     * Get the storage time of the last restoration.
     *
     * @param referential referential to get last storage time for
     * @return storage time for a referential
     */
    public Optional<Instant> getStorageTimeFromLastRestoration(String referential) throws ServiceException {
        if (referentialDAO.checkSchemaExists(referential)) {
            ContextHolder.setContext(referential);
            return storageDAO.findByMaxStoredAtAndRestoredAtNotNull().map(Storage::getStoredAt);
        } else {
            throw new ServiceException(ServiceExceptionCode.INVALID_REQUEST, String.format("Schema %s does not " +
                    "exist", referential));
        }
    }

}
