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
import mobi.chouette.dao.AttributionDAO;
import mobi.chouette.dao.FeedInfoDAO;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.exchange.gtfs.parameters.AttributionsExportModes;
import mobi.chouette.model.Attribution;
import mobi.chouette.model.FeedInfo;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 *
 */
@Log4j

@Stateless(name = DaoGtfsAttributonsProducerCommand.COMMAND)

public class DaoGtfsAttributonsProducerCommand implements Command, Constant {

    public static final String COMMAND = "DaoGtfsAttributonsProducerCommand";

    @EJB
    private AttributionDAO attributionDAO;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;
        Monitor monitor = MonitorFactory.start(COMMAND);

        try {
            ExportableData collection = (ExportableData) context.get(EXPORTABLE_DATA);
            List<Attribution> attributions = attributionDAO.findAll();
            Object configuration = context.get(CONFIGURATION);

            if (!(configuration instanceof GtfsExportParameters)) {
                // fatal wrong parameters
                log.error("invalid parameters for gtfs export " + configuration.getClass().getName());
                return ERROR;
            }

            GtfsExportParameters parameters = (GtfsExportParameters) configuration;
            if (parameters.getAttributionsExportMode().equals(AttributionsExportModes.LINES_ONLY)) {
                attributions = attributions.stream().filter(a -> a.getLine() != null).collect(Collectors.toList());
            } else if (parameters.getAttributionsExportMode().equals(AttributionsExportModes.NONE)) {
                attributions.clear();
            }

            if (collection != null) {
                collection.getAttributions().addAll(attributions);
            } else {
                collection = new ExportableData();
                collection.getAttributions().addAll(attributions);
            }

            context.put(EXPORTABLE_DATA, collection);

            InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
            Command export = CommandFactory.create(initialContext, GtfsAttributionsProducerCommand.class.getName());
            
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

    static {
        CommandFactory.factories.put(DaoGtfsAttributonsProducerCommand.class.getName(),
                new DefaultCommandFactory());
    }


}
