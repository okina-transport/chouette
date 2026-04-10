/**
 * Projet CHOUETTE
 * <p>
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 */
package mobi.chouette.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mobi.chouette.model.util.ObjectIdTypes;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.Min;

import java.io.Serializable;

@Entity
@Table(name = "transfers")
@NoArgsConstructor
@Cacheable
@Getter
@Setter
@ToString(callSuper = true)
public class Transfers extends NeptuneIdentifiedObject implements ObjectIdTypes {

	@Id
	@SequenceGenerator(name = "transfers_id_seq", sequenceName = "transfers_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "transfers_id_seq")
	@Column(name = "id", nullable = false)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
	@JoinColumn(name = "from_stop_id")
	private StopArea fromStop;

	@OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
	@JoinColumn(name = "to_stop_id")
	private StopArea toStop;

	@OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
	@JoinColumn(name = "from_line_id")
	private Line fromLine;

	@OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
	@JoinColumn(name = "to_line_id")
	private Line toLine;

	@Column(name = "from_trip_id")
	private String fromTripId;

	@Column(name = "to_trip_id")
	private String toTripId;

	@Column(name = "transfer_type")
	@Enumerated(EnumType.STRING)
	private TransferType transferType;

	@Column(name = "min_transfer_time")
	@Min(0)
	private Integer minTransferTime;

	public enum TransferType implements Serializable {
		Recommended, Timed, Minimal, NoAllowed, InSeatAllowed, InSeatNotAllowed
	}
}
