package mobi.chouette.exchange.gtfs.model;

import lombok.*;

import java.io.Serializable;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GtfsTrip extends GtfsObject implements Serializable
{

   private static final long serialVersionUID = 1L;

   private String routeId;

   private String serviceId;

   private String tripId;

   private String tripShortName;

   private String tripHeadSign;

   private DirectionType directionId;

   private String blockId;

   private String shapeId;

   private WheelchairAccessibleType wheelchairAccessible;

   private BikesAllowedType bikesAllowed;

   // @Override
   // public String toString()
   // {
   // return id + ":" + TripExporter.CONVERTER.to(new Context(),this);
   // }

   @AllArgsConstructor
   public enum DirectionType implements Serializable
   {
      Outbound, Inbound;

   }

   @AllArgsConstructor
   public enum BikesAllowedType implements Serializable
   {
      NoInformation, Allowed, NoAllowed;

   }

   @AllArgsConstructor
   public enum WheelchairAccessibleType implements Serializable
   {
      NoInformation, Allowed, NoAllowed;

   }
}
