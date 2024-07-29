/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */

package mobi.chouette.exchange.gtfs.exporter.producer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.ObjectIdUtil;
import mobi.chouette.exchange.gtfs.model.GtfsRoute;
import mobi.chouette.exchange.gtfs.model.RouteTypeEnum;
import mobi.chouette.exchange.gtfs.model.exporter.GtfsExporterInterface;
import mobi.chouette.exchange.gtfs.parameters.IdFormat;
import mobi.chouette.exchange.gtfs.parameters.IdParameters;
import mobi.chouette.model.Company;
import mobi.chouette.model.Line;
import mobi.chouette.model.Network;
import mobi.chouette.model.type.OrganisationTypeEnum;
import mobi.chouette.model.type.TadEnum;
import org.apache.commons.lang3.StringUtils;

import static mobi.chouette.common.Constant.COLON_REPLACEMENT_CODE;

/**
 * convert Timetable to Gtfs Calendar and CalendarDate
 * <p>
 * optimise multiple period timetable with calendarDate inclusion or exclusion
 */
@Log4j
public class GtfsRouteProducer extends AbstractProducer
{
   public GtfsRouteProducer(GtfsExporterInterface exporter)
   {
      super(exporter);
   }

   private GtfsRoute route = new GtfsRoute();

   public boolean save(Line neptuneObject, String prefix, boolean keepOriginalId, boolean useExtendedGtfsRouteTypes, IdParameters idParams)
   {
      route.setRouteId(generateCustomRouteId(ObjectIdUtil.toGtfsId(neptuneObject.getObjectId(), prefix, keepOriginalId), idParams));
       if (IdFormat.TRIDENT.equals(idParams.getIdFormat()) && !TadEnum.NO_TAD.equals(neptuneObject.getTad())){
           route.setRouteId(route.getRouteId().replace(":Line:", ":FlexibleLine:"));
       }

      Company c = neptuneObject.getCompany();

       String agencyId;
       if (c == null || !OrganisationTypeEnum.Authority.equals(c.getOrganisationType())) {
           // Use network->authority as agency if it is an authority
           Network network = neptuneObject.getNetwork();
           if (network != null && network.getCompany() != null) {
               if (OrganisationTypeEnum.Operator.equals(network.getCompany().getOrganisationType())) {
                   c = network.getCompany();
               }
           }
       }

       if (c == null) {
           agencyId = neptuneObject.getNetwork().getObjectId();
       } else {
           agencyId = c.getObjectId();
       }
       route.setAgencyId(ObjectIdUtil.toGtfsId(agencyId, prefix, keepOriginalId));
       if(c != null && OrganisationTypeEnum.Operator.equals(c.getOrganisationType()) && agencyId.endsWith("o")){
           route.setAgencyId(StringUtils.chop(route.getAgencyId()));
       }
       route.setAgencyId(route.getAgencyId().replaceAll(COLON_REPLACEMENT_CODE, ":"));
       route.setRouteShortName(null);
       route.setRouteLongName(null);

      route.setRouteShortName(neptuneObject.getNumber());
      
      if(!isEmpty(neptuneObject.getPublishedName())) {
    	  route.setRouteLongName(neptuneObject.getPublishedName());
      } else {
    	  route.setRouteLongName(neptuneObject.getName());
      }

      if (isEmpty(route.getRouteShortName()) && isEmpty(route.getRouteLongName()))
      {
          log.warn("no naming data for line "+neptuneObject.getObjectId());
          return false;
      }

      route.setRouteDesc(null);
      if (!isEmpty(neptuneObject.getComment()))
      {
          route.setRouteDesc(neptuneObject.getComment());
      }

      route.setRouteColor(getColor(neptuneObject.getColor()));
      route.setRouteTextColor(getColor(neptuneObject.getTextColor()));
      route.setRouteUrl(getUrl(neptuneObject.getUrl()));

      if (neptuneObject.getNetwork().getPosition() != null) {
          Integer pos = neptuneObject.getNetwork().getPosition() * 1000;
          pos += neptuneObject.getPosition();
          route.setPosition(pos);
      } else {
          route.setPosition(neptuneObject.getPosition());
      }

      if (neptuneObject.getTransportModeName() != null)
      {
         if(useExtendedGtfsRouteTypes) {
        	 route.setRouteType(RouteTypeEnum.fromTransportModeAndSubMode(neptuneObject.getTransportModeName(), neptuneObject.getTransportSubModeName()));
         } else {
             route.setRouteType(RouteTypeEnum.fromTransportMode(neptuneObject.getTransportModeName()));
         }
      }
      else
      {
         route.setRouteType(RouteTypeEnum.Bus);
      }

      try
      {
         getExporter().getRouteExporter().export(route);
      }
      catch (Exception e)
      {
         log.warn("export failed for line "+neptuneObject.getObjectId(),e);
         return false;
      }

      return true;
   }
}
