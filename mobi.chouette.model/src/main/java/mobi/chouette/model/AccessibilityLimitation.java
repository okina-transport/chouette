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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "accessibility_limitation")
@NoArgsConstructor
@Getter
@Setter
public class AccessibilityLimitation extends NeptuneIdentifiedObject {

	@Id
	@SequenceGenerator(name = "accessibility_limitation_id_seq", sequenceName = "accessibility_limitation_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accessibility_limitation_id_seq")
	@Column(name = "id", nullable = false)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "wheelchair_access")
	protected LimitationStatusEnum wheelchairAccess;

	@Enumerated(EnumType.STRING)
	@Column(name = "step_free_access")
	protected LimitationStatusEnum stepFreeAccess;

	@Enumerated(EnumType.STRING)
	@Column(name = "escalator_free_access")
	protected LimitationStatusEnum escalatorFreeAccess;

	@Enumerated(EnumType.STRING)
	@Column(name = "lift_free_access")
	protected LimitationStatusEnum liftFreeAccess;

	@Enumerated(EnumType.STRING)
	@Column(name = "audible_signals_available")
	protected LimitationStatusEnum audibleSignalsAvailable;

	@Enumerated(EnumType.STRING)
	@Column(name = "visual_signs_available")
	protected LimitationStatusEnum visualSignsAvailable;

}