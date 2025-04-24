package mobi.chouette.exchange.parameters;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mobi.chouette.model.type.StopAreaImportModeEnum;
import mobi.chouette.model.type.TransportModeNameEnum;
import org.apache.log4j.Logger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@ToString
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {
        "noSave",
        "cleanMode",
        "description",
        "stopAreaRemoteIdMapping",
        "stopAreaImportMode",
        "keepObsoleteLines",
        "generateMissingRouteSectionsForModes",
        "keepBoardingAlighting",
        "keepStopGeolocalisation",
        "keepStopNames",
        "generateMapMatching",
        "distanceGeolocation",
        "routesReorganization",
        "objectIdPrefix",
        "useTargetNetwork",
        "targetNetwork",
        "recomputeStopPlacesLocation",
}, name = "actionImportParameter")
@Getter
@Setter
public class AbstractImportParameter extends AbstractParameter {

    @XmlElement(name = "no_save", defaultValue = "false")
    private boolean noSave = false;

    @XmlElement(name = "clean_mode", required = true)
    private String cleanMode;

    @XmlElement(name = "description", required = true)
    private String description;

    /**
     * Whether or not stop area ids from import files should be mapped against remote stop area registry (ie NSR).
     */
    @XmlElement(name = "stop_area_remote_id_mapping", defaultValue = "true")
    private boolean stopAreaRemoteIdMapping = true;

    /**
     * How stop areas in import file should be treated by chouette.
     */
    @XmlElement(name = "stop_area_import_mode", defaultValue = "true")
    private StopAreaImportModeEnum stopAreaImportMode = StopAreaImportModeEnum.CREATE_NEW;

    @XmlElement(name = "keep_obsolete_lines", defaultValue = "false")
    private boolean keepObsoleteLines = true;

    @XmlElement(name = "generate_missing_route_sections_for_modes")
    private Set<TransportModeNameEnum> generateMissingRouteSectionsForModes = new HashSet<>();

    @XmlElement(name = "keep_boarding_alighting", defaultValue = "true")
    private boolean keepBoardingAlighting = true;

    @XmlElement(name = "keep_stop_geolocalisation", defaultValue = "true")
    private boolean keepStopGeolocalisation = false;

    @XmlElement(name = "keep_stop_names", defaultValue = "true")
    private boolean keepStopNames = true;

    @XmlElement(name = "generate_map_matching", defaultValue = "NONE")
    private ImportGenerateMapMatching generateMapMatching = ImportGenerateMapMatching.NONE;

    @XmlElement(name = "distance_geolocation")
    private Long distanceGeolocation = 200L;

    @XmlElement(name = "routes_reorganization", defaultValue = "false")
    private boolean routesReorganization = false;

    @XmlElement(name = "object_id_prefix", required = true)
    private String objectIdPrefix;

    @XmlElement(name = "use_target_network", defaultValue = "false")
    private boolean useTargetNetwork = false;

    @XmlElement(name = "target_network")
    private String targetNetwork = "";

    @XmlElement(name = "recompute_stop_places_location", defaultValue = "true")
    private boolean recomputeStopPlacesLocation = true;

    public boolean isValid(Logger log) {
        return super.isValid(log);
    }

}
