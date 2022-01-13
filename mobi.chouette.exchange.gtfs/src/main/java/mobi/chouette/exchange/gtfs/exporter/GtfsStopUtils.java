package mobi.chouette.exchange.gtfs.exporter;

import mobi.chouette.exchange.gtfs.parameters.IdFormat;
import mobi.chouette.exchange.gtfs.parameters.IdParameters;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.ChouetteAreaEnum;
import org.apache.commons.lang.StringUtils;

public class GtfsStopUtils {

    private static final String TRIDENT_STOP_PLACE_TYPE=":StopPlace:";
    private static final String TRIDENT_QUAY_TYPE=":Quay:";



    public static String getNewStopId(StopArea stop, IdParameters idParams, boolean keepOriginalId, String schemaPrefix) {
        String idPrefix = ChouetteAreaEnum.BoardingPosition.equals(stop.getAreaType()) ? idParams.getStopIdPrefix() : idParams.getCommercialPointIdPrefix();
        if (!keepOriginalId && StringUtils.isNotEmpty(stop.getOriginalStopId())) {
            if (IdFormat.TRIDENT.equals(idParams.getIdFormat()) && StringUtils.isNotEmpty(idPrefix)) {
                return createTridentId(stop, idPrefix);
            }
            if (IdFormat.SOURCE.equals(idParams.getIdFormat())) {
                return createStandardId(stop, idPrefix);
            }
        }
        if (keepOriginalId && IdFormat.SOURCE_GLOBAL.equals(idParams.getIdFormat())) {
            return createTridentIdStandard(stop, schemaPrefix);
        }
        return stop.getObjectId();
    }


    /**
     * Creates a new standard ID
     * e.g. : PREFIXxxxx
     * @param stop
     *       Stop for which a trident Id must be generated
     * @param idPrefix
     *       Prefix that will be used on Id beginning.
     * @return
     */
    private static String createStandardId(StopArea stop, String idPrefix){
        return StringUtils.isEmpty(idPrefix) ? stop.getOriginalStopId() : idPrefix + stop.getOriginalStopId();
    }

    /**
     * Creates a new trident ID depending on the object type : Quay or StopPlace
     * e.g. : PREFIX:Quay:10545 or PREFIX:StopPlace:10545
     * @param stop
     *       Stop for which a trident Id must be generated
     * @param idPrefix
     *       Prefix that will be used on Id beginning.
     * @return
     */
    private static String createTridentId(StopArea stop, String idPrefix) {
        if (stop.getObjectId().contains("Quay")) {
            return idPrefix + TRIDENT_QUAY_TYPE + stop.getOriginalStopId();
        }
        return idPrefix + TRIDENT_STOP_PLACE_TYPE + stop.getOriginalStopId();
    }


    private static String createTridentIdStandard(StopArea stop, String prefix) {
        if (stop.getObjectId().contains("Quay")) {
            return prefix + TRIDENT_QUAY_TYPE + stop.getOriginalStopId();
        }
        return prefix + TRIDENT_STOP_PLACE_TYPE + stop.getOriginalStopId();
    }

}
