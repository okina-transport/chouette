package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.AccessibilityAssessmentDAO;
import mobi.chouette.dao.AccessibilityLimitationDAO;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.VehicleJourneyDAO;
import mobi.chouette.model.AccessibilityAssessment;
import mobi.chouette.model.AccessibilityLimitation;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import mobi.chouette.model.Route;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.VehicleJourneyAtStop;
import mobi.chouette.model.type.*;
import org.rutebanken.netex.model.LimitationStatusEnumeration;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Stateless(name = UpdateLineInfosCommand.COMMAND)
@Log4j
public class UpdateLineInfosCommand implements Command, Constant {

    @EJB
    LineDAO lineDAO;

    @EJB
    VehicleJourneyDAO vehicleJourneyDAO;

    @EJB
    AccessibilityAssessmentDAO accessibilityAssessmentDAO;

    @EJB
    AccessibilityLimitationDAO accessibilityLimitationDAO;

    public static final String COMMAND = "UpdateLineInfosCommand";

    @Override
    public boolean execute(Context context) throws Exception {
        lineDAO.findAll().forEach(line -> {
            List<VehicleJourney> vehicleJourneyList = line.getRoutes().stream()
                    .map((Route::getJourneyPatterns))
                    .flatMap(List::stream)
                    .map(JourneyPattern::getVehicleJourneys)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            long nbVehicleJourney = vehicleJourneyList.size();
            Line lineToUpdate = lineDAO.find(line.getId());

            manageTAD(vehicleJourneyList, lineToUpdate);
            manageBike(vehicleJourneyList, nbVehicleJourney, lineToUpdate);
            managePMR(vehicleJourneyList, nbVehicleJourney, lineToUpdate);

            lineDAO.update(lineToUpdate);
        });
        lineDAO.flush(); // to prevent SQL error outside method
        vehicleJourneyDAO.flush();

         return SUCCESS;
    }

    private void managePMR(List<VehicleJourney> vehicleJourneyList, long nbVehicleJourney, Line lineToUpdate) {
        // PMR
        if (lineToUpdate.getAccessibilityAssessment() != null &&
                lineToUpdate.getAccessibilityAssessment().getLimitations() != null &&
                lineToUpdate.getAccessibilityAssessment().getLimitations().getWheelchairAccess().equals(LimitationStatusEnumeration.TRUE)) {

            vehicleJourneyList.forEach(vehicleJourney -> {
                vehicleJourney.setMobilityRestrictedSuitability(true);
                vehicleJourneyDAO.update(vehicleJourney);

            });
        } else {
            AccessibilityAssessment accessibilityAssessment;
            AccessibilityLimitation accessibilityLimitation;

            if (lineToUpdate.getAccessibilityAssessment() == null) {
                accessibilityAssessment = new AccessibilityAssessment();
                accessibilityAssessment.setLine(lineToUpdate);
            } else {
                accessibilityAssessment = lineToUpdate.getAccessibilityAssessment();
            }

            accessibilityLimitation = accessibilityAssessment.getLimitations() == null ? new AccessibilityLimitation() : accessibilityAssessment.getLimitations();

            int nbVehicleJourneyWithPMR = (int) vehicleJourneyList.stream()
                    .filter(vehicleJourney -> vehicleJourney.getMobilityRestrictedSuitability() != null && vehicleJourney.getMobilityRestrictedSuitability()).count();
            if (nbVehicleJourneyWithPMR == 0) {
                accessibilityLimitation.setWheelchairAccess(LimitationStatusEnumeration.FALSE);
            } else if (nbVehicleJourneyWithPMR == nbVehicleJourney) {
                accessibilityLimitation.setWheelchairAccess(LimitationStatusEnumeration.TRUE);
            } else {
                accessibilityLimitation.setWheelchairAccess(LimitationStatusEnumeration.PARTIAL);
            }

            accessibilityLimitationDAO.create(accessibilityLimitation);
            accessibilityAssessment.setLimitations(accessibilityLimitation);
            accessibilityAssessmentDAO.create(accessibilityAssessment);
            lineToUpdate.setAccessibilityAssessment(accessibilityAssessment);
        }
    }

    private void manageBike(List<VehicleJourney> vehicleJourneyList, long nbVehicleJourney, Line lineToUpdate) {
        // VELOS
        int nbVehicleJourneyWithBike = (int) vehicleJourneyList.stream()
                .filter(vehicleJourney -> vehicleJourney.getBikesAllowed() != null && vehicleJourney.getBikesAllowed()).count();
        if (nbVehicleJourneyWithBike == 0) {
            lineToUpdate.setBike(BikeAccessEnum.NO_ACCESS);
        } else if (nbVehicleJourneyWithBike == nbVehicleJourney) {
            lineToUpdate.setBike(BikeAccessEnum.FULL_ACCESS);
        } else {
            lineToUpdate.setBike(BikeAccessEnum.PARTIAL_ACCESS);
        }
    }

    private void manageTAD(List<VehicleJourney> vehicleJourneyList, Line lineToUpdate) {
        // TAD
        List<VehicleJourneyAtStop> vehicleJourneyAtStopList = vehicleJourneyList.stream()
                .map(VehicleJourney::getVehicleJourneyAtStops)
                .flatMap(List::stream)
                .collect(Collectors.toList());
        int nbVehicleJourneyAtStop = vehicleJourneyAtStopList.size();
        int nbVJASWithTAD = (int) vehicleJourneyAtStopList.stream().filter(this::isVJASTAD).count();
        if (nbVJASWithTAD == 0) {
            lineToUpdate.setTad(TadEnum.NO_TAD);
            lineToUpdate.setFlexibleService(false);
        } else if (nbVJASWithTAD == nbVehicleJourneyAtStop) {
            lineToUpdate.setTad(TadEnum.FULL_TAD);
            lineToUpdate.setFlexibleService(true);
        } else {
            lineToUpdate.setTad(TadEnum.PARTIAL_TAD);
            lineToUpdate.setFlexibleService(false);
        }
    }

    private boolean isVJASTAD(VehicleJourneyAtStop vehicleJourneyAtStop) {

        if (vehicleJourneyAtStop.getBoardingAlightingPossibility() == null){
            return false;
        }

        List<DropOffTypeEnum> tadDropOffTypes = Arrays.asList(DropOffTypeEnum.AgencyCall, DropOffTypeEnum.DriverCall);
        List<PickUpTypeEnum> tadPickUpTypes = Arrays.asList(PickUpTypeEnum.AgencyCall, PickUpTypeEnum.DriverCall);


        return tadDropOffTypes.contains(vehicleJourneyAtStop.getBoardingAlightingPossibility().getDropOffType()) ||
                tadPickUpTypes.contains(vehicleJourneyAtStop.getBoardingAlightingPossibility().getPickUpType());

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
        CommandFactory.factories.put(UpdateLineInfosCommand.class.getName(), new UpdateLineInfosCommand.DefaultCommandFactory());
    }

}
