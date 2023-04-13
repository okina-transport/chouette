package mobi.chouette.exchange.neptune.parser;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.XPPUtil;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.neptune.importer.NeptuneImportParameters;
import mobi.chouette.model.*;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.type.Utils;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.xmlpull.v1.XmlPullParser;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Log4j
public class ChouettePTNetworkParser implements Parser, Constant {
	private static final String CHILD_TAG = "ChouettePTNetwork";
	public static final String STOP_POINT_CONTEXT = "StopPoint";
	private static final String CONTAINED_ID = "containedIn";
	public static final String AREA_CENTROID_CONTEXT = "AreaCentroid";
	public static final String CONTAINED_IN = "containedIn";
	public static final String STOP_AREA_CONTEXT = "StopArea";
	public static final String ACCESS_POINT_CONTEXT = "AccessPoint";
	public static final String ACCESS_LINK_CONTEXT = "AccessLink";
	public static final String ITL_CONTEXT = "ITL";
	public static final String END_OF_LINK_ID = "endOfLinkId";
	public static final String START_OF_LINK_ID = "startOfLinkId";

	public static final String CENTROID_OF_AREA = "centroidOfArea";
	public static final String CONTAINS2 = "contains";
	protected static final String OBJECT_IDS = "encontered_ids";


	@Override
	public void parse(Context context) throws Exception {


		XmlPullParser xpp = (XmlPullParser) context.get(PARSER);

		XPPUtil.nextStartTag(xpp, CHILD_TAG);

		xpp.require(XmlPullParser.START_TAG, null, CHILD_TAG);
		context.put(COLUMN_NUMBER, xpp.getColumnNumber());
		context.put(LINE_NUMBER, xpp.getLineNumber());

		while (xpp.nextTag() == XmlPullParser.START_TAG) {
			if (xpp.getName().equals("PTNetwork")) {
				Parser parser = ParserFactory.create(PTNetworkParser.class
						.getName());
				parser.parse(context);
			} else if (xpp.getName().equals("GroupOfLine")) {
				Parser parser = ParserFactory.create(GroupOfLineParser.class
						.getName());
				parser.parse(context);
			} else if (xpp.getName().equals("Company")) {
				Parser parser = ParserFactory.create(CompanyParser.class
						.getName());
				parser.parse(context);
			} else if (xpp.getName().equals("ChouetteArea")) {
				Parser parser = ParserFactory.create(ChouetteAreaParser.class
						.getName());
				parser.parse(context);
			} else if (xpp.getName().equals("ConnectionLink")) {
				Parser parser = ParserFactory.create(ConnectionLinkParser.class
						.getName());
				parser.parse(context);
			} else if (xpp.getName().equals("Timetable")) {
				Parser parser = ParserFactory.create(TimetableParser.class
						.getName());
				parser.parse(context);
			} else if (xpp.getName().equals("TimeSlot")) {
				Parser parser = ParserFactory.create(TimeSlotParser.class
						.getName());
				parser.parse(context);
			} else if (xpp.getName().equals("ChouetteLineDescription")) {
				Parser parser = ParserFactory
						.create(ChouetteLineDescriptionParser.class.getName());
				parser.parse(context);
			} else if (xpp.getName().equals("Facility")) {
				// TODO [DSU] Facility
				XPPUtil.skipSubTree(log, xpp);
				
			} else if (xpp.getName().equals("AccessPoint")) {
				Parser parser = ParserFactory.create(AccessPointParser.class
						.getName());
				parser.parse(context);
			} else if (xpp.getName().equals("AccessLink")) {
				Parser parser = ParserFactory.create(AccessLinkParser.class
						.getName());
				parser.parse(context);
			} else {
				XPPUtil.skipSubTree(log, xpp);
			}
		}
		mapFileIdsToReferentialIds(context);
		mapIdsInValidationContext(context);
		handleCommercialPoints(context);

	}

