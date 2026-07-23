package mobi.chouette.exchange.importer.updater;

import mobi.chouette.common.CollectionUtil;
import mobi.chouette.common.Context;
import mobi.chouette.common.Pair;
import mobi.chouette.dao.CompanyDAO;
import mobi.chouette.model.Company;
import mobi.chouette.model.Network;
import mobi.chouette.model.NetworkTranslation;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.Collection;
import java.util.List;


@Stateless(name = PTNetworkUpdater.BEAN_NAME)
public class PTNetworkUpdater implements Updater<Network> {

    public static final String BEAN_NAME = "PTNetworkUpdater";

    @EJB(beanName = CompanyUpdater.BEAN_NAME)
    private Updater<Company> companyUpdater;

    @EJB
    private CompanyDAO companyDAO;

    @EJB(beanName = NetworkTranslationUpdater.BEAN_NAME)
    private Updater<NetworkTranslation> networkTranslationUpdater;

    @Override
    public void update(Context context, Network oldValue, Network newValue) throws Exception {

        if (newValue.isSaved()) {
            return;
        }
        newValue.setSaved(true);

        if (newValue.getObjectId() != null
                && !newValue.getObjectId().equals(oldValue.getObjectId())) {
            oldValue.setObjectId(newValue.getObjectId());
        }
        if (newValue.getObjectVersion() != null
                && !newValue.getObjectVersion().equals(
                oldValue.getObjectVersion())) {
            oldValue.setObjectVersion(newValue.getObjectVersion());
        }
        if (newValue.getCreationTime() != null
                && !newValue.getCreationTime().equals(
                oldValue.getCreationTime())) {
            oldValue.setCreationTime(newValue.getCreationTime());
        }
        if (newValue.getCreatorId() != null
                && !newValue.getCreatorId().equals(oldValue.getCreatorId())) {
            oldValue.setCreatorId(newValue.getCreatorId());
        }
        if (newValue.getName() != null
                && !newValue.getName().equals(oldValue.getName())) {
            oldValue.setName(newValue.getName());
        }
        if (newValue.getComment() != null
                && !newValue.getComment().equals(oldValue.getComment())) {
            oldValue.setComment(newValue.getComment());
        }
        if (newValue.getVersionDate() != null
                && !newValue.getVersionDate().equals(oldValue.getVersionDate())) {
            oldValue.setVersionDate(newValue.getVersionDate());
        }
        if (newValue.getDescription() != null
                && !newValue.getDescription().equals(oldValue.getDescription())) {
            oldValue.setDescription(newValue.getDescription());
        }
        if (newValue.getRegistrationNumber() != null
                && !newValue.getRegistrationNumber().equals(
                oldValue.getRegistrationNumber())) {
            oldValue.setRegistrationNumber(newValue.getRegistrationNumber());
        }
        if (newValue.getSourceType() != null
                && !newValue.getSourceType().equals(oldValue.getSourceType())) {
            oldValue.setSourceType(newValue.getSourceType());
        }
        if (newValue.getSourceName() != null
                && !newValue.getSourceName().equals(oldValue.getSourceName())) {
            oldValue.setSourceName(newValue.getSourceName());
        }
        if (newValue.getSourceIdentifier() != null
                && !newValue.getSourceIdentifier().equals(
                oldValue.getSourceIdentifier())) {
            oldValue.setSourceIdentifier(newValue.getSourceIdentifier());
        }

        // Company
        Referential cache = (Referential) context.get(CACHE);

        if (newValue.getCompany() == null) {
            oldValue.setCompany(null);
        } else {
            String objectId = newValue.getCompany().getObjectId();
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
            companyUpdater.update(context, oldValue.getCompany(), newValue.getCompany());
        }

        updateTranslations(context, oldValue, newValue);
    }

    private void updateTranslations(Context context, Network oldValue, Network newValue) throws Exception {
        Referential referential = (Referential) context.get(REFERENTIAL);
        List<NetworkTranslation> newTranslations = referential.getNetworkTranslationsByObjectId()
                .getOrDefault(newValue.getObjectId(), List.of());

        Collection<NetworkTranslation> addedTranslations = CollectionUtil.substract(
                newTranslations, oldValue.getTranslations(), NeptuneIdentifiedObjectComparator.INSTANCE);
        for (NetworkTranslation translation : addedTranslations) {
            translation.setNetwork(oldValue);
        }

        Collection<NetworkTranslation> removedTranslations = CollectionUtil.substract(
                oldValue.getTranslations(), newTranslations, NeptuneIdentifiedObjectComparator.INSTANCE);
        for (NetworkTranslation translation : removedTranslations) {
            oldValue.getTranslations().remove(translation);
        }

        Collection<Pair<NetworkTranslation, NetworkTranslation>> modifiedTranslations = CollectionUtil.intersection(
                oldValue.getTranslations(), newTranslations, NeptuneIdentifiedObjectComparator.INSTANCE);
        for (Pair<NetworkTranslation, NetworkTranslation> pair : modifiedTranslations) {
            networkTranslationUpdater.update(context, pair.getLeft(), pair.getRight());
        }
    }

}
