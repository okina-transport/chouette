package mobi.chouette.exchange.importer.updater;

import mobi.chouette.dao.AgencyDAO;
import mobi.chouette.dao.FareAttributeDAO;
import mobi.chouette.model.Agency;
import mobi.chouette.model.FareAttribute;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class FareAttributeOptimiser {

    @EJB
    private AgencyDAO agencyDAO;

    @EJB
    private FareAttributeDAO fareAttributeDAO;

    public void initialize(Referential cache, Referential referential) {
        initializeAgency(cache, referential.getAgencies().values());
        initializeFareAttribute(cache, referential.getFareAttributes().values());
    }

    private void initializeAgency(Referential cache, Collection<Agency> list) {
        if (list != null && !list.isEmpty()) {
            List<String> ids = list.stream()
                    .filter(agency -> agency.getAgencyId() != null)
                    .map(agency -> agency.getAgencyId())
                    .collect(Collectors.toList());

            List<Agency> objects = agencyDAO.findByAgencyIds(ids);
            for (Agency object : objects) {
                cache.getAgencies().put(object.getAgencyId(), object);
            }

            for (Agency item : list) {
                Agency object = cache.getAgencies().get(item.getAgencyId());
                if (object == null) {
                    object = ObjectFactory.getAgency(cache, item.getAgencyId());
                }
            }
        }
    }

    private void initializeFareAttribute(Referential cache, Collection<FareAttribute> list) {
        if (list != null && !list.isEmpty()) {
            List<String> ids = list.stream()
                    .filter(agency -> agency.getObjectId() != null)
                    .map(agency -> agency.getObjectId())
                    .collect(Collectors.toList());

            List<FareAttribute> objects = fareAttributeDAO.findByObjectId(ids);
            for (FareAttribute object : objects) {
                cache.getFareAttributes().put(object.getObjectId(), object);
            }

            for (FareAttribute item : list) {
                FareAttribute object = cache.getFareAttributes().get(item.getObjectId());
                if (object == null) {
                    object = ObjectFactory.getFareAttribute(cache, item.getObjectId());
                }
            }
        }
    }
}
