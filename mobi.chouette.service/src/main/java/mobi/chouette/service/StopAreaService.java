package mobi.chouette.service;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.core.CoreException;
import mobi.chouette.dao.ProviderDAO;
import mobi.chouette.exchange.report.ActionReport;
import mobi.chouette.exchange.stopplace.PublicationDeliveryQuayParser;
import mobi.chouette.exchange.stopplace.PublicationDeliveryStopPlaceParser;
import mobi.chouette.exchange.stopplace.StopAreaUpdateContext;
import mobi.chouette.exchange.stopplace.StopAreaUpdateService;
import mobi.chouette.exchange.validation.report.ValidationReport;
import mobi.chouette.model.Provider;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.util.Referential;
import mobi.chouette.persistence.hibernate.ContextHolder;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton(name = StopAreaService.BEAN_NAME)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Log4j
public class StopAreaService {

	public static final String BEAN_NAME = "StopAreaService";

	@EJB(beanName = StopAreaUpdateService.BEAN_NAME)
	StopAreaUpdateService stopAreaUpdateService;

	@EJB
	private ProviderDAO providerDAO;

	private ExecutorService executor;

	public StopAreaService() {
		final AtomicInteger counter = new AtomicInteger(0);
		ThreadFactory threadFactory = (r) -> {
			Thread t = new Thread(r);
			t.setName("stop-area-reference-updater-thread-" + (counter.incrementAndGet()));
			t.setPriority(Thread.MIN_PRIORITY);
			return t;
		};
		int processors = Runtime.getRuntime().availableProcessors();
		executor = Executors.newFixedThreadPool(processors, threadFactory);
		;

	}

	public void createOrUpdateStopPlacesFromNetexStopPlaces(InputStream inputStream) throws CoreException {
		PublicationDeliveryStopPlaceParser parser = new PublicationDeliveryStopPlaceParser(inputStream);

		StopAreaUpdateContext updateContext = parser.getUpdateContext();

		int changedStopCnt = updateContext.getChangedStopCount();
		if (changedStopCnt > 0) {
			log.info("Updating " + changedStopCnt + " stop areas");
			updateSchemas(updateContext);
			log.info("Updated " + changedStopCnt + " stop areas");
		} else {
			log.debug("Received update without any stop areas. Doing nothing");
		}
	}




	/**
	 * Applies stop place modifications on all schemas impacted by modifications
	 *
	 * @param updateContext
	 * 		Context with all modifications to apply
	 */
	private void updateSchemas(StopAreaUpdateContext updateContext ) throws CoreException {

		for (String impactedSchema : updateContext.getImpactedSchemas()) {
			//deleted schemas are ignored
			if (!isSchemaExisting(impactedSchema))
				continue;

			log.info("Starting update on schema: " + impactedSchema);
			Context chouetteDbContext = createContext();
			ContextHolder.clear();
			ContextHolder.setContext(impactedSchema);
			resetSavedStatusToFalse(updateContext, impactedSchema);
			stopAreaUpdateService.createOrUpdateStopAreas(chouetteDbContext, updateContext);
			log.info("Update completed on schema: " + impactedSchema);
		}

		log.info("Update references started");
		updateStopAreaReferencesPerReferential(updateContext);
		log.info("Update references completed");
	}

	/**
	 * Tells if the schema exists or not
	 * @param schemaName
	 * @return
	 * 	True : schema is still existing
	 * 	False : schema no longer exists
	 */
	private boolean isSchemaExisting(String schemaName){
		ContextHolder.setContext("admin");
		List<Provider> providers = providerDAO.getAllProviders();
		return providers.stream()
				         .anyMatch(provider->provider.getCode().equals(schemaName));

	}


	/**
	 * Reset all "saved" status to false.
	 * (when all modifications are applied to schema 1, all stopAreas are marked as "saved".
	 * We need to reset this status in order to allow modifications for other schemas)
	 * @param updateContext
	 * 		Context with all modifications that need to be applied.
	 * @param currentSchemaName
	 * 		Current schema on which modifications will be applied
	 */
	private void resetSavedStatusToFalse(StopAreaUpdateContext updateContext, String currentSchemaName){

		for (StopArea activeStopArea : updateContext.getActiveStopAreas()) {
			activeStopArea.setSaved(false);
			setOriginalStopId(activeStopArea, updateContext, currentSchemaName);
			activeStopArea.getContainedStopAreas().forEach(containedStopArea -> {
				setOriginalStopId(containedStopArea, updateContext, currentSchemaName);
				containedStopArea.setSaved(false);
			});
		}
	}

	/**
	 * Read the NetexId of the stop area and recover the associated importedId to set it as "originalStopId" in the StopArea.
	 *
	 * 2 cases :
	 * 	- a selected-id has been defined for this schema : this selected-id is set as original_stop_id
	 * 	- no selected-id is defined for this schema : an imported-id is set as original_stop_id
	 *
	 * @param stopAreaToFeed
	 * @param updateContext
	 * @param currentSchemaName
	 */
	private void setOriginalStopId(StopArea stopAreaToFeed, StopAreaUpdateContext updateContext, String currentSchemaName){
		String stopAreaObjectId = stopAreaToFeed.getObjectId();

		if (updateContext.getSelectedIdsByNetexId().containsKey(stopAreaObjectId)){

			List<String> selectedIds = updateContext.getSelectedIdsByNetexId().get(stopAreaObjectId);
			boolean hasBeenUpdated = setOriginalStopIdFromList(stopAreaToFeed, selectedIds, currentSchemaName);
			if (hasBeenUpdated){
				//a selected-id has been found for this point. No need to look in imported-id list
				return;
			}
		}

		List<String> importedIds = updateContext.getImportedIdsByNetexId().get(stopAreaObjectId);

		if (importedIds == null || importedIds.isEmpty())
			return;

		setOriginalStopIdFromList(stopAreaToFeed, importedIds, currentSchemaName);
	}


