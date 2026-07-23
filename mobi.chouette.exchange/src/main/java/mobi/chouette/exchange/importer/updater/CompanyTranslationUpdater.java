package mobi.chouette.exchange.importer.updater;

import mobi.chouette.common.Context;
import mobi.chouette.dao.CompanyDAO;
import mobi.chouette.model.Company;
import mobi.chouette.model.CompanyTranslation;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless(name = CompanyTranslationUpdater.BEAN_NAME)
public class CompanyTranslationUpdater extends TranslationUpdater<CompanyTranslation> implements Updater<CompanyTranslation> {

    public static final String BEAN_NAME = "CompanyTranslationUpdater";

    @EJB
    private CompanyDAO companyDAO;

    @Override
    public void update(Context context, CompanyTranslation oldValue, CompanyTranslation newValue) throws Exception {
        super.update(context, oldValue, newValue);
        if (newValue.isSaved()) {
            return;
        }
        newValue.setSaved(true);

        if (newValue.getTargetObjectId() == null) {
            oldValue.setCompany(null);
            return;
        }

        Referential cache = (Referential) context.get(CACHE);
        String objectId = newValue.getTargetObjectId();
        Company company = cache.getCompanies().get(objectId);
        if (company == null) {
            company = companyDAO.findByObjectId(objectId);
            if (company != null) {
                cache.getCompanies().put(objectId, company);
            }
        }
        if (company == null) {
            company = ObjectFactory.getCompany(cache, objectId);
        }
        oldValue.setCompany(company);
    }

}
