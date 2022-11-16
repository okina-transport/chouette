package mobi.chouette.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.math.BigDecimal;

/**
 * Class Latitude, Longitude for OSRM map-matching
 */
@Log4j
@Entity
@Table(name = "profile_osrm_lat_lng")
@NoArgsConstructor
@Getter
@Setter
public class LatLngMapMatching {

    /**
     * Id BDD
     */
    @Id
    @SequenceGenerator(name = "PROFILE_OSRM_LAT_LNG_ID_SEQUENCE", sequenceName = "PROFILE_OSRM_LAT_LNG_ID_SEQUENCE", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PROFILE_OSRM_LAT_LNG_ID_SEQUENCE")
    @Column(name = "id")
    private Long id;

    /**
     * Latitude
     */
    @Column(name = "lat")
    private BigDecimal lat;

    /**
     * Longitude
     */
    @Column(name = "lng")
    private BigDecimal lng;

    @Column(name = "position")
    private int position;

    @Column(name = "turn_back")
    private boolean turnBack = false;

    @ManyToOne
    @JoinColumn(name = "stop_point_id")
    private StopPoint stopPoint;

    public LatLngMapMatching(StopPoint stopPoint) {
        this.lat = stopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObject().getLatitude();
        this.lng = stopPoint.getScheduledStopPoint().getContainedInStopAreaRef().getObject().getLongitude();
        this.stopPoint = stopPoint;
    }

    public LatLngMapMatching(String lat, String lng) {
        this.lat = new BigDecimal(lat);
        this.lng = new BigDecimal(lng);
    }
}
