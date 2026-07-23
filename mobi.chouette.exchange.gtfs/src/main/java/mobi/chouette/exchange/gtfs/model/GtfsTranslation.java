package mobi.chouette.exchange.gtfs.model;

import lombok.*;

import java.io.Serializable;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GtfsTranslation extends GtfsObject implements Serializable {

    private static final long serialVersionUID = 1L;

    private String tableName;

    private String fieldName;

    private String language;

    private String translation;

    private String recordId;

    private String recordSubId;

    private String fieldValue;

    public void clear() {
        tableName = null;
        fieldName = null;
        language = null;
        translation = null;
        recordId = null;
        recordSubId = null;
        fieldValue = null;
    }

}
