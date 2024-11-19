package mobi.chouette.exchange.neptune.importer;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mobi.chouette.exchange.parameters.AbstractImportParameter;

import javax.xml.bind.annotation.*;

@XmlRootElement(name = "neptune-import")
@NoArgsConstructor
@ToString(callSuper = true)
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {
        "stopAreaPrefixToRemove",
        "areaCentroidPrefixToRemove",
        "linePrefixToRemove",
        "ignoreCommercialPoints",
        "splitIdOnDot",
})
@Getter
@Setter
public class NeptuneImportParameters extends AbstractImportParameter {

    @XmlElement(name = "stopArea_prefix_to_remove")
    public String stopAreaPrefixToRemove = "";

    @XmlElement(name = "areaCentroid_prefix_to_remove")
    public String areaCentroidPrefixToRemove = "";

    @XmlElement(name = "line_prefix_to_remove")
    public String linePrefixToRemove = "";

    @XmlElement(name = "ignore_commercial_points")
    public boolean ignoreCommercialPoints = false;

    @XmlElement(name = "split_id_on_dot", defaultValue = "true")
    private boolean splitIdOnDot = true;

}
