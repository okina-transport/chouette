/**
 * Projet CHOUETTE
 * <p>
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 */

package mobi.chouette.exchange.gtfs.exporter.producer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.ObjectIdUtil;
import mobi.chouette.exchange.gtfs.exporter.GtfsStopUtils;
import mobi.chouette.exchange.gtfs.model.GtfsFareAttribute;
import mobi.chouette.exchange.gtfs.model.exporter.GtfsExporterInterface;
import mobi.chouette.exchange.gtfs.parameters.IdParameters;
import mobi.chouette.model.ConnectionLink;
import mobi.chouette.model.Interchange;
import mobi.chouette.model.StopArea;

@Log4j
public class GtfsFareAttributeProducer extends AbstractProducer {
    private final GtfsFareAttribute transfer = new GtfsFareAttribute();

    public GtfsFareAttributeProducer(GtfsExporterInterface exporter) {
        super(exporter);
    }

    public boolean save(ConnectionLink neptuneObject, String prefix, boolean keepOriginalId, IdParameters idParams) {
        transfer.clear();

//        StopArea startArea = neptuneObject.getStartOfLink();
//        StopArea endArea = neptuneObject.getEndOfLink();
//        String fromStopId = GtfsStopUtils.getNewStopId(startArea, idParams, keepOriginalId, prefix);
//        String endStopId = GtfsStopUtils.getNewStopId(endArea, idParams, keepOriginalId, prefix);
//
//        transfer.setFromStopId(fromStopId);
//        transfer.setToStopId(endStopId);
//
//
//        if ("FORBIDDEN".equals(neptuneObject.getName())) {
//            transfer.setTransferType(GtfsFareAttribute.TransferType.NoAllowed);
//        } else if (neptuneObject.getDefaultDuration() != null && neptuneObject.getDefaultDuration().getStandardSeconds() > 1) {
//            transfer.setTransferType(GtfsFareAttribute.TransferType.Minimal);
//        } else {
//            transfer.setTransferType(GtfsFareAttribute.TransferType.Recommended);
//        }
//
//        if (neptuneObject.getDefaultDuration() == null) {
//            transfer.setMinTransferTime(0);
//        } else {
//            transfer.setMinTransferTime((int) neptuneObject.getDefaultDuration().getStandardSeconds());
//        }

        try {
            getExporter().getFareAttributeExporter().export(transfer);
        } catch (Exception e) {
            log.error("fail to produce fare attributes " + e.getClass().getName() + " " + e.getMessage());
            return false;
        }
        return true;
    }

    public boolean save(Interchange neptuneObject, String prefix, boolean keepOriginalId, IdParameters idParams) {
        transfer.clear();
//        if (neptuneObject.getFeederStopPoint() != null) {
//            transfer.setFromStopId(GtfsStopUtils.getNewStopId(neptuneObject.getFeederStopPoint().getContainedInStopAreaRef().getObject(), idParams, keepOriginalId, prefix));
//        }
//        if (neptuneObject.getConsumerStopPoint() != null) {
//            transfer
//                    .setToStopId(GtfsStopUtils.getNewStopId(neptuneObject.getConsumerStopPoint().getContainedInStopAreaRef().getObject(), idParams, keepOriginalId, prefix));
//        }
//        if (Boolean.TRUE.equals(neptuneObject.getGuaranteed())) {
//            transfer.setTransferType(GtfsFareAttribute.TransferType.Timed);
//        } else if (neptuneObject.getMinimumTransferTime() != null) {
//            transfer.setTransferType(GtfsFareAttribute.TransferType.Minimal);
//            transfer.setMinTransferTime(Integer.valueOf((int) (neptuneObject.getMinimumTransferTime().getStandardSeconds())));
//        } else if (neptuneObject.getPriority() != null && neptuneObject.getPriority() >= 0) {
//            transfer.setTransferType(GtfsFareAttribute.TransferType.Recommended);
//        } else {
//            transfer.setTransferType(GtfsFareAttribute.TransferType.NoAllowed);
//        }
//
//        if (neptuneObject.getFeederVehicleJourney() != null) {
//            transfer.setFromTripId(ObjectIdUtil.toGtfsId(neptuneObject.getFeederVehicleJourney().getObjectId(), prefix, keepOriginalId));
//        }
////		transfer.setFromRouteId(
////				toGtfsId(neptuneObject.getFeederVehicleJourney().getRoute().getLine().getObjectId(), prefix, keepOriginalId));
//
//        if (neptuneObject.getConsumerVehicleJourney() != null) {
//            transfer.setToTripId(ObjectIdUtil.toGtfsId(neptuneObject.getConsumerVehicleJourney().getObjectId(), prefix, keepOriginalId));
//        }
////		transfer.setToRouteId(
////				toGtfsId(neptuneObject.getConsumerVehicleJourney().getRoute().getLine().getObjectId(), prefix, keepOriginalId));

        try {
            getExporter().getFareAttributeExporter().export(transfer);
        } catch (Exception e) {
            log.error("fail to produce fare attributes " + e.getClass().getName() + " " + e.getMessage(), e);
            return false;
        }
        return true;
    }

}
