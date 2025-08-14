package mobi.chouette.exchange.gtfs.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.AttributionDAO;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.exchange.importer.utils.MdmUtils;
import mobi.chouette.model.*;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Stateless(name = GenerateAttributionsCommand.COMMAND)
@Log4j
public class GenerateAttributionsCommand implements Command, Constant {

    public static final String COMMAND = "GenerateAttributionsCommand";

    @EJB
    private AttributionDAO attributionDAO;

    @EJB
    private LineDAO lineDAO;

    public static final boolean IS_MDM_ACTIVATED = Boolean.parseBoolean(System.getenv("IS_MDM_ACTIVATED"));

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
        ChouetteData chouetteData = (ChouetteData) context.get(CHOUETTE_DATA_TO_MDM);
        JobData jobData = (JobData) context.get(JOB_DATA);
        List<Attribution> createdAttributions = new ArrayList<>();

        List<Line> lines = lineDAO.findAll();
        for (Line l : lines) {
            Attribution lineAttribution = new Attribution();
            lineAttribution.setLine(l);
            if(l.getCompany() != null && StringUtils.isNotEmpty(l.getCompany().getName())){
                lineAttribution.setOrganisationName(l.getCompany().getName());
            }
            lineAttribution.setIsProducer(true);
            lineAttribution.setIsOperator(true);
            attributionDAO.insertAttribution(lineAttribution);
            createdAttributions.add(lineAttribution);

            for (Route r : l.getRoutes()) {
                for (JourneyPattern jp : r.getJourneyPatterns()) {
                    for (VehicleJourney vj : jp.getVehicleJourneys()) {
                        Attribution vehicleJourneyAttribution = new Attribution();
                        vehicleJourneyAttribution.setVehicleJourney(vj);
                        if(l.getCompany() != null && StringUtils.isNotEmpty(l.getCompany().getName())) {
                            vehicleJourneyAttribution.setOrganisationName(l.getCompany().getName());
                        }
                        vehicleJourneyAttribution.setIsProducer(true);
                        vehicleJourneyAttribution.setIsOperator(true);
                        createdAttributions.add(vehicleJourneyAttribution);
                        attributionDAO.insertAttribution(vehicleJourneyAttribution);
                    }
                }
            }
        }

        if (IS_MDM_ACTIVATED){
            for (Attribution createdAttribution : createdAttributions) {
                MdmUtils.fillMdmDataWithAttribution(chouetteData, jobData.getReferential(), createdAttribution);
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
