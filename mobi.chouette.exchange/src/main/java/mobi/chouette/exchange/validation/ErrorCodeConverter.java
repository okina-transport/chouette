package mobi.chouette.exchange.validation;


import mobi.chouette.common.Context;
import mobi.chouette.exchange.importer.updater.TiamatErrorsEnum;

/**
 * Convert a generic error code, to a specific error code (that depends on import Type)
 *
 */
public interface ErrorCodeConverter {

    String convert(TiamatErrorsEnum genericErrorCode);

}
