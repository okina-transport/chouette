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
import mobi.chouette.exchange.gtfs.model.GtfsTripCompany;
import mobi.chouette.exchange.gtfs.model.exporter.GtfsExporterInterface;
import mobi.chouette.model.TripCompany;

@Log4j
public class GtfsTripCompanyProducer extends AbstractProducer {

	public GtfsTripCompanyProducer(GtfsExporterInterface exporter) {
		super(exporter);
	}

	public boolean save(TripCompany neptuneObject, GtfsExportParameters configuration) {
		GtfsTripCompany company = new GtfsTripCompany();

		company.setCompanyId(ObjectIdUtil.toGtfsId(neptuneObject.getObjectId(), configuration.getObjectIdPrefix(), configuration.isKeepOriginalId()));
		company.setCompanyName(neptuneObject.getName());
		company.setCompanyAddress(neptuneObject.getAddress());
		company.setCompanyZipcode(neptuneObject.getZipcode());
		company.setCompanyCity(neptuneObject.getCity());
		company.setCompanyPhone(neptuneObject.getPhone());
		company.setCompanyEmail(neptuneObject.getEmail());

		try {
			getExporter().getTripCompanyExporter().export(company);
		} catch (Exception e) {
			log.error("fail to produce trip company " + e.getClass().getName() + " " + e.getMessage(), e);
			return false;
		}
		return true;
	}
}
