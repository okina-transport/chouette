package mobi.chouette.service;

import lombok.extern.log4j.Log4j;
import mobi.chouette.dao.ScheduledStopPointDAO;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.model.ScheduledStopPoint;
import mobi.chouette.model.SimpleObjectReference;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.ChouetteAreaEnum;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;


import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

@Singleton(name = StopAreaService.BEAN_NAME)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Log4j
public class StopAreaServiceUtils {

    public static final String BEAN_NAME = "StopAreaServiceUtils";


    @EJB
    StopAreaDAO stopAreaDAO;

    @EJB
    ScheduledStopPointDAO scheduledStopPointDAO;


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void cleanSchema(){
        scheduledStopPointDAO.truncate();
        stopAreaDAO.truncate();

    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void createStopArea(String netexId){
        StopArea sa = new StopArea();
        sa.setObjectId(netexId);
        sa.setAreaType(ChouetteAreaEnum.BoardingPosition);
        stopAreaDAO.create(sa);


        StopArea saParent = new StopArea();
        saParent.setObjectId(netexId.replace("Quay","StopPlace"));
        saParent.setAreaType(ChouetteAreaEnum.CommercialStopPoint);
        stopAreaDAO.create(saParent);

        sa.setParent(saParent);



    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void checkStopAreaExistence(String netexId){
        StopArea result = stopAreaDAO.findByObjectId(netexId);
        assertNotNull(result, "StopAra not found:" + netexId);


    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void checkStopAreaAbsence(String netexId){
        StopArea result = stopAreaDAO.findByObjectId(netexId);
        assertNull(result, "StopAra found whereas it should be empty:" + netexId);

    }


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void addUsageToStopArea(String netexId){
        StopArea stopArea = stopAreaDAO.findByObjectId(netexId);
        assertNotNull(stopArea, "StopAra not found:" + netexId);


        ScheduledStopPoint newScheduled = new ScheduledStopPoint();
        newScheduled.setContainedInStopAreaRef(new SimpleObjectReference(stopArea));
        newScheduled.setObjectId("MOBIITI:ScheduledStopPoint:1");
        scheduledStopPointDAO.create(newScheduled);

    }


}
