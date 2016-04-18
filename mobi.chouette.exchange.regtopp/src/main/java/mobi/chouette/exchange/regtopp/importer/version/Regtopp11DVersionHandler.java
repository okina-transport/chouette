package mobi.chouette.exchange.regtopp.importer.version;

import java.nio.file.Path;
import java.util.Arrays;

import mobi.chouette.exchange.regtopp.model.importer.parser.ParseableFile;
import mobi.chouette.exchange.regtopp.model.importer.parser.RegtoppImporter;
import mobi.chouette.exchange.regtopp.model.v11.RegtoppDayCodeDKO;
import mobi.chouette.exchange.regtopp.model.v11.RegtoppDayCodeHeaderDKO;
import mobi.chouette.exchange.regtopp.model.v11.RegtoppDestinationDST;
import mobi.chouette.exchange.regtopp.model.v11.RegtoppFootnoteMRK;
import mobi.chouette.exchange.regtopp.model.v11.RegtoppLineLIN;
import mobi.chouette.exchange.regtopp.model.v11.RegtoppPathwayGAV;
import mobi.chouette.exchange.regtopp.model.v11.RegtoppStopHPL;
import mobi.chouette.exchange.regtopp.model.v11.RegtoppZoneSON;
import mobi.chouette.exchange.regtopp.model.v12.RegtoppRoutePointRUT;
import mobi.chouette.exchange.regtopp.model.v12.RegtoppRouteTMS;
import mobi.chouette.exchange.regtopp.model.v12.RegtoppTableVersionTAB;
import mobi.chouette.exchange.regtopp.model.v12.RegtoppTripIndexTIX;
import mobi.chouette.exchange.regtopp.model.v12.RegtoppVehicleJourneyVLP;
import mobi.chouette.exchange.report.FileInfo;

public class Regtopp11DVersionHandler implements VersionHandler {

	@Override
	public void registerFileForIndex(RegtoppImporter importer, Path fileName, String extension, FileInfo file) {
		// TODO
	}

	@Override
	public String createStopPointId(RegtoppRouteTMS tms) {
		// TODO Auto-generated method stub
		return null;
	}
}
