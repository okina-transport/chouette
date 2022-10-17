package mobi.chouette.exchange.concerto.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class ConcertoStopArea extends ConcertoObject implements Serializable
{

   private static final long serialVersionUID = 1L;

   @Getter
   @Setter
   private String type;

   @Getter
   @Setter
   private UUID uuid;

   @Getter
   @Setter
   private UUID referent_uuid;

   @Getter
   @Setter
   private UUID parent_uuid;

   @Getter
   @Setter
   private LocalDate date;

   @Getter
   @Setter
   private String name;

   @Getter
   @Setter
   private ConcertoObjectId objectId;

   @Getter
   @Setter
   private UUID[] lines;

   @Getter
   @Setter
   private String attributes;

   @Getter
   @Setter
   private String References;

   @Getter
   @Setter
   private Boolean collectedAlways;

   @Getter
   @Setter
   private Boolean collectChildren;

   @Getter
   @Setter
   private Boolean collectGeneralMessages;

   @Getter
   @Setter
   private String zdep;

   @Getter
   @Setter
   private String zder;

   @Getter
   @Setter
   private String zdlr;
}
