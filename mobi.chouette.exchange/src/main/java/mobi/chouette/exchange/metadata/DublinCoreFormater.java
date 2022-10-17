/**
 * 
 */
package mobi.chouette.exchange.metadata;

import mobi.chouette.exchange.metadata.Metadata.Box;
import mobi.chouette.exchange.metadata.Metadata.Period;
import mobi.chouette.exchange.metadata.Metadata.Resource;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

/**
 * @author michel
 *
 */
public class DublinCoreFormater implements Formater
{

   private static DecimalFormat doubleFormat = new DecimalFormat("#.000",DecimalFormatSymbols.getInstance(Locale.US)); 
   private static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

   /* (non-Javadoc)
    * @see fr.certu.chouette.export.metadata.writer.Formater#format(fr.certu.chouette.export.metadata.model.Metadata.Period)
    */
   @Override
   public String format(Period period)
   {
      return "start="+dateFormat.format(period.getStart())+
            "; end="+dateFormat.format(period.getEnd())+
            "; scheme=W3C-DTF;";
   }

   /* (non-Javadoc)
    * @see fr.certu.chouette.export.metadata.writer.Formater#format(fr.certu.chouette.export.metadata.model.Metadata.Box)
    */
   @Override
   public String format(Box box)
   {
      return "northlimit="+doubleFormat.format(box.getNorthLimit())+
            "; southlimit="+doubleFormat.format(box.getSouthLimit())+
            "; westlimit="+doubleFormat.format(box.getWestLimit())+
            "; eastlimit="+doubleFormat.format(box.getEastLimit());
   }

   /* (non-Javadoc)
    * @see fr.certu.chouette.export.metadata.writer.Formater#format(java.util.List)
    */
   @Override
   public String format(Resource resource)
   {
      StringBuilder builder = new StringBuilder();
      if (resource.getFileName() != null)
      {
         builder.append(resource.getFileName()+" : ");
      }
      if (resource.getNetworkName() != null)
      {
         builder.append("Network "+resource.getNetworkName()+", ");
      }
      builder.append("Line "+resource.getLineName());

      return builder.toString();
   }

   /* (non-Javadoc)
 * @see fr.certu.chouette.export.metadata.writer.Formater#formatDate(java.util.Calendar)
 */
   @Override
   public synchronized String formatDate(TemporalAccessor date)
   {
      return dateFormat.format(date);
   }
}
