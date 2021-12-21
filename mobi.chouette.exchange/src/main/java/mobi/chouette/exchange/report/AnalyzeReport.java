package mobi.chouette.exchange.report;

import lombok.Data;
import lombok.EqualsAndHashCode;
import mobi.chouette.common.Constant;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.TransportModeNameEnum;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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


	// used to store each quay transport mode
	private Map<String, TransportModeNameEnum> quayTransportMode = new HashMap<>();

	// used to store all lines using a specific quay (key = quay id, value = list of lines using this quay)
	private Map<String, Set<String>> quayLineUse = new HashMap<>();

	// used to store stopPlace with multiple transport modes
	private Set<String> stopPlaceWithMultipleTransportModes = new HashSet();

	//Used to store all quay in error (that are on 2 lines with different transport mode)
	private Set<String> quayWithDifferentTransportModes = new HashSet();

	// used to store all stop place childrens with transportModes (key = stop place id, value = list of quays under the stop place)
	private Map<String, Set<String>> stopPlaceChildrens = new HashMap<>();



	public Map<String, TransportModeNameEnum> getQuayTransportMode() {
		return quayTransportMode;
	}


	public Map<String, Set<String>> getQuayLineUse() {
		return quayLineUse;
	}

	public Set<String> getQuayWithDifferentTransportModes() {
		return quayWithDifferentTransportModes;
	}



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
				String stopCode = StringUtils.isEmpty(stop.getName()) ? stop.getObjectId() : stop.getName();
				object.put("stop_name", stopCode);
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
				object.put("original_stop_id",originalStopId);
				array.put(object);
			}
		}

		if (!quayWithDifferentTransportModes.isEmpty()){
			canLaunchImport = false;
			JSONArray array = new JSONArray();
			analyzeReport.put("quays_with_different_transport_modes", array);

			for (String quayId : quayWithDifferentTransportModes) {
				JSONObject object = new JSONObject();
				object.put("quay_id", quayId);
				object.put("used_in_lines", quayLineUse.get(quayId));
				array.put(object);
			}
		}else{
			//each quay has a single transport mode.
			// Now checking if each stop place has all children with the same transport mode
			writeStopPlaceWithDifferentTransportModesInError(analyzeReport);
		}

		analyzeReport.put("can_launch_import:",canLaunchImport );
		JSONObject object = new JSONObject();
		object.put("analyze_report", analyzeReport);
		return object;
	}

	private void writeStopPlaceWithDifferentTransportModesInError(JSONObject analyzeReport) throws JSONException {
		checkStopPlaceTransportModes();

		if (!stopPlaceWithMultipleTransportModes.isEmpty()){
			canLaunchImport = false;

			JSONArray array = new JSONArray();
			analyzeReport.put("stop_place_with_different_transport_modes", array);

			for (String stopPlaceId : stopPlaceWithMultipleTransportModes) {
				JSONObject object = new JSONObject();
				object.put("stop_place_id", stopPlaceId);
				object.put("quays_with_transportModes",buildQuayListWithTransportModes(stopPlaceId));
				array.put(object);
			}
		}
	}

	/**
	 * Create a list with all quays ids and transport modes, for a specifid stop place
	 * @param stopPlaceId
	 * 	The stop place for which the list must be created
	 * @return
	 * 	The list of quays with transport modes
	 */
	private String buildQuayListWithTransportModes(String stopPlaceId){

		return stopPlaceChildrens.get(stopPlaceId).stream()
								.map(quayId -> quayId + "(" + quayTransportMode.get(quayId).toString() + ")")
								.collect(Collectors.joining(","));
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
					.map(stop -> StringUtils.isEmpty(stop.getName()) ? stop.getObjectId() : stop.getName())
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


		if (!quayWithDifferentTransportModes.isEmpty()){
			canLaunchImport = false;
			printQuaysWithDifferentTransportModes(out);
		}else{
			checkStopPlaceTransportModes();
			printStopPlaceWithDifferentTransportModes(out);
		}

		out.print("\"canLaunchImport\": " + canLaunchImport + "\n");
		out.println("\n}}");
	}


	private void printStopPlaceWithDifferentTransportModes(PrintStream out){

		if (stopPlaceWithMultipleTransportModes.isEmpty())
			return;

		canLaunchImport = false;
		out.print("\"stopplaces_with_different_transport_modes\": [\n");
		String endOfline;

		String[] stopPlacesInErrorArray = stopPlaceWithMultipleTransportModes.toArray(new String[stopPlaceWithMultipleTransportModes.size()]);

		for (int i = 0; i < stopPlacesInErrorArray.length; i++){
			String stopPlaceId = stopPlacesInErrorArray[i];
			endOfline = i == stopPlacesInErrorArray.length - 1 ? "\" }\n" : "\" },\n";
			out.print("{ \"stop_place_id\": \"" + stopPlaceId + "\",\n");
			out.print(" \"quays_with_transportModes\": \"" + buildQuayListWithTransportModes(stopPlaceId) + endOfline);
		}


		out.println("]");
		out.print(",\n");
	}

	private void printQuaysWithDifferentTransportModes(PrintStream out){

		out.print("\"quays_with_different_transport_modes\": [\n");
		String endOfline;

		String[] quaysInErrorArray = quayWithDifferentTransportModes.toArray(new String[quayWithDifferentTransportModes.size()]);

		for (int i = 0; i < quaysInErrorArray.length; i++){
			String quayId = quaysInErrorArray[i];
			endOfline = i == quaysInErrorArray.length - 1 ? "\" }\n" : "\" },\n";
			out.print("{ \"quay_id\": \"" + quayId + "\",\n");

			String quayLineInUseStr =  quayLineUse.get(quayId).stream().collect(Collectors.joining(","));
			out.print(" \"used_in_lines\": \"" + quayLineInUseStr + endOfline);
		}


		out.println("]");
		out.print(",\n");
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


	/**
	 * Read each stopPlace and checks that all children quays have the same transport mode
	 *
	 */
	private void checkStopPlaceTransportModes(){

		buildStopPlaceChildren();

		for (Map.Entry<String, Set<String>> stopPlaceEntry : stopPlaceChildrens.entrySet()) {
			String stopPlaceId = stopPlaceEntry.getKey();


			List<TransportModeNameEnum> transportPortModeList = stopPlaceEntry.getValue().stream()
																				.map(quayTransportMode::get)
																				.distinct()
																				.collect(Collectors.toList());

			if (transportPortModeList.size() > 1){
				//multiple transport modes has been found for a single StopPlace -> error
				stopPlaceWithMultipleTransportModes.add(stopPlaceId);
			}
		}
	}

	/**
	 * Read all quays and build a map to associate a stop place with all its children
	 */
	private void buildStopPlaceChildren() {
		for (StopArea stop : stops) {
			String originalStopId = stop.getOriginalStopId();

			if (stop.getParent() != null){
				StopArea parent = stop.getParent();
				String parentId = parent.getOriginalStopId();

				if (StringUtils.isEmpty(parentId))
					continue;


				//  We store all childrens
				Set<String> childrens;
				if(!stopPlaceChildrens.containsKey(parentId)){
					childrens = new HashSet<>();
					stopPlaceChildrens.put(parentId, childrens);
				}else{
					childrens = stopPlaceChildrens.get(parentId);
				}
				childrens.add(originalStopId);
			}
		}
	}


	@Override
	public void print(PrintStream stream) {
		print(stream, new StringBuilder() , 1, true);

	}
}
