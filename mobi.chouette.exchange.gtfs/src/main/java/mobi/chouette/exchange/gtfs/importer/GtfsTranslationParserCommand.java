package mobi.chouette.exchange.gtfs.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.exchange.gtfs.model.importer.GtfsImporter;
import mobi.chouette.exchange.gtfs.parser.GtfsTranslationParser;
import mobi.chouette.exchange.importer.ParserFactory;

import javax.naming.InitialContext;
import java.io.IOException;

@Log4j
public class GtfsTranslationParserCommand implements Command, Constant {

    public static final String COMMAND = "GtfsTranslationParserCommand";

    static {
        CommandFactory.factories.put(GtfsTranslationParserCommand.class.getName(), new DefaultCommandFactory());
    }

    @Override
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;

        Monitor monitor = MonitorFactory.start(COMMAND);

        try {
            GtfsImporter importer = (GtfsImporter) context.get(PARSER);

            if (importer.hasTranslationImporter()) {
                GtfsTranslationParser gtfsTranslationParser = (GtfsTranslationParser) ParserFactory.create(GtfsTranslationParser.class.getName());
                gtfsTranslationParser.parse(context);
            }

            result = SUCCESS;
        } catch (Exception e) {
            log.error("error : ", e);
            throw e;
        } finally {
            log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
        }
        return result;
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = new GtfsTranslationParserCommand();
            return result;
        }
    }
}
