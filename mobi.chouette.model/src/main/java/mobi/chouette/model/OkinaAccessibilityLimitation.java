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
import org.rutebanken.netex.model.LimitationStatusEnumeration;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "accessibility_limitation")
@NoArgsConstructor
@Getter
@Setter
public class OkinaAccessibilityLimitation implements Serializable {

	@Id
	@SequenceGenerator(name = "accessibility_limitation_id_seq", sequenceName = "accessibility_limitation_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accessibility_limitation_id_seq")
	@Column(name = "id", nullable = false)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "wheelchair_access")
	protected LimitationStatusEnumeration wheelchairAccess;

	@Enumerated(EnumType.STRING)
	@Column(name = "step_free_access")
	protected LimitationStatusEnumeration stepFreeAccess;

	@Enumerated(EnumType.STRING)
	@Column(name = "escalator_free_access")
	protected LimitationStatusEnumeration escalatorFreeAccess;

	@Enumerated(EnumType.STRING)
	@Column(name = "lift_free_access")
	protected LimitationStatusEnumeration liftFreeAccess;

	@Enumerated(EnumType.STRING)
	@Column(name = "audible_signals_available")
	protected LimitationStatusEnumeration audibleSignalsAvailable;

	@Enumerated(EnumType.STRING)
	@Column(name = "visual_signs_available")
	protected LimitationStatusEnumeration visualSignsAvailable;

}