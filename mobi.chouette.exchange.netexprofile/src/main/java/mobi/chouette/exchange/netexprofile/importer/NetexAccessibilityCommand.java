package mobi.chouette.exchange.netexprofile.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.AccessibilityAssessmentDAO;
import mobi.chouette.dao.VehicleJourneyDAO;
import mobi.chouette.exchange.netexprofile.importer.util.NetexImportUtil;
import org.rutebanken.netex.model.AccessibilityAssessment;
import mobi.chouette.model.VehicleJourney;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j
@Stateless(name = NetexAccessibilityCommand.COMMAND)
public class NetexAccessibilityCommand implements Command, Constant {

    @EJB
    AccessibilityAssessmentDAO accessibilityAssessmentDAO;

    @EJB
    VehicleJourneyDAO vehicleJourneyDAO;

    public static final String COMMAND = "NetexAccessibilityCommand";

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean execute(Context context) throws Exception {
        LocalDateTime start = LocalDateTime.now();

        Map<AccessibilityAssessment, List<VehicleJourney>> accessibilityMap =
                (Map<AccessibilityAssessment, List<VehicleJourney>>) context.get(NETEX_ACCESSIBILITY_MAP);

        if (accessibilityMap == null || accessibilityMap.isEmpty()) {
            log.info("No accessibility assessments found to process.");
            return SUCCESS;
        }

        for (Map.Entry<AccessibilityAssessment, List<VehicleJourney>> entry : accessibilityMap.entrySet()) {
            AccessibilityAssessment accessibilityAssessment = entry.getKey();
            if (accessibilityAssessment != null) {
                mobi.chouette.model.AccessibilityAssessment chouetteAccessibilityAssessment = NetexImportUtil.convertToChouetteAccessibilityAssessment(accessibilityAssessment, context);
                List<VehicleJourney> vehicleJourneys = entry.getValue();
                mobi.chouette.model.AccessibilityAssessment existingAccessibility = accessibilityAssessmentDAO.findByAttributes(chouetteAccessibilityAssessment);

                if (existingAccessibility == null) {
                    accessibilityAssessmentDAO.create(chouetteAccessibilityAssessment);
                    accessibilityAssessmentDAO.flush();
                    existingAccessibility = chouetteAccessibilityAssessment;
                }

                updateVehicleJourneysWithAccessibility(existingAccessibility.getId(), vehicleJourneys);
            }
        }

        LocalDateTime end = LocalDateTime.now();
        Duration duration = Duration.between(start, end);
        log.info("NetexAccessibilityCommand completed in: "
                + duration.toMinutes() + " minutes, " + duration.getSeconds() % 60 + " seconds");

        return SUCCESS;
    }

    private void updateVehicleJourneysWithAccessibility(Long accessibilityId, List<VehicleJourney> vehicleJourneys) {
        List<String> listIds = vehicleJourneys.stream().map(vj -> String.valueOf(vj.getObjectId())).collect(Collectors.toList());
        long updatedLines = vehicleJourneyDAO.updateAccessibilityId(accessibilityId, listIds);
        log.info("Updated vehicle journeys for accessibility " + accessibilityId + ": " + updatedLines);
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = null;
            try {
                String name = "java:app/mobi.chouette.exchange.netexprofile/" + NetexAccessibilityCommand.COMMAND;
                result = (Command) context.lookup(name);
            } catch (NamingException e) {
                log.error("Failed to lookup NetexAccessibilityCommand", e);
            }
            return result;
        }

    }

    static {
        CommandFactory.factories.put(NetexAccessibilityCommand.class.getName(), new NetexAccessibilityCommand.DefaultCommandFactory());
    }
}
