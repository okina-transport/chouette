/**
 * Projet CHOUETTE
 * <p>
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 */

package mobi.chouette.exchange.gtfs.exporter;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.TripExtensionDAO;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.model.TripExtension;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.List;

/**
 *
 */
@Log4j

@Stateless(name = DaoGtfsTripExtensionProducerCommand.COMMAND)

public class DaoGtfsTripExtensionProducerCommand implements Command, Constant {
    public static final String COMMAND = "DaoGtfsTripExtensionProducerCommand";

    static {
        CommandFactory.factories.put(DaoGtfsTripExtensionProducerCommand.class.getName(),
                new DefaultCommandFactory());
    }

    @EJB
    private TripExtensionDAO tripExtensionDAO;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;
        Monitor monitor = MonitorFactory.start(COMMAND);

        try {
            ExportableData collection = (ExportableData) context.get(EXPORTABLE_DATA);
            List<TripExtension> tripExtensions = tripExtensionDAO.findAll();
            Object configuration = context.get(CONFIGURATION);

            if (!(configuration instanceof GtfsExportParameters)) {
                // fatal wrong parameters
                log.error("invalid parameters for gtfs export " + configuration.getClass().getName());
                return ERROR;
            }

            if (collection != null) {
                collection.getTripExtensions().addAll(tripExtensions);
            } else {
                collection = new ExportableData();
                collection.getTripExtensions().addAll(tripExtensions);
            }

            context.put(EXPORTABLE_DATA, collection);

            InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
            Command export = CommandFactory.create(initialContext, GtfsTripExtensionProducerCommand.class.getName());

            result = export.execute(context);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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
                String name = "java:app/mobi.chouette.exchange.gtfs/"
                        + COMMAND;
                result = (Command) context.lookup(name);
            } catch (NamingException e) {
                // try another way on test context
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
