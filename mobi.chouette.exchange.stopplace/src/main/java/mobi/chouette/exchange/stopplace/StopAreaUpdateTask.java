package mobi.chouette.exchange.stopplace;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.core.CoreException;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.exchange.importer.updater.Updater;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.Utils;
import mobi.chouette.persistence.hibernate.ContextHolder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j
public class StopAreaUpdateTask {

	private StopAreaDAO stopAreaDAO;

	private Updater<StopArea> stopAreaUpdater;

	private Context context;

	private StopAreaUpdateContext updateContext;


	private Map<String, StopArea> removedContainedStopAreas = new HashMap<>();

	public StopAreaUpdateTask(StopAreaDAO stopAreaDAO, Updater<StopArea> stopAreaUpdater, Context context, StopAreaUpdateContext updateContext) {
		this.stopAreaDAO = stopAreaDAO;
		this.stopAreaUpdater = stopAreaUpdater;
		this.context = context;
		this.updateContext = updateContext;
	}

	public void update() throws CoreException {

		for (String inactiveStopAreaId : updateContext.getInactiveStopAreaIds()) {
			try{
				removeStopArea(inactiveStopAreaId);
			}catch(Exception e){
				log.error("Error while deleting stop area:" + inactiveStopAreaId, e);
			}
		}

		String currentSchema = ContextHolder.getContext();
		List<String> impactedStopAreasIds = updateContext.getImpactedStopAreasBySchema().get(currentSchema);

		if (impactedStopAreasIds != null) {
			//Filtering to apply modification only on points used by the current schema
			Set<StopArea> impactedStopAreas = new HashSet<>();

			for(StopArea stopArea : updateContext.getActiveStopAreas()){
				impactedStopAreas.add(SerializationUtils.clone(stopArea));
			}

			impactedStopAreas = impactedStopAreas.stream()
					.filter(stopArea -> isStopAreaImpacted(stopArea, impactedStopAreasIds))
					.collect(Collectors.toSet());

			for(StopArea stopArea : impactedStopAreas){
				stopArea.getContainedStopAreas()
						.removeIf(containedStopArea -> StringUtils.isEmpty(containedStopArea.getOriginalStopId()) ||
								!impactedStopAreasIds.contains(containedStopArea.getObjectId()));
			}

			impactedStopAreas.stream()
					.map(this::createCopy)
					.forEach(this::createOrUpdate);
		}

		Map<String, Set<String>> mergedQuaysMap = new HashMap<>(updateContext.getMergedQuays());
		for (String mergeQuay : mergedQuaysMap.keySet()) {

			// On vérifie que le quai dans lequel les autres ont été fusionnés existe en base
			StopArea quay = stopAreaDAO.findByObjectId(mergeQuay);
			if (quay == null) {
				continue;
			}

			// On récupère la liste des quais fusionnés (ne devant plus exister)
			List<String> mergedQuays = new ArrayList<>(mergedQuaysMap.get(mergeQuay));
			if (!mergedQuays.isEmpty()) {
				for (String quayToMergeId : mergedQuays) {
					List<String> quayIds = Arrays.asList(quayToMergeId.split(":"));
					//On s'assure de travailler avec les quais du schéma en cours
					if (isQuayInCurrentSchema(quayIds, currentSchema)) {
						List<StopArea> quaysToMerge = stopAreaDAO.findByOriginalId(quayIds.get(quayIds.size() - 1));
						if (quaysToMerge != null && !quaysToMerge.isEmpty()) {
							for(StopArea stopArea : quaysToMerge) {
								if (!stopArea.getObjectId().equals(quay.getObjectId())) {
									stopAreaDAO.mergeStopArea30m(stopArea.getId(), quay.getId());
								}
							}
						}
					// ou des quais partagés
					} else if (isQuayIsGlobal(quayIds)) {
						StopArea stopArea = stopAreaDAO.findByObjectId(quayToMergeId);
						if (stopArea != null && !stopArea.getId().equals(quay.getId())) {
							stopAreaDAO.mergeStopArea30m(stopArea.getId(), quay.getId());
						}
					}
				}
			}
		}

		// De-activate auto delete of unused values.
		// Generates exceptions because deleted points are still in use. Need to investigate
		//removedContainedStopAreas.values().forEach(containedStopArea -> removeContainedStopArea(containedStopArea));

		stopAreaDAO.flush();
		stopAreaDAO.clear();


	}

	private boolean isQuayInCurrentSchema(List<String> quayIds, String currentSchema) {
		return quayIds.stream().anyMatch(quayId -> quayId.equals(currentSchema));
	}

	private boolean isQuayIsGlobal(List<String> quayIds) {
		return quayIds.stream().anyMatch(quayId -> quayId.equals("MOBIITI"));
	}

	/**
	 * Tells if a stop area's modifications should be applied on the specified schema
	 * @param stopArea
	 * 			stop area that must be checked
	 * @param impactedIds
	 * 			List of impactedIds for the current schema
	 * @return
	 * 	True : stop area's modifications should be applied on the schema
	 * 	False : this stop area is not concerned by this schema
	 */
	private boolean isStopAreaImpacted(StopArea stopArea,  List<String> impactedIds){

		if (impactedIds.isEmpty())
			return false;

		if (impactedIds.contains(stopArea.getObjectId()))
			return true;

		return stopArea.getContainedStopAreas().stream()
				.anyMatch(containedStopArea -> isStopAreaImpacted(containedStopArea, impactedIds));

	}




