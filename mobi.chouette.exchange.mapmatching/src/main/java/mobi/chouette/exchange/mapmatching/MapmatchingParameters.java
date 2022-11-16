package mobi.chouette.exchange.mapmatching;

import lombok.NoArgsConstructor;
import lombok.ToString;
import mobi.chouette.exchange.parameters.AbstractParameter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "mapmatching")
@NoArgsConstructor
@ToString(callSuper=true)
@XmlAccessorType(XmlAccessType.FIELD)
public class MapmatchingParameters extends AbstractParameter {

}
