package mobi.chouette.dao;

import mobi.chouette.model.VehicleJourneyTranslation;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.Collection;

@Stateless(name = "VehicleJourneyTranslationDAO")
public class VehicleJourneyTranslationDAOImpl extends GenericDAOImpl<VehicleJourneyTranslation> implements VehicleJourneyTranslationDAO {

    public VehicleJourneyTranslationDAOImpl() {
        super(VehicleJourneyTranslation.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Collection<VehicleJourneyTranslation> findAllNewTransaction() {
        Collection<VehicleJourneyTranslation> translations = this.findAll();
        if (CollectionUtils.isNotEmpty(translations)) {
            translations.stream()
                    .filter(t -> StringUtils.isBlank(t.getFieldValue()))
                    .forEach(t -> Hibernate.initialize(t.getVehicleJourney()));
        }
        return translations;
    }

}
