package mobi.chouette.exchange.importer.updater;

import javax.ejb.Local;

import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;

@Local
public interface Updater<T> extends Constant {
	// test keys
	String DATABASE_LINE_1 = "2-DATABASE-Line-1";
	String DATABASE_LINE_2 = "2-DATABASE-Line-2";
	String DATABASE_ROUTE_1 = "2-DATABASE-Route-1";
	String DATABASE_JOURNEY_PATTERN_1 = "2-DATABASE-JourneyPattern-1";
	String DATABASE_VEHICLE_JOURNEY_1 = "2-DATABASE-VehicleJourney-1";
	String DATABASE_VEHICLE_JOURNEY_2 = "2-DATABASE-VehicleJourney-2";
	String DATABASE_STOP_POINT_1 = "2-DATABASE-StopPoint-1";
	String DATABASE_STOP_POINT_2 = "2-DATABASE-StopPoint-2";
	String DATABASE_STOP_POINT_3 = "2-DATABASE-StopPoint-3";
	String DATABASE_STOP_AREA_1 = "2-DATABASE-StopArea-1";
	String DATABASE_STOP_AREA_2 = "2-DATABASE-StopArea-2";
	String DATABASE_ACCESS_POINT_1 = "2-DATABASE-AccessPoint-1";
	String DATABASE_CONNECTION_LINK_1 = "2-DATABASE-ConnectionLink-1";

	void update(Context context, T oldValue, T newValue) throws Exception;

}
