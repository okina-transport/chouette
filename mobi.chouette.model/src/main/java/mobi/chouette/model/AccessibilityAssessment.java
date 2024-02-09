/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */
package mobi.chouette.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import mobi.chouette.model.type.LimitationStatusEnum;

import javax.persistence.CascadeType;
import javax.persistence.Column;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;

import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import java.io.Serializable;


@Entity
@Table(name = "accessibility_assessment")
@NoArgsConstructor
@Getter
@Setter
public class AccessibilityAssessment extends NeptuneIdentifiedObject {

	@Id
	@SequenceGenerator(name = "accessibility_assessment_id_seq", sequenceName = "accessibility_assessment_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accessibility_assessment_id_seq")
	@Column(name = "id", nullable = false)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "mobility_impaired_access")
	protected LimitationStatusEnum mobilityImpairedAccess = LimitationStatusEnum.UNKNOWN;

	@OneToOne(cascade = { CascadeType.ALL})
	@JoinColumn(name = "accessibility_limitation_id", referencedColumnName = "id")
	protected AccessibilityLimitation accessibilityLimitation;

	@OneToOne(mappedBy = "accessibilityAssessment")
	private Line line;

	@OneToOne(mappedBy = "accessibilityAssessment")
	private VehicleJourney vehicleJourney;
}