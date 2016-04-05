package mobi.chouette.exchange.regtopp.model;

import java.io.Serializable;

import org.beanio.annotation.Field;
import org.beanio.annotation.Record;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Record(minOccurs = 1)
public class RegtoppFootnoteMRK extends RegtoppObject implements Serializable {

	public static final String FILE_EXTENSION = "MRK";

	private static final long serialVersionUID = 1L;

	@Getter
	@Setter
	@Field(at = 0,length = 3)
	private String adminCode;

	@Getter
	@Setter
	@Field(at = 3,length = 1)
	private String counter;

	@Getter
	@Setter
	@Field(at = 4,length = 3)
	private String footnoteId;

	@Getter
	@Setter
	@Field(at = 7, length = 80, trim = true)
	private String description;
	
	@Override
	public String getIndexingKey() {
		return adminCode+counter+footnoteId;
	}


}
