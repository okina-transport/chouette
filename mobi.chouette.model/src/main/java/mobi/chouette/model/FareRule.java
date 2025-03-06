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

@Entity
@Table(name = "fare_rules")
@NoArgsConstructor
@Cacheable
@Getter
@Setter
@ToString(callSuper = true)
public class FareRule extends NeptuneIdentifiedObject implements ObjectIdTypes {
    @Id
    @GenericGenerator(name = "fare_rule_id_seq", strategy = "mobi.chouette.persistence.hibernate.ChouetteIdentifierGenerator",
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "sequence_name", value = "fare_rule_id_seq"),
                    @org.hibernate.annotations.Parameter(name = "increment_size", value = "1")})
    @GeneratedValue(generator = "fare_rule_id_seq")
    @Column(name = "id", nullable = false)
    protected Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;

    @Column(name = "origin_id")
    private Integer originId;

    @Column(name = "destination_id")
    private Integer destinationId;

    @Column(name = "contains_id")
    private Integer containsId;
}
