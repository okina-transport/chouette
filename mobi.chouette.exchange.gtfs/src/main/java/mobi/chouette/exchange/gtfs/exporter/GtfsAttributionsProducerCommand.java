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
import mobi.chouette.exchange.gtfs.exporter.producer.GtfsAttributionProducer;
import mobi.chouette.exchange.gtfs.exporter.producer.GtfsFeedInfoProducer;
import mobi.chouette.exchange.gtfs.model.GtfsAttribution;
import mobi.chouette.exchange.gtfs.model.GtfsFeedInfo;
import mobi.chouette.exchange.gtfs.model.exporter.GtfsExporter;
import mobi.chouette.model.Attribution;
import mobi.chouette.model.FeedInfo;

import javax.naming.InitialContext;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
@Log4j
public class GtfsAttributionsProducerCommand implements Command, Constant {
    public static final String COMMAND = "GtfsAttributionsProducerCommand";

    @Override
    public boolean execute(Context context) throws Exception {
        Monitor monitor = MonitorFactory.start(COMMAND);

        try {

            GtfsExporter exporter = (GtfsExporter) context.get(GTFS_EXPORTER);
            GtfsAttributionProducer attributionProducer = new GtfsAttributionProducer(exporter);
            ExportableData collection = (ExportableData) context.get(EXPORTABLE_DATA);

            GtfsExportParameters configuration = (GtfsExportParameters) context.get(CONFIGURATION);
            String schemaPrefix = configuration.getObjectIdPrefix();
            boolean keepOriginal = configuration.isKeepOriginalId();

            Integer gtfsAttributionsId = 1;
            for (Attribution neptuneAttribution : collection.getAttributions()) {
                attributionProducer.save(neptuneAttribution, gtfsAttributionsId++, schemaPrefix, keepOriginal);
            }

            return SUCCESS;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
        }

        return ERROR;
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = new GtfsAttributionsProducerCommand();
            return result;
        }
    }

    static {
        CommandFactory.factories.put(GtfsAttributionsProducerCommand.class.getName(), new DefaultCommandFactory());
    }

}
