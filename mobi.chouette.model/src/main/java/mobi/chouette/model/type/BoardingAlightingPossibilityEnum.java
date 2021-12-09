package mobi.chouette.model.type;

import java.util.Arrays;

/**
 * Boarding Alighting Possibility values
 */
public enum BoardingAlightingPossibilityEnum
{

   // Drop off SCHEDULED
   /**
    * Traveler can board and alight (default value)
    */
   BoardAndAlight(DropOffTypeEnum.Scheduled,PickUpTypeEnum.Scheduled),
   /**
    * Traveler can only alight
    */
   AlightOnly(DropOffTypeEnum.Scheduled,PickUpTypeEnum.NoAvailable),
   /**
    * Traveler can alight on standard hours. Traveler can board on driver call
    */
   DropOffStdPickUpOnDriverCall(DropOffTypeEnum.Scheduled,PickUpTypeEnum.DriverCall),
   /**
    * Traveler can alight on standard hours. Traveler can board on driver call
    */
   DropOffStdPickUpOnAgencyCall(DropOffTypeEnum.Scheduled,PickUpTypeEnum.AgencyCall),


   // Drop off NO AVAILABLE
   /**
    * Traveler can only board
    */
   BoardOnly(DropOffTypeEnum.NoAvailable,PickUpTypeEnum.Scheduled),
   /**
    * Traveler can not alight nor board
    */
   NeitherBoardOrAlight(DropOffTypeEnum.NoAvailable,PickUpTypeEnum.NoAvailable),
   /**
    * Traveler can board only on driver call
    */
   BoardOnDriverCall(DropOffTypeEnum.NoAvailable,PickUpTypeEnum.DriverCall),
   /**
    * Traveler can board only on request
    */
   BoardOnRequest(DropOffTypeEnum.NoAvailable,PickUpTypeEnum.AgencyCall),


   // Drop off DRIVER CALL

   /**
    * Traveler can board on standard hours, and alight only on driver call
    */
   DropOffDriverCallPickUpStd(DropOffTypeEnum.DriverCall,PickUpTypeEnum.Scheduled),
   /**
    * Traveler can alight only on driver call
    */
   DropOffDriverCall(DropOffTypeEnum.DriverCall,PickUpTypeEnum.NoAvailable),
   /**
    * Traveler can board on driver call, and alight only on driver call
    */
   DropOffDriverCallPickUpDriverCall(DropOffTypeEnum.DriverCall,PickUpTypeEnum.DriverCall),
   /**
    * Traveler can board agency call and alight on driver call
    */
   DropOffDriverCallPickUpAgencyCall(DropOffTypeEnum.DriverCall,PickUpTypeEnum.AgencyCall),


   // Drop off AGENCY CALL

   /**
    * Traveler can board on standard hours, and alight only on request
    */
   DropOffAgencyCallPickUpStd(DropOffTypeEnum.AgencyCall,PickUpTypeEnum.Scheduled),
   /**
    * Traveler can alight only on request
    */
   AlightOnRequest(DropOffTypeEnum.AgencyCall,PickUpTypeEnum.NoAvailable),
   /**
    * Traveler can board on driver call, and alight only on agency call
    */
   DropOffAgencyCallPickUpDriverCall(DropOffTypeEnum.AgencyCall,PickUpTypeEnum.DriverCall),
   /**
    * Traveler can board and alight only on request
    */
   BoardAndAlightOnRequest(DropOffTypeEnum.AgencyCall,PickUpTypeEnum.AgencyCall);




   private DropOffTypeEnum dropOffType;

   private PickUpTypeEnum pickUpType;


    BoardingAlightingPossibilityEnum(DropOffTypeEnum dropOffType,PickUpTypeEnum pickUpType) {
      this.dropOffType = dropOffType;
      this.pickUpType = pickUpType;
   }

   public static BoardingAlightingPossibilityEnum fromDropOffAndPickUp(DropOffTypeEnum dropOff,PickUpTypeEnum pickUp){

      if (dropOff == null){
         dropOff = DropOffTypeEnum.Scheduled;
      }

      if (pickUp == null){
         pickUp = PickUpTypeEnum.Scheduled;
      }

      DropOffTypeEnum finalDropOff = dropOff;
      PickUpTypeEnum finalPickUp = pickUp;
      return Arrays.stream(BoardingAlightingPossibilityEnum.values())
               .filter(boardingAlightingEnum -> boardingAlightingEnum.getDropOffType().equals(finalDropOff) && boardingAlightingEnum.getPickUpType().equals(finalPickUp))
              .findFirst().orElseThrow(() -> new IllegalArgumentException("Boarding alighting not found for parameters:" + finalDropOff + ", " + finalPickUp));

   }

   public DropOffTypeEnum getDropOffType() {
      return dropOffType;
   }

   public PickUpTypeEnum getPickUpType() {
      return pickUpType;
   }
}
