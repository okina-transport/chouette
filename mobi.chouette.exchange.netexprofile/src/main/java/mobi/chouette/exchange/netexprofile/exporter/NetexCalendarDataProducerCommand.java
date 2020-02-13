package mobi.chouette.exchange.netexprofile.exporter;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.util.Referential;

import javax.naming.InitialContext;
import java.io.IOException;

@Log4j
public class NetexCalendarDataProducerCommand implements Command, Constant {

    public static final String COMMAND = "NetexCalendarDataProducerCommand";

    @Override
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;
        Monitor monitor = MonitorFactory.start(COMMAND);
        ActionReporter reporter = ActionReporter.Factory.getInstance();

        try {
            Referential referential = (Referential) context.get(REFERENTIAL);
            if (referential == null) {
                return ERROR;
            }

            ExportableNetexData exportableNetexData = (ExportableNetexData) context.get(EXPORTABLE_NETEX_DATA);
            if (exportableNetexData == null) {
                return ERROR;
            }

            NetexCalendarDataProducer producer = new NetexCalendarDataProducer();
            producer.produce(context);

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
            Command result = new NetexCalendarDataProducerCommand();
            return result;
        }
    }

    static {
        CommandFactory.factories.put(NetexCalendarDataProducerCommand.class.getName(), new DefaultCommandFactory());
    }

}
