package mobi.chouette.exchange.neptune.importer;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mobi.chouette.exchange.parameters.AbstractImportParameter;

@XmlRootElement(name = "neptune-import")
@NoArgsConstructor
@ToString(callSuper = true)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder={
        "stopAreaPrefixToRemove",
        "areaCentroidPrefixToRemove",
        "linePrefixToRemove",
        "ignoreCommercialPoints",
        "splitIdOnDot",
        "objectIdPrefix"
})
public class NeptuneImportParameters extends AbstractImportParameter {

    @Getter
    @Setter
    @XmlElement(name = "stopArea_prefix_to_remove")
    public String stopAreaPrefixToRemove = "";

    @Getter
    @Setter
    @XmlElement(name = "areaCentroid_prefix_to_remove")
    public String areaCentroidPrefixToRemove = "";


    @Getter
    @Setter
    @XmlElement(name = "line_prefix_to_remove")
    public String linePrefixToRemove = "";

    @Getter
    @Setter
    @XmlElement(name = "ignore_commercial_points")
    public boolean ignoreCommercialPoints = false;

    @Getter@Setter
    @XmlElement(name = "split_id_on_dot", defaultValue="true")
    private boolean splitIdOnDot = true;


    @Getter@Setter
    @XmlElement(name = "object_id_prefix", required=true)
    private String objectIdPrefix;

}
