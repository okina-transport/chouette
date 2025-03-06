/**
 * Projet CHOUETTE
 * <p>
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 */

package mobi.chouette.exchange.gtfs.exporter.producer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.exchange.gtfs.model.GtfsFareRule;
import mobi.chouette.exchange.gtfs.model.exporter.GtfsExporterInterface;
import mobi.chouette.model.FareRule;

@Log4j
public class GtfsFareRuleProducer extends AbstractProducer {
    private final GtfsFareRule rule = new GtfsFareRule();

    public GtfsFareRuleProducer(GtfsExporterInterface exporter) {
        super(exporter);
    }

    public boolean save(FareRule neptuneObject) {
        rule.setFareId(neptuneObject.getObjectId());
        rule.setRouteId(neptuneObject.getRoute().getObjectId());
        rule.setOriginId(neptuneObject.getOriginId());
        rule.setDestinationId(neptuneObject.getDestinationId());
        rule.setContainsId(neptuneObject.getContainsId());
        try {
            getExporter().getFareRuleExporter().export(rule);
        } catch (Exception e) {
            log.error("fail to produce fare rules " + e.getClass().getName() + " " + e.getMessage(), e);
            return false;
        }
        return true;
    }
}
