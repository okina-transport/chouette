package mobi.chouette.exchange.importer.geometry.osrm;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.locationtech.jts.geom.LineString;
import org.testng.Assert;
import org.testng.annotations.Test;

public class OsrmRouteSectionGeneratorTest {


	private OsrmRouteSectionGenerator osrmRouteSectionGenerator = new OsrmRouteSectionGenerator();

	@Test
	public void mapBusRouteSection_success() throws Exception {

		String validResponse = FileUtils.readFileToString(new File("src/test/data/osrm/OsrmBus.json"));

		LineString lineString = osrmRouteSectionGenerator.mapToLineString(validResponse);
		Assert.assertNotNull(lineString);
	}


}
