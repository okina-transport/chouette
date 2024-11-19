package mobi.chouette.exchange.parameters;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.log4j.Logger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


@NoArgsConstructor
@ToString
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {
        "name",
        "userName",
        "organisationName",
        "referentialName",
        "test"
}, name = "actionParameters")
@Getter
@Setter
public class AbstractParameter {

    @XmlElement(name = "name", required = true)
    private String name;

    @XmlElement(name = "user_name", required = true)
    private String userName;

    @XmlElement(name = "organisation_name")
    private String organisationName;

    @XmlElement(name = "referential_name")
    private String referentialName;

    @XmlElement(name = "test")
    private boolean test = false;

    public boolean isValid(Logger log) {
        return true;
    }

}
