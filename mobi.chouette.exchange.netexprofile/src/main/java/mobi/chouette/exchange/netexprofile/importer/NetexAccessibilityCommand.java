package mobi.chouette.exchange.netexprofile.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.AccessibilityAssessmentDAO;
import mobi.chouette.dao.AccessibilityLimitationDAO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static mobi.chouette.model.util.ObjectIdTypes.ACCESSIBILITYASSESSMENT_KEY;
import static mobi.chouette.model.util.ObjectIdTypes.ACCESSIBILITYLIMITATION_KEY;

@Log4j
@Stateless(name = NetexAccessibilityCommand.COMMAND)
public class NetexAccessibilityCommand implements Command, Constant {

    @EJB
    AccessibilityAssessmentDAO accessibilityAssessmentDAO;

    @EJB
    AccessibilityLimitationDAO accessibilityLimitationDAO;

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
                    chouetteAccessibilityAssessment.setObjectId(NetexImportUtil.composeObjectIdFromNetexId(context, ACCESSIBILITYASSESSMENT_KEY, generateUniqueObjectId(ACCESSIBILITYASSESSMENT_KEY)));
                    chouetteAccessibilityAssessment.getAccessibilityLimitation().setObjectId(NetexImportUtil.composeObjectIdFromNetexId(context, ACCESSIBILITYLIMITATION_KEY, generateUniqueObjectId(ACCESSIBILITYLIMITATION_KEY)));
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

    private String generateUniqueObjectId(String type) {
        // Récupérer tous les objectIds existants en base pour les AccessibilityAssessment
        List<String> existingObjectIds = new ArrayList<>();

        if (Objects.equals(type, ACCESSIBILITYASSESSMENT_KEY)) {
            existingObjectIds = accessibilityAssessmentDAO.findAllAccessibilityAssessmentObjectIds();
        }
        else if (Objects.equals(type, ACCESSIBILITYLIMITATION_KEY)) {
            existingObjectIds = accessibilityLimitationDAO.findAllAccessibilityLimitationObjectIds();
        }

        Pattern pattern = Pattern.compile("(\\d+)$");

        // Extraire les parties numériques des objectIds
        int maxId = existingObjectIds.stream()
                .map(objectId -> {
                    Matcher matcher = pattern.matcher(objectId);
                    if (matcher.find()) {
                        return Integer.parseInt(matcher.group(1));
                    } else {
                        return 0;
                    }
                })
                .max(Integer::compare)
                .orElse(0);

        // Incrémenter l'ID et vérifier s'il est unique
        String newObjectId;
        boolean objectIdExists;
        do {
            maxId++;
            newObjectId = String.valueOf(maxId);
            objectIdExists = existingObjectIds.contains(newObjectId);
        } while (objectIdExists);  // Continuer tant que l'objectId existe

        return newObjectId;
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