	/**
	 * If user selected "ignoreCommercialPoints" => all commercial points are removed
	 * If user did not select "ignoreCommercialPoints" => nothing is done
	 * @param context
	 */
	private void handleCommercialPoints(Context context){
		NeptuneImportParameters parameters = (NeptuneImportParameters) context.get(CONFIGURATION);

		// "ignoreCommercialPoints" option is not selected. Nothing is done
		if (!parameters.ignoreCommercialPoints)
			return;

		Referential referential = (Referential) context.get(REFERENTIAL);

		List<String> removedCommercialPoints = referential.getSharedStopAreas().values().stream()
				.filter(stopArea -> ChouetteAreaEnum.CommercialStopPoint.equals(stopArea.getAreaType()))
				.map(StopArea::getObjectId)
				.collect(Collectors.toList());


		removeStopAreasFromMap(removedCommercialPoints,referential.getSharedStopAreas());
		removeStopAreasFromMap(removedCommercialPoints,referential.getStopAreas());

		//Rebuild commercial points
		buildCommercialPoints(context);

		Context validationContext = (Context) context.get(VALIDATION_CONTEXT);
		Context stopAreaContext = (Context) validationContext.get(STOP_AREA_CONTEXT);

		removedCommercialPoints.forEach(stopAreaToRemove -> stopAreaContext.remove(stopAreaToRemove));

		removeAreaCentroidValidationData(context);

	}

	/**
	 * If user selected "ignoreCommercialPoints" => all validation data about areaCentroid must be ignored
	 * @param context
	 */
	private void removeAreaCentroidValidationData(Context context) {
		Context validationContext = (Context) context.get(VALIDATION_CONTEXT);
		Context areaCentroidContext = (Context) validationContext.get("AreaCentroid");
		validationContext.remove("AreaCentroid");

		Context stopAreaContext = (Context) validationContext.get("StopArea");
		for (Object value : stopAreaContext.values()) {
			Context objectContext = (Context) value;
			objectContext.remove("centroidOfArea");
		}

	}

	private void buildCommercialPoints(Context context){

		Map<String,String> areaCentroidMap =  (Map<String,String>) context.get(AREA_CENTROID_MAP);
		Referential referential = (Referential) context.get(REFERENTIAL);

		List<StopArea> boardingPositions = referential.getSharedStopAreas().values().stream()
																		.filter(stopArea -> ChouetteAreaEnum.BoardingPosition.equals(stopArea.getAreaType()))
																		.collect(Collectors.toList());

		for (StopArea boardingPosition : boardingPositions) {
			StopArea commercialPoint = initArea(referential, boardingPosition, null);
			boardingPosition.setParent(commercialPoint);
			areaCentroidMap.put(boardingPosition.getOriginalStopId(),commercialPoint.getObjectId());
		}
	}



	/**
	 * Commercial stop point initialization with first boarding position values
	 *
	 * @param area
	 *            commercial stop point
	 * @param stop
	 *            boarding position
	 * @param referential
	 * @param objectId
	 */
	private StopArea initArea(Referential referential, StopArea stop, String objectId) {
		LocalDateTime now = LocalDateTime.now();
		String[] token = stop.getObjectId().split(":");
		if (objectId == null)
			objectId = token[0] + ":StopPlace:COM_" + token[2];
		StopArea area = ObjectFactory.getStopArea(referential, objectId);
		area.setName(stop.getName());
		area.setObjectId(objectId);
		area.setObjectVersion(stop.getObjectVersion());
		area.setCreationTime(now);
		area.setAreaType(ChouetteAreaEnum.CommercialStopPoint);
		return area;
	}

	private void removeStopAreasFromMap(List<String> stopAreasToRemove, Map<String, StopArea> stopAreaMap){
		stopAreasToRemove.forEach(stopAreaMap::remove);

		stopAreaMap.values().stream()
				.forEach(stopArea -> {
					if (stopArea.getParent() != null && stopAreasToRemove.contains(stopArea.getParent().getObjectId()))
						stopArea.setParent(null);
				});
	}

	private String buildTridentId(String referentialName, StopArea tmpStopArea){
		String type = ChouetteAreaEnum.BoardingPosition.equals(tmpStopArea.getAreaType()) ? "Quay" : "StopPlace";
		return referentialName + ":" + type + ":" + tmpStopArea.getOriginalStopId();
	}



