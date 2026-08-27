package mobi.chouette.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Response of the OSRM "route" service (only the "routes" array is used by Chouette).
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OsrmRouteResponse {
    private List<OsrmRoute> routes;

    public OsrmRoute getFirstRoute() {
        return routes.get(0);
    }
}
