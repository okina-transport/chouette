package mobi.chouette.ws;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class JobListing {

	@JsonProperty("jobs")
	private Collection<JobInfo> list;

}
