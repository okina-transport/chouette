package mobi.chouette.exchange.netexprofile.parser;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.PrecisionModel;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.NetexParserUtils;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.netexprofile.importer.util.NetexImportUtil;
import mobi.chouette.exchange.netexprofile.util.JtsGmlConverter;
import mobi.chouette.model.RouteSection;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.rutebanken.netex.model.RouteLink;
import org.rutebanken.netex.model.RouteLinksInFrame_RelStructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Log4j
public class RouteLinkParser extends NetexParser implements Parser, Constant {

	@Override
	public void parse(Context context) throws Exception {
		Referential referential = (Referential) context.get(REFERENTIAL);
		RouteLinksInFrame_RelStructure routeLinksInInFrameStruct = (RouteLinksInFrame_RelStructure) context.get(NETEX_LINE_DATA_CONTEXT);

		List<RouteLink> routeLinks = routeLinksInInFrameStruct.getRouteLink();
		GeometryFactory factory = new GeometryFactory(new PrecisionModel(10), 4326);

		Map<String, Set<String>> routeSectionUsedInMultipleFiles = (Map<String, Set<String>>) context.get(ROUTE_LINKS_USED_IN_MULTIPLE_FILES);
		if (routeSectionUsedInMultipleFiles == null) {
			routeSectionUsedInMultipleFiles = new HashMap<>();
		}

		context.remove(ROUTE_LINKS_USED_MULTIPLE_TIMES_IN_THE_SAME_FILE);
		context.remove(ROUTE_LINKS_USED_SAME_FROM_AND_TO_SCHEDULED_STOP_POINT);
		List<String> routeSectionUsedMultipleTimesInTheSameFile = new ArrayList<>();
		List<String> routeSectionUsedSameFromAndToScheduledStopPoint = new ArrayList<>();


		for (RouteLink routeLink : routeLinks) {
			String routeSectionId = NetexImportUtil.composeObjectIdFromNetexId(context,"RouteSection",routeLink.getId());
			RouteSection routeSection = ObjectFactory.getRouteSection(referential, routeSectionId);

			routeSectionUsedMultipleTimesInTheSameFile.add(routeSection.getObjectId());
			context.put(ROUTE_LINKS_USED_MULTIPLE_TIMES_IN_THE_SAME_FILE, routeSectionUsedMultipleTimesInTheSameFile);

			String fileName = (String) context.get(FILE_NAME);
			Set<String> filesUsingRouteSection = routeSectionUsedInMultipleFiles.get(routeSection.getObjectId()) != null ? routeSectionUsedInMultipleFiles.get(routeSection.getObjectId()) :  new HashSet<>();
			filesUsingRouteSection.add(fileName);
			routeSectionUsedInMultipleFiles.put(routeSection.getObjectId(), filesUsingRouteSection);
			context.put(ROUTE_LINKS_USED_IN_MULTIPLE_FILES, routeSectionUsedInMultipleFiles);

			routeSection.setObjectVersion(NetexParserUtils.getVersion(routeLink));

			String fromScheduledStopPointId = NetexImportUtil.composeObjectIdFromNetexId(context,"ScheduledStopPoint", routeLink.getFromPointRef().getRef());
			routeSection.setFromScheduledStopPoint(ObjectFactory.getScheduledStopPoint(referential, fromScheduledStopPointId));
			String toScheduledStopPointId = NetexImportUtil.composeObjectIdFromNetexId(context,"ScheduledStopPoint", routeLink.getToPointRef().getRef());
			routeSection.setToScheduledStopPoint(ObjectFactory.getScheduledStopPoint(referential,toScheduledStopPointId));


			if (routeSection.getFromScheduledStopPoint() != null &&
					routeSection.getToScheduledStopPoint() != null &&
					routeSection.getFromScheduledStopPoint().getObjectId().equals(routeSection.getToScheduledStopPoint().getObjectId())) {
				routeSectionUsedSameFromAndToScheduledStopPoint.add(routeSection.getObjectId());
				context.put(ROUTE_LINKS_USED_SAME_FROM_AND_TO_SCHEDULED_STOP_POINT, routeSectionUsedSameFromAndToScheduledStopPoint);
			}


			routeSection.setDistance(routeLink.getDistance());


			LineString lineString = JtsGmlConverter.fromGmlToJts(routeLink.getLineString());
			Coordinate[] coords = lineString.getCoordinates();

			Coordinate[] inputCoords = new Coordinate[2];

			StopArea previousLocation = routeSection.getFromScheduledStopPoint().getContainedInStopAreaRef().getObject();
			inputCoords[0] = new Coordinate(previousLocation.getLongitude().doubleValue(), previousLocation.getLatitude().doubleValue());
			StopArea location = routeSection.getToScheduledStopPoint().getContainedInStopAreaRef().getObject();
			inputCoords[1] = new Coordinate(location.getLongitude().doubleValue(), location.getLatitude().doubleValue());

			routeSection.setProcessedGeometry(factory.createLineString(coords));
			routeSection.setInputGeometry(factory.createLineString(inputCoords));
			routeSection.setNoProcessing(false);

		}

	}

	static {
		ParserFactory.register(RouteLinkParser.class.getName(), new ParserFactory() {
			private RouteLinkParser instance = new RouteLinkParser();

			@Override
			protected Parser create() {
				return instance;
			}
		});
	}
}
