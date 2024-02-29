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
import mobi.chouette.model.util.LimitationStatusEnum;

import javax.persistence.*;
import java.util.List;


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

	@OneToMany(mappedBy = "accessibilityAssessment")
	private List<VehicleJourney> vehicleJourney;
}