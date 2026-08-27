package mobi.chouette.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * One leg (inter-stop segment) of an OSRM route.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OsrmLeg {
    private double distance;
    private double duration;
}
