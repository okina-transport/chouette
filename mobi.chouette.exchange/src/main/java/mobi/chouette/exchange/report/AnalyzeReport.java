package mobi.chouette.exchange.report;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mobi.chouette.common.Constant;
import mobi.chouette.exchange.report.ActionReporter.FILE_STATE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_TYPE;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.LocalDate;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@XmlRootElement(name = "analyze_report")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "exploitationPeriod", "files", "lines", "journeys", "stops"})
@Data
@EqualsAndHashCode(callSuper = false)
public class AnalyzeReport extends AbstractReport implements Constant, Report {

	@XmlElement(name = "exploitationPeriod")
	private String exploitationPeriod;

	@XmlElement(name = "oldestPeriod")
	private LocalDate oldestPeriodOfCalendars;

	@XmlElement(name = "newestPeriod")
	private LocalDate newestPeriodOfCalendars;

	@XmlElement(name = "files")
	private List<FileReport> files = new ArrayList<>();

	@XmlElement(name = "lines")
	private List<String> lines = new ArrayList<>();

	@XmlElement(name = "journeys")
	private List<String> journeys = new ArrayList<>();

	@XmlElement(name = "stops")
	private List<String> stops = new ArrayList<>();

	@XmlTransient
	private Date date = new Date(0);



	/**
	 *
	 * @param file
	 */
	protected void addFileReport(FileReport file) {
		files.add(file);
	}



	public JSONObject toJson() throws JSONException {
		JSONObject analyzeReport = new JSONObject();



		if (!files.isEmpty()) {
			JSONArray array = new JSONArray();
			analyzeReport.put("files", array);
			for (FileReport file : files) {
				array.put(file.toJson());
			}
		}

		if (!lines.isEmpty()) {
			JSONArray array = new JSONArray();
			analyzeReport.put("lines", array);
			for (String line : lines) {
				JSONObject object = new JSONObject();
				object.put("name", line);
				array.put(object);
			}
		}

		if (!journeys.isEmpty()) {
			analyzeReport.put("number_of_journeys", journeys.size());
		}

		if (!stops.isEmpty()) {
			JSONArray array = new JSONArray();
			analyzeReport.put("stops", array);
			for (String stop : stops) {
				JSONObject object = new JSONObject();
				object.put("stop_name", stop);
				array.put(object);
			}
		}

		JSONObject object = new JSONObject();
		object.put("analyze_report", analyzeReport);
		return object;
	}

	@Override
	public boolean isEmpty() {
		// used to know if report has to be saved
		// Analyze Report has to be saved any time
		return false;
	}


	@Override
	public void print(PrintStream out, StringBuilder ret , int level, boolean first) {
		ret.setLength(0);
		level = 0;
		out.print("{\"analyze_report\": {\n");

		out.print("\"exploitation_period\": { \n");
		out.print("\"start\": \"" + oldestPeriodOfCalendars + "\", \n");
		out.print("\"end\": \"" + newestPeriodOfCalendars + "\" \n");
		out.print("}\n");




		if (!files.isEmpty()){
			printArray(out, ret, level + 1, "files", files, false);
			out.print(",\n");
		}

		if (!lines.isEmpty())
			printStringList(out,lines,"lines","lineName");

		out.print(",\n");
		out.print("\"journeys_count\": " + journeys.size() + "\n");

		if (!stops.isEmpty())
			printStringList(out,stops,"stops","stopName");


		out.println("\n}}");
	}

	private void printStringList(PrintStream out,List<String> listToPrint, String categoryName, String itemName){
		out.print(",\n");
		out.print("\"" + categoryName + "\": [\n");
		String endOfline;

		for (int i = 0; i < listToPrint.size(); i++){
			endOfline = i == listToPrint.size() - 1 ? "\" }\n" : "\" },\n";
			out.print("{ \""+ itemName + "\": \"" + listToPrint.get(i) + endOfline);
		}
		out.println("]");
	}

	@Override
	public void print(PrintStream stream) {
		print(stream, new StringBuilder() , 1, true);

	}
}
