package mobi.chouette.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "variations")
@NoArgsConstructor
public class Variations extends NeptuneObject {

    @Getter
    @Setter
    @SequenceGenerator(name = "variations_id_seq", sequenceName = "variations_id_seq", allocationSize = 10)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "variations_id_seq")
    @Id
    @Column(name = "id", nullable = false)
    protected Long id;

    /**
     * type
     *
     * @return The actual value
     */
    @Getter
    @Setter
    @Column(name = "typev")
    private String type;

    /**
     * description
     *
     * @return The actual value
     */
    @Getter
    @Setter
    @Column(name = "descriptionv")
    private String description;

    /**
     * job
     *
     * @return The actual value
     */
    @Getter
    @Setter
    @Column(name = "jobv")
    private Long job;
}



