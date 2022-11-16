package mobi.chouette.service;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.math.BigDecimal;

/**
 * Class Latitude, Longitude
 */
@Getter
@Setter
@NoArgsConstructor
public class LatLng {
    private String lat;
    private String lng;

    public LatLng(BigDecimal lat, BigDecimal lng){
        this.lat = lat.toString();
        this.lng = lng.toString();
    }
}

