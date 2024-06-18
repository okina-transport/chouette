package mobi.chouette.dao;


import mobi.chouette.model.AlternativeRegistrationNumber;


import java.util.Optional;


public interface AlternativeRegistrationNumberDAO extends GenericDAO<AlternativeRegistrationNumber> {

    Optional<AlternativeRegistrationNumber> findByOriginalRegistrationNumber(String originalRegistrationNumber);

}
