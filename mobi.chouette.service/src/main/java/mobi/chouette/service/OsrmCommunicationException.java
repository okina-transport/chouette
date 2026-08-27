package mobi.chouette.service;

public class OsrmCommunicationException extends Exception {
    public OsrmCommunicationException(String message) {
        super(message);
    }
    public OsrmCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
