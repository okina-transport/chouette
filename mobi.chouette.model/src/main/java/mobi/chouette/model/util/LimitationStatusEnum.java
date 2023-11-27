package mobi.chouette.model.util;

public enum LimitationStatusEnum {
    TRUE("true"),

    FALSE("false"),

    UNKNOWN("unknown"),

    PARTIAL("partial");
    private final String value;

    LimitationStatusEnum(String v) { value = v; }

    public static LimitationStatusEnum fromValue(String v) {
        for (LimitationStatusEnum c : LimitationStatusEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

    public String getValue() { return value; }
}
