package mobi.chouette.exchange.importer.updater;

import mobi.chouette.common.Context;
import mobi.chouette.model.TripCompany;

import javax.ejb.Stateless;

@Stateless(name = TripCompanyUpdater.BEAN_NAME)
public class TripCompanyUpdater implements Updater<TripCompany> {

    public static final String BEAN_NAME = "TripCompanyUpdater";

    @Override
    public void update(Context context, TripCompany oldValue, TripCompany newValue) throws Exception {
        if (newValue.isSaved()) {
            return;
        }
        newValue.setSaved(true);

        if (newValue.getObjectId() != null && !newValue.getObjectId().equals(oldValue.getObjectId())) {
            oldValue.setObjectId(newValue.getObjectId());
        }
		if (newValue.getOriginalCompanyId() != null && !newValue.getOriginalCompanyId().equals(oldValue.getOriginalCompanyId())) {
			oldValue.setOriginalCompanyId(newValue.getOriginalCompanyId());
		}
        if (newValue.getName() != null && !newValue.getName().equals(oldValue.getName())) {
            oldValue.setName(newValue.getName());
        }
        if (newValue.getAddress() != null && !newValue.getAddress().equals(oldValue.getAddress())) {
            oldValue.setAddress(newValue.getAddress());
        }
        if (newValue.getZipcode() != null && !newValue.getZipcode().equals(oldValue.getZipcode())) {
            oldValue.setZipcode(newValue.getZipcode());
        }
        if (newValue.getCity() != null && !newValue.getCity().equals(oldValue.getCity())) {
            oldValue.setCity(newValue.getCity());
        }
        if (newValue.getPhone() != null && !newValue.getPhone().equals(oldValue.getPhone())) {
            oldValue.setPhone(newValue.getPhone());
        }
        if (newValue.getEmail() != null && !newValue.getEmail().equals(oldValue.getEmail())) {
            oldValue.setEmail(newValue.getEmail());
        }
    }

}
