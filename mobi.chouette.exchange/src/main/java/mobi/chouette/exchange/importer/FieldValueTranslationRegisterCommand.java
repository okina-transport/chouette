package mobi.chouette.exchange.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.*;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ActionReporter.ERROR_CODE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_STATE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_TYPE;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.Translation;
import mobi.chouette.model.util.NamingUtil;
import mobi.chouette.model.util.Referential;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Persists GTFS translations.txt rows keyed by field_value (no record_id) directly, by objectId,
 * independently of any owner entity - they are never attached to an entity's translations list, so
 * their owner foreign key column is always left empty. Matching them against a specific entity's raw,
 * untranslated field text is done on the fly wherever they're consumed (e.g. NeTEx export), not resolved
 * or stored ahead of time.
 */
@Log4j
@Stateless(name = FieldValueTranslationRegisterCommand.COMMAND)
public class FieldValueTranslationRegisterCommand implements Command {

    public static final String COMMAND = "FieldValueTranslationRegisterCommand";

    static {
        CommandFactory.factories.put(FieldValueTranslationRegisterCommand.class.getName(), new DefaultCommandFactory());
    }

    @EJB
    private NetworkTranslationDAO networkTranslationDAO;

    @EJB
    private CompanyTranslationDAO companyTranslationDAO;

    @EJB
    private LineTranslationDAO lineTranslationDAO;

    @EJB
    private StopAreaTranslationDAO stopAreaTranslationDAO;

    @EJB
    private VehicleJourneyTranslationDAO vehicleJourneyTranslationDAO;

    @EJB
    private VehicleJourneyAtStopTranslationDAO vehicleJourneyAtStopTranslationDAO;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean execute(Context context) throws Exception {

        boolean result = SUCCESS;
        Monitor monitor = MonitorFactory.start(COMMAND);

        try {
            Referential referential = (Referential) context.get(REFERENTIAL);

            register(context, networkTranslationDAO, referential.getNetworkTranslationsByFieldValue(),
                    "select t from NetworkTranslation t where t.network is null");
            referential.getNetworkTranslationsByFieldValue().clear();
            register(context, companyTranslationDAO, referential.getCompanyTranslationsByFieldValue(),
                    "select t from CompanyTranslation t where t.company is null");
            referential.getCompanyTranslationsByFieldValue().clear();
            register(context, lineTranslationDAO, referential.getLineTranslationsByFieldValue(),
                    "select t from LineTranslation t where t.line is null");
            referential.getLineTranslationsByFieldValue().clear();
            register(context, stopAreaTranslationDAO, referential.getStopAreaTranslationsByFieldValue(),
                    "select t from StopAreaTranslation t where t.stopArea is null");
            referential.getStopAreaTranslationsByFieldValue().clear();
            register(context, vehicleJourneyTranslationDAO, referential.getVehicleJourneyTranslationsByFieldValue(),
                    "select t from VehicleJourneyTranslation t where t.vehicleJourney is null");
            referential.getVehicleJourneyTranslationsByFieldValue().clear();
            register(context, vehicleJourneyAtStopTranslationDAO, referential.getVehicleJourneyAtStopTranslationsByFieldValue(),
                    "select t from VehicleJourneyAtStopTranslation t where t.vehicleJourneyAtStop is null");
            referential.getVehicleJourneyAtStopTranslationsByFieldValue().clear();
        } finally {
            log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
        }

        return result;
    }

    private <T extends Translation> void register(Context context, GenericDAO<T> dao, List<T> newValues, String ownerIsNullQuery) throws Exception {
        Map<String, T> staleByObjectId = new HashMap<>();
        for (T translation : dao.find(ownerIsNullQuery, Collections.emptyList())) {
            staleByObjectId.put(translation.getObjectId(), translation);
        }

        for (T translation : newValues) {
            try {
                staleByObjectId.remove(translation.getObjectId());
                T existing = dao.findByObjectId(translation.getObjectId());
                if (existing == null) {
                    dao.create(translation);
                } else {
                    boolean changed = false;
                    if (!Objects.equals(existing.getFieldName(), translation.getFieldName())) {
                        existing.setFieldName(translation.getFieldName());
                        changed = true;
                    }
                    if (!Objects.equals(existing.getLanguage(), translation.getLanguage())) {
                        existing.setLanguage(translation.getLanguage());
                        changed = true;
                    }
                    if (!Objects.equals(existing.getTranslation(), translation.getTranslation())) {
                        existing.setTranslation(translation.getTranslation());
                        changed = true;
                    }
                    if (!Objects.equals(existing.getFieldValue(), translation.getFieldValue())) {
                        existing.setFieldValue(translation.getFieldValue());
                        changed = true;
                    }
                    if (changed) {
                        dao.update(existing);
                    }
                }
                dao.flush();
            } catch (Exception ex) {
                log.error(ex.getMessage());
                ActionReporter reporter = ActionReporter.Factory.getInstance();
                reporter.addObjectReport(context, translation.getObjectId(), OBJECT_TYPE.TRANSLATION, NamingUtil.getName(translation), OBJECT_STATE.ERROR, IO_TYPE.INPUT);
                if (ex.getCause() != null) {
                    Throwable e = ex.getCause();
                    while (e.getCause() != null) {
                        log.error(e.getMessage());
                        e = e.getCause();
                    }
                    if (e instanceof SQLException sqlException) {
                        e = sqlException.getNextException();
                        reporter.addErrorToObjectReport(context, translation.getObjectId(), OBJECT_TYPE.TRANSLATION, ERROR_CODE.WRITE_ERROR, e.getMessage());
                    } else {
                        reporter.addErrorToObjectReport(context, translation.getObjectId(), OBJECT_TYPE.TRANSLATION, ERROR_CODE.INTERNAL_ERROR, e.getMessage());
                    }
                } else {
                    reporter.addErrorToObjectReport(context, translation.getObjectId(), OBJECT_TYPE.TRANSLATION, ERROR_CODE.INTERNAL_ERROR, ex.getMessage());
                }
                throw ex;
            }
        }

        for (T stale : staleByObjectId.values()) {
            dao.delete(stale);
        }
        if (!staleByObjectId.isEmpty()) {
            dao.flush();
        }
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = null;
            try {
                String name = "java:app/mobi.chouette.exchange/" + COMMAND;
                result = (Command) context.lookup(name);
            } catch (NamingException e) {
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
