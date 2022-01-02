package mobi.chouette.exchange.netexprofile.parser;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.PrecisionModel;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.TimeUtil;
import mobi.chouette.exchange.NetexParserUtils;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.netexprofile.importer.NetexprofileImportParameters;
import mobi.chouette.exchange.netexprofile.importer.util.NetexImportUtil;
import mobi.chouette.exchange.netexprofile.util.JtsGmlConverter;
import mobi.chouette.model.ConnectionLink;
import mobi.chouette.model.RouteSection;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.rutebanken.netex.model.RouteLink;
import org.rutebanken.netex.model.RouteLinksInFrame_RelStructure;
import org.rutebanken.netex.model.SiteConnection;

import java.util.List;

@Log4j
public class SiteConnectionParser extends NetexParser implements Parser, Constant {

	@Override
	public void parse(Context context) throws Exception {
		Referential referential = (Referential) context.get(REFERENTIAL);
		List<SiteConnection> siteConnections = (List<SiteConnection>) context.get(NETEX_LINE_DATA_CONTEXT);
		NetexprofileImportParameters parameters = (NetexprofileImportParameters) context.get(CONFIGURATION);


		for (SiteConnection siteConnection : siteConnections) {

			String fromQuayId = NetexImportUtil.composeObjectId("Quay", parameters.getObjectIdPrefix(), siteConnection.getFrom().getQuayRef().getRef());
			StopArea fromStopArea = ObjectFactory.getStopArea(referential,fromQuayId);

			String toQuayId = NetexImportUtil.composeObjectId("Quay", parameters.getObjectIdPrefix(), siteConnection.getTo().getQuayRef().getRef());
			StopArea toStopArea = ObjectFactory.getStopArea(referential,toQuayId);

			String siteConnectionId = NetexImportUtil.composeObjectIdFromNetexId("ConnectionLink", parameters.getObjectIdPrefix(), siteConnection.getId());
			ConnectionLink connectionLink = ObjectFactory.getConnectionLink(referential, siteConnectionId);
			connectionLink.setStartOfLink(fromStopArea);
			connectionLink.setEndOfLink(toStopArea);
			connectionLink.setLinkDistance(siteConnection.getDistance());
			connectionLink.setDefaultDuration(TimeUtil.toJodaDuration(siteConnection.getWalkTransferDuration().getDefaultDuration()));
			connectionLink.setName(siteConnection.getName().getValue());
		}

	}

	static {
		ParserFactory.register(SiteConnectionParser.class.getName(), new ParserFactory() {
			private SiteConnectionParser instance = new SiteConnectionParser();

			@Override
			protected Parser create() {
				return instance;
			}
		});
	}
}
