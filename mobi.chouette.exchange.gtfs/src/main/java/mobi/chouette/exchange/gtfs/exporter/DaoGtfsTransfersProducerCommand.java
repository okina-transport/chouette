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
import mobi.chouette.dao.TransfersDAO;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.model.Transfers;

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

@Stateless(name = DaoGtfsTransfersProducerCommand.COMMAND)

public class DaoGtfsTransfersProducerCommand implements Command, Constant {
    public static final String COMMAND = "DaoGtfsTransfersProducerCommand";

    static {
        CommandFactory.factories.put(DaoGtfsTransfersProducerCommand.class.getName(),
                new DefaultCommandFactory());
    }

    @EJB
    private TransfersDAO fareRuleDAO;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;
        Monitor monitor = MonitorFactory.start(COMMAND);

        try {
            ExportableData collection = (ExportableData) context.get(EXPORTABLE_DATA);
            List<Transfers> rules = fareRuleDAO.findAll();
            Object configuration = context.get(CONFIGURATION);

            if (!(configuration instanceof GtfsExportParameters)) {
                // fatal wrong parameters
                log.error("invalid parameters for gtfs export " + configuration.getClass().getName());
                return ERROR;
            }

            if (collection != null) {
                collection.getTransfers().addAll(rules);
            } else {
                collection = new ExportableData();
                collection.getTransfers().addAll(rules);
            }

            context.put(EXPORTABLE_DATA, collection);

            InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
            Command export = CommandFactory.create(initialContext, GtfsTransfersProducerCommand.class.getName());

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
