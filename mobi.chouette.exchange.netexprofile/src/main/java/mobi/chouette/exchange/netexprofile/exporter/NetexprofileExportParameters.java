package mobi.chouette.exchange.netexprofile.exporter;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mobi.chouette.exchange.parameters.AbstractExportParameter;

import javax.xml.bind.annotation.*;

@XmlRootElement(name = "netexprofile-export")
@NoArgsConstructor
@ToString(callSuper = true)
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
@XmlType(propOrder={"projectionType","addExtension", "exportStops", "defaultCodespacePrefix", "exportedFileName"})
public class NetexprofileExportParameters extends AbstractExportParameter {

    @XmlElement(name = "projection_type")
    private String projectionType;

    @XmlElement(name = "add_extension")
    private boolean addExtension = false;

    @XmlElement(name = "export_stops", defaultValue = "false")
    private boolean exportStops;

    @XmlElement(name = "default_codespace_prefix", required = true)
    private String defaultCodespacePrefix;

    @XmlElement(name = "exported_filename")
    private String exportedFileName;


}