	private void createOrUpdate(StopArea stopArea) {
		try {
			StopArea existing = stopAreaDAO.findByObjectId(stopArea.getObjectId());
			if (existing == null) {
				createNewStopArea(stopArea);

			} else {
				updateExistingStopArea(stopArea, existing);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to update stop place: "+ stopArea.getObjectId()+e.getMessage(), e);
		}

	}

	private void updateExistingStopArea(StopArea stopArea, StopArea existing) throws Exception {
		log.debug("Updating existing StopArea : " + stopArea);

		Map<String, StopArea> existingContainedStopAreas = existing.getContainedStopAreas().stream().collect(Collectors.toMap(StopArea::getObjectId,
				Function.identity()));


		existing.getContainedStopAreas().clear();
		for (StopArea containedStopArea : new ArrayList<>(stopArea.getContainedStopAreas())) {

			StopArea existingContainedStopAreaForSameParent = existingContainedStopAreas.remove(containedStopArea.getObjectId());

			// Remove from removed collection to avoid moved contained stop area being deleted
			removedContainedStopAreas.remove(containedStopArea.getObjectId());

			if (existingContainedStopAreaForSameParent == null) {
				createOrMoveStopArea(existing, containedStopArea);

			} else {
				log.debug("Updating existing contained StopArea : " + stopArea);
				updateExistingStopArea(containedStopArea, existingContainedStopAreaForSameParent);
			}
		}

		for (StopArea obsoleteStopArea : existingContainedStopAreas.values()) {
			if (Objects.equals(obsoleteStopArea.objectIdPrefix(), stopArea.objectIdPrefix())) {
				registerRemovedContainedStopArea(obsoleteStopArea);
			} else {
				log.info("Keep unknown StopArea : " + obsoleteStopArea.getObjectId() + " as id belongs to different code space than parent stop: " + stopArea.getObjectId());
			}

		}
		stopAreaUpdater.update(context, existing, stopArea);
		stopAreaDAO.update(existing);
	}

	private void createOrMoveStopArea(StopArea parent, StopArea stopArea) throws Exception {
		// Contained stop area with ID does not already exist for parent StopArea, but may exist for another. If so, move the existing contained stop area to new parent.
		StopArea existing = stopAreaDAO.findByObjectId(stopArea.getObjectId());
		if (existing != null) {
			log.info("Moving contained StopArea: " + stopArea + " to new parent : " + parent);
			existing.setDetached(true);
			updateExistingStopArea(stopArea, existing);
		} else {
			log.debug("Creating new contained StopArea: " + stopArea);
			stopArea.setParent(parent);
			createNewStopArea(stopArea);
		}
	}

	private void createNewStopArea(StopArea stopArea) throws Exception {
		log.debug("Creating new StopArea : " + stopArea);

		List<StopArea> containedStopAreas = new ArrayList<>();

		// Contained stops for new stop place might already exists and/or may be listed for removal because it has been removed from its previous owner
		if (!CollectionUtils.isEmpty(stopArea.getContainedStopAreas())) {
			containedStopAreas.addAll(stopArea.getContainedStopAreas());
			stopArea.getContainedStopAreas().clear();
		}
		stopAreaDAO.create(stopArea);

		for (StopArea containedStopArea : containedStopAreas) {
			// Remove from removed collection to avoid moved contained stop area being deleted
			removedContainedStopAreas.remove(containedStopArea.getObjectId());

			createOrMoveStopArea(stopArea, containedStopArea);
		}
	}

	private StopArea createCopy(StopArea stopArea){
		StopArea newStopArea = new StopArea();
		Utils.copyStopArea(stopArea,newStopArea);
		newStopArea.setObjectId(stopArea.getObjectId());
		if (!stopArea.getContainedStopAreas().isEmpty()){

			ArrayList<StopArea> oldContainedList = new ArrayList<>(stopArea.getContainedStopAreas());
			List<StopArea> containedCopies = oldContainedList.stream()
					.map(this::createCopy)
					.collect(Collectors.toList());
			newStopArea.setContainedStopAreas(containedCopies);
		}
		return newStopArea;
	}

	private void removeStopArea(String objectId) {
		log.info("Deleting obsolete StopArea : " + objectId);

		StopArea stopArea = stopAreaDAO.findByObjectId(objectId);
		if (stopArea != null) {
			new ArrayList<>(stopArea.getContainedStopAreas()).forEach(containedStopArea -> registerRemovedContainedStopArea(containedStopArea));
			stopAreaDAO.delete(stopArea);
		} else {
			log.warn("Could not remove unknown stopArea: " + objectId);
		}

	}

	private void removeContainedStopArea(StopArea containedStopArea) {
		log.info("Deleting obsolete contained StopArea: " + containedStopArea.getObjectId());
		stopAreaDAO.delete(containedStopArea);
		if (containedStopArea.getContainedStopAreas() != null) {
			containedStopArea.getContainedStopAreas().forEach(grandChild -> removeContainedStopArea(grandChild));
		}
	}


	private void registerRemovedContainedStopArea(StopArea obsoleteStopArea) {
		StopArea oldParent = obsoleteStopArea.getParent();
		obsoleteStopArea.setParent(null);
		stopAreaDAO.update(oldParent);
		removedContainedStopAreas.put(obsoleteStopArea.getObjectId(), obsoleteStopArea);
	}

}
