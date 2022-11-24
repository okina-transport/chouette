package mobi.chouette.exchange.gtfs.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.AttributionDAO;
import mobi.chouette.model.*;
import mobi.chouette.model.util.Referential;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;

@Stateless(name = GenerateAttributionsCommand.COMMAND)
@Log4j
public class GenerateAttributionsCommand implements Command, Constant {

    public static final String COMMAND = "GenerateAttributionsCommand";

    @EJB
    private AttributionDAO attributionDAO;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean execute(Context context) throws Exception {
        Monitor monitor = MonitorFactory.start(COMMAND);
        log.info("Generating attributions for all lines and vehicle journeys");

        try {
            createAttributions(context);
        } catch (Exception e) {
            log.warn("Attribution generation failed with exception : " + e.getMessage(), e);
        } finally {
            log.info(Color.YELLOW + monitor.stop() + Color.NORMAL);
        }
        return SUCCESS;
    }

    private void createAttributions(Context context) {
        attributionDAO.deleteAll();

        Referential referential = (Referential) context.get(REFERENTIAL);
        for (Company c : referential.getCompanies().values()) {
            for (Line l : c.getLines()) {
                Attribution lineAttribution = new Attribution();
                lineAttribution.setLine(l);
                lineAttribution.setOrganisationName(c.getRegistrationNumber().replaceAll("o+$", ""));
                lineAttribution.setIsProducer(true);
                lineAttribution.setIsOperator(true);
                attributionDAO.insertAttribution(lineAttribution);

                for (Route r : l.getRoutes()) {
                    for (JourneyPattern jp : r.getJourneyPatterns()) {
                        for (VehicleJourney vj : jp.getVehicleJourneys()) {
                            Attribution vehicleJourneyAttribution = new Attribution();
                            vehicleJourneyAttribution.setVehicleJourney(vj);
                            vehicleJourneyAttribution.setOrganisationName(c.getRegistrationNumber().replaceAll("o+$", ""));
                            vehicleJourneyAttribution.setIsProducer(true);
                            vehicleJourneyAttribution.setIsOperator(true);
                            attributionDAO.insertAttribution(vehicleJourneyAttribution);
                        }
                    }
                }
            }
        }
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = null;
            try {
                String name = "java:app/mobi.chouette.exchange.gtfs/" + COMMAND;
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
        CommandFactory.factories.put(GenerateAttributionsCommand.class.getName(), new GenerateAttributionsCommand.DefaultCommandFactory());
    }
}
