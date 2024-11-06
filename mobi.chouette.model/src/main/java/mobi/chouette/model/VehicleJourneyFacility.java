package mobi.chouette.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vehicle_journeys_facilities")
@NoArgsConstructor
@Getter
@Setter
public class VehicleJourneyFacility extends NeptuneIdentifiedObject {

    @Id
    @SequenceGenerator(name = "vehicle_journeys_facilities_id_seq", sequenceName = "vehicle_journeys_facilities_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vehicle_journeys_facilities_id_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    private String description;

    private String provider;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "vehicle_journeys_facilities_key_values", joinColumns = @JoinColumn(name = "vehicle_journeys_facility_id"))
    private List<KeyValue> keyValues = new ArrayList<>(0);

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_journey_id", updatable = false)
    private VehicleJourney vehicleJourney;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VehicleJourneyFacility )) return false;
        return id != null && id.equals(((VehicleJourneyFacility) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