	private void mapFileIdsToReferentialIds(Context context){

		Referential referential = (Referential) context.get(REFERENTIAL);
		NeptuneImportParameters parameters = (NeptuneImportParameters) context.get(CONFIGURATION);
		Map<String,String> fileToReferentialStopIdMap =  (Map<String,String>) context.get(FILE_TO_REFERENTIAL_STOP_ID_MAP);
		String referentialName = parameters.getReferentialName();

		List<StopArea> oldStopArea = referential.getSharedStopAreas().values().stream()
																			   .filter(stopArea -> !stopArea.getObjectId().startsWith(MOBIITI_PREFIX) &&
																					   				!stopArea.getObjectId().startsWith(referentialName))
																				.collect(Collectors.toList());
		//replace all stopAreas with olfd IDs by stopAreas with new Ids at root level
		for (StopArea stopArea : oldStopArea){
			String newId = buildTridentId(referentialName,stopArea);
			StopArea newStopArea = ObjectFactory.getStopArea(referential, newId);
			Utils.copyStopArea(stopArea,newStopArea);
			fileToReferentialStopIdMap.put(stopArea.getObjectId(),newId);
		}


		List<StopArea> newStopAreas = referential.getSharedStopAreas().values().stream()
																			.filter(stopArea ->stopArea.getObjectId().startsWith(referentialName))
																			.collect(Collectors.toList());

		//replace all parents in new stopAreas
		for (StopArea stopArea : newStopAreas){
			StopArea parentArea = stopArea.getParent();
			if (parentArea == null)
				continue;

			String parentId = parentArea.getObjectId();
			String newParentId = fileToReferentialStopIdMap.get(parentId);
			StopArea newParentArea = referential.getSharedStopAreas().get(newParentId);
			stopArea.setParent(newParentArea);
		}

		newStopAreas = referential.getSharedStopAreas().values().stream()
												.filter(stopArea ->stopArea.getObjectId().startsWith(referentialName))
												.collect(Collectors.toList());

		newStopAreas.forEach(stopArea->mapIdsInContainedInStopAreas(context,stopArea));


		oldStopArea.forEach(oldId->{
			referential.getSharedStopAreas().remove(oldId.getObjectId());
			referential.getStopAreas().remove(oldId.getObjectId());
		});

		List<ScheduledStopPoint> oldScheduledStopPoint = new ArrayList(referential.getScheduledStopPoints().values());
		for (ScheduledStopPoint scheduleStopPoint: oldScheduledStopPoint){
			String oldStopAreaId = scheduleStopPoint.getContainedInStopAreaRef().getObjectId();
			String newStopAreaId = fileToReferentialStopIdMap.get(oldStopAreaId);
			StopArea newStopArea = referential.getSharedStopAreas().get(newStopAreaId);
			scheduleStopPoint.setContainedInStopAreaRef(new SimpleObjectReference(newStopArea));
		}

		List<ConnectionLink> connectionLinksList = referential.getSharedConnectionLinks().values().stream().collect(Collectors.toList());

		for (ConnectionLink connectionLink: connectionLinksList){
			String oldStartId = connectionLink.getStartOfLink().getObjectId();
			String newStartStopAreaId = fileToReferentialStopIdMap.get(oldStartId);

			if (newStartStopAreaId == null) continue;

			StopArea newStartStopArea = referential.getSharedStopAreas().get(newStartStopAreaId);
			connectionLink.setStartOfLink(newStartStopArea);

			String oldEndId = connectionLink.getEndOfLink().getObjectId();
			String newEndStopAreaId = fileToReferentialStopIdMap.get(oldEndId);
			if (newEndStopAreaId == null) continue;
			StopArea newEndStopArea = referential.getSharedStopAreas().get(newEndStopAreaId);
			connectionLink.setEndOfLink(newEndStopArea);
		}

		List<AccessLink> accessLinksList = referential.getSharedAccessLinks().values().stream().collect(Collectors.toList());

		for (AccessLink accessLink: accessLinksList){


			String stopAreaId = accessLink.getStopArea().getObjectId();
			String newStopAreaId = fileToReferentialStopIdMap.get(stopAreaId);

			if (newStopAreaId == null) continue;

			StopArea newStartStopArea = referential.getSharedStopAreas().get(newStopAreaId);
			accessLink.setStopArea(newStartStopArea);

		}


		List<AccessPoint> accessPointList = referential.getSharedAccessPoints().values().stream().collect(Collectors.toList());
		for (AccessPoint accessPoint: accessPointList){
			StopArea containedInArea = accessPoint.getContainedIn();
			if (containedInArea == null)
				continue;

			String containedInId = containedInArea.getObjectId();
			String newStopAreaId = fileToReferentialStopIdMap.get(containedInId);

			if (newStopAreaId == null) continue;
			StopArea newContainedArea = referential.getSharedStopAreas().get(newStopAreaId);
			accessPoint.setContainedIn(newContainedArea);
		}
	}

