package mobi.chouette.service;

import lombok.extern.log4j.Log4j;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.model.Line;

import javax.ejb.*;
import java.util.List;

@Singleton(name = LineService.BEAN_NAME)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Log4j
public class LineService {

    public static final String BEAN_NAME = "LineService";

    @EJB
    private LineDAO lineDAO;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<Line> exportLineIdsForSchema() {
        return lineDAO.findAll();
    }


}
