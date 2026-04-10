package mobi.chouette.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "storage")
@NoArgsConstructor
public class Storage {

    @Id
    @SequenceGenerator(name = "storage_id_seq", sequenceName = "storage_id_seq", allocationSize = 20)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "storage_id_seq")
    @Getter
    @Setter
    private Long id;

    @Column(name = "stored_at", nullable = false)
    @Getter
    @Setter
    private Instant storedAt;

    @Column(name = "restored_at")
    @Getter
    @Setter
    private Instant restoredAt;

}
