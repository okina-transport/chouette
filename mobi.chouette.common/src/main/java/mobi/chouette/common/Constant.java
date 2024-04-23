package mobi.chouette.common;

public interface Constant {

	boolean ERROR = false;
	boolean SUCCESS = true;

	String INITIAL_CONTEXT = "initial_context";
	String JOB_ID = "job_id";
	String JOB_DATA = "job_data";
	String ROOT_PATH = "referentials";
	String CONFIGURATION = "configuration";
	String MAPPING_LINE_UUID = "mapping_line_uuid";
	String VALIDATION = "validation";
	String SOURCE = "source";
	String SOURCE_FILE = "source_file";
	String SOURCE_DATABASE = "source_database";

	String OPTIMIZED = "optimized";
	String COPY_IN_PROGRESS = "copy_in_progress";
	String FILE_URL = "file_url";
	String FILE_NAME = "file_name";
	String SCHEMA = "schema";
	String IMPORTER = "importer";
	String EXPORTER = "exporter";
	String VALIDATOR = "validator";	
	String INPUT = "input";
	String OUTPUT = "output";
	String PARAMETERS_FILE = "parameters.json";
	String ACTION_PARAMETERS_FILE = "action_parameters.json";
	String VALIDATION_PARAMETERS_FILE = "validation_parameters.json";
	String REPORT = "report";
	String ANALYSIS_REPORT = "analysisReport";
	String SAVE_MAIN_VALIDATION_REPORT = "save_main_validation_report";
	String VALIDATION_REPORT = "validation_report";
	String REPORT_FILE = "action_report.json";
	String ANALYSIS_REPORT_FILE = "analysis_report.json";
	String MAP_MATCHING_REPORT_FILE = "map_matching_report.json";
	String VALIDATION_FILE = "validation_report.json";
	String CANCEL_ASKED = "cancel_asked";
	String COMMAND_CANCELLED = "command_cancelled";
	String CLEAR_TABLE_CATEGORIES_FOR_LINES = "clear_table_categoriesfor_lines";
	String CLEAR_FOR_IMPORT = "clear_for_import";

	String COLUMN_NUMBER = "column_number";
	String LINE_NUMBER = "line_number";

	String LINE_COLOR = "line_color";
	// String OBJECT_LOCALISATION = "object_localisation";
	String VALIDATION_CONTEXT = "validation_context";

	String REFERENTIAL = "referential";
	String CACHE = "cache";
	String PARSER = "parser";
	String AREA_BLOC = "area_bloc";
	String CONNECTION_LINK_BLOC = "connection_link_bloc";
	String ALL_SCHEMAS = "all_schemas";

	
	String VALIDATION_DATA = "validation_data";
	String EXPORTABLE_DATA = "exportable_data";
	String SCHEDULED_STOP_POINTS = "scheduled_stop_points";
	String SHARED_DATA_KEYS = "shared_data_keys";
	String METADATA = "metadata";
	String LINE = "line";
	String LINE_ID = "line_id";
	String FEED_INFO = "feed_info";
	String KEEP_STOP_GEOLOCALISATION = "KeepStopGeolocalisation";
	String KEEP_STOP_NAMES = "KeepStopNames";
	String CLOSE_OLD_CALENDARS = "CloseOldCalendars";
	String UPDATE_STOP_ACCESSIBILITY = "updateStopAccessibility";

	char SEP = '|';
	String NULL = "\\N";

	String BUFFER_VJAS = "buffer_vjas";

	String CREATION_DATE = "CreationDate";

	String AREA_CENTROID_MAP = "areaCentroidMap";

	String FILE_TO_REFERENTIAL_STOP_ID_MAP = "fileToReferentialStopIdMap";
	String QUAY_TO_STOPPLACE_MAP = "quayToStopPlaceMap";
	String RAIL_UIC_REGEXP = "railUICregexp";

	String IMPORTED_ID = "imported-id";
	String SELECTED_ID = "selected-id";
	String NETEX_VALID_PREFIX = System.getenv("NETEX_VALID_PREFIX");
	String COLON_REPLACEMENT_CODE="##3A##";

	String SANITIZED_REPLACEMENT_CODE="__3A__";

	String INCOMING_LINE_LIST = "incomingLineList";

	String TOTAL_NB_OF_LINES = "totalNbOfLines";
	String CURRENT_LINE_NB = "currentLineNb";

	String TIAMAT_ERROR_CODE_CONVERTER = "TiamatErrorCodeConverter";

	String SUPERSPACE_PREFIX = System.getProperty("iev.superspace.prefix");

	String CURRENT_LINE_ID = "currentLineId";
	String CURRENT_SCHEDULED_STOP_POINT = "currentScheduledStopPoint";

	String ROUTE_LINKS_USED_IN_MULTIPLE_FILES = "routeLinksUsedInMutipleFiles";
	String ROUTE_LINKS_USED_MULTIPLE_TIMES_IN_THE_SAME_FILE = "routeLinksUsedMutipleTimesInTheSameFile";
	String ROUTE_LINKS_USED_SAME_FROM_AND_TO_SCHEDULED_STOP_POINT = "routeLinksUsedSameFromAndToScheduledStopPoint";

	String SCHEDULE_STOP_POINT_STOP_AREA_NULL = "scheduleStopPointInStopAreaRefNull";

	String STOP_PLACES_WITHOUT_QUAY = "stopPlacesWithoutQuay";

	String MULTIMODAL_STOP_PLACES = "multimodalStopPlaces";
	String STREAM_TO_CLOSE = "streamToClose";

	String DETECT_CHANGED_TRIPS = "detectChangedTrips";

	String GTFS_ACCESSIBILITY_MAP = "GTFSAccessibilityMap";

	String GTFS_SELF_REFERENCING_STOPS = "GTFSSelfReferencingStops";

	String SET_NEW_LINES_POS_TO_ZERO = "setNewLinesPosToZero";
}
