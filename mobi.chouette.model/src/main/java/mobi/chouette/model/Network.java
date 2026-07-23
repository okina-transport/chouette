/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */
package mobi.chouette.model;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mobi.chouette.model.type.PTNetworkSourceTypeEnum;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


/**
 * Chouette Public Transport Network : a set of lines
 * <p/>
 * Neptune mapping : PTNetwork <br/>
 * Gtfs mapping : none
 */
@Entity
@Table(name = "networks")
@Cacheable
@NoArgsConstructor
@ToString(callSuper=true, exclude = { "lines" })
public class Network extends NeptuneIdentifiedObject {

	private static final long serialVersionUID = -8986371268064619423L;

	@Getter
	@Setter
	@SequenceGenerator(name = "networks_id_seq", sequenceName = "networks_id_seq", allocationSize = 10)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "networks_id_seq")
	@Id
	@Column(name = "id", nullable = false)
	protected Long id;
	
	/**
	 * name
	 * 
	 * @return The actual value
	 */
	@Getter
	@Column(name = "name", nullable = false)
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
	 * version date
	 * 
	 * @param versionDate
	 *            New value
	 * @return The actual value
	 */
	@Getter
	@Setter
	@Column(name = "version_date")
	private LocalDate versionDate;

	/**
	 * description
	 * 
	 * @return The actual value
	 */
	@Getter
	@Column(name = "description")
	private String description;

	/**
	 * set description <br/>
	 * truncated to 255 characters if too long
	 * 
	 * @param value
	 *            New value
	 */
	public void setDescription(String value) {
		description = StringUtils.abbreviate(value, 255);
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
	 * source type
	 * 
	 * @param sourceType
	 *            New value
	 * @return The actual value
	 */
	@Getter
	@Setter
	@Enumerated(EnumType.STRING)
	@Column(name = "source_type")
	private PTNetworkSourceTypeEnum sourceType;

	/**
	 * source name
	 * 
	 * @return The actual value
	 */
	@Getter
	@Column(name = "source_name")
	private String sourceName;

	/**
	 * set source name <br/>
	 * truncated to 255 characters if too long
	 * 
	 * @param value
	 *            New value
	 */
	public void setSourceName(String value) {
		sourceName = StringUtils.abbreviate(value, 255);
	}

	/**
	 * source identifier
	 * 
	 * @return The actual value
	 */
	@Getter
	@Column(name = "source_identifier")
	private String sourceIdentifier;

	/**
	 * set source identifier <br/>
	 * truncated to 255 characters if too long
	 * 
	 * @param value
	 *            New value
	 */
	public void setSourceIdentifier(String value) {
		sourceIdentifier = StringUtils.abbreviate(value, 255);

	}

	/**
	 * company reference
	 *
	 * @param company
	 *            New value
	 * @return The actual value
	 */
	@Getter
	@Setter
	@ManyToOne(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST })
	@JoinColumn(name = "company_id")
	private Company company;

	/**
	 * lines
	 * 
	 * @param lines
	 *            New value
	 * @return The actual value
	 */
	@Getter
	@Setter
	@OneToMany(mappedBy = "network")
	private List<Line> lines = new ArrayList<Line>(0);

	/**
	 * translations
	 *
	 * @param translations
	 *            New value
	 * @return The actual value
	 */
	@Getter
	@Setter
	@OneToMany(mappedBy = "network", cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, orphanRemoval = true)
	private List<NetworkTranslation> translations = new ArrayList<>(0);

	/**
	 * Position des réseaux
	 */
	@Getter
	@Setter
	@Column(name = "pos")
	private Integer position;

	@Getter
	@Setter
	private Boolean supprime = false;

}
