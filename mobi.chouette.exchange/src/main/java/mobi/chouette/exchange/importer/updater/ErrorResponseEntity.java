package mobi.chouette.exchange.importer.updater;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
public class ErrorResponseEntity {

    public ErrorResponseEntity() {
    }

    public ErrorResponseEntity(String message) {
        errors.add(new Error(message));
    }


    public ErrorResponseEntity(String message, int errorCode) {
        errors.add(new Error(message, errorCode));
    }

    public List<Error> errors = new ArrayList<>();


    public static class Error {
        public String message;
        public int errorCode;

        public Error() {}

        public Error(String message) {
            this.message = message;
        }

        public Error(String message, int errorCode)
        {
            this.message = message;
            this.errorCode = errorCode;
        }
    }

}