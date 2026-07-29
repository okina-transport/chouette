package mobi.chouette.dao;

import mobi.chouette.model.StopAreaTranslation;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.Collection;

@Stateless(name = "StopAreaTranslationDAO")
public class StopAreaTranslationDAOImpl extends GenericDAOImpl<StopAreaTranslation> implements StopAreaTranslationDAO {

    public StopAreaTranslationDAOImpl() {
        super(StopAreaTranslation.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Collection<StopAreaTranslation> findAllNewTransaction() {
        Collection<StopAreaTranslation> translations = this.findAll();
        if (CollectionUtils.isNotEmpty(translations)) {
            translations.stream()
                    .filter(t -> StringUtils.isBlank(t.getFieldValue()))
                    .forEach(t -> Hibernate.initialize(t.getStopArea()));
        }
        return translations;
    }

}
