package mobi.chouette.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static mobi.chouette.common.TimeUtil.toLocalDate;


@Entity
@Table(name = "blocks")
@NoArgsConstructor
@ToString(callSuper = true, exclude = {"vehicleJourneys"})
public class Block extends NeptuneIdentifiedObject {

    @Getter
    @Setter
    @GenericGenerator(name = "blocks_id_seq", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
            @Parameter(name = "sequence_name", value = "blocks_id_seq"),
            @Parameter(name = "increment_size", value = "100")})
    @GeneratedValue(generator = "blocks_id_seq")
    @Id
    @Column(name = "id", nullable = false)
    protected Long id;


    /**
     * Name of the block.
     */
    @Getter
    @Column(name = "name")
    private String name;

    /**
     * set name code <br/>
     * truncated to 255 characters if too long
     *
     * @param value New value
     */
    public void setName(String value) {
        name = StringUtils.abbreviate(value, 255);
    }

    /**
     * Identification of block, not intended for the public.
     */
    @Getter
    @Column(name = "private_code")
    private String privateCode;

    /**
     * set private code <br/>
     * truncated to 255 characters if too long
     *
     * @param value New value
     */
    public void setPrivateCode(String value) {
        privateCode = StringUtils.abbreviate(value, 255);

    }

    /**
     * Description of the block.
     */
    @Getter
    @Column(name = "description")
    private String description;

    /**
     * set description code <br/>
     * truncated to 255 characters if too long
     *
     * @param value New value
     */
    public void setDescription(String value) {
        description = StringUtils.abbreviate(value, 255);

    }

    /**
     * Start time of the block.
     */
    @Getter
    @Setter
    @Column(name = "start_time")
    private LocalTime startTime;

    /**
     * End time of the block.
     */
    @Getter
    @Setter
    @Column(name = "end_time")
    private LocalTime endTime;

    /**
     * End time offset of the block.
     */
    @Getter
    @Setter
    @Column(name = "end_time_day_offset")
    private Integer endTimeDayOffset;


    /**
     * Start point of the block.
     */
    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_point_id")
    private ScheduledStopPoint startPoint;

    /**
     * End point of the block.
     */
    @Getter
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_point_id")
    private ScheduledStopPoint endPoint;

    /**
     * timetables
     */
    @Getter
    @Setter
    @ManyToMany(cascade = {CascadeType.PERSIST}, fetch = FetchType.LAZY)
    @JoinTable(name = "time_tables_blocks", joinColumns = {@JoinColumn(name = "block_id", nullable = false, updatable = false)}, inverseJoinColumns = {@JoinColumn(name = "time_table_id", nullable = false, updatable = false)})
    private List<Timetable> timetables = new ArrayList<>(0);

    /**
     * Vehicle Journeys.
     */
    @Getter
    @Setter
    @ManyToMany(cascade = {CascadeType.PERSIST}, fetch = FetchType.EAGER)
    @OrderColumn(name = "position")
    @JoinTable(name = "blocks_vehicle_journeys", joinColumns = {@JoinColumn(name = "block_id")}, inverseJoinColumns = {@JoinColumn(name = "vehicle_journey_id")})
    private List<VehicleJourney> vehicleJourneys = new ArrayList<>();

    public void addVehicleJourney(VehicleJourney vehicleJourney) {
        if (vehicleJourney != null) {
            vehicleJourney.getBlocks().add(this);
            vehicleJourneys.add(vehicleJourney);
        }
    }

    public void removeVehicleJourney(VehicleJourney vehicleJourney) {
        if (vehicleJourney != null) {
            vehicleJourney.getBlocks().remove(this);
            vehicleJourneys.remove(vehicleJourney);
        }
    }

    /**
     * Dead Runs.
     */
    @Getter
    @Setter
    @ManyToMany(cascade = {CascadeType.PERSIST}, fetch = FetchType.EAGER)
    @OrderColumn(name = "position")
    @JoinTable(name = "blocks_dead_runs", joinColumns = {@JoinColumn(name = "block_id")}, inverseJoinColumns = {@JoinColumn(name = "dead_run_id")})
    private List<DeadRun> deadRuns = new ArrayList<>();

    public void addDeadRun(DeadRun deadRun) {
        if (deadRun != null) {
            deadRun.getBlocks().add(this);
            deadRuns.add(deadRun);
        }
    }

    public void removeDeadRun(DeadRun deadRun) {
        if (deadRun != null) {
            deadRun.getBlocks().remove(this);
            deadRuns.remove(deadRun);
        }
    }


    public boolean hasActiveTimetablesOnPeriod(LocalDate startDate, LocalDate endDate) {
        return getTimetables().stream().anyMatch(t -> t.isActiveOnPeriod(startDate, endDate));
    }

    public boolean filter(Date startDate, Date endDate) {
        return hasActiveTimetablesOnPeriod(toLocalDate(startDate), toLocalDate(endDate));
    }



}
