package mobi.chouette.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Profile with metadata
 */
@Entity
@NoArgsConstructor
@Table(name = "profile_osrm_inter_stop_journey_pattern")
@Getter
@Setter
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, scope = ProfileOSRMJourneyPattern.class, property = "id")
public class ProfileOSRMInterStopJourneyPattern {

    @Id
    @SequenceGenerator(name = "PROFILE_OSRM_INTER_STOP_JOURNEY_PATTERN_ID_SEQUENCE", sequenceName = "PROFILE_OSRM_INTER_STOP_JOURNEY_PATTERN_ID_SEQUENCE", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PROFILE_OSRM_INTER_STOP_JOURNEY_PATTERN_ID_SEQUENCE")
    @Column(name = "id")
    private Long id;

    /**
     * Distance in meters
     */
    @Column(name = "distance")
    private double distance;

    /**
     * Duration in seconds
     */
    @Column(name = "duration")
    private double duration;

    @ManyToOne
    @JoinColumn(name = "departure_stop_point_id")
    private StopPoint departureStopPoint;

    @ManyToOne
    @JoinColumn(name = "arrival_stop_point_id")
    private StopPoint arrivalStopPoint;

}

