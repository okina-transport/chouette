package mobi.chouette.dao;

import mobi.chouette.model.VehicleJourneyTranslation;

import java.util.Collection;

public interface VehicleJourneyTranslationDAO extends GenericDAO<VehicleJourneyTranslation> {

    Collection<VehicleJourneyTranslation> findAllNewTransaction();

}
