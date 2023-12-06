package mobi.chouette.exchange.netexprofile.parser;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.importer.util.NetexImportUtil;
import mobi.chouette.model.ScheduledStopPoint;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.rutebanken.netex.model.ScheduledStopPointsInFrame_RelStructure;

import java.util.ArrayList;
import java.util.List;

public class ScheduledStopPointParser implements Parser, Constant {
	@Override
	public void parse(Context context) throws Exception {
		Referential referential = (Referential) context.get(REFERENTIAL);
		ScheduledStopPointsInFrame_RelStructure scheduledStopPointsInFrameStruct = (ScheduledStopPointsInFrame_RelStructure) context.get(NETEX_LINE_DATA_CONTEXT);

		List<String> stopAreaNoRefScheduledStopPoint = new ArrayList<>();
		context.remove(SCHEDULE_STOP_POINT_STOP_AREA_NULL);

		if (scheduledStopPointsInFrameStruct != null) {

			for (org.rutebanken.netex.model.ScheduledStopPoint netexScheduledStopPoint : scheduledStopPointsInFrameStruct.getScheduledStopPoint()) {

				String scheduledStopPointId = NetexImportUtil.composeObjectIdFromNetexId(context,"ScheduledStopPoint",netexScheduledStopPoint.getId());
				ScheduledStopPoint scheduledStopPoint = ObjectFactory.getScheduledStopPoint(referential, scheduledStopPointId);
				if (netexScheduledStopPoint.getName() != null) {
					scheduledStopPoint.setName(netexScheduledStopPoint.getName().getValue());
				}
				StopArea stopArea = scheduledStopPoint.getContainedInStopAreaRef().getObject();
				if (stopArea == null || stopArea.getLongitude() == null || stopArea.getLatitude() == null ){
					stopAreaNoRefScheduledStopPoint.add(stopArea != null ? stopArea.getObjectId() : scheduledStopPoint.getObjectId());
					context.put(SCHEDULE_STOP_POINT_STOP_AREA_NULL, stopAreaNoRefScheduledStopPoint);
				}
			}
		}

	}

	static {
		ParserFactory.register(ScheduledStopPointParser.class.getName(),
				new ParserFactory() {
					private ScheduledStopPointParser instance = new ScheduledStopPointParser();

					@Override
					protected Parser create() {
						return instance;
					}
				});
	}
}
