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

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * Base fields shared by every owner-specific translation entity (NetworkTranslation, CompanyTranslation,
 * LineTranslation, StopAreaTranslation, VehicleJourneyTranslation, VehicleJourneyAtStopTranslation).
 * Each concrete subclass owns its own id/sequence and a single nullable @ManyToOne foreign key to the
 * entity it translates; that FK is null for legacy GTFS translations.txt rows keyed by field_value
 * instead of record_id (matched dynamically by raw field text at export/publication time).
 */
@MappedSuperclass
@NoArgsConstructor
@ToString(callSuper = true)
public abstract class Translation extends NeptuneIdentifiedObject implements ObjectIdTypes {

    /**
     * Chouette object field name (e.g. stopName, name), mapped from the GTFS translations.txt
     * field_name (e.g. stop_name, route_long_name, trip_headsign)
     */
    @Getter
    @Setter
    @Column(name = "field_name")
    private String fieldName;

    @Getter
    @Setter
    @Column(name = "language")
    private String language;

    @Getter
    @Setter
    @Column(name = "translation")
    private String translation;

    /**
     * GTFS translations.txt field_value: raw value of the translated field, used as the lookup key
     * instead of a resolved owner reference when the source row has no record_id (legacy field_value matching)
     */
    @Getter
    @Setter
    @Column(name = "field_value")
    private String fieldValue;

    @Getter
    @Setter
    private transient String targetObjectId;
}