	/***
	 *  Read a list of ids and search for an id starting with "schemaName:".
	 *  If found, set original_stop_id with the found id
	 * @param stopAreaToUpdate
	 * 	The stop area for wich the original_stop_id must be set
	 * @param idList
	 * 	a list of id containing schemanames (e.g : ORG1:StopPlace:xxx, ORG2:Quay:yyyy)
	 * @param schemaName
	 * the schemato search
	 * @return
	 * 		true : an update of original_stop_id has been made
	 * 		false : no update has been made
	 */
	private boolean setOriginalStopIdFromList(StopArea stopAreaToUpdate, List<String> idList, String schemaName){


		Optional<String> foundId = idList.stream()
					.filter(id -> id.contains(":") && id.split(":").length == 3 && id.split(":")[0].toLowerCase().equals(schemaName))
					.map(id -> id.split(":")[2])
					.findFirst();

		if (!foundId.isPresent()){
			return false;
		}

		stopAreaToUpdate.setOriginalStopId(foundId.get());
		return true;
	}

	private void updateStopAreaReferencesPerReferential(StopAreaUpdateContext updateContext) {
		int updatedStopPointCnt = 0;


		Map<String, Set<String>> replacementMap = updateContext.getMergedQuays();
		if (!replacementMap.isEmpty()) {

			List<String> schemaList = new ArrayList<>(updateContext.getImpactedSchemas());

			for (String referential : schemaList) {

				if (!isSchemaExisting(referential))
					continue;

				ContextHolder.setContext(referential);
				log.info("Updating stop area references for stop points for referential " + referential);
				int updatedCnt = stopAreaUpdateService.updateStopAreaReferences(replacementMap);
				log.info("Updated stop area references for " + updatedCnt + " stop points for referential " + referential);
			}
		}

		log.info("Updated stop area references for " + updatedStopPointCnt + " stop points");
	}

	public void deleteStopArea(String objectId) {
		ContextHolder.clear();
		stopAreaUpdateService.deleteStopArea(objectId);
	}

	public void deleteUnusedStopAreas() {
		ContextHolder.clear();
		int deletedStopPointCnt = 0;
		List<Future<Integer>> futures = new ArrayList();

		ContextHolder.setContext("admin");
		List<String> schemaList = providerDAO.getAllWorkingSchemas();
		for (String referential : schemaList) {
			//need to use a future, in order to force hibernate to reset the transaction to switch between 2 tenants. do not remove
			StopAreaDeleteTask deleteTask = new StopAreaDeleteTask(referential);
			futures.add(executor.submit(deleteTask));

		}
		try {
			for (Future<Integer> future : futures) {
				deletedStopPointCnt += future.get();
			}
		} catch (ExecutionException e) {
			throw new RuntimeException("Exception while updating StopArea references: " + e.getMessage(), e);
		} catch (InterruptedException ie) {
			throw new RuntimeException("Interrupted while waiting for StopArea reference update", ie);
		}

		log.info("Deleted " + deletedStopPointCnt + " stop points");
	}


	/**
	 * Delete stop_areas quays that are listed in the netex file in parameter.
	 * After the delete are completed, delete empty commercial stop areas (with no child)
	 *
	 * @param inputStream
	 * @throws CoreException
	 */
	public void deleteStopAreas(InputStream inputStream) throws CoreException {

		PublicationDeliveryQuayParser parser = new PublicationDeliveryQuayParser(inputStream);
		StopAreaUpdateContext updateContext = parser.getUpdateContext();

		ContextHolder.setContext("admin");
		List<String> schemaList = providerDAO.getAllWorkingSchemas();
		List<String> stopAreasToDelete = new ArrayList<>(updateContext.getInactiveStopAreaIds());

		for (String referential : schemaList) {
			log.info("Starting stopArea usage check on schema: " + referential);
			ContextHolder.clear();
			ContextHolder.setContext(referential);
			if (stopAreaUpdateService.isAStopAreaUsed(stopAreasToDelete)){
				throw new IllegalArgumentException("One of the stop area is still in use");
			}
			log.info("Check completed. StopAreas not in use on schema : " + referential);
		}


		for (String referential : schemaList) {
			log.info("Starting stopArea delete on schema: " + referential);
			ContextHolder.clear();
			ContextHolder.setContext(referential);
			stopAreaUpdateService.deleteStopAreas(stopAreasToDelete);
			log.info("Delete completed on schema: " + referential);
		}
	}


	private Context createContext() {
		Context context = new Context();
		Referential referential = new Referential();
		context.put(Constant.REFERENTIAL, referential);
		context.put(Constant.CACHE, referential);
		context.put(Constant.REPORT, new ActionReport());
		context.put(Constant.VALIDATION_REPORT, new ValidationReport());
		return context;
	}

	public void setStopAreaUpdateService(StopAreaUpdateService stopAreaUpdateService) {
		this.stopAreaUpdateService = stopAreaUpdateService;
	}

	class StopAreaDeleteTask implements Callable<Integer> {

		private final String referential;

		public StopAreaDeleteTask(String referential) {
			this.referential = referential;
		}

		@Override
		public Integer call() throws Exception {
			ContextHolder.setContext(referential);
			if(log.isDebugEnabled()) {
				log.info("Starting delete for referential:" + referential);
			}
			Integer count = stopAreaUpdateService.delete();
			if(log.isDebugEnabled()) {
				log.info("Delete completed for referential:" + referential);
			}
			return count;
		}
	}

}
