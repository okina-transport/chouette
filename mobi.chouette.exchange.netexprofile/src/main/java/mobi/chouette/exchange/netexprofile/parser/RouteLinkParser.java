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
import org.rutebanken.netex.model.LinkSequenceProjection_VersionStructure;
import org.rutebanken.netex.model.RouteLink;
import org.rutebanken.netex.model.RouteLinksInFrame_RelStructure;
import org.rutebanken.netex.model.ServiceLink;
import org.rutebanken.netex.model.ServiceLinksInFrame_RelStructure;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;

@Log4j
public class RouteLinkParser extends NetexParser implements Parser, Constant {

	@Override
	public void parse(Context context) throws Exception {
		Referential referential = (Referential) context.get(REFERENTIAL);
		RouteLinksInFrame_RelStructure routeLinksInInFrameStruct = (RouteLinksInFrame_RelStructure) context.get(NETEX_LINE_DATA_CONTEXT);

		List<RouteLink> routeLinks = routeLinksInInFrameStruct.getRouteLink();
		GeometryFactory factory = new GeometryFactory(new PrecisionModel(10), 4326);

		for (RouteLink routeLink : routeLinks) {

			String routeSectionId = NetexImportUtil.composeObjectIdFromNetexId(context,"RouteSection",routeLink.getId());
			RouteSection routeSection = ObjectFactory.getRouteSection(referential, routeSectionId);
			routeSection.setObjectVersion(NetexParserUtils.getVersion(routeLink));

			String fromScheduledStopPointId = NetexImportUtil.composeObjectIdFromNetexId(context,"ScheduledStopPoint", routeLink.getFromPointRef().getRef());
			if (routeSection.getFromScheduledStopPoint() != null && !routeSection.getFromScheduledStopPoint().getObjectId().equals(fromScheduledStopPointId)) {
				List<String> wrongRouteSections = (List<String>) context.get(WRONG_ROUTE_SECTIONS);
				if (wrongRouteSections == null) {
					wrongRouteSections = new ArrayList<>();
				}
				wrongRouteSections.add(routeSection.getObjectId());
				context.put(WRONG_ROUTE_SECTIONS, wrongRouteSections);
			}
			routeSection.setFromScheduledStopPoint(ObjectFactory.getScheduledStopPoint(referential, fromScheduledStopPointId));
			String toScheduledStopPointId = NetexImportUtil.composeObjectIdFromNetexId(context,"ScheduledStopPoint", routeLink.getToPointRef().getRef());
			routeSection.setToScheduledStopPoint(ObjectFactory.getScheduledStopPoint(referential,toScheduledStopPointId));
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
