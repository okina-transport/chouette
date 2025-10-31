package mobi.chouette.exchange.netexprofile.parser;

import javax.xml.bind.JAXBElement;

import mobi.chouette.exchange.netexprofile.importer.NetexprofileImportParameters;
import mobi.chouette.exchange.netexprofile.importer.util.NetexImportUtil;
import mobi.chouette.model.*;
import mobi.chouette.model.ScheduledStopPoint;
import org.rutebanken.netex.model.*;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;import java.util.ArrayList;import java.util.List;

@Log4j
public class StopAssignmentParser extends NetexParser implements Parser, Constant {

	private static final String AUTO_CREATED_QUAY_SUFFIX = "automaticaly-created-missing-quay";

	@Override
	public void parse(Context context) throws Exception {
		StopAssignmentsInFrame_RelStructure assignmentStruct = (StopAssignmentsInFrame_RelStructure) context.get(NETEX_LINE_DATA_CONTEXT);

		if (assignmentStruct != null) {
			Referential referential = (Referential) context.get(REFERENTIAL);

			for (JAXBElement<? extends StopAssignment_VersionStructure> stopAssignmentElement : assignmentStruct.getStopAssignment()) {
				PassengerStopAssignment stopAssignment = (PassengerStopAssignment) stopAssignmentElement.getValue();
				JAXBElement<? extends ScheduledStopPointRefStructure> scheduledStopPointRef = stopAssignment.getScheduledStopPointRef();

				// TODO à revoir pour changement de profil
//				ScheduledStopPointRefStructure scheduledStopPointRef = stopAssignment.getScheduledStopPointRef();
				NetexprofileImportParameters parameters = (NetexprofileImportParameters) context.get(CONFIGURATION);

				if (stopAssignment.getQuayRef() != null){
					QuayRefStructure quayRef = stopAssignment.getQuayRef().getValue();
					String generatedId = NetexImportUtil.composeObjectId("Quay",parameters.getObjectIdPrefix(),quayRef.getRef());
					mobi.chouette.model.StopArea quay = ObjectFactory.getStopArea(referential, generatedId);
					if(quay.getAreaType() == null) {
						quay.setAreaType(ChouetteAreaEnum.BoardingPosition);
					}
					String scheduledStopPointId = NetexImportUtil.composeObjectIdFromNetexId(context,"ScheduledStopPoint",scheduledStopPointRef.getValue().getRef());
					ScheduledStopPoint scheduledStopPoint = ObjectFactory.getScheduledStopPoint(referential, scheduledStopPointId);
					scheduledStopPoint.setContainedInStopAreaRef(new SimpleObjectReference<>(quay));
				} else if (stopAssignment.getStopPlaceRef() != null){
					StopPlaceRefStructure stopPlaceRef = stopAssignment.getStopPlaceRef().getValue();
					String stopPlaceId = NetexImportUtil.composeObjectId("StopPlace",parameters.getObjectIdPrefix(),stopPlaceRef.getRef());
					mobi.chouette.model.StopArea stopPlace = ObjectFactory.getStopArea(referential, stopPlaceId);

					String virtualQuayId = NetexImportUtil.composeObjectId("Quay",parameters.getObjectIdPrefix(),stopPlaceRef.getRef());
					mobi.chouette.model.StopArea virtualQuay = ObjectFactory.getStopArea(referential, virtualQuayId);
					virtualQuay.setAreaType(ChouetteAreaEnum.BoardingPosition);
					virtualQuay.setName(stopPlace.getName());
				    virtualQuay.setComment(AUTO_CREATED_QUAY_SUFFIX);
				    virtualQuay.setParent(stopPlace);
				    virtualQuay.setLatitude(stopPlace.getLatitude());
					virtualQuay.setLongitude(stopPlace.getLongitude());

					KeyValue keyValue = new KeyValue();
					keyValue.setKey(AUTO_CREATED_QUAY_SUFFIX);
					keyValue.setValue(String.valueOf(true));
					List<KeyValue> keyValues = new ArrayList<>(1);
					keyValues.add(keyValue);
					virtualQuay.setKeyValues(keyValues);

					String scheduledStopPointId = NetexImportUtil.composeObjectIdFromNetexId(context,"ScheduledStopPoint",scheduledStopPointRef.getValue().getRef());
					ScheduledStopPoint scheduledStopPoint = ObjectFactory.getScheduledStopPoint(referential, scheduledStopPointId);
					scheduledStopPoint.setContainedInStopAreaRef(new SimpleObjectReference<>(stopPlace));
					scheduledStopPoint.setContainedInStopAreaRef(new SimpleObjectReference<>(virtualQuay));
				}
				// TODO à revoir pour changement de profil
//				ScheduledStopPoint scheduledStopPoint = ObjectFactory.getScheduledStopPoint(referential, scheduledStopPointRef.getRef());

			}
		}
	}

	//private String getObjectIdFromRef()

	static {
		ParserFactory.register(StopAssignmentParser.class.getName(), new ParserFactory() {
			private StopAssignmentParser instance = new StopAssignmentParser();

			@Override
			protected Parser create() {
				return instance;
			}
		});
	}

}