	private void mapIdsInContainedInStopAreas(Context context, StopArea stopArea){
		List<StopArea> childrenToDelete = new ArrayList<>();
		Map<String,String> fileToReferentialStopIdMap =  (Map<String,String>) context.get(FILE_TO_REFERENTIAL_STOP_ID_MAP);
		Referential referential = (Referential) context.get(REFERENTIAL);

		if (fileToReferentialStopIdMap == null)
			return;

		List<StopArea> containedInStopAreas = new ArrayList<>(stopArea.getContainedStopAreas());
		if (containedInStopAreas.size() == 0)
			return;

		for (StopArea child : containedInStopAreas){

			if (child == null || child.getObjectId() == null || !fileToReferentialStopIdMap.containsKey(child.getObjectId()))
				continue;

			childrenToDelete.add(child);
			String newChildId = fileToReferentialStopIdMap.get(child.getObjectId());
			StopArea newChild = referential.getSharedStopAreas().get(newChildId);
			if (!stopArea.getContainedStopAreas().contains(newChild))
				stopArea.getContainedStopAreas().add(newChild);
		}

		childrenToDelete.forEach(child->stopArea.getContainedStopAreas().remove(child));

	}

	private void mapIdsInValidationContext(Context context){
		Context validationContext = (Context) context.get(VALIDATION_CONTEXT);
		Map<String,String> fileToReferentialStopIdMap =  (Map<String,String>) context.get(FILE_TO_REFERENTIAL_STOP_ID_MAP);

		Context stopPointContext = (Context) validationContext.get(STOP_POINT_CONTEXT);
		updateContainedInContext(stopPointContext,fileToReferentialStopIdMap);

		Context areaCentroidContext = (Context) validationContext.get(AREA_CENTROID_CONTEXT);
		updateContainedInContext(areaCentroidContext,fileToReferentialStopIdMap);

		Context accessPointContext = (Context) validationContext.get(ACCESS_POINT_CONTEXT);
		updateContainedInContext(accessPointContext,fileToReferentialStopIdMap);

		Context ITLContext = (Context) validationContext.get(ITL_CONTEXT);
		mapContextKeys(ITLContext,fileToReferentialStopIdMap);

		Context stopAreaContext = (Context) validationContext.get(STOP_AREA_CONTEXT);
		mapContextKeys(stopAreaContext,fileToReferentialStopIdMap);
		updatePropertiesInContext(stopAreaContext,fileToReferentialStopIdMap);

		Context accessLinkContext = (Context) validationContext.get(ACCESS_LINK_CONTEXT);
		updatePropertiesInContext(accessLinkContext,fileToReferentialStopIdMap);
	}

	private void updateContainedInContext(Context context, Map<String,String> fileToReferentialStopIdMap ){

		if(context == null)
			return;

		for (Object objectContext : context.values()){
			Context placeContext = (Context) objectContext;
			String containedIn = (String) placeContext.get(CONTAINED_IN);
			if (containedIn == null)
				continue;

			if (fileToReferentialStopIdMap.containsKey(containedIn)){
				placeContext.put(CONTAINED_IN,fileToReferentialStopIdMap.get(containedIn));
			}
		}
	}


	
	private void mapContextKeys(Context context, Map<String,String> fileToReferentialStopIdMap){
		
		if (context == null || fileToReferentialStopIdMap == null)
			return;

		List<String> oldObjectsToDelete = new ArrayList<>();
		List<String> rawIdsFromfile = fileToReferentialStopIdMap.keySet().stream()
																		 .collect(Collectors.toList());

		List<Map.Entry<String, Object>> oldObjects = generateOldObjectList(context,rawIdsFromfile);

		oldObjects.forEach(entry -> {
									oldObjectsToDelete.add(entry.getKey());
									String newId = fileToReferentialStopIdMap.get(entry.getKey());
									context.put(newId,entry.getValue());
									});

		oldObjectsToDelete.forEach(context::remove);
		
	}
	

