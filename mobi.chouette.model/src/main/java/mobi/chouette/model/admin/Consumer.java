package mobi.chouette.model.admin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Setter
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Consumer {


	@Id
	@SequenceGenerator(name = "CONSUMER_ID_SEQ", sequenceName = "CONSUMER_ID_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CONSUMER_ID_SEQ")
	protected Long id;
	protected String name;
	@Enumerated(EnumType.STRING)
	protected ConsumerType type;
	@Column(name = "s3_url")
	protected String s3Url;
	@Column(name = "service_url")
	protected String serviceUrl;
	protected String login;
	@Column(name = "secret_key")
	protected byte[] secretKey;
	@Transient
	protected String secretKeyNotEncrypted;
	protected byte[] password;
	@Transient
	protected String passwordNotEncrypted;
	protected Integer port;
	@Column(name = "destination_path")
	protected String destinationPath;
	protected boolean notification;

	@Column(name = "dataset_id")
	protected String datasetId;

	@Column(name = "export_date")
	protected String exportDate;

	protected String description;

	@Column(name = "append_description")
	protected boolean appendDescription;


	@ElementCollection
	@CollectionTable(name="notification_url", joinColumns=@JoinColumn(name="consumer_id"))
	@Column(name="url")
	protected List<String> notificationUrls = new ArrayList<>();

}
