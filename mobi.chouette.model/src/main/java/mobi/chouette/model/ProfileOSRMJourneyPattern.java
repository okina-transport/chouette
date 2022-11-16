package mobi.chouette.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * Profile with metadata
 */
@Entity
@NoArgsConstructor
@Table(name = "profile_osrm_journey_pattern")
@Getter
@Setter
public class ProfileOSRMJourneyPattern {

    @Id
    @SequenceGenerator(name = "PROFILE_OSRM_JOURNEY_PATTERN_ID_SEQUENCE", sequenceName = "PROFILE_OSRM_JOURNEY_PATTERN_ID_SEQUENCE", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PROFILE_OSRM_JOURNEY_PATTERN_ID_SEQUENCE")
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

    /**
     * Type of profile used
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "profile")
    private OSRMProfile profile;

    /**
     * Inter-stop description
     */
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "profile_osrm_journey_pattern_id", referencedColumnName = "id")
    private List<ProfileOSRMInterStopJourneyPattern> profileOSRMInterStopJourneyPatternList = new ArrayList<>(0);

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journey_pattern_id")
    private JourneyPattern journeyPattern;

    /**
     * set parent
     *
     * @param journeyPattern
     */
    public void setjourneyPattern(JourneyPattern journeyPattern) {
        if (this.journeyPattern != null) {
            this.journeyPattern.getProfileOSRMJourneyPatterns().remove(this);
        }
        this.journeyPattern = journeyPattern;
        if (journeyPattern != null) {
            journeyPattern.getProfileOSRMJourneyPatterns().add(this);
        }
    }

    /**
     * set parent
     *
     * @param profileOSRMInterStopJourneyPattern
     */
    public void addProfileInterStop(ProfileOSRMInterStopJourneyPattern profileOSRMInterStopJourneyPattern) {
        this.getProfileOSRMInterStopJourneyPatternList().add(profileOSRMInterStopJourneyPattern);
    }

}
