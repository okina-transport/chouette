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
import mobi.chouette.exchange.gtfs.model.GtfsFareRule;
import mobi.chouette.exchange.gtfs.model.exporter.GtfsExporterInterface;
import mobi.chouette.exchange.gtfs.parameters.IdFormat;
import mobi.chouette.exchange.gtfs.parameters.IdParameters;
import mobi.chouette.model.FareRule;
import mobi.chouette.model.Line;

@Log4j
public class GtfsFareRuleProducer extends AbstractProducer {
	private final GtfsFareRule rule = new GtfsFareRule();

	public GtfsFareRuleProducer(GtfsExporterInterface exporter) {
		super(exporter);
	}

	public boolean save(FareRule neptuneObject, GtfsExportParameters configuration) {
		String prefix = configuration.getObjectIdPrefix();
		IdParameters idParams = new IdParameters(configuration.getStopIdPrefix(),configuration.getIdFormat(),configuration.getIdSuffix(),configuration.getLineIdPrefix(), configuration.getCommercialPointIdPrefix());

		for (Line line : neptuneObject.getLines()) {
			rule.setFareId(ObjectIdUtil.toGtfsId(neptuneObject.getObjectId(), configuration.getObjectIdPrefix(), IdFormat.TRIDENT.equals(configuration.getIdFormat())));
			if (neptuneObject.getLines() != null) {
				rule.setRouteId(generateCustomRouteId(ObjectIdUtil.toGtfsId(line.getObjectId(), prefix, configuration.isKeepOriginalId()), idParams));
			}
			if (neptuneObject.getOriginId() != null) {
				rule.setOriginId(neptuneObject.getOriginId());
			}
			if (neptuneObject.getDestinationId() != null) {
				rule.setDestinationId(neptuneObject.getDestinationId());
			}
			if (neptuneObject.getContainsId() != null) {
				rule.setContainsId(neptuneObject.getContainsId());
			}
			try {
				getExporter().getFareRuleExporter().export(rule);
			} catch (Exception e) {
				log.error("fail to produce fare rules " + e.getClass().getName() + " " + e.getMessage(), e);
				return false;
			}
		}

		return true;
	}
}
