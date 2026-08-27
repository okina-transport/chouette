package mobi.chouette.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.List;

public class FakeOSRMService {
    private static final double toRad = 0.017453292519943;
    private static final double EARTH_RADIUS = 6371008.8;

    public static OsrmRouteResponse getStraightRoute(List<LatLng> points) {
        LatLng firstPoint = points.get(0);
        LatLng lastPoint = points.get(points.size() - 1);

        OsrmRoute route = new OsrmRoute();
        route.setGeometry(buildLineStringGeometry(points));
        route.setDistance(calculateDistanceInMeters(
                Double.parseDouble(firstPoint.getLng()), Double.parseDouble(firstPoint.getLat()),
                Double.parseDouble(lastPoint.getLng()), Double.parseDouble(lastPoint.getLat())));
        route.setDuration(0);

        OsrmRouteResponse response = new OsrmRouteResponse();
        response.setRoutes(Collections.singletonList(route));
        return response;
    }

    private static ObjectNode buildLineStringGeometry(List<LatLng> points) {
        ArrayNode coordinates = JsonNodeFactory.instance.arrayNode();
        for (LatLng point : points) {
            coordinates.addArray()
                    .add(Double.parseDouble(point.getLng()))
                    .add(Double.parseDouble(point.getLat()));
        }

        ObjectNode geometry = JsonNodeFactory.instance.objectNode();
        geometry.set("coordinates", coordinates);
        geometry.put("type", "LineString");
        return geometry;
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
