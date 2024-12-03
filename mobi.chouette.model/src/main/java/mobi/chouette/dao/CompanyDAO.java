package mobi.chouette.dao;

import mobi.chouette.model.Company;
import mobi.chouette.model.type.OrganisationTypeEnum;

import java.util.List;

public interface CompanyDAO extends GenericDAO<Company> {

        List<Company> findActiveCompaniesByNameAndOrganisationType(String name, OrganisationTypeEnum organisationType);

}
