package mobi.chouette.exchange.importer.updater;

import mobi.chouette.model.LineTranslation;

import javax.ejb.Stateless;

@Stateless(name = LineTranslationUpdater.BEAN_NAME)
public class LineTranslationUpdater extends TranslationUpdater<LineTranslation> implements Updater<LineTranslation> {

    public static final String BEAN_NAME = "LineTranslationUpdater";

}
