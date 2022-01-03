package mobi.chouette.exchange.transfer;

public interface Constant extends mobi.chouette.common.Constant {

	public static final String LINES = "LINES";
	public static final String STOP_AREAS = "STOP_AREAS";
	public static final String OPERATORS = "OPERATORS";
	public static final String FEED_INFOS ="FEED_INFOS";
	public static final String PROGRESSION = "PROGRESSION";

	public static final String PROPERTY_OKINA_DATASOURCE_HOST = System.getenv("CHOUETTE_DB_HOST");
	public static final String PROPERTY_OKINA_DATASOURCE_NAME = System.getenv("CHOUETTE_DB_NAME");
	public static final String PROPERTY_OKINA_DATASOURCE_PORT = System.getenv("CHOUETTE_DB_PORT");
	public static final String PROPERTY_OKINA_DATASOURCE_USER = System.getenv("CHOUETTE_DB_USER");
	public static final String PROPERTY_OKINA_DATASOURCE_PASSWORD = System.getenv("CHOUETTE_DB_PASSWORD");
}
