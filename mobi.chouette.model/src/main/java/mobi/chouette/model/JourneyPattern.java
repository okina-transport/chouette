package mobi.chouette.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.ws.rs.DefaultValue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiLineString;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mobi.chouette.model.type.SectionStatusEnum;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.wololo.geojson.GeoJSON;
import org.wololo.jts2geojson.GeoJSONWriter;

/**
 * Chouette Journey Pattern : pattern for vehicle journeys in a route
 * <p/>
 * Neptune mapping : JourneyPattern <br/>
 * Gtfs mapping : none
 * 
 */
@Entity
@Table(name = "journey_patterns")
@NoArgsConstructor
@ToString(callSuper=true, exclude = { "route" })
public class JourneyPattern extends NeptuneIdentifiedObject {
	private static final long serialVersionUID = 7895941111990419404L;

	@Getter
	@Setter
	@GenericGenerator(name = "journey_patterns_id_seq", strategy = "mobi.chouette.persistence.hibernate.ChouetteIdentifierGenerator", 
		parameters = {
			@Parameter(name = "sequence_name", value = "journey_patterns_id_seq"),
			@Parameter(name = "increment_size", value = "20") })
	@GeneratedValue(generator = "journey_patterns_id_seq")
	@Id
	@Column(name = "id", nullable = false)
	protected Long id;
	
	/**
	 * name
	 * 
	 * @return The actual value
	 */
	@Getter
	@Column(name = "name")
	private String name;

	/**
	 * set name <br/>
	 * truncated to 255 characters if too long
	 * 
	 * @param value
	 *            New value
	 */
	public void setName(String value) {
		name = StringUtils.abbreviate(value, 255);
	}

	/**
	 * comment
	 * 
	 * @return The actual value
	 */
	@Getter
	@Column(name = "comment")
	private String comment;

	/**
	 * set comment <br/>
	 * truncated to 255 characters if too long
	 * 
	 * @param value
	 *            New value
	 */
	public void setComment(String value) {
		comment = StringUtils.abbreviate(value, 255);
	}

	/**
	 * registration number
	 * 
	 * @return The actual value
	 */
	@Getter
	@Column(name = "registration_number")
	private String registrationNumber;

	/**
	 * set registration number <br/>
	 * truncated to 255 characters if too long
	 * 
	 * @param value
	 *            New value
	 */
	public void setRegistrationNumber(String value) {
		registrationNumber = StringUtils.abbreviate(value, 255);
	}


	/**
	 * Le trajet a-t-il été suppérimé via l'interface ?
	 * On le désactive en fait seulement.
	 */
	@Getter
	@Setter
	@DefaultValue("false")
	private Boolean supprime = false;

	/**
	 * published name
	 * 
	 * @return The actual value
	 */
	@Getter
	@Column(name = "published_name")
	private String publishedName;

	/**
	 * set published name <br/>
	 * truncated to 255 characters if too long
	 * 
	 * @param value
	 *            New value
	 */
	public void setPublishedName(String value) {
		publishedName = StringUtils.abbreviate(value, 255);
	}

	
	/**
	 * Section status
	 * 
	 * @param sectionStatus
	 *            New value
	 * @return The actual value
	 * @since 3.2.0
	 */
	@Getter
	@Setter
	@Enumerated(EnumType.ORDINAL)
	@Column(name = "section_status")
	private SectionStatusEnum sectionStatus = SectionStatusEnum.Todo;
	
