package mobi.chouette.exchange.gtfs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import mobi.chouette.common.JobData;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobDataTest implements JobData {

	private Long id;

	private String inputFilename;

	private String outputFilename;

	private String action;
	
	private String type;
	
	private String referential;
	
	private String pathName;
}
