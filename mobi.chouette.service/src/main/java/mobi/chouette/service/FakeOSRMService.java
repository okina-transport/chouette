package mobi.chouette.service;

import lombok.extern.log4j.Log4j;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import java.util.List;

@Log4j
public class FakeOSRMService {
    private static final double toRad = 0.017453292519943;
    private static final double EARTH_RADIUS = 6371008.8;

    public static JSONObject getStraightRoute(List<LatLng> points) {

        StringBuilder jsonString = new StringBuilder();

        try {

            jsonString.append("{ \n");
            jsonString.append(buildCodeJson());
            jsonString.append(",");
            jsonString.append(buildRoutesJson(points));
            jsonString.append("}");

            return  new JSONObject(jsonString.toString());
        } catch (JSONException e) {
            log.error("Error while building straight route",e);
            return new JSONObject();
        }
    }

    private static String buildRoutesJson(List<LatLng> points) {
        StringBuilder routesObjectJson = new StringBuilder();
        routesObjectJson.append(" \"routes\": [\n {");
        routesObjectJson.append(buildGeometryJson(points));
        routesObjectJson.append(",");
        routesObjectJson.append(buildDistanceJson(points));
        routesObjectJson.append(",");
        routesObjectJson.append(buildDurationJson());

        routesObjectJson.append("} ]");
        return routesObjectJson.toString();

    }

    private static String buildDurationJson() {
        StringBuilder durationObjectJson = new StringBuilder();
        durationObjectJson.append("\"duration\": 0");
        return durationObjectJson.toString();

    }

    private static String buildDistanceJson(List<LatLng> points) {

        LatLng firstPoint = points.get(0);
        LatLng lastPoint = points.get(points.size() - 1);

        StringBuilder distanceObjectJson = new StringBuilder();
        distanceObjectJson.append( " \"distance\": ");
        distanceObjectJson.append(calculateDistanceInMeters(Double.valueOf(firstPoint.getLng()), Double.valueOf(firstPoint.getLat()), Double.valueOf(lastPoint.getLng()), Double.valueOf(lastPoint.getLng())));
        return distanceObjectJson.toString();
    }

    private static String buildGeometryJson(List<LatLng> points) {
        StringBuilder geometryObjectJson = new StringBuilder();
        geometryObjectJson.append("  \"geometry\": {\n");
        geometryObjectJson.append(buildCoordinatesJson(points));
        geometryObjectJson.append(",");
        geometryObjectJson.append(" \"type\": \"LineString\" }");
        return geometryObjectJson.toString();
    }

    private static String buildCoordinatesJson(List<LatLng> points) {
        StringBuilder coordinatesObjectJson = new StringBuilder();
        coordinatesObjectJson.append( " \"coordinates\": [");

        int index = 0;

        for (LatLng point : points) {
            if (index != 0){
                coordinatesObjectJson.append(",");
            }

            coordinatesObjectJson.append("[");
            coordinatesObjectJson.append(point.getLng());
            coordinatesObjectJson.append(",");
            coordinatesObjectJson.append(point.getLat());
            coordinatesObjectJson.append("]");
            index++;
        }

        coordinatesObjectJson.append(" ]");
        return coordinatesObjectJson.toString();
    }


    private static String buildCodeJson() {
        StringBuilder codeObjectJson = new StringBuilder();
        codeObjectJson.append(" \"code\" : \"Ok\" ");
        return codeObjectJson.toString();
    }

    /**
     * lifted from computeHaversineFormula in RouteSectionCheckPoints
     *
     * @see http://mathforum.org/library/drmath/view/51879.html
     */
    public static double calculateDistanceInMeters(double lon1, double lat1, double lon2, double lat2) {

        double lon1AsRad = lon1 * toRad;
        double lat1AsRad = lat1 * toRad;
        double lon2AsRad = lon2 * toRad;
        double lat2AsRad = lat2 * toRad;


        double dlon = Math.sin((lon2AsRad - lon1AsRad) / 2);
        double dlat = Math.sin((lat2AsRad - lat1AsRad) / 2);
        double a = (dlat * dlat) + Math.cos(lat1AsRad) * Math.cos(lat2AsRad)
                * (dlon * dlon);
        double c = 2. * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = EARTH_RADIUS * c;
        return d;
    }
}
