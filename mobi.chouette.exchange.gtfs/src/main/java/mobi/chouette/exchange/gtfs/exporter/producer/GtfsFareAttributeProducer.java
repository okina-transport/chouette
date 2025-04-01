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
import mobi.chouette.model.Company;
import mobi.chouette.model.FareAttribute;
import mobi.chouette.model.type.OrganisationTypeEnum;
import org.apache.commons.lang3.StringUtils;

import static mobi.chouette.common.Constant.COLON_REPLACEMENT_CODE;

@Log4j
public class GtfsFareAttributeProducer extends AbstractProducer {
	GtfsFareAttribute fare = new GtfsFareAttribute();

	public GtfsFareAttributeProducer(GtfsExporterInterface exporter) {
		super(exporter);
	}

	public boolean save(FareAttribute neptuneObject, GtfsExportParameters configuration) {
		String agencyId = configuration.getAgencyId();

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
		if (neptuneObject.getTransferDuration() != null) {
			fare.setTransferDuration(neptuneObject.getTransferDuration());
		}

		Company c = neptuneObject.getCompany();
		if (c != null) {
			if (StringUtils.isEmpty(agencyId)) {
				agencyId = neptuneObject.getCompany().getObjectId();
				fare.setAgencyId(ObjectIdUtil.toGtfsId(agencyId, configuration.getObjectIdPrefix(), configuration.isKeepOriginalId()));
				if (OrganisationTypeEnum.Operator.equals(c.getOrganisationType()) && agencyId.endsWith("o")) {
					fare.setAgencyId(StringUtils.chop(fare.getAgencyId()));
				}
				fare.setAgencyId(fare.getAgencyId().replaceAll(COLON_REPLACEMENT_CODE, ":"));
			} else {
				fare.setAgencyId(agencyId);
			}
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
