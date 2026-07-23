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
@Table(name = "vehicle_journey_translations")
@NoArgsConstructor
@ToString(callSuper = true, exclude = "vehicleJourney")
public class VehicleJourneyTranslation extends Translation {

	@Getter
	@Setter
	@SequenceGenerator(name = "vehicle_journey_translations_id_seq", sequenceName = "vehicle_journey_translations_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vehicle_journey_translations_id_seq")
	@Id
	@Column(name = "id", nullable = false)
	protected Long id;

	@Getter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vehicle_journey_id")
	private VehicleJourney vehicleJourney;

	public void setVehicleJourney(VehicleJourney vehicleJourney) {
		if (this.vehicleJourney != null) {
			this.vehicleJourney.getTranslations().remove(this);
		}
		this.vehicleJourney = vehicleJourney;
		if (vehicleJourney != null) {
			vehicleJourney.getTranslations().add(this);
		}
	}
}
