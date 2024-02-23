package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.AccessibilityAssessmentDAO;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.HashMap;

@Stateless(name = AccessibilityCommand.COMMAND)
@Log4j
public class AccessibilityCommand implements Command, Constant {
    @EJB
    AccessibilityAssessmentDAO accessibilityAssessmentDAO;


    public static final String COMMAND = "AccessibilityCommand";

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean execute(Context context) throws Exception {
        HashMap<String, String> mapIdsVjAa = (HashMap<String, String>) context.get(MAP_IDS_VJ_AA);
        HashMap<String, String> mapIdsAaAl = (HashMap<String, String>) context.get(MAP_IDS_AA_AL);

        log.info("Update vj, accessibility assessment and accessibility limitation started");
        accessibilityAssessmentDAO.updateVjAccessiblityAssessment(mapIdsVjAa);
        accessibilityAssessmentDAO.updateAccessiblityAssessmentAccessibilityLimitation(mapIdsAaAl);
        log.info("Update vj, accessibility assessment and accessibility limitation finished");

        return SUCCESS;
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


    static {
        CommandFactory.factories.put(AccessibilityCommand.class.getName(), new AccessibilityCommand.DefaultCommandFactory());
    }

}
