package mobi.chouette.exchange.netexprofile.importer;

import mobi.chouette.exchange.importer.updater.TiamatErrorsEnum;
import mobi.chouette.exchange.netexprofile.importer.validation.france.AbstractFranceNetexProfileValidator;
import mobi.chouette.exchange.validation.ErrorCodeConverter;

public class NetexprofileErrorCodeConverter implements ErrorCodeConverter {
    @Override
    public String convert(TiamatErrorsEnum genericErrorCode) {
        switch (genericErrorCode){
            case DUPLICATE_IMPORTED_ID:
                return "2-Stop-1";
            case TRANSPORT_MODE_MISMATCH:
                return AbstractFranceNetexProfileValidator._1_NETEX_TRANSPORT_MODE_MISMATCH;
            default:
                throw new IllegalArgumentException("Tiamat Error code not defined for Netex import:" + genericErrorCode.getErrorCode());

        }
    }
}
