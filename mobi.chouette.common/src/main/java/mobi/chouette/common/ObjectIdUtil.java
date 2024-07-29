package mobi.chouette.common;

import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;

public class ObjectIdUtil {

    public static final String COLON = ":";
    public static final String COLON_REPLACEMENT_CODE = "##3A##";

    /**
     * @param neptuneId original objectId
     * @param prefix optional prefix
     * @param keepOriginalId keep original id with or without prefix
     */
    public static String toGtfsId(String neptuneId, @Nullable String prefix, boolean keepOriginalId) {
        if (keepOriginalId && StringUtils.isEmpty(prefix)) {
            return neptuneId;
        }
        String[] tokens = neptuneId.split(COLON);
        if (keepOriginalId) {
            return prefix + COLON + tokens[1] + COLON + tokens[2];
        } else {
            return tokens.length == 1 ? tokens[0] : tokens[2];
        }
    }

    /**
     * Convert GTFS stop id to StopArea id with correct type (StopPlace / Quay).
     *
     * If gtfs id is structured like a full ID (containing : type :) it used as is. If not a new Id is composed.
     */
    public static String toStopAreaId(boolean isSplitOnDot, String objectIdPrefix, String type, String id) {
        if (id != null && id.contains(COLON + type + COLON)) {
            return id;
        }
        return composeObjectId(isSplitOnDot, objectIdPrefix, type, id);
    }

    public static String composeObjectId(boolean isSplitOnDot, String objectIdPrefix, String type, String id) {
        if(StringUtils.isEmpty(id)) return "";
        if(isSplitOnDot) {
            String[] tokens = id.split("\\.");
            if (tokens.length == 2) {
                return tokens[0].trim() + COLON + type + COLON + tokens[1].trim();
            }
        }
        return objectIdPrefix + COLON + type + COLON + replaceColons(id.trim());
    }

    public static String composeNeptuneObjectId(String objectIdPrefix, String type, String id) {
        if (StringUtils.isEmpty(id)) return "";
        String[] tokens = id.split(":");
        if(tokens.length == 3){
            id = tokens[2];
        }
        else if(tokens.length == 1){
            id = tokens[0];
        }
        else {
            return "";
        }
        return objectIdPrefix + ":" + type + ":" + replaceColons(id.trim());
    }


    public static String extractOriginalId(String objectId) {
        return objectId.split(COLON)[2];
    }

    /**
     * Generate a commercial stop id PROVIDER:StopPlace:COM_xxx from a boarding Stop id (PROVIDER:Quay:xxx)
     */
    public static String generateCommercialStopId(String objectId){
        String[] token = objectId.split(COLON);
        return token[0] + ":StopPlace:COM_" + token[2];
    }

    /**
     * Replace colons(:) in input string by a special code handled by application (##3A##)
     * @param inputString
     * @return the string with colons encoded
     */
    private static String replaceColons(String inputString){
        return inputString.replace(COLON, COLON_REPLACEMENT_CODE);
    }

}
