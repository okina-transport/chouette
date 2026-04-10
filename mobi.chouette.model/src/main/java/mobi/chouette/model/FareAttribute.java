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
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "fare_attributes")
@NoArgsConstructor
@Cacheable
@Getter
@Setter
@ToString(callSuper = true)
public class FareAttribute extends NeptuneIdentifiedObject implements ObjectIdTypes {
	@Id
	@SequenceGenerator(name = "fare_attribute_id_seq", sequenceName = "fare_attribute_id_seq", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fare_attribute_id_seq")
	@Column(name = "id", nullable = false)
	protected Long id;

	private Float price;

	@Column(name = "currency_type")
	private String currencyType;

	@Column(name = "payment_method")
	@Enumerated(EnumType.STRING)
	private PaymentMethodType paymentMethod;

	@Enumerated(EnumType.STRING)
	private AttributeTransfersType transfers;

	@ManyToOne(cascade = {CascadeType.PERSIST})
	@JoinColumn(name = "company_id")
	private Company company;

	@Column(name = "transfer_duration")
	private Float transferDuration;

	public enum PaymentMethodType implements Serializable {
		OnBoard, BeforeBoarding
	}

	public enum AttributeTransfersType implements Serializable {
		NoTransfers, One, Two
	}
}
