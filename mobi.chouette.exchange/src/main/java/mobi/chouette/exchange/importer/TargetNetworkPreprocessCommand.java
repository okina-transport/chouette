package mobi.chouette.exchange.importer;

import lombok.extern.slf4j.Slf4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.ObjectIdUtil;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.CompanyDAO;
import mobi.chouette.exchange.parameters.AbstractImportParameter;
import mobi.chouette.model.Company;
import mobi.chouette.model.Network;
import mobi.chouette.model.type.OrganisationTypeEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.ObjectIdTypes;
import mobi.chouette.model.util.Referential;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Stateless(name = TargetNetworkPreprocessCommand.COMMAND)
@Slf4j
public class TargetNetworkPreprocessCommand implements Command {

    public static final String COMMAND = "TargetNetworkPreprocessCommand";
    public static final String DEFAULT_URL = "https://www.okina.fr";


    @EJB
    CompanyDAO companyDAO;

    public TargetNetworkPreprocessCommand(CompanyDAO companyDAO) {
        this.companyDAO = companyDAO;
    }

    public TargetNetworkPreprocessCommand() {
    }

    @Override
    public boolean execute(Context context) throws Exception {
        AbstractImportParameter parameters = (AbstractImportParameter) context.get(CONFIGURATION);

        if (StringUtils.isBlank(parameters.getTargetNetwork())) {
            log.error("Import parameters useTargetNetwork is true but targetNetwork is blank");
            throw new IllegalArgumentException("Import parameters useTargetNetwork is true but targetNetwork is blank");
        }

        Referential referential = (Referential) context.get(REFERENTIAL);

        List<Company> companies = companyDAO.findActiveCompaniesByNameAndOrganisationType(parameters.getTargetNetwork(), OrganisationTypeEnum.Operator);

        // Look for active operator company from database with name equals to target network
        // (company is required by GTFS in order to fill agency_id properly)
        Company targetOperatorCompany;
        String targetCompanyOriginalId;
        if (CollectionUtils.isNotEmpty(companies)) {
            if (companies.size() > 1) {
                throw new IllegalStateException(String.format("There must be only active one operator company with " +
                                "name %s in database, make sure there is only one before restarting import",
                        parameters.getTargetNetwork()));
            }
            log.info("Found active operator company with name '{}' in database", parameters.getTargetNetwork());
            targetOperatorCompany = companies.get(0);
            targetCompanyOriginalId =
                    StringUtils.chomp(ObjectIdUtil.extractOriginalId(targetOperatorCompany.getObjectId()), "o");
        } else {
            log.info("No active operator company with name '{}' in database, create a default one and put it in " +
                            "referential", parameters.getTargetNetwork());
            targetCompanyOriginalId = UUID.randomUUID().toString();
            targetOperatorCompany = ObjectFactory.getCompany(referential,
                    ObjectIdUtil.composeNeptuneObjectId(parameters.getObjectIdPrefix(), ObjectIdTypes.OPERATOR_KEY,
                            targetCompanyOriginalId + "o"));
            targetOperatorCompany.setName(parameters.getTargetNetwork());
            targetOperatorCompany.setOrganisationType(OrganisationTypeEnum.Operator);
            targetOperatorCompany.setRegistrationNumber(targetCompanyOriginalId + "o");
            targetOperatorCompany.setUrl(DEFAULT_URL);
        }

        log.info("Target operator company objectId: '{}'", targetOperatorCompany.getObjectId());
        context.put(TARGET_COMPANY_OBJECT_ID, targetOperatorCompany.getObjectId());

        companies = companyDAO.findActiveCompaniesByNameAndOrganisationType(parameters.getTargetNetwork(),
                OrganisationTypeEnum.Authority);

        Company targetAuthorityCompany;
        if (CollectionUtils.isNotEmpty(companies)) {
            if (companies.size() > 1) {
                throw new IllegalStateException(String.format("There must be only one active authority company with " +
                                "name %s in database, make sure there is only one before restarting import",
                        parameters.getTargetNetwork()));
            }
            log.info("Found active authority company with name '{}' in database", parameters.getTargetNetwork());
            targetAuthorityCompany = companies.get(0);
        } else {
            log.info("No active authority company with name '{}' in database, create a default one and put it in " +
                            "referential", parameters.getTargetNetwork());
            targetAuthorityCompany = ObjectFactory.getCompany(referential,
                    ObjectIdUtil.composeNeptuneObjectId(parameters.getObjectIdPrefix(),
                            ObjectIdTypes.AUTHORITY_KEY, targetCompanyOriginalId));
            targetAuthorityCompany.setName(parameters.getTargetNetwork());
            targetAuthorityCompany.setOrganisationType(OrganisationTypeEnum.Authority);
            targetAuthorityCompany.setRegistrationNumber(targetCompanyOriginalId);
            targetOperatorCompany.setUrl(DEFAULT_URL);
        }

        log.info("Target authority company objectId: '{}'", targetAuthorityCompany.getObjectId());

        Network targetNetwork = ObjectFactory.getPTNetwork(referential, ObjectIdUtil.composeNeptuneObjectId(parameters.getObjectIdPrefix(), ObjectIdTypes.PTNETWORK_KEY, targetCompanyOriginalId));
        targetNetwork.setName(parameters.getTargetNetwork());
        targetNetwork.setCompany(targetAuthorityCompany);

        log.info("Target network objectId: '{}'", targetNetwork.getObjectId());
        context.put(TARGET_NETWORK_OBJECT_ID, targetNetwork.getObjectId());

        return true;
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = null;
            try {
                String name = "java:app/mobi.chouette.exchange/" + COMMAND;
                result = (Command) context.lookup(name);
            } catch (NamingException e) {
                // try another way on test context
                String name = "java:module/" + COMMAND;
                try {
                    result = (Command) context.lookup(name);
                } catch (NamingException e1) {
                    log.error(String.valueOf(e));
                }
            }
            return result;
        }
    }


    static {
        CommandFactory.factories.put(TargetNetworkPreprocessCommand.class.getName(), new TargetNetworkPreprocessCommand.DefaultCommandFactory());
    }


}
