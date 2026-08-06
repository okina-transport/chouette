package mobi.chouette.exchange.importer.updater;

import mobi.chouette.model.CompanyTranslation;

import javax.ejb.Stateless;

@Stateless(name = CompanyTranslationUpdater.BEAN_NAME)
public class CompanyTranslationUpdater extends TranslationUpdater<CompanyTranslation> implements Updater<CompanyTranslation> {

    public static final String BEAN_NAME = "CompanyTranslationUpdater";

}
