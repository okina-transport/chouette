package mobi.chouette.exchange.parameters;

public enum CleanModeEnum {
    CONTIGUOUS("contiguous"),
    OVERLAP("overlap"),
    PURGE("purge");

    private final String value;

    CleanModeEnum(String value) {
        this.value = value;
    }

    public static CleanModeEnum fromValue(String v) {
        for (CleanModeEnum c: CleanModeEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
