package mobi.chouette.exchange.mapmatching;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mobi.chouette.exchange.parameters.AbstractParameter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "mapmatching")
@NoArgsConstructor
@ToString(callSuper=true)
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
public class MapmatchingParameters extends AbstractParameter {

    @XmlElement(name = "generate_map_matching")
    private String generateMapMatching;
}
