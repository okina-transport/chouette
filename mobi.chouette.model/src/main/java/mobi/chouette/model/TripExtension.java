package mobi.chouette.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "trip_extensions")
@NoArgsConstructor
@ToString(callSuper = true, exclude = { "vehicleJourney" })
public class TripExtension extends NeptuneIdentifiedObject {

	private static final long serialVersionUID = 1L;

	@Getter
	@Setter
	@SequenceGenerator(name = "trip_extensions_id_seq", sequenceName = "trip_extensions_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trip_extensions_id_seq")
	@Id
	@Column(name = "id", nullable = false)
	protected Long id;

	@Getter
	@Setter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vehicle_journey_id")
	private VehicleJourney vehicleJourney;

	@Getter
	@Setter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "line_id")
	private Line line;

	@Getter
	@Setter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "contract_company_id")
	private TripCompany contractCompany;

	@Getter
	@Setter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "exec_company_id")
	private TripCompany execCompany;

	@Getter
	@Setter
	@Column(name = "indic_reservation")
	private String indicReservation;

}
