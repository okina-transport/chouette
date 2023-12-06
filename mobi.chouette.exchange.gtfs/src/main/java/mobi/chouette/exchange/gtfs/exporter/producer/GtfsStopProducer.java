/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */

package mobi.chouette.exchange.gtfs.exporter.producer;

import mobi.chouette.exchange.gtfs.exporter.GtfsStopUtils;
import mobi.chouette.exchange.gtfs.model.GtfsStop;
import mobi.chouette.exchange.gtfs.model.GtfsStop.WheelchairBoardingType;
import mobi.chouette.exchange.gtfs.model.RouteTypeEnum;
import mobi.chouette.exchange.gtfs.model.exporter.GtfsExporterInterface;
import mobi.chouette.exchange.gtfs.parameters.IdParameters;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.ChouetteAreaEnum;

import java.util.Collection;
import java.util.Optional;
import java.util.TimeZone;

/**
 * convert Timetable to Gtfs Calendar and CalendarDate
 * <p>
 * optimise multiple period timetable with calendarDate inclusion or exclusion
 */
public class GtfsStopProducer extends AbstractProducer {
	GtfsStop stop = new GtfsStop();


	public GtfsStopProducer(GtfsExporterInterface exporter) {
		super(exporter);
	}

	public boolean save(StopArea neptuneObject, Collection<StopArea> validParents, boolean keepOriginalId, boolean useTPEGRouteTypes, IdParameters idParams, String schemaPrefix, Boolean exportCommercialPoints, Boolean googleMapsCompatibility) {
		String stopId = GtfsStopUtils.getNewStopId(neptuneObject, idParams, keepOriginalId, schemaPrefix);
		return save(neptuneObject, validParents, keepOriginalId, useTPEGRouteTypes, stopId, idParams, schemaPrefix, exportCommercialPoints, googleMapsCompatibility);
	}

	public boolean save(StopArea neptuneObject, Collection<StopArea> validParents, boolean keepOriginalId, boolean useTPEGRouteTypes, String newStopId, IdParameters idParams, String prefix, Boolean exportCommercialPoints, Boolean googleMapsCompatibility) {
		Optional<StopArea> parent = Optional.ofNullable(neptuneObject.getParent());
		if (validParents != null && !validParents.isEmpty() && parent.isPresent()) {
			if (parent.get().getObjectId().equals(neptuneObject.getObjectId())) {
				return false;
			}
		}

		ChouetteAreaEnum chouetteAreaType = neptuneObject.getAreaType();
		if (chouetteAreaType.compareTo(ChouetteAreaEnum.BoardingPosition) == 0)
			stop.setLocationType(GtfsStop.LocationType.Stop);
		else if (chouetteAreaType.compareTo(ChouetteAreaEnum.Quay) == 0)
			stop.setLocationType(GtfsStop.LocationType.Stop);
		else if (chouetteAreaType.compareTo(ChouetteAreaEnum.CommercialStopPoint) == 0) {
			if (exportCommercialPoints != null && exportCommercialPoints) {
				return false;
			}
			stop.setLocationType(GtfsStop.LocationType.Station);
		}
		else
			return false; // StopPlaces and ITL type not available

		stop.setStopId(newStopId);

		
		// If name is empty, try to use parent name
		String name = neptuneObject.getName();
		if (name == null && neptuneObject.getParent() != null) {
				name = neptuneObject.getParent().getName();
		}
	
		if(name == null) {
			return false;
		}
		stop.setStopName(neptuneObject.getName());

		if (neptuneObject.getLatitude() == null)
		{
			return false;
		}
		stop.setStopLat(neptuneObject.getLatitude());
		if (neptuneObject.getLongitude() == null)
		{
			return false;
		}
		stop.setStopLon(neptuneObject.getLongitude());
		stop.setStopCode(neptuneObject.getRegistrationNumber());
		stop.setStopDesc(neptuneObject.getComment());
		stop.setStopUrl(getUrl(neptuneObject.getUrl()));
		stop.setZoneId(neptuneObject.getZoneId());

		// manage stop_timezone
		stop.setStopTimezone(null);
		if (!isEmpty(neptuneObject.getTimeZone()))
		{
			TimeZone tz = TimeZone.getTimeZone(neptuneObject.getTimeZone());
			if (tz != null)
			{
				stop.setStopTimezone(tz);
			}
		}

		stop.setParentStation(null);
		if (stop.getLocationType().equals(GtfsStop.LocationType.Stop))
		{
			if (neptuneObject.getParent() != null && validParents.contains(neptuneObject.getParent()))
			{
				stop.setParentStation(GtfsStopUtils.getNewStopId(neptuneObject.getParent(), idParams, keepOriginalId, prefix));
			}
		}

		if (neptuneObject.getMobilityRestrictedSuitable() != null)
		{
			if (neptuneObject.getMobilityRestrictedSuitable())
			{
				stop.setWheelchairBoarding(WheelchairBoardingType.Allowed);
			}
			else
			{
				stop.setWheelchairBoarding(WheelchairBoardingType.NoAllowed);
			}
		}
		else
		{
			stop.setWheelchairBoarding(WheelchairBoardingType.NoInformation);
		}
		
		stop.setPlatformCode(neptuneObject.getPlatformCode());

		
	      if (neptuneObject.getTransportModeName() != null)
	      {
	         if (useTPEGRouteTypes) {
	        	 stop.setVehicleType(RouteTypeEnum.from(neptuneObject.getTransportModeName(), null));
	         } else {
	    	  
	             switch (neptuneObject.getTransportModeName())
	             {
	             case Tram:
	                stop.setVehicleType(RouteTypeEnum.Tram);
	                break;
	             case Metro:
	                stop.setVehicleType(RouteTypeEnum.Subway);
	                break;
	             case Rail:
	                stop.setVehicleType(RouteTypeEnum.Rail);
	                break;
	             case Water:
	             case Ferry:
	                stop.setVehicleType(RouteTypeEnum.Ferry);
	                break;
	             case Funicular:
	            	 stop.setVehicleType(RouteTypeEnum.Funicular);
	            	 break;
	             case Cableway:
	            	 stop.setVehicleType(RouteTypeEnum.Gondola);
	            	 break;
	             case TrolleyBus:
	             case Coach:
	             case Bus:
	             default:
	                stop.setVehicleType(RouteTypeEnum.Bus);
	             }
	         }
	      
	      }
	      else
	      {
	         stop.setVehicleType(null);
	      }

		if(googleMapsCompatibility != null && googleMapsCompatibility)
		{
			// Don't export stop_code if the export targets Google Maps
			stop.setStopCode(null);
		}

		try
		{
			getExporter().getStopExporter().export(stop);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
