package mobi.chouette.dao;

import lombok.extern.log4j.Log4j;
import mobi.chouette.model.AccessibilityAssessment;
import org.hibernate.Session;
import org.jboss.jca.adapters.jdbc.WrappedConnection;
import org.postgresql.PGConnection;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


@Stateless(name = "AccessibilityAssessmentDAO")
@Log4j
public class AccessibilityAssessmentDAOImpl extends GenericDAOImpl<AccessibilityAssessment> implements AccessibilityAssessmentDAO {

    public AccessibilityAssessmentDAOImpl() {
        super(AccessibilityAssessment.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    private String[] linesToCopy;

    private int MAX_NB_OF_LINES = 1000;


    @Override
    public void deleteUnusedAccessibilityAssessments() {
        em.createQuery("DELETE FROM AccessibilityAssessment aa " +
                        "WHERE NOT EXISTS (SELECT 1 FROM Line l WHERE l.accessibilityAssessment.id = aa.id) " +
                        "AND NOT EXISTS (SELECT 1 FROM VehicleJourney vj WHERE vj.accessibilityAssessment.id = aa.id)")
                .executeUpdate();
    }

}
