package mobi.chouette.service;

import lombok.Getter;

@Getter
public enum JobAction {
	ANALYZE_FILE("analyzeFile"),
	IMPORTER("importer"),
	VALIDATOR("validator"),
	EXPORTER("exporter"),
	GLOBAL_EXPORT("globalExport"),
	MAPMATCHING("mapmatching");

	private final String value;

	JobAction(String value) {
		this.value = value;
	}
}
