package mobi.chouette.exchange.gtfs.model;

import lombok.*;

import java.io.Serializable;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class GtfsTripExtension extends GtfsObject implements Serializable
{

   private static final long serialVersionUID = 1L;

   @Getter
   @Setter
   private String tripId;

   @Getter
   @Setter
   private String routeId;

   @Getter
   @Setter
   private String contractCompanyId;

   @Getter
   @Setter
   private String execCompanyId;

   @Getter
   @Setter
   private String indicReservation;

}
