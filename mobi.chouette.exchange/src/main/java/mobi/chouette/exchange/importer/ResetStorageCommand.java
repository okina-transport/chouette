package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.StorageDAO;
import mobi.chouette.model.Storage;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.Optional;

@Log4j
@Stateless(name = ResetStorageCommand.COMMAND)
public class ResetStorageCommand implements Command {

    public static final String COMMAND = "ResetStorageCommand";

    static {
        CommandFactory.factories.put(ResetStorageCommand.class.getName(), new ResetStorageCommand.DefaultCommandFactory());
    }

    @EJB
    StorageDAO storageDAO;

    @Override
    public boolean execute(Context context) throws Exception {
        Optional<Storage> storage = storageDAO.findByMaxStoredAt();
        if (storage.isPresent()) {
            storage.get().setRestoredAt(null);
            storageDAO.update(storage.get());
        }
        return true;
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = null;
            try {
                String name = "java:app/mobi.chouette.exchange/" + COMMAND;
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
