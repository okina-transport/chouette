package mobi.chouette.exchange.neptune.extension;

import mobi.chouette.exchange.neptune.JsonExtension;
import mobi.chouette.model.Footnote;
import mobi.chouette.model.Line;
import mobi.chouette.model.Route;
import mobi.chouette.model.VehicleJourney;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.Test;

public class VehicleJourneyParserTests {

	private CommentExtension parser = new CommentExtension();

	private Line line = null;

	private Route route;

	private Footnote buildFootnote(String key, Line line) {
		Footnote note = new Footnote();
		note.setKey(key);
		note.setCode("code" + key);
		note.setLabel("label" + key);
		note.setLine(line);
		return note;
	}

	private void addLine(VehicleJourney vj) {
		if (line == null) {
			line = new Line();

			line.getFootnotes().add(buildFootnote("1", line));
			line.getFootnotes().add(buildFootnote("2", line));
			line.getFootnotes().add(buildFootnote("3", line));
			line.getFootnotes().add(buildFootnote("4", line));

			route = new Route();
			route.setLine(line);
		}
		if (vj.getRoute() == null)
			vj.setRoute(route);
	}
}
