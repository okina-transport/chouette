package mobi.chouette.exchange.parameters;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import mobi.chouette.model.type.StopAreaImportModeEnum;
import mobi.chouette.model.type.TransportModeNameEnum;

import org.apache.log4j.Logger;

@NoArgsConstructor
@ToString
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "noSave", "cleanMode", "stopAreaRemoteIdMapping", "stopAreaImportMode", "keepObsoleteLines", "generateMapMatching",
		"generateMissingRouteSectionsForModes","keepBoardingAlighting", "keepStopGeolocalisation", "keepStopNames" }, name = "actionImportParameter")
public class AbstractImportParameter extends AbstractParameter {

	@XmlElement(name = "no_save", defaultValue = "false")
	@Getter
	@Setter
	private boolean noSave = false;

	@XmlElement(name = "clean_mode", required=true)
	@Getter
	@Setter
	private String cleanMode;

	/**
	 * Whether or not stop area ids from import files should be mapped against remote stop area registry (ie NSR).
	 *
	 */
	@XmlElement(name = "stop_area_remote_id_mapping", defaultValue = "true")
	@Getter
	@Setter
	private boolean stopAreaRemoteIdMapping = true;

	/**
	 * How stop areas in import file should be treated by chouette.
	 */
	@XmlElement(name = "stop_area_import_mode", defaultValue = "true")
	@Getter
	@Setter
	private StopAreaImportModeEnum stopAreaImportMode = StopAreaImportModeEnum.CREATE_NEW;

	@XmlElement(name = "keep_obsolete_lines", defaultValue = "false")
	@Getter
	@Setter
	private boolean keepObsoleteLines = true;

	@XmlElement(name = "generate_missing_route_sections_for_modes")
	@Getter
	@Setter
	private Set<TransportModeNameEnum> generateMissingRouteSectionsForModes = new HashSet<>();


	@XmlElement(name = "keep_boarding_alighting", defaultValue = "true")
	@Getter
	@Setter
	private boolean keepBoardingAlighting = true;

	@XmlElement(name = "keep_stop_geolocalisation", defaultValue = "true")
	@Getter
	@Setter
	private boolean keepStopGeolocalisation = false;

	@XmlElement(name = "keep_stop_names", defaultValue = "true")
	@Getter
	@Setter
	private boolean keepStopNames = true;

	@XmlElement(name = "generate_map_matching", defaultValue = "false")
	@Getter
	@Setter
	private boolean generateMapMatching = true;

	public boolean isValid(Logger log) {
		return super.isValid(log);
	}

}
