package mobi.chouette.exchange.gtfs.importer;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mobi.chouette.exchange.parameters.AbstractImportParameter;
import org.apache.log4j.Logger;

import javax.xml.bind.annotation.*;
import java.util.Arrays;

@XmlRootElement(name = "gtfs-import")
@NoArgsConstructor
@ToString(callSuper=true)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder={
		"objectIdPrefix",
		"splitIdOnDot",
		"maxDistanceForConnectionLink",
		"maxDistanceForCommercial",
		"ignoreEndChars",
		"ignoreLastWord",
		"referencesType",
		"parseInterchanges",
		"parseConnectionLinks",
		"routeMerge",
		"splitCharacter",
		"commercialPointIdPrefixToRemove",
		"quayIdPrefixToRemove",
		"linePrefixToRemove",
		"removeParentStations",
		"importShapesFile",
		"updateStopAccessibility",
		"railUICprocessing",
		"routeSortOrder",
		"useTargetNetwork",
		"targetNetwork",
		"renameRoutesAfterMerge",
})
@Getter
@Setter
public class GtfsImportParameters extends AbstractImportParameter {

	@XmlElement(name = "object_id_prefix", required=true)
	private String objectIdPrefix;

	@XmlElement(name = "split_id_on_dot", defaultValue="true")
	private boolean splitIdOnDot = true;

	@XmlElement(name = "max_distance_for_connection_link", defaultValue="0")
	private int maxDistanceForConnectionLink = 0;

	@XmlElement(name = "max_distance_for_commercial", defaultValue="0")
	private int maxDistanceForCommercial = 0;

	@XmlElement(name = "ignore_end_chars", defaultValue="0")
	private int ignoreEndChars = 0;

	@XmlElement(name = "ignore_last_word", defaultValue="false")
	private boolean ignoreLastWord = false;

	@XmlElement(name = "references_type")
	private String referencesType;

	@XmlElement(name = "parse_interchanges")
	private boolean parseInterchanges = false;

	@XmlElement(name = "parse_connection_links")
	private boolean parseConnectionLinks = true;

	@XmlElement(name = "route_merge")
	public Boolean routeMerge = false;

	@XmlElement(name = "split_character")
	public String splitCharacter = "";

	@XmlElement(name = "commercial_point_prefix_to_remove")
	public String commercialPointIdPrefixToRemove = "";

	@XmlElement(name = "quay_id_prefix_to_remove")
	public String quayIdPrefixToRemove = "";

	@XmlElement(name = "line_prefix_to_remove")
	public String linePrefixToRemove = "";

	@XmlElement(name = "remove_parent_stations", defaultValue="false")
	private boolean removeParentStations = false;

	@XmlElement(name = "import_shapes_file", defaultValue = "true")
	private boolean importShapesFile = true;

	@XmlElement(name = "update_stop_accessibility", defaultValue = "false")
	private boolean updateStopAccessibility = false;

	@XmlElement(name = "rail_uic_processing", defaultValue = "false")
	private boolean railUICprocessing = false;

	@XmlElement(name = "route_sort_order", defaultValue = "false")
	private boolean routeSortOrder = false;

	@XmlElement(name = "use_target_network", defaultValue = "false")
	private boolean useTargetNetwork = false;

	@XmlElement(name = "target_network")
	private String targetNetwork = "";

	@XmlElement(name = "rename_routes_after_merge", defaultValue = "false")
	private boolean renameRoutesAfterMerge = false;

	public boolean isValid(Logger log, String[] allowedTypes)
	{
		if (!super.isValid(log)) return false;
		
		if (objectIdPrefix == null || objectIdPrefix.isEmpty()) {
			log.error("missing object_id_prefix");
			return false;
		}

		if (referencesType != null && !referencesType.isEmpty()) {
			if (!Arrays.asList(allowedTypes).contains(referencesType.toLowerCase())) {
				log.error("invalid type " + referencesType);
				return false;
			}
		}
		return true;

	}
}
