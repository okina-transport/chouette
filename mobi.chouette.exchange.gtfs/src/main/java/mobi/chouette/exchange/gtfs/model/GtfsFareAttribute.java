package mobi.chouette.exchange.gtfs.model;

import lombok.*;

import javax.validation.constraints.Min;
import java.io.Serializable;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class GtfsFareAttribute extends GtfsObject implements Serializable {

    private static final long serialVersionUID = 1L;

    @Getter
    @Setter
    private String fareId;

    @Getter
    @Setter
    @Min(0)
    private Integer price;

    @Getter
    @Setter
    private String currencyType;

    @Getter
    @Setter
    private PaymentMethodType paymentMethod;

    @Getter
    @Setter
    private AttributeTransfersType transfers;

    @Getter
    @Setter
    private String agencyId;

    @Getter
    @Setter
    @Min(0)
    private Integer transferDuration;

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
