package mobi.chouette.exchange.importer.updater;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.TripCompanyDAO;
import mobi.chouette.model.Line;
import mobi.chouette.model.TripCompany;
import mobi.chouette.model.TripExtension;
import mobi.chouette.model.util.Referential;

import javax.ejb.EJB;
import javax.ejb.Stateless;

@Log4j
@Stateless(name = TripExtensionUpdater.BEAN_NAME)
public class TripExtensionUpdater implements Updater<TripExtension> {

    public static final String BEAN_NAME = "TripExtensionUpdater";

    @EJB
    private LineDAO lineDAO;

    @EJB
    private TripCompanyDAO tripCompanyDAO;

    @Override
    public void update(Context context, TripExtension oldValue, TripExtension newValue) throws Exception {
        if (newValue.isSaved()) {
            return;
        }
        newValue.setSaved(true);

        Referential cache = (Referential) context.get(CACHE);

        if (newValue.getObjectId() != null && !newValue.getObjectId().equals(oldValue.getObjectId())) {
            oldValue.setObjectId(newValue.getObjectId());
        }
        if (newValue.getIndicReservation() != null && !newValue.getIndicReservation().equals(oldValue.getIndicReservation())) {
            oldValue.setIndicReservation(newValue.getIndicReservation());
        }

        // Line (trip_extensions.txt route_id)
        if (newValue.getLine() == null) {
            oldValue.setLine(null);
        } else {
            String objectId = newValue.getLine().getObjectId();
            Line line = cache.getLines().get(objectId);
            if (line == null) {
                line = lineDAO.findByObjectId(objectId);
                if (line != null) {
                    cache.getLines().put(objectId, line);
                }
            }
            if (line != null) {
                oldValue.setLine(line);
            }
        }

        if (newValue.getContractCompany() == null) {
            oldValue.setContractCompany(null);
        } else {
            TripCompany contractCompany = resolveTripCompany(cache, newValue.getContractCompany());
            if (contractCompany != null) {
                oldValue.setContractCompany(contractCompany);
            }
        }

        if (newValue.getExecCompany() == null) {
            oldValue.setExecCompany(null);
        } else {
            TripCompany execCompany = resolveTripCompany(cache, newValue.getExecCompany());
            if (execCompany != null) {
                oldValue.setExecCompany(execCompany);
            }
        }
    }

    private TripCompany resolveTripCompany(Referential cache, TripCompany newCompany) {
        if (newCompany == null) {
            return null;
        }
        String objectId = newCompany.getObjectId();
        TripCompany company = cache.getSharedTripCompanies().get(objectId);
        if (company == null) {
            company = tripCompanyDAO.findByObjectId(objectId);
            if (company != null) {
                cache.getSharedTripCompanies().put(objectId, company);
            }
        }
        if (company == null) {
            log.warn("trip_extensions.txt references unknown company " + objectId + " - ignoring reference");
            return null;
        }
        return company;
    }

}
