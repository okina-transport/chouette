package mobi.chouette.exchange.report;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mobi.chouette.common.Constant;
import mobi.chouette.exchange.report.ActionReporter.FILE_STATE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_TYPE;
import mobi.chouette.model.StopArea;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


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
	private List<StopArea> stops = new ArrayList<>();

	@XmlElement(name = "wrongGeolocStopAreas")
	private List<Pair<StopArea,StopArea>> wrongGeolocStopAreas = new ArrayList<>();

	@XmlElement(name = "duplicateOriginalStopIds")
	private List<String> duplicateOriginalStopIds = new ArrayList<>();

	@XmlElement(name = "canLaunchImport")
	private boolean canLaunchImport = true;

	@XmlTransient
	private Date date = new Date(0);

	private Map<String,String> lineTextColorMap = new HashMap<>();
	private Map<String,String> lineBackgroundColorMap = new HashMap<>();
	private Map<String,String> lineShortNameMap = new HashMap<>();



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
			for (StopArea stop : stops) {
				JSONObject object = new JSONObject();
				object.put("stop_name", stop.getName());
				array.put(object);
			}
		}


		if (!wrongGeolocStopAreas.isEmpty()){
			canLaunchImport = false;
			JSONArray array = new JSONArray();
			analyzeReport.put("stops", array);
			for (Pair<StopArea,StopArea> wrongGeolocPair : wrongGeolocStopAreas) {
				JSONObject object = new JSONObject();
				StopArea existingStop = wrongGeolocPair.getLeft();
				StopArea incomingStop = wrongGeolocPair.getRight();
				object.put("original_stop_id", incomingStop.getOriginalStopId());

				object.put("existingName", existingStop.getName());
				object.put("existingLatitude", existingStop.getLatitude());
				object.put("existingLongitude", existingStop.getLongitude());

				object.put("incomingName", incomingStop.getName());
				object.put("incomingLatitude", incomingStop.getLatitude());
				object.put("incomingLongitude", incomingStop.getLongitude());
				array.put(object);
			}
		}

		if (!duplicateOriginalStopIds.isEmpty()) {
			canLaunchImport = false;
			JSONArray array = new JSONArray();
			analyzeReport.put("duplicateOriginalStopIds", array);
			for (String originalStopId : duplicateOriginalStopIds) {
				JSONObject object = new JSONObject();
				object.put("original_stsop_id",originalStopId);
				array.put(object);
			}
		}

		analyzeReport.put("can_launch_import:",canLaunchImport );
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


	public void addLineTextColor(String lineName, String lineTextColor){
		if (!lineTextColorMap.containsKey(lineName))
			lineTextColorMap.put(lineName,lineTextColor);
	}

	public void addLineBackgroundColor(String lineName, String lineBackgroundColor){
		if (!lineBackgroundColorMap.containsKey(lineName))
			lineBackgroundColorMap.put(lineName,lineBackgroundColor);
	}

	public void addLineShortName(String lineName, String shortName){
		if (!lineShortNameMap.containsKey(lineName))
			lineShortNameMap.put(lineName,shortName);
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
			printLineList(out);

		out.print(",\n");
		out.print("\"journeys_count\": " + journeys.size() + "\n");

		if (!stops.isEmpty()){
			List<String> stopList = stops.stream()
					.map(StopArea::getName)
					.collect(Collectors.toList());

			printStringList(out,stopList,"stops","stopName");
		}

		if (!wrongGeolocStopAreas.isEmpty()){
			canLaunchImport = false;
			printWrongGeolocList(out);
		}

		if (!duplicateOriginalStopIds.isEmpty()){
			canLaunchImport = false;
			printStringList(out,duplicateOriginalStopIds,"duplicateOriginalStopId","originalStopId");
		}

		out.print(",\n");
		out.print("\"canLaunchImport\": " + canLaunchImport + "\n");

		out.println("\n}}");
	}

	private void printWrongGeolocList(PrintStream out) {
		out.print(",\n");
		out.print("\"wrongGeolocStopAreas\": [\n");
		String endOfline;

		for (int i = 0; i < wrongGeolocStopAreas.size(); i++){
			endOfline = i == wrongGeolocStopAreas.size() - 1 ? "\" }\n" : "\" },\n";

			StopArea existingStop = wrongGeolocStopAreas.get(i).getLeft();
			StopArea incomingStop = wrongGeolocStopAreas.get(i).getRight();

			out.print("{ \"original_stop_id\": \"" + incomingStop.getOriginalStopId() + "\",\n");
			out.print(" \"existing_name\": \"" + existingStop.getName() + "\",\n");
			out.print(" \"existing_latitude\": \"" + existingStop.getLatitude() + "\",\n");
			out.print(" \"existing_longitude\": \"" + existingStop.getLongitude() + "\",\n");

			out.print(" \"incoming_name\": \"" + incomingStop.getName() + "\",\n");
			out.print(" \"incoming_latitude\": \"" + incomingStop.getLatitude() + "\",\n");
			out.print(" \"incoming_longitude\": \"" + incomingStop.getLongitude() + endOfline);
		}
		out.println("]");
	}

	private void printLineList(PrintStream out){
		out.print(",\n");
		out.print("\"lines\": [\n");
		String endOfline;

		for (int i = 0; i < lines.size(); i++){
			endOfline = i == lines.size() - 1 ? "\" }\n" : "\" },\n";
			String lineName = lines.get(i);
			String lineTextColor = lineTextColorMap.containsKey(lineName) ?  lineTextColorMap.get(lineName) : "000000";
			String lineBackgroundColor = lineBackgroundColorMap.containsKey(lineName) ?  lineBackgroundColorMap.get(lineName) : "FFFFFF";
			String lineShortName = lineShortNameMap.containsKey(lineName) ?  lineShortNameMap.get(lineName) : "";

			out.print("{ \"lineName\": \"" + lines.get(i) + "\", \"lineTextColor\":\"" + lineTextColor + "\", \"lineBackgroundColor\": \"" + lineBackgroundColor + "\", \"shortName\":\""+ lineShortName + endOfline);
		}
		out.println("]");
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
