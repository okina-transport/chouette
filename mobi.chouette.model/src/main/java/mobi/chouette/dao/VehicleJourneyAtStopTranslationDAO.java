package mobi.chouette.dao;

import mobi.chouette.model.VehicleJourneyAtStopTranslation;

import java.util.List;

public interface VehicleJourneyAtStopTranslationDAO extends GenericDAO<VehicleJourneyAtStopTranslation> {

    void persistTranslations(List<VehicleJourneyAtStopTranslation> translations);
}
