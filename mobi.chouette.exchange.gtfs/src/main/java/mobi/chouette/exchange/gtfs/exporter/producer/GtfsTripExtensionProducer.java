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
import mobi.chouette.exchange.gtfs.model.GtfsTripExtension;
import mobi.chouette.exchange.gtfs.model.exporter.GtfsExporterInterface;
import mobi.chouette.exchange.gtfs.parameters.IdParameters;
import mobi.chouette.model.Line;
import mobi.chouette.model.TripCompany;
import mobi.chouette.model.TripExtension;
import mobi.chouette.model.VehicleJourney;

@Log4j
public class GtfsTripExtensionProducer extends AbstractProducer {

	public GtfsTripExtensionProducer(GtfsExporterInterface exporter) {
		super(exporter);
	}

	public boolean save(TripExtension neptuneObject, GtfsExportParameters configuration) {
		VehicleJourney vehicleJourney = neptuneObject.getVehicleJourney();
		if (vehicleJourney == null) {
			return true;
		}

		String prefix = configuration.getObjectIdPrefix();
		boolean keepOriginalId = configuration.isKeepOriginalId();
		IdParameters idParams = new IdParameters(configuration.getStopIdPrefix(), configuration.getIdFormat(), configuration.getIdSuffix(), configuration.getLineIdPrefix(), configuration.getCommercialPointIdPrefix());

		GtfsTripExtension tripExtension = new GtfsTripExtension();
		tripExtension.setTripId(ObjectIdUtil.toGtfsId(vehicleJourney.getObjectId(), prefix, keepOriginalId));

		Line line = neptuneObject.getLine();
		if (line != null) {
			tripExtension.setRouteId(generateCustomRouteId(ObjectIdUtil.toGtfsId(line.getObjectId(), prefix, keepOriginalId), idParams));
		}

		TripCompany contractCompany = neptuneObject.getContractCompany();
		if (contractCompany != null) {
			tripExtension.setContractCompanyId(contractCompany.getOriginalCompanyId());
		}

		TripCompany execCompany = neptuneObject.getExecCompany();
		if (execCompany != null) {
			tripExtension.setExecCompanyId(execCompany.getOriginalCompanyId());
		}

		tripExtension.setIndicReservation(neptuneObject.getIndicReservation());

		try {
			getExporter().getTripExtensionExporter().export(tripExtension);
		} catch (Exception e) {
			log.error("fail to produce trip extension " + e.getClass().getName() + " " + e.getMessage(), e);
			return false;
		}
		return true;
	}
}
