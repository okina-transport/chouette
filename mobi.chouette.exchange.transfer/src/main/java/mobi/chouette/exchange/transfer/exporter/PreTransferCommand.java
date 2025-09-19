package mobi.chouette.exchange.transfer.exporter;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.persistence.hibernate.ContextHolder;
import mobi.chouette.service.StorageService;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;

@Log4j
@Stateless(name = PreTransferCommand.COMMAND)
public class PreTransferCommand implements Command {

    public static final String COMMAND = "PreTransferCommand";

    static {
        CommandFactory.factories.put(PreTransferCommand.class.getName(), new PreTransferCommand.DefaultCommandFactory());
    }

    @EJB
    private StorageService storageService;

    @Override
    public boolean execute(Context context) throws Exception {
        if (!"true".equalsIgnoreCase(System.getenv("ENABLE_TRANSPORTATION_PLAN_STORAGE"))) {
            log.info("ENABLE_TRANSPORTATION_PLAN_STORAGE not set to true, skip");
            return true;
        }
        TransferExportParameters parameters = (TransferExportParameters) context.get(CONFIGURATION);
        String validationSchema = ContextHolder.getContext();
        String productionSchema = parameters.getDestReferentialName();
        storageService.storeReferential(validationSchema, productionSchema);
        return true;
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = null;
            try {
                String name = "java:app/mobi.chouette.exchange.transfer/" + COMMAND;
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
