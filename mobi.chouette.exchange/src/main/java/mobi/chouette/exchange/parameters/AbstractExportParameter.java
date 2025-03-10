package mobi.chouette.exchange.parameters;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.log4j.Logger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@ToString(callSuper = true)
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
@XmlType(propOrder = { "referencesType", "ids", "startDate", "endDate", "addMetadata" ,"validateAfterExport", "exportedReferentials", "exportConfigurationId"}, name = "actionExportParameter")
public class AbstractExportParameter extends AbstractParameter {

	@XmlElement(name = "references_type", required = true)
	private String referencesType;

	@XmlElement(name = "reference_ids")
	private List<Long> ids;

	@XmlElement(name = "start_date")
	private Date startDate;

	@XmlElement(name = "end_date")
	private Date endDate;

	@XmlElement(name = "add_metadata", defaultValue = "true")
	private boolean addMetadata = true;

	@XmlElement(name = "validate_after_export", defaultValue = "false")
	private boolean validateAfterExport = true;

	@XmlElement(name = "exported_referentials")
	protected String exportedReferentials;

	@XmlElement(name = "export_configuration_id")
	protected Long exportConfigurationId;

	/**
	 * Return a list with all additional referentials that must be locked for this job to execute. Defaults to empty.
	 */
	public List<String> getAdditionalRequiredReferentialLocks(){
		return new ArrayList<>();
	}

	public boolean isValid(Logger log, String[] allowedTypes) {
		if (!super.isValid(log)) return false;
		
		if (startDate != null && endDate != null && startDate.after(endDate)) {
			log.error("end date before start date ");
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
