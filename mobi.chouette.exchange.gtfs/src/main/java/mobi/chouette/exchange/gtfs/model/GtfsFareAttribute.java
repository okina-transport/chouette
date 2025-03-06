package mobi.chouette.exchange.gtfs.model;

import lombok.*;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.io.Serializable;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GtfsFareAttribute extends GtfsObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fareId;

    private Float price;

    private String currencyType;

    @Enumerated(EnumType.STRING)
    private PaymentMethodType paymentMethod;

    @Enumerated(EnumType.STRING)
    private AttributeTransfersType transfers;

    private String agencyId;

    private Float transferDuration;

    public void clear() {
        fareId = null;
        price = null;
        currencyType = null;
        paymentMethod = null;
        transfers = null;
        agencyId = null;
        transferDuration = null;
    }

    public enum PaymentMethodType implements Serializable {
        OnBoard, BeforeBoarding
    }

    public enum AttributeTransfersType implements Serializable {
        NoTransfers, One, Two
    }
}
