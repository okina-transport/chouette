package mobi.chouette.exchange.gtfs.importer;

import mobi.chouette.exchange.importer.updater.TiamatErrorsEnum;
import mobi.chouette.exchange.validation.ErrorCodeConverter;

import static mobi.chouette.exchange.gtfs.validation.Constant.GTFS_2_GTFS_Stop_7;

public class GtfsErrorCodeConverter implements ErrorCodeConverter {
    @Override
    public String convert(TiamatErrorsEnum genericErrorCode) {
        switch (genericErrorCode){
            case TRANSPORT_MODE_MISMATCH:
                return GTFS_2_GTFS_Stop_7;
            default:
                throw new IllegalArgumentException("Tiamat Error code not defined for GTFS import:" + genericErrorCode.getErrorCode());

        }
    }
}
