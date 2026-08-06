package mobi.chouette.exchange.importer.updater;

import mobi.chouette.model.VehicleJourneyAtStopTranslation;

import javax.ejb.Stateless;

@Stateless(name = VehicleJourneyAtStopTranslationUpdater.BEAN_NAME)
public class VehicleJourneyAtStopTranslationUpdater extends TranslationUpdater<VehicleJourneyAtStopTranslation> implements
        Updater<VehicleJourneyAtStopTranslation> {

    public static final String BEAN_NAME = "VehicleJourneyAtStopTranslationUpdater";

}