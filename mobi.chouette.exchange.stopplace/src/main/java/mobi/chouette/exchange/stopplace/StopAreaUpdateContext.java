package mobi.chouette.exchange.stopplace;

import lombok.Getter;
import mobi.chouette.model.StopArea;

import java.util.*;

public class StopAreaUpdateContext {

	@Getter
	private Set<String> inactiveStopAreaIds = new HashSet<>();
	@Getter
	private Set<StopArea> activeStopAreas = new HashSet<>();
	@Getter
	private Map<String, Set<String>> mergedQuays = new HashMap<>();
	@Getter
	private Set<String> impactedSchemas = new HashSet<>();
	@Getter
	private Map<String, List<String>> impactedStopAreasBySchema = new HashMap<>();
	@Getter
	private Map<String, List<String>> importedIdsByNetexId = new HashMap<>();
	@Getter
	private Map<String, List<String>> selectedIdsByNetexId = new HashMap<>();



	public int getChangedStopCount() {
		return getActiveStopAreas().size() + getInactiveStopAreaIds().size();
	}

}
