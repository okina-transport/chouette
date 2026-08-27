package mobi.chouette.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * One candidate route of an OSRM "route" response.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OsrmRoute {
    private double distance;
    private double duration;
    /** GeoJSON geometry of the route, consumed by {@link OSRMService#getLineStringFromOSRM}. */
    private JsonNode geometry;
    private List<OsrmLeg> legs;
}
