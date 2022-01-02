package mobi.chouette.exchange.neptune.importer;

import mobi.chouette.exchange.importer.updater.TiamatErrorsEnum;
import mobi.chouette.exchange.neptune.validation.StopAreaValidator;
import mobi.chouette.exchange.validation.ErrorCodeConverter;

public class NeptuneErrorCodeConverter implements ErrorCodeConverter {
    @Override
    public String convert(TiamatErrorsEnum genericErrorCode) {
        switch (genericErrorCode){
            case TRANSPORT_MODE_MISMATCH:
                return StopAreaValidator.STOP_AREA_8;
            default:
                throw new IllegalArgumentException("Tiamat Error code not defined for Neptune import:" + genericErrorCode.getErrorCode());

        }
    }
}
