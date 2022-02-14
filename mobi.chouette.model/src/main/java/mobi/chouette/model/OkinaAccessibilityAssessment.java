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
public class OkinaAccessibilityAssessment implements Serializable {

	@Id
	@SequenceGenerator(name = "accessibility_assessment_id_seq", sequenceName = "accessibility_assessment_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "accessibility_assessment_id_seq")
	@Column(name = "id", nullable = false)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "mobility_impaired_access")
	protected LimitationStatusEnumeration mobilityImpairedAccess = LimitationStatusEnumeration.UNKNOWN;

	@OneToOne
	@JoinColumn(name = "accessibility_limitation_id", referencedColumnName = "id")
	protected OkinaAccessibilityLimitation limitations;

	@OneToOne(mappedBy = "accessibilityAssessment")
	private Line line;

}