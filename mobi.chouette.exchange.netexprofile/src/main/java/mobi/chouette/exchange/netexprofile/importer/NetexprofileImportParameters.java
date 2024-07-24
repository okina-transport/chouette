package mobi.chouette.exchange.netexprofile.importer;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mobi.chouette.exchange.parameters.AbstractImportParameter;

import javax.xml.bind.annotation.*;

@XmlRootElement(name = "netexprofile-import")
@NoArgsConstructor
@ToString(callSuper = true)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {
		"parseSiteFrames",
		"validateAgainstSchema",
		"validateAgainstProfile",
		"continueOnLineErrors",
		"cleanOnErrors",
		"objectIdPrefix",
		"importMode",
		"netexImportLayouts",
		"netexImportColors",
		"useTargetNetwork",
		"targetNetwork",
})
@Getter
@Setter
public class NetexprofileImportParameters extends AbstractImportParameter {

	@XmlElement(name = "parse_site_frames")
	private boolean parseSiteFrames = true;

	@XmlElement(name = "validate_against_schema")
	private boolean validateAgainstSchema = true;

	@XmlElement(name = "validate_against_profile")
	private boolean validateAgainstProfile = true;

	@XmlElement(name = "continue_on_line_errors")
	private boolean continueOnLineErrors = false;

	@XmlElement(name = "clean_on_error")
	private boolean cleanOnErrors = false;
	
	@XmlElement(name = "object_id_prefix", required=true)
	private String objectIdPrefix;

	@XmlElement(name = "import_mode", required=true)
	private String importMode;

	@XmlElement(name = "netex_import_layouts", defaultValue = "false")
	private boolean netexImportLayouts = false;

	@XmlElement(name = "netex_import_colors", defaultValue = "false")
	private boolean netexImportColors = false;

	@XmlElement(name = "use_target_network")
	private boolean useTargetNetwork;

	@XmlElement(name = "target_network")
	private String targetNetwork;

}
