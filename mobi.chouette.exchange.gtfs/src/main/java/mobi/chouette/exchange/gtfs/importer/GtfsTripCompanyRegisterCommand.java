package mobi.chouette.exchange.gtfs.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.TripCompanyDAO;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.exchange.gtfs.model.importer.GtfsImporter;
import mobi.chouette.exchange.importer.updater.TripCompanyUpdater;
import mobi.chouette.exchange.importer.updater.Updater;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.model.TripCompany;
import mobi.chouette.model.util.Referential;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Log4j
@Stateless(name = GtfsTripCompanyRegisterCommand.COMMAND)
public class GtfsTripCompanyRegisterCommand implements Command, Constant {

    public static final String COMMAND = "GtfsTripCompanyRegisterCommand";

    static {
        CommandFactory.factories.put(GtfsTripCompanyRegisterCommand.class.getName(), new DefaultCommandFactory());
    }

    @EJB
    private TripCompanyDAO tripCompanyDAO;

    @EJB(beanName = TripCompanyUpdater.BEAN_NAME)
    private Updater<TripCompany> tripCompanyUpdater;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean execute(Context context) throws Exception {
        boolean result = SUCCESS;
        Monitor monitor = MonitorFactory.start(COMMAND);

        try {
            GtfsImporter importer = (GtfsImporter) context.get(PARSER);
            if (!importer.hasTripCompanyImporter()) {
                // companies.txt not present this run - transparent processing: leave existing
                // trip companies untouched
                return SUCCESS;
            }

            Referential referential = (Referential) context.get(REFERENTIAL);

            Map<String, TripCompany> staleByObjectId = new HashMap<>();
            for (TripCompany tripCompany : tripCompanyDAO.findAll()) {
                staleByObjectId.put(tripCompany.getObjectId(), tripCompany);
            }

            for (TripCompany newValue : referential.getSharedTripCompanies().values()) {
                staleByObjectId.remove(newValue.getObjectId());
                TripCompany existing = tripCompanyDAO.findByObjectId(newValue.getObjectId());
                if (existing == null) {
                    newValue.setDetached(false);
                    tripCompanyDAO.create(newValue);
                } else {
                    tripCompanyUpdater.update(context, existing, newValue);
                    tripCompanyDAO.update(existing);
                }
                tripCompanyDAO.flush();
            }

            for (TripCompany stale : staleByObjectId.values()) {
                tripCompanyDAO.delete(stale);
            }
            if (!staleByObjectId.isEmpty()) {
                tripCompanyDAO.flush();
            }
        } catch (Exception e) {
            log.error("unable to register trip companies : " + e.getMessage(), e);
            ActionReporter reporter = ActionReporter.Factory.getInstance();
            reporter.setActionError(context, ActionReporter.ERROR_CODE.INTERNAL_ERROR, e.getMessage());
        } finally {
            log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
        }

        return result;
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = null;
            try {
                String name = "java:app/mobi.chouette.exchange.gtfs/" + COMMAND;
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
