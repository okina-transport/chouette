package mobi.chouette.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.vividsolutions.jts.geom.LineString;
import lombok.extern.log4j.Log4j;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.model.LatLngMapMatching;
import mobi.chouette.model.OSRMProfile;
import mobi.chouette.model.SearchAddress;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.URLConnectionEngine;
import org.wololo.geojson.GeoJSON;
import org.wololo.geojson.Geometry;
import org.wololo.jts2geojson.GeoJSONReader;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
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
    private static final String SEPARATOR_OSRM = "/osrm/";
    private static final String SEPERATOR = "/";
    private static final String TRIP_V1 = "trip/v1/";
    private static final String ROUTE_V1 = "route/v1/";
    private static final String COMMA = ",";
    public static final String DISTANCE = "distance";
    public static final String PROPERTY_OSRM_URL = System.getenv("OSRM_URL");

    private static final String API_ADDRESS = "https://api-adresse.data.gouv.fr/search/?q=";

    @EJB
    StopAreaDAO stopAreaDao;

    /**
     * Fetch request to OTP
     *
     * @param url
     * @return
     */
    public javax.ws.rs.core.Response createRequest(String url) {
        javax.ws.rs.client.Client clientWeb = ClientBuilder.newBuilder()
                .build()
                .register(JacksonJaxbJsonProvider.class)
                .register(JacksonJsonProvider.class);

        return clientWeb.target(PROPERTY_OSRM_URL + StringUtils.substringAfter(url, SEPARATOR_OSRM))
                .request(MediaType.APPLICATION_JSON)
                .buildGet().invoke();
    }

    /**
     * Get GeoJSON from JSON OSRM
     *
     * @param geoJson
     * @return
     */
    public GeoJSON getGeoJSONFromOSRM(JSONObject geoJson) throws Exception {
        GeoJSON featureCollection;
        try {
            featureCollection = new ObjectMapper().readValue(geoJson.toString(), Geometry.class);
        } catch (IOException e) {
            log.error(e);
            String motif = "Unable to transform response to GeoJson.";
            log.error(motif);
            throw new Exception(motif);
        }
        return featureCollection;
    }

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
                points.stream().map(p -> new LatLng(p.getLat(), p.getLng())).collect(Collectors.toList()));
    }

    public JSONObject getPointsRoute(OSRMProfile profile, List<LatLng> points) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("alternatives", "false");
        params.put("steps", "true");
        params.put("annotations", "true");
        params.put("geometries", "geojson");
        params.put("overview", "full");
        return getRoute(profile, points, params);
    }

    public JSONObject getTrip(OSRMProfile profile, List<LatLng> points) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("roundtrip", "false");
        params.put("source", "first");
        params.put("destination", "last");
        params.put("steps", "true");
        params.put("geometries", "geojson");
        params.put("overview", "full");
        return getTrip(profile, points, params);
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


    private JSONObject getTrip(OSRMProfile profile, List<LatLng> points, Map<String, String> params) throws Exception {

        Response response = callOsrm(profile, points, params, TRIP_V1);

        try {
            return new JSONObject(response.readEntity(String.class));
        } catch (JSONException e) {
            log.error("GetTrip() - Response to JSON - " + e.getMessage());
            throw new Exception("Unable to understand the trace received");
        }
    }

    private Response callOsrm(OSRMProfile profile, List<LatLng> points, Map<String, String> params, String tripV1) throws Exception {
        javax.ws.rs.client.Client clientWeb = new ResteasyClientBuilder().httpEngine(new URLConnectionEngine())
                .build()
                .register(JacksonJaxbJsonProvider.class)
                .register(JacksonJsonProvider.class);

        javax.ws.rs.core.Response response = clientWeb.target(PROPERTY_OSRM_URL + tripV1 + profile + SEPERATOR + getPoints(points) + EXTENSION_JSON + computeQueryParams(params))
                .request(MediaType.APPLICATION_JSON)
                .buildGet().invoke();

        if (response.getStatus() != 200) {
            response.close();
            throw new Exception("Communication problem with the OSRM.");
        }

        return response;
    }

    private JSONObject getRoute(OSRMProfile profile, List<LatLng> points, Map<String, String> params) throws Exception {
        Response response = callOsrm(profile, points, params, ROUTE_V1);

        try {
            return new JSONObject(response.readEntity(String.class));
        } catch (JSONException e) {
            log.error("GetRoute() - Response to JSON - " + e.getMessage(), e);
            throw new Exception("Unable to understand the proposed route");
        }
    }

    public SearchAddress searchAddressAndStopAreas(String address) throws Exception {
        SearchAddress searchAddress = searchAddress(address);
        searchAddress.getFeatures().addAll(stopAreaDao.findByNamePatternSearchAddressFeatures(address));
        return searchAddress;
    }

    public SearchAddress searchAddress(String address) throws Exception {
        javax.ws.rs.client.Client clientWeb = ClientBuilder.newBuilder()
                .build()
                .register(JacksonJaxbJsonProvider.class)
                .register(JacksonObjectMapper.class)
                .register(JacksonJsonProvider.class);

        javax.ws.rs.core.Response response;
        try {
            response = clientWeb.target(API_ADDRESS + URLEncoder.encode(address, "UTF-8"))
                    .request(MediaType.APPLICATION_JSON)
                    .buildGet().invoke();
        } catch (UnsupportedEncodingException e) {
            log.error(e);
            throw new Exception("Unable to convert for search.");
        }

        if (response.getStatus() != 200) {
            response.close();
            throw new Exception("Unable to search.");
        }
        return response.readEntity(SearchAddress.class);
    }

    public double transformDistanceBetweenTwoPointsToNumber(JSONObject response){
        try {
            String distance = response.getString(DISTANCE);
            double factor = Math.pow(10, 1);
            return Math.round((Double.parseDouble(distance) / 1000) * factor) / factor;
        } catch (JSONException e) {
            return 0;
        }
    }

    public JSONObject getDistanceBetweenTwoPoints(String from, String to) throws Exception {
        List<LatLngMapMatching> points = new ArrayList<>(0);
        // Regexp to match gps coordinates
        Pattern pattern = Pattern.compile("^(\\-?\\d+(\\.\\d+)?),\\s*(\\-?\\d+(\\.\\d+)?)$");
        if (!pattern.matcher(from).matches()) {
            SearchAddress fromResult = searchAddress(from);
            if (fromResult == null || fromResult.getFeatures().isEmpty()) {
                throw new Exception("The starting stop is not understandable.");
            }
            points.add(new LatLngMapMatching(fromResult.getFeatures().get(0).getGeometry().getCoordinates()[1], fromResult.getFeatures().get(0).getGeometry().getCoordinates()[0]));
        } else {
            points.add(new LatLngMapMatching(from.split(COMMA)[0], from.split(COMMA)[1]));
        }

        if (!pattern.matcher(to).matches()) {
            SearchAddress toResult = searchAddress(to);
            if (toResult == null || toResult.getFeatures().isEmpty()) {
                throw new Exception("The end stop is not understandable.");
            }
            points.add(new LatLngMapMatching(toResult.getFeatures().get(0).getGeometry().getCoordinates()[1], toResult.getFeatures().get(0).getGeometry().getCoordinates()[0]));
        } else {
            points.add(new LatLngMapMatching(to.split(COMMA)[0], to.split(COMMA)[1]));
        }

        try {
            return getRoute(OSRMProfile.DRIVING, points).getJSONArray(ROUTES).getJSONObject(0);
        } catch (JSONException e) {
            throw new Exception("Unable to read response from OSRM");
        }
    }

}
