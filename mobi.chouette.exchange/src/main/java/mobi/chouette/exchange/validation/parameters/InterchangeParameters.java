package mobi.chouette.exchange.validation.parameters;

import lombok.Data;
import mobi.chouette.model.Interchange;

import javax.xml.bind.annotation.*;
import java.util.Arrays;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
@XmlType(propOrder={"objectId", "name"})
public class InterchangeParameters {

	@XmlTransient
	public static String[] fields = { "ObjectId", "Name"} ;
	
	static {
		ValidationParametersUtil.addFieldList(Interchange.class.getSimpleName(), Arrays.asList(fields));
	}

	@XmlElement(name = "objectid")
	private FieldParameters objectId;

	@XmlElement(name = "name")
	private FieldParameters name;

}
