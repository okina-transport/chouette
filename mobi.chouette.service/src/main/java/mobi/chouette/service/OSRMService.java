package mobi.chouette.service;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.vividsolutions.jts.geom.LineString;
import lombok.extern.log4j.Log4j;
import mobi.chouette.model.LatLngMapMatching;
import mobi.chouette.model.OSRMProfile;
import org.wololo.jts2geojson.GeoJSONReader;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * OSRM Request
 */
@Stateless
@LocalBean
@Log4j
public class OSRMService {

    public static final String EXTENSION_JSON = ".json";
    public static final String PROPERTY_OSRM_URL = System.getenv("OSRM_URL");
    private static final String SEPERATOR = "/";
    private static final String ROUTE_V1 = "route/v1/";

    /** Built once per bean instance and reused for every OSRM call instead of one per request. */
    private Client client;

    @PostConstruct
    private void init() {
        client = ClientBuilder.newBuilder()
                .build()
                .register(JacksonJaxbJsonProvider.class)
                .register(JacksonJsonProvider.class);
    }

    @PreDestroy
    private void destroy() {
        client.close();
    }

    /**
     * Get LineString from a GeoJSON geometry node of an OSRM route
     *
     * @param geometry
     * @return
     */
    public LineString getLineStringFromOSRM(com.fasterxml.jackson.databind.JsonNode geometry) {
        GeoJSONReader geoJSONReader = new GeoJSONReader();
        return (LineString) geoJSONReader.read(geometry.toString());
    }

    public OsrmRouteResponse getRoute(OSRMProfile profile, List<LatLngMapMatching> points) throws Exception {
        return getPointsRoute(profile,
                points.stream()
                      .map(p -> new LatLng(p.getLat(), p.getLng()))
                      .toList());
    }

    public OsrmRouteResponse getPointsRoute(OSRMProfile profile, List<LatLng> points) throws Exception {
        if (OSRMProfile.AIR.equals(profile) || OSRMProfile.FERRY.equals(profile) || OSRMProfile.METRO.equals(profile)) {
            return FakeOSRMService.getStraightRoute(points);
        }

        Map<String, String> params = new HashMap<>();
        params.put("alternatives", "false");
        params.put("steps", "true");
        params.put("annotations", "true");
        params.put("geometries", "geojson");
        params.put("overview", "full");

        try {
            return callOsrm(profile, points, params);
        } catch (ProcessingException e) {
            log.error("GetRoute() - Response to JSON - " + e.getMessage(), e);
            throw new OsrmCommunicationException("Unable to understand the proposed route", e);
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

    private OsrmRouteResponse callOsrm(OSRMProfile profile, List<LatLng> points, Map<String, String> params) throws OsrmCommunicationException {
        String uri = PROPERTY_OSRM_URL + ROUTE_V1 + profile + SEPERATOR + getPoints(points) + EXTENSION_JSON + computeQueryParams(params);

        try (Response response = client.target(uri)
                .request(MediaType.APPLICATION_JSON)
                .buildGet().invoke()) {

            if (response.getStatus() != 200) {
                throw new OsrmCommunicationException("Communication problem with the OSRM.");
            }

            return response.readEntity(OsrmRouteResponse.class);
        }
    }
}
