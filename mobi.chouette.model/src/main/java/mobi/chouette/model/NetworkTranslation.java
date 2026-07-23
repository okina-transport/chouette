package mobi.chouette.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "network_translations")
@NoArgsConstructor
@ToString(callSuper = true, exclude = "network")
public class NetworkTranslation extends Translation {

	@Getter
	@Setter
	@SequenceGenerator(name = "network_translations_id_seq", sequenceName = "network_translations_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "network_translations_id_seq")
	@Id
	@Column(name = "id", nullable = false)
	protected Long id;

	@Getter
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "network_id")
	private Network network;

	public void setNetwork(Network network) {
		if (this.network != null) {
			this.network.getTranslations().remove(this);
		}
		this.network = network;
		if (network != null) {
			network.getTranslations().add(this);
		}
	}
}
