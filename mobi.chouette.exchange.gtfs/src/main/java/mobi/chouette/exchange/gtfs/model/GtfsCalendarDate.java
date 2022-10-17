package mobi.chouette.exchange.gtfs.model;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class GtfsCalendarDate extends GtfsObject implements Serializable
{

   private static final long serialVersionUID = 1L;

   @Getter
   @Setter
   private String serviceId;

   @Getter
   @Setter
   private LocalDate date;

   @Getter
   @Setter
   private ExceptionType exceptionType;

   // @Override
   // public String toString()
   // {
   // return id + ":" + CalendarDateExporter.CONVERTER.to(new Context(),this);
   // }

   public enum ExceptionType
   {
      Unknown, Added, Removed;
   }
}
