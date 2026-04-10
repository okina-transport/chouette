package mobi.chouette.exchange.stopplace;

import com.google.common.collect.Lists;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.core.CoreException;
import mobi.chouette.dao.ScheduledStopPointDAO;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.exchange.importer.updater.StopAreaUpdater;
import mobi.chouette.exchange.importer.updater.Updater;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.ChouetteAreaEnum;
import org.apache.commons.collections4.CollectionUtils;

import javax.ejb.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Singleton(name = StopAreaUpdateService.BEAN_NAME)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Log4j
public class StopAreaUpdateService {

    public static final String BEAN_NAME = "StopAreaUpdateService";
    private static final int DELETE_UNUSED_BATCH_SIZE = 1000;
    @EJB
    private StopAreaDAO stopAreaDAO;

    @EJB(beanName = StopAreaUpdater.BEAN_NAME)
    private Updater<StopArea> stopAreaUpdater;


    @EJB
    private ScheduledStopPointDAO scheduledStopPointDAO;


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void createOrUpdateStopAreas(Context context, StopAreaUpdateContext updateContext) throws CoreException {
        StopAreaUpdateTask stopAreaUpdateTask = new StopAreaUpdateTask(stopAreaDAO, stopAreaUpdater, context, updateContext);
        stopAreaUpdateTask.update();
        List<String> stopAreasToDelete = stopAreaUpdateTask.getStopAreasToDelete();
        for (String objectid : stopAreasToDelete) {
            stopAreaDAO.safeDeleteStopArea(objectid);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void deleteStopArea(String objectId) {
        StopArea stopArea = stopAreaDAO.findByObjectId(objectId);
        if (stopArea != null) {
            cascadeDeleteStopArea(stopArea);
        } else {
            log.info("Ignored delete for unknown stop area: " + objectId);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void deleteStopAreas(List<String> stopAreaList) {
        stopAreaList.forEach(this::deleteStopArea);
        stopAreaDAO.deleteEmptyStopPlaces();
    }

    /**
     * Update stop area references in seperate transaction in order to iterate over all referentials
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int updateStopAreaReferences(Map<String, Set<String>> replacementMap) {
        final AtomicInteger updatedStopPoints = new AtomicInteger();
        replacementMap.forEach((newStopAreaId, oldStopAreaIds) -> updatedStopPoints.addAndGet(scheduledStopPointDAO.replaceContainedInStopAreaReferences(oldStopAreaIds, newStopAreaId)));
        return updatedStopPoints.get();
    }


    private void cascadeDeleteStopArea(StopArea stopArea) {
        stopArea.getContainedStopAreas().forEach(this::cascadeDeleteStopArea);
        stopAreaDAO.safeDeleteStopArea(stopArea.getObjectId());
        log.info("Deleted stop area: " + stopArea.getObjectId());
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Integer delete(Set<String> quayIds) {

        Set<String> boardingPositionObjectIds = new HashSet<>(stopAreaDAO.getBoardingPositionObjectIds());

        log.debug("Total no of boarding positions: " + boardingPositionObjectIds.size());

        boardingPositionObjectIds.removeAll(quayIds);

        log.debug("No of boarding positions not in Stop Place Registry: " + boardingPositionObjectIds.size());


        List<String> inUseBoardingPositionsForReferential = scheduledStopPointDAO.getAllStopAreaObjectIds();
        boardingPositionObjectIds.removeAll(inUseBoardingPositionsForReferential);
        log.debug("Removed: " + inUseBoardingPositionsForReferential.size() + " in use boarding positions.Potentially not used boarding positions left: " + boardingPositionObjectIds.size());


        final AtomicInteger deletedStopAreasCnt = new AtomicInteger();

        if (CollectionUtils.isNotEmpty(boardingPositionObjectIds)) {
            log.info("Found " + boardingPositionObjectIds.size() + " unused boarding positions. Deleting boarding positions and commercial stops where all boarding positions are unused");
            if (boardingPositionObjectIds.size() > DELETE_UNUSED_BATCH_SIZE) {
                Lists.partition(new ArrayList<>(boardingPositionObjectIds), DELETE_UNUSED_BATCH_SIZE).forEach(batch -> deletedStopAreasCnt.addAndGet(deleteBatchOfUnusedStopAreas(batch, boardingPositionObjectIds)));
            } else {
                deletedStopAreasCnt.addAndGet(deleteBatchOfUnusedStopAreas(boardingPositionObjectIds, boardingPositionObjectIds));
            }
        }
        log.info("Finished deleting unused stop areas. Cnt: " + deletedStopAreasCnt.get());
        return deletedStopAreasCnt.get();
    }

    private int deleteBatchOfUnusedStopAreas(Collection<String> unusedBoardingPositionObjectIdBatch, Set<String> allUnusedBoardingPositionObjectIds) {
        Set<StopArea> unusedBoardingPositions = new HashSet<>(stopAreaDAO.findByObjectId(unusedBoardingPositionObjectIdBatch));

        List<StopArea> unusedStopAreas = unusedBoardingPositions.stream()
                .map(StopArea::getParent)
                .distinct()
                .filter(Objects::nonNull)
                .filter(stop -> stop.getContainedStopAreas().stream().allMatch(boardingPosition -> allUnusedBoardingPositionObjectIds.contains(boardingPosition.getObjectId())))
                .peek(stop -> log.debug("Deleting unused stop area: " + stop)).collect(Collectors.toList());

        unusedBoardingPositions.stream().peek(boardingPosition -> boardingPosition.setParent(null)).forEach(boardingPosition -> stopAreaDAO.safeDeleteStopArea(boardingPosition.getObjectId()));
        unusedStopAreas.forEach(stop -> stopAreaDAO.safeDeleteStopArea(stop.getObjectId()));

        return unusedStopAreas.size() + unusedBoardingPositions.size();
    }

    /**
     * Checks if one of the stop Areas is ued in the schema
     *
     * @param objectid
     * @return
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void isAStopAreaUsed(String objectid) {
        StopArea stopArea = stopAreaDAO.findByObjectId(objectid);
        if (stopArea != null && stopArea.getAreaType().equals(ChouetteAreaEnum.CommercialStopPoint)) {
            for (StopArea childStopArea : stopArea.getContainedStopAreas()) {
                if (stopAreaDAO.isStopAreaUsed(childStopArea.getObjectId())) {
                    throw new IllegalArgumentException("One of the stop area is still in use : " + childStopArea.getObjectId());
                }
            }
        } else {
            if (stopAreaDAO.isStopAreaUsed(objectid)) {
                throw new IllegalArgumentException("One of the stop area is still in use : " + objectid);
            }
        }

    }


}
