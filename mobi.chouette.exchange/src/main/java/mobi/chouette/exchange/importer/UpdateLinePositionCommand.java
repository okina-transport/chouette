package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.NetworkDAO;
import mobi.chouette.model.Line;
import mobi.chouette.model.Network;
import org.apache.commons.collections.CollectionUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

/**
 * Update position of non-deleted lines inside their network.
 * Lines are sorted by their position value if present, otherwise their published name.
 * After lines sorting, set first line position to 1, second to 2, etc ...
 */
@Log4j
@Stateless(name = UpdateLinePositionCommand.COMMAND)
public class UpdateLinePositionCommand implements Command {

    public static final String COMMAND = "UpdateLinePositionCommand";

    static {
        CommandFactory.factories.put(UpdateLinePositionCommand.class.getName(), new DefaultCommandFactory());
    }

    @EJB
    LineDAO lineDAO;
    @EJB
    NetworkDAO networkDAO;

    public UpdateLinePositionCommand() {
    }

    public UpdateLinePositionCommand(LineDAO lineDAO, NetworkDAO networkDAO) {
        this.lineDAO = lineDAO;
        this.networkDAO = networkDAO;
    }

    @Override
    public boolean execute(Context context) throws Exception {
        List<Network> networks = networkDAO.findAll();
        if (CollectionUtils.isEmpty(networks)) {
            log.warn("No networks found");
            return true;
        }
        boolean flush = false;
        for (Network network : networks) {
            List<Line> lines = lineDAO.findByNetworkIdNotDeleted(network.getId());
            if (CollectionUtils.isEmpty(lines)) {
                log.info("No activated lines found for network " + network.getName());
                continue;
            }
            // sort lines with a position by their position ASC
            // lines without position are then sorted by their published name ASC
            // eg:"
            // [
            //  Line{pos=null, name = 'B'},
            //  Line{pos=null, name = 'A'}',
            //  Line{pos=3, name = 'C'},
            //  Line{pos=null, name = null},
            //  Line{pos=1, name = 'D'},
            //  ]
            // would be sorted like this
            // [
            //  Line{pos=1, name = 'D'},
            //  Line{pos=3, name = 'C'},
            //  Line{pos=null, name = 'A'}',
            //  Line{pos=null, name = 'B'},
            //  Line{pos=null, name = null},
            // ]
            lines.sort(Comparator.comparing(Line::getPosition, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(Line::getPublishedName, Comparator.nullsLast(Comparator.naturalOrder())));
            Integer position = 1;
            for (Line line : lines) {
                if (!position.equals(line.getPosition())) {
                    log.info(String.format("Update line %d position (old: %d, new: %d)", line.getId(), line.getPosition(), position));
                    line.setPosition(position);
                    lineDAO.update(line);
                    flush = true;
                }
                position++;
            }
        }
        if (flush) {
            lineDAO.flush();
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