	private List<Map.Entry<String, Object>> generateOldObjectList(Context context, List<String> rawIdsFromfile){
		return context.entrySet().stream()
				.filter(entry -> rawIdsFromfile.contains(entry.getKey()))
				.collect(Collectors.toList());
	}


	private void updatePropertiesInContext(Context context,Map<String,String> fileToReferentialStopIdMap){

		if (context == null)
			return;

		for (Map.Entry<String, Object> contextOjectEntry : context.entrySet()) {
			Context objectContext = (Context) contextOjectEntry.getValue();
			List<String> oldContains = (List<String>) objectContext.get(CONTAINS2);
			if (oldContains != null)
				objectContext.put(CONTAINS2,generateMappedIdsList(oldContains,fileToReferentialStopIdMap));

			String oldCentroid = (String) objectContext.get(CENTROID_OF_AREA);

			if (oldCentroid != null && fileToReferentialStopIdMap.containsKey(oldCentroid))
				objectContext.put(CENTROID_OF_AREA,fileToReferentialStopIdMap.get(oldCentroid));

			String startLinkId = (String) objectContext.get(START_OF_LINK_ID);
			if (startLinkId != null && fileToReferentialStopIdMap.containsKey(startLinkId))
				objectContext.put(START_OF_LINK_ID,fileToReferentialStopIdMap.get(startLinkId));

			String endLinkId = (String) objectContext.get(END_OF_LINK_ID);
			if (endLinkId != null && fileToReferentialStopIdMap.containsKey(endLinkId))
				objectContext.put(END_OF_LINK_ID,fileToReferentialStopIdMap.get(endLinkId));

		}


	}

	private List<String> generateMappedIdsList(List<String> oldList, Map<String,String> fileToReferentialStopIdMap){
		if (fileToReferentialStopIdMap == null || oldList == null || oldList.size() == 0)
			return new ArrayList<>();

		return oldList.stream()
				.map(originalString -> fileToReferentialStopIdMap.containsKey(originalString) ? fileToReferentialStopIdMap.get(originalString) : originalString)
				.collect(Collectors.toList());
	}



	private List<String> convertContainsToNewIds(Map<String,String> fileToReferentialStopIdMap, List<String> inputList){
		List<String> convertedList = new ArrayList<>();
		for (String inputId : inputList){
			convertedList.add(fileToReferentialStopIdMap.containsKey(inputId) ? fileToReferentialStopIdMap.get(inputId) : inputId);
		}
		return convertedList;
	}

	protected static Context getObjectContext(Context context, String localContextName, String objectId) {
		Context validationContext = (Context) context.get(VALIDATION_CONTEXT);
		if (validationContext == null) {
			validationContext = new Context();
			context.put(VALIDATION_CONTEXT, validationContext);
			validationContext.put(OBJECT_IDS, new HashSet<String>());
		}

		Set<String> objectIds = (Set<String>) validationContext.get(OBJECT_IDS);
		objectIds.add(objectId);

		Context localContext = (Context) validationContext.get(localContextName);
		if (localContext == null) {
			localContext = new Context();
			validationContext.put(localContextName, localContext);
		}
		Context objectContext = (Context) localContext.get(objectId);
		if (objectContext == null) {
			objectContext = new Context();
			localContext.put(objectId, objectContext);
		}
		return objectContext;

	}



	static {
		ParserFactory.register(ChouettePTNetworkParser.class.getName(),
				new ParserFactory() {
					private ChouettePTNetworkParser instance = new ChouettePTNetworkParser();

					@Override
					protected Parser create() {
						return instance;
					}
				});
	}
}
