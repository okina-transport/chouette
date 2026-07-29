package mobi.chouette.dao;

import mobi.chouette.model.LineTranslation;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.Collection;

@Stateless(name = "LineTranslationDAO")
public class LineTranslationDAOImpl extends GenericDAOImpl<LineTranslation> implements LineTranslationDAO {

    public LineTranslationDAOImpl() {
        super(LineTranslation.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Collection<LineTranslation> findAllNewTransaction() {
        Collection<LineTranslation> translations = this.findAll();
        if (CollectionUtils.isNotEmpty(translations)) {
            translations.stream()
                    .filter(t -> StringUtils.isBlank(t.getFieldValue()))
                    .forEach(t -> Hibernate.initialize(t.getLine()));
        }
        return translations;
    }

}
