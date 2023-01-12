package mobi.chouette.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;


/**
 * Represent the last time a referential was updated through dataset import.
 */
@Entity
@Table(name = "referential_last_update")
public class ReferentialLastUpdate {

    @Id
    @Column(name = "id")
    protected long id;

    @Getter
    @Setter
    @Column(name = "last_update_timestamp")
    protected LocalDateTime lastUpdateTimestamp;


}
