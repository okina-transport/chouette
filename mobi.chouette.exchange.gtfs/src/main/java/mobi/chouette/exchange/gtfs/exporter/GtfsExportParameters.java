package mobi.chouette.exchange.gtfs.exporter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mobi.chouette.exchange.gtfs.parameters.AttributionsExportModes;
import mobi.chouette.exchange.gtfs.parameters.IdFormat;
import mobi.chouette.exchange.parameters.AbstractExportParameter;
import org.apache.log4j.Logger;

import javax.xml.bind.annotation.*;

@XmlRootElement(name = "gtfs-export")
@NoArgsConstructor
@ToString(callSuper = true)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"objectIdPrefix", "timeZone", "keepOriginalId", "useExtendedGtfsRouteTypes", "exportedFileName", "stopIdPrefix", "lineIdPrefix", "idFormat", "idSuffix", "commercialPointIdPrefix", "mappingLinesIds", "commercialPointExport", "attributionsExportMode", "googleMapsCompatibility", "agencyId", "agencyName", "agencyTimezone", "agencyURL", "agencyLang", "fareFiles", "serviceJourneyTripObjectName"})
@Getter
@Setter
public class GtfsExportParameters extends AbstractExportParameter {

	@XmlElement(name = "time_zone", required = true)
	private String timeZone;

	@XmlElement(name = "object_id_prefix", required = true)
	private String objectIdPrefix;

	@XmlElement(name = "keep_original_id")
	private boolean keepOriginalId = false;

	@XmlElement(name = "use_extended_gtfs_route_types")
	private boolean useExtendedGtfsRouteTypes = false;

	@XmlElement(name = "exported_filename")
	private String exportedFileName;

	@XmlElement(name = "stop_id_prefix")
	private String stopIdPrefix;

	@XmlElement(name = "line_id_prefix")
	private String lineIdPrefix;

	@XmlElement(name = "id_format")
	private IdFormat idFormat;

	@XmlElement(name = "id_suffix")
	private String idSuffix;

	@XmlElement(name = "commercial_point_id_prefix")
	private String commercialPointIdPrefix;

	@XmlElement(name = "commercial_point_export")
	private Boolean commercialPointExport;

	@XmlElement(name = "google_maps_compatibility")
	private Boolean googleMapsCompatibility;

	@XmlElement(name = "mapping_lines_ids")
	private boolean mappingLinesIds = false;

	@XmlElement(name = "agency_id")
	private String agencyId;

	@XmlElement(name = "agency_name")
	private String agencyName;

	@XmlElement(name = "agency_timezone")
	private String agencyTimezone;

	@XmlElement(name = "agency_url")
	private String agencyURL;

	@XmlElement(name = "agency_lang")
	private String agencyLang;

	@XmlElement(name = "attributions_export_mode")
	private AttributionsExportModes attributionsExportMode = AttributionsExportModes.NONE;

	@XmlElement(name = "fare_files")
	private boolean fareFiles = false;

	@XmlElement(name = "service_journey_trip_object_name")
	private boolean serviceJourneyTripObjectName = false;


	public boolean isValid(Logger log, String[] allowedTypes) {
		if (!super.isValid(log, allowedTypes))
			return false;

		if (timeZone == null || timeZone.isEmpty()) {
			log.error("missing time_zone");
			return false;
		}

		return true;

	}
}
