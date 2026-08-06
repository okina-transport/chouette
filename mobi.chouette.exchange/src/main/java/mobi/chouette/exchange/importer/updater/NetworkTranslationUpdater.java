package mobi.chouette.exchange.importer.updater;

import mobi.chouette.model.NetworkTranslation;

import javax.ejb.Stateless;

@Stateless(name = NetworkTranslationUpdater.BEAN_NAME)
public class NetworkTranslationUpdater extends TranslationUpdater<NetworkTranslation> implements Updater<NetworkTranslation> {

    public static final String BEAN_NAME = "NetworkTranslationUpdater";

}