package mobi.chouette.exchange.gtfs.model.importer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class GtfsRouteNamePosition {
    private int routeShortNameIndex;
    private int routeLongNameIndex;
}
