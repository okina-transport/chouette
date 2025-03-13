/**
 * Projet CHOUETTE
 * <p>
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 */

package mobi.chouette.exchange.gtfs.exporter.producer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.ObjectIdUtil;
import mobi.chouette.exchange.gtfs.exporter.GtfsExportParameters;
import mobi.chouette.exchange.gtfs.model.GtfsFareAttribute;
import mobi.chouette.exchange.gtfs.model.exporter.GtfsExporterInterface;
import mobi.chouette.exchange.gtfs.parameters.IdFormat;
import mobi.chouette.model.FareAttribute;

@Log4j
public class GtfsFareAttributeProducer extends AbstractProducer {
	GtfsFareAttribute fare = new GtfsFareAttribute();

	public GtfsFareAttributeProducer(GtfsExporterInterface exporter) {
		super(exporter);
	}

	public boolean save(FareAttribute neptuneObject, GtfsExportParameters configuration) {
		fare.setFareId(ObjectIdUtil.toGtfsId(neptuneObject.getObjectId(), configuration.getObjectIdPrefix(), IdFormat.TRIDENT.equals(configuration.getIdFormat())));
		if (neptuneObject.getPrice() != null) {
			fare.setPrice(neptuneObject.getPrice());
		}
		if (neptuneObject.getCurrencyType() != null) {
			fare.setCurrencyType(neptuneObject.getCurrencyType());
		}
		if (neptuneObject.getPaymentMethod() != null) {
			fare.setPaymentMethod(GtfsFareAttribute.PaymentMethodType.valueOf(String.valueOf(neptuneObject.getPaymentMethod())));
		}
		if (neptuneObject.getTransfers() != null) {
			fare.setTransfers(GtfsFareAttribute.AttributeTransfersType.valueOf(String.valueOf(neptuneObject.getTransfers())));
		}
		if (neptuneObject.getAgency() != null) {
			fare.setAgencyId(neptuneObject.getAgency().getAgencyId());
		}
		if (neptuneObject.getTransferDuration() != null) {
			fare.setTransferDuration(neptuneObject.getTransferDuration());
		}
		try {
			getExporter().getFareAttributeExporter().export(fare);
		} catch (Exception e) {
			log.error("fail to produce fare attributes " + e.getClass().getName() + " " + e.getMessage(), e);
			return false;
		}
		return true;
	}

}
