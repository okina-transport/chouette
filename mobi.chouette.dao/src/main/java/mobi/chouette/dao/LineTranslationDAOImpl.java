package mobi.chouette.dao;

import mobi.chouette.model.LineTranslation;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless(name = "LineTranslationDAO")
public class LineTranslationDAOImpl extends GenericDAOImpl<LineTranslation> implements LineTranslationDAO {

    public LineTranslationDAOImpl() {
        super(LineTranslation.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

}
