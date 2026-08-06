package mobi.chouette.exchange.gtfs.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.exchange.gtfs.parser.GtfsTripCompanyParser;
import mobi.chouette.exchange.importer.ParserFactory;

import javax.naming.InitialContext;
import java.io.IOException;

@Log4j
public class GtfsTripCompanyParserCommand implements Command, Constant {

    public static final String COMMAND = "GtfsTripCompanyParserCommand";

    static {
        CommandFactory.factories.put(GtfsTripCompanyParserCommand.class.getName(), new DefaultCommandFactory());
    }

    @Override
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;

        Monitor monitor = MonitorFactory.start(COMMAND);

        try {
            GtfsTripCompanyParser gtfsTripCompanyParser = (GtfsTripCompanyParser) ParserFactory.create(GtfsTripCompanyParser.class.getName());
            gtfsTripCompanyParser.parse(context);

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
            Command result = new GtfsTripCompanyParserCommand();
            return result;
        }
    }
}
