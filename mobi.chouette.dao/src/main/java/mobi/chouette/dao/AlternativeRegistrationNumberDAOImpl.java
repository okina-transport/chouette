package mobi.chouette.dao;


import mobi.chouette.model.AlternativeRegistrationNumber;


import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import java.util.List;
import java.util.Optional;


@Stateless
public class AlternativeRegistrationNumberDAOImpl extends GenericDAOImpl<AlternativeRegistrationNumber> implements AlternativeRegistrationNumberDAO {

    public AlternativeRegistrationNumberDAOImpl() {
        super(AlternativeRegistrationNumber.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }


    @Override
    public Optional<AlternativeRegistrationNumber> findByOriginalRegistrationNumber(String originalRegistrationNumber) {


        Query query = em.createNativeQuery("SELECT * FROM alternative_registration_numbers WHERE original_registration_number = :originalRegistrationNumberParam", AlternativeRegistrationNumber.class);
        query.setParameter("originalRegistrationNumberParam", originalRegistrationNumber);

        List<AlternativeRegistrationNumber> results = query.getResultList();

        if (results.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of((AlternativeRegistrationNumber) results.get(0));
    }
}
