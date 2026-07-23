package mobi.chouette.dao;

import mobi.chouette.model.CompanyTranslation;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless(name = "CompanyTranslationDAO")
public class CompanyTranslationDAOImpl extends GenericDAOImpl<CompanyTranslation> implements CompanyTranslationDAO {

    public CompanyTranslationDAOImpl() {
        super(CompanyTranslation.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

}