	/**
	 * route reverse reference
	 * 
	 * @return The actual value
	 */
	@Getter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "route_id")
	private Route route;

	/**
	 * set route reverse reference
	 * 
	 * @param route
	 */
	public void setRoute(Route route) {
		if (this.route != null) {
			this.route.getJourneyPatterns().remove(this);
		}
		this.route = route;
		if (route != null) {
			route.getJourneyPatterns().add(this);
		}
	}

	/**
	 * first stop
	 * 
	 * @param departureStopPoint
	 *            New value
	 * @return The actual value
	 */
	@Getter
	@Setter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "departure_stop_point_id")
	private StopPoint departureStopPoint;

	/**
	 * last stop
	 * 
	 * @param arrivalStopPoint
	 *            New value
	 * @return The actual value
	 */
	@Getter
	@Setter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "arrival_stop_point_id")
	private StopPoint arrivalStopPoint;

	/**
	 * stop list
	 * 
	 * @param stopPoints
	 *            New value
	 * @return The actual value
	 */
	@Getter
	@Setter
	@ManyToMany
	@OrderBy(value="position")
	@JoinTable(name = "journey_patterns_stop_points", joinColumns = { @JoinColumn(name = "journey_pattern_id", nullable = false, updatable = false) }, inverseJoinColumns = { @JoinColumn(name = "stop_point_id", nullable = false, updatable = false) })
	private List<StopPoint> stopPoints = new ArrayList<StopPoint>(0);

	/**
	 * vehicle journeys
	 * 
	 * @param vehicleJourneys
	 *            New value
	 * @return The actual value
	 */
	@Getter
	@Setter
	@OneToMany(mappedBy = "journeyPattern", cascade = { CascadeType.PERSIST})
	private List<VehicleJourney> vehicleJourneys = new ArrayList<VehicleJourney>(
			0);

	/**
	 * route sections
	 * 
	 * @param routeSections
	 *            New value
	 * @return The actual value
	 * @since 3.2.0
	 * 
	 */
	@Getter
	@Setter
	@ManyToMany(cascade = { CascadeType.PERSIST }, fetch = FetchType.LAZY)
	@OrderColumn(name="rank")
	@JoinTable(name = "journey_pattern_sections", joinColumns = { @JoinColumn(name = "journey_pattern_id", nullable = false, updatable = false) }, inverseJoinColumns = { @JoinColumn(name = "route_section_id", nullable = false, updatable = false) })
	private List<RouteSection> routeSections = new ArrayList<RouteSection>(0);
	
	/**
	 * add a stop point if not already present
	 * 
	 * @param stopPoint
	 */
	public void addStopPoint(StopPoint stopPoint) {
		if (!stopPoints.contains(stopPoint)) {
			stopPoints.add(stopPoint);
		}
	}

	/**
	 * remove a stop point
	 * 
	 * @param stopPoint
	 */
	public void removeStopPoint(StopPoint stopPoint) {
		if (stopPoints.contains(stopPoint)) {
			stopPoints.remove(stopPoint);
		}
	}
	
	/**
	 * footnotes refs
	 * 
	 * @param footnotes
	 *            New value
	 * @return The actual value
	 */
	@Getter
	@Setter
	@ManyToMany( cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinTable(name = "footnotes_journey_patterns", joinColumns = { @JoinColumn(name = "journey_pattern_id") }, inverseJoinColumns = { @JoinColumn(name = "footnote_id") })
	private List<Footnote> footnotes = new ArrayList<>(0);

	/**
	 * Returns true if JourneyPattern has correct no of RouteSections and all of them have geometry defined.
	 */
	public boolean hasCompleteRouteSections() {
		if (getRouteSections().size() == (getStopPoints().size() - 1)) {
			return getRouteSections().stream().map(RouteSection::getInputGeometry).allMatch(Objects::nonNull);
		}
		return false;
	}

	/**
	 * GeoJSON attached to the path.
	 */
	@Getter
	private String geojson;

	public void setGeojson(GeoJSON json) {
		if (json != null) {
			geojson = json.toString();
		}
	}

	@JsonIgnore
	public GeoJSON getGeoJSONFromRouteSections() {
		List<com.vividsolutions.jts.geom.LineString> listSections = this.getRouteSections().stream()
				.map(RouteSection::getProcessedGeometry)
				.collect(Collectors.toList());

		GeometryFactory geometryFactory = new GeometryFactory();
		MultiLineString mls = new MultiLineString(GeometryFactory.toLineStringArray(listSections), geometryFactory);

		GeoJSONWriter writer = new GeoJSONWriter();
		return writer.write(mls);
	}

	/**
	 * Positions to generate map-matching with OSRM
	 */
	@Getter
	@Setter
	@OneToMany(cascade = {CascadeType.ALL})
	@OrderBy("position ASC")
	@JoinColumn(name = "journey_pattern_id", referencedColumnName = "id")
	private List<LatLngMapMatching> latLngMapMatching = new ArrayList<>(0);


	/**
	 * List of trip-related profiles
	 *
	 * @param ProfileOSRMJourneyPattern
	 * New value
	 * @return list of profiles
	 */
	@Getter
	@Setter
	@OneToMany(mappedBy = "journeyPattern")
	@JsonIgnore
	private List<ProfileOSRMJourneyPattern> profileOSRMJourneyPatterns = new ArrayList<>(0);

	/**
	 * keyvalues
	 *
	 * @param keyvalue
	 *            New value
	 * @return The actual value
	 */
	@Getter
	@Setter
	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "journey_patterns_key_values", joinColumns = @JoinColumn(name = "journey_pattern_id"))
	private List<KeyValue> keyValues = new ArrayList<>(0);


	/**
	 * Current destination display for this journey pattern
	 *
	 * @param destinationDisplay
	 * New value
	 */
	@Getter
	@Setter
	@ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
	@JoinColumn(name = "destination_display_id")
	private DestinationDisplay destinationDisplay;

}
