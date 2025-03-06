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
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.exchange.gtfs.exporter.producer.GtfsFareRuleProducer;
import mobi.chouette.exchange.gtfs.model.exporter.GtfsExporter;
import mobi.chouette.model.FareRule;

import javax.naming.InitialContext;
import java.io.IOException;

/**
 *
 */
@Log4j
public class GtfsFareRuleProducerCommand implements Command, Constant {
    public static final String COMMAND = "GtfsFareRuleProducerCommand";

    static {
        CommandFactory.factories.put(GtfsFareRuleProducerCommand.class.getName(), new DefaultCommandFactory());
    }

    @Override
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;
        Monitor monitor = MonitorFactory.start(COMMAND);
        GtfsExporter exporter = (GtfsExporter) context.get(GTFS_EXPORTER);
        GtfsFareRuleProducer fareProducer = new GtfsFareRuleProducer(exporter);

        try {
            ExportableData collection = (ExportableData) context.get(EXPORTABLE_DATA);

            if (!collection.getFareRules().isEmpty()) {
                for (FareRule fareRule : collection.getFareRules()) {
                    fareProducer.save(fareRule);
                }
            }
            context.put(EXPORTABLE_DATA, collection);
            result = SUCCESS;
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
            Command result = new GtfsFareRuleProducerCommand();
            return result;
        }
    }

}
