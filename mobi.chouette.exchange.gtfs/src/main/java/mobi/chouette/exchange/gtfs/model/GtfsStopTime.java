package mobi.chouette.exchange.gtfs.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mobi.chouette.model.type.DropOffTypeEnum;
import mobi.chouette.model.type.PickUpTypeEnum;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class GtfsStopTime extends GtfsObject implements Serializable
{

   private static final long serialVersionUID = 1L;

   @Getter
   @Setter
   private String tripId;

   @Getter
   @Setter
   private GtfsTime arrivalTime;

   @Getter
   @Setter
   private GtfsTime departureTime;

   @Getter
   @Setter
   private String stopId;

   @Getter
   @Setter
   private Integer stopSequence;

   @Getter
   @Setter
   private String stopHeadsign;

   @Getter
   @Setter
   private PickUpTypeEnum pickupType;

   @Getter
   @Setter
   private DropOffTypeEnum dropOffType;

   @Getter
   @Setter
   private Float shapeDistTraveled;

   @Getter
   @Setter
   private Integer timepoint;

}
