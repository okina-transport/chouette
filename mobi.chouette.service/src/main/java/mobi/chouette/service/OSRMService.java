package mobi.chouette.service;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.vividsolutions.jts.geom.LineString;
import lombok.extern.log4j.Log4j;
import mobi.chouette.model.LatLngMapMatching;
import mobi.chouette.model.OSRMProfile;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.wololo.jts2geojson.GeoJSONReader;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.stream.Collectors;


/**
 * OSRM Request
 */
@Stateless
@LocalBean
@Log4j
public class OSRMService {

    public static final String EXTENSION_JSON = ".json";
    public static final String ROUTES = "routes";
    public static final String GEOMETRY = "geometry";
    public static final String PROPERTY_OSRM_URL = System.getenv("OSRM_URL");
    private static final String SEPERATOR = "/";
    private static final String ROUTE_V1 = "route/v1/";

    /**
     * Get LineString from JSON OSRM
     *
     * @param geoJson
     * @return
     */
    public LineString getLineStringFromOSRM(JSONObject geoJson) {
        LineString featureCollection;
        GeoJSONReader geoJSONReader = new GeoJSONReader();

        featureCollection = (LineString) geoJSONReader.read(geoJson.toString());

        return featureCollection;
    }

    public JSONObject getRoute(OSRMProfile profile, List<LatLngMapMatching> points) throws Exception {
        return getPointsRoute(profile,
                points.stream()
                      .map(p -> new LatLng(p.getLat(), p.getLng()))
                      .toList());
    }

    public JSONObject getPointsRoute(OSRMProfile profile, List<LatLng> points) throws Exception {
        if (OSRMProfile.AIR.equals(profile) || OSRMProfile.FERRY.equals(profile) || OSRMProfile.METRO.equals(profile)) {
            return FakeOSRMService.getStraightRoute(points);
        } else {
            Map<String, String> params = new HashMap<>();
            params.put("alternatives", "false");
            params.put("steps", "true");
            params.put("annotations", "true");
            params.put("geometries", "geojson");
            params.put("overview", "full");
            return getRoute(profile, points, params);
        }
    }

    private String getPoints(List<LatLng> points) {
        StringBuilder stringBuilder = new StringBuilder();
        points.forEach(point -> stringBuilder.append(point.getLng()).append(",").append(point.getLat()).append(";"));
        return stringBuilder.deleteCharAt(stringBuilder.toString().length() - 1).toString();
    }

    String computeQueryParams(Map<String, String> params) {
        if (params.isEmpty()) return "";
        return "?" + params.entrySet()
                .stream()
                .filter(Objects::nonNull)
                .map(entry -> entry.getKey() + "=" + (entry.getValue() != null ? entry.getValue() : ""))
                .collect(Collectors.joining("&"));
    }

    private String callOsrm(OSRMProfile profile, List<LatLng> points, Map<String, String> params, String tripV1) throws Exception {
        javax.ws.rs.client.Client clientWeb = ClientBuilder.newBuilder()
                .build()
                .register(JacksonJaxbJsonProvider.class)
                .register(JacksonJsonProvider.class);

        try {
            javax.ws.rs.core.Response response = clientWeb.target(PROPERTY_OSRM_URL + tripV1 + profile + SEPERATOR + getPoints(points) + EXTENSION_JSON + computeQueryParams(params))
                    .request(MediaType.APPLICATION_JSON)
                    .buildGet().invoke();

            if (response.getStatus() != 200) {
                response.close();
                throw new OsrmCommunicationException("Communication problem with the OSRM.");
            }

            return response.readEntity(String.class);
        } finally {
            clientWeb.close();
        }
    }

    private JSONObject getRoute(OSRMProfile profile, List<LatLng> points, Map<String, String> params) throws Exception {
        String body = callOsrm(profile, points, params, ROUTE_V1);

        try {
            return new JSONObject(body);
        } catch (JSONException e) {
            log.error("GetRoute() - Response to JSON - " + e.getMessage(), e);
            throw new OsrmCommunicationException("Unable to understand the proposed route");
        }
    }
}
