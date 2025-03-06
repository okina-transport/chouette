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
import mobi.chouette.exchange.gtfs.exporter.producer.GtfsFareAttributeProducer;
import mobi.chouette.exchange.gtfs.model.exporter.GtfsExporter;
import mobi.chouette.model.FareAttribute;

import javax.naming.InitialContext;
import java.io.IOException;

/**
 *
 */
@Log4j
public class GtfsFareAttributeProducerCommand implements Command, Constant {
    public static final String COMMAND = "GtfsFareAttributeProducerCommand";

    static {
        CommandFactory.factories.put(GtfsFareAttributeProducerCommand.class.getName(), new DefaultCommandFactory());
    }

    @Override
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;
        Monitor monitor = MonitorFactory.start(COMMAND);
        GtfsExporter exporter = (GtfsExporter) context.get(GTFS_EXPORTER);
        GtfsFareAttributeProducer fareProducer = new GtfsFareAttributeProducer(exporter);

        try {
            ExportableData collection = (ExportableData) context.get(EXPORTABLE_DATA);

            if (!collection.getFareAttributes().isEmpty()) {
                for (FareAttribute fareAttribute : collection.getFareAttributes()) {
                    fareProducer.save(fareAttribute);
                }
            }
            context.put(EXPORTABLE_DATA, collection);
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
            Command result = new GtfsFareAttributeProducerCommand();
            return result;
        }
    }

}
