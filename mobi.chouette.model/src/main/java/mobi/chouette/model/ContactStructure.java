package mobi.chouette.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.*;

@Entity
@Table(name = "contact_structures")
@ToString
@EqualsAndHashCode(callSuper = false)
public class ContactStructure extends NeptuneObject {
	@Getter
	@Setter
	@Column(name = "contact_person")
	private String contactPerson;
	@Getter
	@Setter
	@Column(name = "email")
	private String email;
	@Getter
	@Setter
	@Column(name = "fax")
	private String fax;
	@Getter
	@Setter
	@Column(name = "url")
	private String url;
	@Getter
	@Setter
	@Column(name = "phone")
	private String phone;

	@Getter
	@Setter
	@Column(name = "further_details")
	private String furtherDetails;


	@Getter
	@Setter
	@GenericGenerator(name = "contact_structures_id_seq", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
			@Parameter(name = "sequence_name", value = "contact_structures_id_seq"),
			@Parameter(name = "increment_size", value = "100")})
	@GeneratedValue(generator = "contact_structures_id_seq")
	@Id
	@Column(name = "id", nullable = false)
	protected Long id;

}
