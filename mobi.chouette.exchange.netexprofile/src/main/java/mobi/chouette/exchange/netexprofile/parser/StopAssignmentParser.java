package mobi.chouette.exchange.netexprofile.parser;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.importer.NetexprofileImportParameters;
import mobi.chouette.exchange.netexprofile.importer.util.NetexImportUtil;
import mobi.chouette.model.ScheduledStopPoint;
import mobi.chouette.model.SimpleObjectReference;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.rutebanken.netex.model.*;

import javax.xml.bind.JAXBElement;

@Log4j
public class StopAssignmentParser extends NetexParser implements Parser, Constant {

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

				QuayRefStructure quayRef = stopAssignment.getQuayRef().getValue();


				NetexprofileImportParameters parameters = (NetexprofileImportParameters) context.get(CONFIGURATION);
				String generatedId = NetexImportUtil.composeObjectId("Quay",parameters.getObjectIdPrefix(),quayRef.getRef());

				mobi.chouette.model.StopArea quay = ObjectFactory.getStopArea(referential, generatedId);
				if(quay.getAreaType() == null) {
					quay.setAreaType(ChouetteAreaEnum.BoardingPosition);
				}

				String scheduledStopPointId = NetexImportUtil.composeObjectIdFromNetexId(context,"ScheduledStopPoint",scheduledStopPointRef.getValue().getRef());
				ScheduledStopPoint scheduledStopPoint = ObjectFactory.getScheduledStopPoint(referential, scheduledStopPointId);

				// TODO à revoir pour changement de profil
//				ScheduledStopPoint scheduledStopPoint = ObjectFactory.getScheduledStopPoint(referential, scheduledStopPointRef.getRef());

				scheduledStopPoint.setContainedInStopAreaRef(new SimpleObjectReference<>(quay));


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
