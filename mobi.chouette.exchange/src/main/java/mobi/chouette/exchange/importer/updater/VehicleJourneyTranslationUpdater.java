package mobi.chouette.exchange.importer.updater;

import mobi.chouette.model.VehicleJourneyTranslation;

import javax.ejb.Stateless;

@Stateless(name = VehicleJourneyTranslationUpdater.BEAN_NAME)
public class VehicleJourneyTranslationUpdater extends TranslationUpdater<VehicleJourneyTranslation> implements
        Updater<VehicleJourneyTranslation> {

    public static final String BEAN_NAME = "VehicleJourneyTranslationUpdater";

}