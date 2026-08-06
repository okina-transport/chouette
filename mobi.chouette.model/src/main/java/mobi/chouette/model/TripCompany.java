package mobi.chouette.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "trip_companies")
@NoArgsConstructor
@ToString(callSuper = true)
public class TripCompany extends NeptuneIdentifiedObject {

	private static final long serialVersionUID = 1L;

	@Getter
	@Setter
	@SequenceGenerator(name = "trip_companies_id_seq", sequenceName = "trip_companies_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "trip_companies_id_seq")
	@Id
	@Column(name = "id", nullable = false)
	protected Long id;

	@Getter
	@Column(name = "name")
	private String name;

	public void setName(String value) {
		name = StringUtils.abbreviate(value, 255);
	}

	@Getter
	@Column(name = "address")
	private String address;

	public void setAddress(String value) {
		address = StringUtils.abbreviate(value, 255);
	}

	@Getter
	@Column(name = "zipcode")
	private String zipcode;

	public void setZipcode(String value) {
		zipcode = StringUtils.abbreviate(value, 255);
	}

	@Getter
	@Column(name = "city")
	private String city;

	public void setCity(String value) {
		city = StringUtils.abbreviate(value, 255);
	}

	@Getter
	@Column(name = "phone")
	private String phone;

	public void setPhone(String value) {
		phone = StringUtils.abbreviate(value, 255);
	}

	@Getter
	@Column(name = "email")
	private String email;

	public void setEmail(String value) {
		email = StringUtils.abbreviate(value, 255);
	}

}
