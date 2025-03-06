package mobi.chouette.dao;

import mobi.chouette.model.FareAttribute;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless(name = "FareAttributeDAO")
public class FareAttributeDAOImpl extends GenericDAOImpl<FareAttribute> implements FareAttributeDAO {

    public FareAttributeDAOImpl() {
        super(FareAttribute.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }
}
