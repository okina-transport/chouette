package mobi.chouette.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.NaturalId;

import javax.persistence.*;
import javax.ws.rs.DefaultValue;

@Entity
@Table(name = "attributions")
@Cacheable
@NoArgsConstructor
public class Attribution {

    @Id
    @Getter
    @Setter
    @SequenceGenerator(name = "attributions_id_seq", sequenceName = "attributions_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "attributions_id_seq")
    @Column(name = "id", nullable = false)
    private Long id;

    @Getter
    @Setter
    @NaturalId(mutable=true)
    @Column(name = "objectid", nullable = false, unique = true)
    protected String objectId;

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id")
    private Agency agency;

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "line_id")
    private Line line;

    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_journey_id")
    private VehicleJourney vehicleJourney;

    @Getter
    @Setter
    @Column(name = "organisation_name")
    protected String organisationName;

    @Getter
    @Setter
    @Column(name = "is_producer")
    private Boolean isProducer;

    @Getter
    @Setter
    @Column(name = "is_operator")
    private Boolean isOperator;

    @Getter
    @Setter
    @Column(name = "is_authority")
    private Boolean isAuthority;

    @Getter
    @Setter
    @Column(name = "attribution_url")
    protected String attributionURL;

    @Getter
    @Setter
    @Column(name = "attribution_email")
    protected String attributionEmail;

    @Getter
    @Setter
    @Column(name = "attribution_phone")
    protected String attributionPhone;


}
