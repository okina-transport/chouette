package mobi.chouette.exchange.gtfs.model;

import lombok.*;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.Min;
import java.io.Serializable;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GtfsTransfer extends GtfsObject implements Serializable {

	private static final long serialVersionUID = 1L;

	private String fromStopId;

	private String toStopId;

	private String fromRouteId;

	private String toRouteId;

	private String fromTripId;

	private String toTripId;

	@Enumerated(EnumType.STRING)
	private TransfersTypeEnum transferType;

	@Min(0)
	private Integer minTransferTime;

	public void clear() {
		fromStopId = null;
		toStopId = null;
		fromRouteId = null;
		toRouteId = null;
		fromTripId = null;
		toTripId = null;
		transferType = null;
		minTransferTime = null;
	}

	public enum TransfersTypeEnum implements Serializable {
		Recommended(0),    // Point de transfert Recommandé
		Timed(1),          // Point de transfert chronométré
		Minimal(2),        // Transfert nécessite un minimum de temps
		NoAllowed(3),      // Transferts ne sont pas possibles
		InSeatAllowed(4),  // Transfert au siège autorisé
		InSeatNotAllowed(5); // Transfert au siège non autorisé

		private final int value;

		TransfersTypeEnum(int value) {
			this.value = value;
		}

		public static TransfersTypeEnum fromValue(int value) {
			for (TransfersTypeEnum type : TransfersTypeEnum.values()) {
				if (type.getValue() == value) {
					return type;
				}
			}
			throw new IllegalArgumentException("Valeur non valide pour TransferType : " + value);
		}

		public int getValue() {
			return value;
		}
	}
}
