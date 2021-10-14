package mobi.chouette.exchange.neptune.parser;

import mobi.chouette.exchange.neptune.importer.NeptuneImportParameters;
import mobi.chouette.model.Line;
import org.joda.time.LocalDateTime;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.XPPUtil;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.importer.ParserUtils;
import mobi.chouette.exchange.neptune.validation.JourneyPatternValidator;
import mobi.chouette.exchange.validation.ValidatorFactory;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Route;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.util.NeptuneUtil;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;

import org.xmlpull.v1.XmlPullParser;

@Log4j
public class JourneyPatternParser implements Parser, Constant {
	private static final String CHILD_TAG = "JourneyPattern";

	@Override
	public void parse(Context context) throws Exception {

		XmlPullParser xpp = (XmlPullParser) context.get(PARSER);
		Referential referential = (Referential) context.get(REFERENTIAL);
		NeptuneImportParameters configuration = (NeptuneImportParameters) context.get(CONFIGURATION);

		xpp.require(XmlPullParser.START_TAG, null, CHILD_TAG);
		int columnNumber =  xpp.getColumnNumber();
		int lineNumber =  xpp.getLineNumber();

		JourneyPatternValidator validator = (JourneyPatternValidator) ValidatorFactory.create(JourneyPatternValidator.class.getName(), context);

		JourneyPattern journeyPattern = null;
		String objectId = null;
		while (xpp.nextTag() == XmlPullParser.START_TAG) {

			if (xpp.getName().equals("objectId")) {
				objectId = ParserUtils.getText(xpp.nextText());
				objectId = AbstractConverter.composeObjectId(configuration, Line.JOURNEYPATTERN_KEY, objectId);
				journeyPattern = ObjectFactory.getJourneyPattern(referential,
						objectId);
				journeyPattern.setFilled(true);
			} else if (xpp.getName().equals("objectVersion")) {
				Integer version = ParserUtils.getInt(xpp.nextText());
				journeyPattern.setObjectVersion(version);
			} else if (xpp.getName().equals("creationTime")) {
				LocalDateTime creationTime = ParserUtils.getLocalDateTime(xpp.nextText());
				journeyPattern.setCreationTime(creationTime);
			} else if (xpp.getName().equals("creatorId")) {
				journeyPattern
				.setCreatorId(ParserUtils.getText(xpp.nextText()));
			} else if (xpp.getName().equals("name")) {
				journeyPattern.setName(ParserUtils.getText(xpp.nextText()));
			} else if (xpp.getName().equals("publishedName")) {
				journeyPattern.setPublishedName(ParserUtils.getText(xpp
						.nextText()));
			} else if (xpp.getName().equals("routeId")) {
				String routeId = ParserUtils.getText(xpp.nextText());
				String routeObjectId = AbstractConverter.composeObjectId(configuration, Line.ROUTE_KEY, routeId);
				validator.addRouteId(context, objectId, routeObjectId);
				Route route = ObjectFactory.getRoute(referential, routeObjectId);
				journeyPattern.setRoute(route);
			} else if (xpp.getName().equals("origin")) {
				String origin = ParserUtils.getText(xpp.nextText());
				String originObjectId = AbstractConverter.composeObjectId(configuration, Line.STOPPOINT_KEY, origin);
				StopPoint departureStopPoint = ObjectFactory.getStopPoint(
						referential, originObjectId);
				journeyPattern.setDepartureStopPoint(departureStopPoint);

			} else if (xpp.getName().equals("destination")) {
				String destination = ParserUtils.getText(xpp.nextText());
				String destinationObjectId = AbstractConverter.composeObjectId(configuration, Line.STOPPOINT_KEY, destination);
				StopPoint arrivalStopPoint = ObjectFactory.getStopPoint(
						referential, destinationObjectId);
				journeyPattern.setArrivalStopPoint(arrivalStopPoint);

			} else if (xpp.getName().equals("stopPointList")) {
				String stopPointId = ParserUtils.getText(xpp.nextText());
				String stopPointObjectId = AbstractConverter.composeObjectId(configuration, Line.STOPPOINT_KEY, stopPointId);
				validator.addStopPointList(context, objectId, stopPointObjectId);
				StopPoint stopPoint = ObjectFactory.getStopPoint(referential,
						stopPointObjectId);
				journeyPattern.addStopPoint(stopPoint);
			} else if (xpp.getName().equals("registration")) {

				while (xpp.nextTag() == XmlPullParser.START_TAG) {
					if (xpp.getName().equals("registrationNumber")) {
						journeyPattern.setRegistrationNumber(ParserUtils
								.getText(xpp.nextText()));
					} else {
						XPPUtil.skipSubTree(log, xpp);
					}
				}
			} else if (xpp.getName().equals("comment")) {
				journeyPattern.setComment(ParserUtils.getText(xpp.nextText()));
			} else if (xpp.getName().equals("lineIdShortcut")) {
				String lineIdShortcut = ParserUtils.getText(xpp.nextText());
				String lineObjectId = AbstractConverter.composeObjectId(configuration, Line.LINE_KEY, lineIdShortcut);
				validator.addLineIdShortcut(context, objectId, lineObjectId);
			} else {
				XPPUtil.skipSubTree(log, xpp);
			}
		}
		validator.addLocation(context, journeyPattern, lineNumber, columnNumber);
		NeptuneUtil.refreshDepartureArrivals(journeyPattern);
	}

	static {
		ParserFactory.register(JourneyPatternParser.class.getName(),
				new ParserFactory() {
			private JourneyPatternParser instance = new JourneyPatternParser();

			@Override
			protected Parser create() {
				return instance;
			}
		});
	}
}
