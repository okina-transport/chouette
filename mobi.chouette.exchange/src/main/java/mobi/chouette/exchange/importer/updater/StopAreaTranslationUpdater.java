package mobi.chouette.exchange.importer.updater;

import mobi.chouette.model.StopAreaTranslation;

import javax.ejb.Stateless;

@Stateless(name = StopAreaTranslationUpdater.BEAN_NAME)
public class StopAreaTranslationUpdater extends TranslationUpdater<StopAreaTranslation> implements
        Updater<StopAreaTranslation> {

    public static final String BEAN_NAME = "StopAreaTranslationUpdater";

}