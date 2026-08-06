package mobi.chouette.exchange.gtfs.model;

import lombok.*;

import java.io.Serializable;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class GtfsTripCompany extends GtfsObject implements Serializable
{

   private static final long serialVersionUID = 1L;

   @Getter
   @Setter
   private String companyId;

   @Getter
   @Setter
   private String companyName;

   @Getter
   @Setter
   private String companyAddress;

   @Getter
   @Setter
   private String companyZipcode;

   @Getter
   @Setter
   private String companyCity;

   @Getter
   @Setter
   private String companyPhone;

   @Getter
   @Setter
   private String companyEmail;

}
