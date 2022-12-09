package mobi.chouette.exchange.gtfs.model;

import lombok.*;

import java.io.Serializable;
import java.net.URL;
import java.util.TimeZone;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class GtfsAttribution extends GtfsObject implements Serializable
{

   private static final long serialVersionUID = 1L;

   @Getter
   @Setter
   private String attributionId;

   @Getter
   @Setter
   private String agencyId;

   @Getter
   @Setter
   private String routeId ;

   @Getter
   @Setter
   private String tripId;

   @Getter
   @Setter
   private String organizationName;

   @Getter
   @Setter
   private Integer isProducer;

   @Getter
   @Setter
   private Integer isOperator;

   @Getter
   @Setter
   private Integer isAuthority;

   @Getter
   @Setter
   private String attributionUrl;

   @Getter
   @Setter
   private String attributionEmail;

   @Getter
   @Setter
   private String attributionPhone;

}
