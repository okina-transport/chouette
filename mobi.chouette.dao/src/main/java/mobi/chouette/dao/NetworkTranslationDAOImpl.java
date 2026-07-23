package mobi.chouette.dao;

import mobi.chouette.model.NetworkTranslation;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless(name = "NetworkTranslationDAO")
public class NetworkTranslationDAOImpl extends GenericDAOImpl<NetworkTranslation> implements NetworkTranslationDAO {

    public NetworkTranslationDAOImpl() {
        super(NetworkTranslation.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

}
