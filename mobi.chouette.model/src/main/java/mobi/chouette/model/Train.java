package mobi.chouette.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.util.List;

/**
 * Chouette Train: TODO
 */
@Entity
@Table(name = "trains")
@Cacheable
@NoArgsConstructor
@ToString
@Getter
@Setter
public class Train extends NeptuneIdentifiedObject {

    @Id
    @SequenceGenerator(name = "train_id_seq", sequenceName = "train_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "train_id_seq")
    @Column(nullable = false)
    private Long id;

    @Column(name = "published_name", nullable = false)
    private String publishedName;

    @Column
    private String description;

    @Column
    private String version;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "trains")
    private List<VehicleJourney> vehicleJourneys;

}
