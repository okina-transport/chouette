/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */

package mobi.chouette.exchange.gtfs.exporter.producer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.ObjectIdUtil;
import mobi.chouette.exchange.gtfs.model.GtfsAttribution;
import mobi.chouette.exchange.gtfs.model.exporter.GtfsExporterInterface;
import mobi.chouette.model.Attribution;

/**
 * convert Attribution to Gtfs Attribution
 */
@Log4j
public class GtfsAttributionProducer extends AbstractProducer
{
   public GtfsAttributionProducer(GtfsExporterInterface exporter)
   {
      super(exporter);
   }

   public boolean save(Attribution neptuneObject, Integer nextId, String schemaPrefix, boolean keepOriginal) {
        GtfsAttribution attribution = new GtfsAttribution();

        attribution.setAttributionId(nextId.toString());

        if (neptuneObject.getLine() != null) {
            attribution.setRouteId(ObjectIdUtil.toGtfsId(neptuneObject.getLine().getObjectId(), schemaPrefix, keepOriginal));
        }

        if (neptuneObject.getVehicleJourney() != null) {
            attribution.setTripId(ObjectIdUtil.toGtfsId(neptuneObject.getVehicleJourney().getObjectId(), schemaPrefix, keepOriginal));
        }

        attribution.setOrganizationName(neptuneObject.getOrganisationName());

        if (neptuneObject.getIsProducer() != null && neptuneObject.getIsProducer()) {
            attribution.setIsProducer(1);
        } else {
            attribution.setIsProducer(0);
        }

        if (neptuneObject.getIsAuthority() != null && neptuneObject.getIsAuthority()) {
            attribution.setIsAuthority(1);
        } else {
            attribution.setIsAuthority(0);
        }

        if (neptuneObject.getIsOperator() != null && neptuneObject.getIsOperator()) {
            attribution.setIsOperator(1);
        } else {
            attribution.setIsOperator(0);
        }

        attribution.setAttributionUrl(neptuneObject.getAttributionURL());
        attribution.setAttributionEmail(neptuneObject.getAttributionEmail());
        attribution.setAttributionPhone(neptuneObject.getAttributionPhone());


        try
        {
            getExporter().getAttributionExporter().export(attribution);
        } catch (Exception e) {
            log.error("fail to produce attribution "+e.getClass().getName()+" "+e.getMessage());
            return false;
        }
        return true;
   }
}
