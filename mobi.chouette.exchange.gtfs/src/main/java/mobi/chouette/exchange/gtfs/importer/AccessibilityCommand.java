package mobi.chouette.exchange.gtfs.importer;

import com.google.common.collect.Lists;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.AccessibilityAssessmentDAO;
import mobi.chouette.dao.VehicleJourneyDAO;
import mobi.chouette.exchange.gtfs.model.GtfsTrip;
import mobi.chouette.model.AccessibilityAssessment;
import mobi.chouette.model.AccessibilityLimitation;
import mobi.chouette.model.type.LimitationStatusEnum;
import org.rutebanken.netex.model.LimitationStatusEnumeration;

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


@Log4j
@Stateless(name = AccessibilityCommand.COMMAND)
public class AccessibilityCommand implements Command, Constant {
    @EJB
    AccessibilityAssessmentDAO accessibilityAssessmentDAO;

    @EJB
    VehicleJourneyDAO vehicleJourneyDAO;


    public static final String COMMAND = "AccessibilityCommand";

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean execute(Context context) throws Exception {
        LocalDateTime start = LocalDateTime.now();
        GtfsImportParameters parameters = (GtfsImportParameters) context.get(CONFIGURATION);
        String prefix = parameters.getObjectIdPrefix();

        createGTFSAccessibilityAssessment(prefix);

        Map<GtfsTrip.WheelchairAccessibleType, List<String>> gtfsAccessibilityMap = (Map) context.get(GTFS_ACCESSIBILITY_MAP);

        if(!gtfsAccessibilityMap.get(GtfsTrip.WheelchairAccessibleType.Allowed).isEmpty()){
            log.info("Updating vehicleJourneys for type 1. Nb of vehicleJourneys:" + gtfsAccessibilityMap.get(GtfsTrip.WheelchairAccessibleType.Allowed).size());
            AccessibilityAssessment gtfsAccessibility1 = accessibilityAssessmentDAO.findByObjectId(prefix + ":AccessibilityAssessment:GTFS_1");
            updateIdsByBatch(gtfsAccessibility1.getId(), gtfsAccessibilityMap.get(GtfsTrip.WheelchairAccessibleType.Allowed));
        }

        if(!gtfsAccessibilityMap.get(GtfsTrip.WheelchairAccessibleType.NoAllowed).isEmpty()){
            log.info("Updating vehicleJourneys for type 2. Nb of vehicleJourneys:" + gtfsAccessibilityMap.get(GtfsTrip.WheelchairAccessibleType.NoAllowed).size());
            AccessibilityAssessment gtfsAccessibility2 = accessibilityAssessmentDAO.findByObjectId(prefix + ":AccessibilityAssessment:GTFS_2");
            updateIdsByBatch(gtfsAccessibility2.getId(), gtfsAccessibilityMap.get(GtfsTrip.WheelchairAccessibleType.NoAllowed));
        }

        AccessibilityAssessment gtfsAccessibility0 = accessibilityAssessmentDAO.findByObjectId(prefix + ":AccessibilityAssessment:GTFS_0");
        long updatedlines = vehicleJourneyDAO.updateDefaultAccessibility(gtfsAccessibility0.getId());
        log.info("updated lines for accessibility 0:" + updatedlines);

        LocalDateTime end = LocalDateTime.now();
        Duration duration = Duration.between(start, end);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        log.info("AccessibilityCommand duration:" + " - " + hours + " hours, " + minutes + " minutes, " + seconds + " seconds");
        return SUCCESS;
    }

    private void updateIdsByBatch(Long id, List<String> objectIds) {
        for (List<String> batch : Lists.partition(objectIds, 30000)) {
            long updatedlines = vehicleJourneyDAO.updateAcessibilityId(id, batch);
            log.info("Accessibility - updated vehicle journeys:" + updatedlines);
        }
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void createGTFSAccessibilityAssessment(String prefix)
    {
        AccessibilityAssessment gtfsAccessibility0 = accessibilityAssessmentDAO.findByObjectId(prefix + ":AccessibilityAssessment:GTFS_0");
        if (gtfsAccessibility0 == null){
            AccessibilityAssessment accessibilityAssessment = new AccessibilityAssessment();
            AccessibilityLimitation accessibilityLimitation = new AccessibilityLimitation();
            accessibilityAssessment.setMobilityImpairedAccess(LimitationStatusEnum.UNKNOWN);
            accessibilityLimitation.setWheelchairAccess(LimitationStatusEnumeration.UNKNOWN);
            accessibilityAssessment.setAccessibilityLimitation(accessibilityLimitation);
            accessibilityAssessment.setObjectId(prefix + ":AccessibilityAssessment:GTFS_0");
            accessibilityLimitation.setObjectId(prefix + ":AccessibilityLimitation:GTFS_0");
            accessibilityAssessmentDAO.create(accessibilityAssessment);
        }

        AccessibilityAssessment gtfsAccessibility1 = accessibilityAssessmentDAO.findByObjectId(prefix + ":AccessibilityAssessment:GTFS_1");
        if (gtfsAccessibility1 == null){
            AccessibilityAssessment accessibilityAssessment = new AccessibilityAssessment();
            AccessibilityLimitation accessibilityLimitation = new AccessibilityLimitation();
            accessibilityAssessment.setMobilityImpairedAccess(LimitationStatusEnum.TRUE);
            accessibilityLimitation.setWheelchairAccess(LimitationStatusEnumeration.TRUE);
            accessibilityAssessment.setAccessibilityLimitation(accessibilityLimitation);
            accessibilityAssessment.setObjectId(prefix + ":AccessibilityAssessment:GTFS_1");
            accessibilityLimitation.setObjectId(prefix + ":AccessibilityLimitation:GTFS_1");
            accessibilityAssessmentDAO.create(accessibilityAssessment);
        }

        AccessibilityAssessment gtfsAccessibility2 = accessibilityAssessmentDAO.findByObjectId(prefix + ":AccessibilityAssessment:GTFS_2");
        if (gtfsAccessibility2 == null){
            AccessibilityAssessment accessibilityAssessment = new AccessibilityAssessment();
            AccessibilityLimitation accessibilityLimitation = new AccessibilityLimitation();
            accessibilityAssessment.setMobilityImpairedAccess(LimitationStatusEnum.FALSE);
            accessibilityLimitation.setWheelchairAccess(LimitationStatusEnumeration.FALSE);
            accessibilityAssessment.setAccessibilityLimitation(accessibilityLimitation);
            accessibilityAssessment.setObjectId(prefix + ":AccessibilityAssessment:GTFS_2");
            accessibilityLimitation.setObjectId(prefix + ":AccessibilityLimitation:GTFS_2");
            accessibilityAssessmentDAO.create(accessibilityAssessment);
        }
        accessibilityAssessmentDAO.flush();

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
        CommandFactory.factories.put(AccessibilityCommand.class.getName(), new AccessibilityCommand.DefaultCommandFactory());
    }



}
