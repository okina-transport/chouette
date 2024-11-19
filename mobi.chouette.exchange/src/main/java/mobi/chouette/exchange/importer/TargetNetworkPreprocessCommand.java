package mobi.chouette.exchange.importer;

import lombok.extern.slf4j.Slf4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.ObjectIdUtil;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.CompanyDAO;
import mobi.chouette.dao.NetworkDAO;
import mobi.chouette.exchange.parameters.AbstractImportParameter;
import mobi.chouette.model.Company;
import mobi.chouette.model.Network;
import mobi.chouette.model.type.OrganisationTypeEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Stateless(name = TargetNetworkPreprocessCommand.COMMAND)
@Slf4j
public class TargetNetworkPreprocessCommand implements Command {

    public static final String COMMAND = "TargetNetworkPreprocessCommand";
    public static final String DEFAULT_URL = "https://www.okina.fr";


    @EJB
    CompanyDAO companyDAO;

    @EJB
    NetworkDAO networkDAO;

    public TargetNetworkPreprocessCommand(CompanyDAO companyDAO, NetworkDAO networkDAO) {
        this.companyDAO = companyDAO;
        this.networkDAO = networkDAO;
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

        List<Company> companies = companyDAO.findByName(parameters.getTargetNetwork());
        List<Network> networks = networkDAO.findByName(parameters.getTargetNetwork());

        Referential referential = (Referential) context.get(REFERENTIAL);

        Company targetCompany;
        Network targetNetwork;
        if (CollectionUtils.isNotEmpty(companies)) {
            Optional<Company> optAuthority = companies.stream().filter(c -> c.getOrganisationType() == OrganisationTypeEnum.Authority).findFirst();
            if (optAuthority.isPresent()) {
                log.info("Found target company with name {} in database", parameters.getTargetNetwork());
                targetCompany = optAuthority.get();
            } else {
                log.warn("Found target company with name {} in database but it is an Operator company, create a new " +
                        "Authority company from it", parameters.getTargetNetwork());
                // there is one company named {targetNetwork} in database, but it is an operator company
                // we need an authority company
                targetCompany = new Company();
                targetCompany.setOrganisationType(OrganisationTypeEnum.Authority);
                targetCompany.setName(parameters.getTargetNetwork());
                // remove extra 'o' from operators objectid
                targetCompany.setObjectId(StringUtils.chop(companies.get(0).getObjectId()).replace(":Operator:", ":Authority:"));
                targetCompany.setRegistrationNumber(StringUtils.chop(companies.get(0).getRegistrationNumber()));
                targetCompany.setTimeZone(companies.get(0).getTimeZone());
                targetCompany.setLang(companies.get(0).getLang());
                targetCompany.setPhone(companies.get(0).getPhone());
                targetCompany.setUrl(companies.get(0).getUrl());
                targetCompany.setFareUrl(companies.get(0).getFareUrl());
                targetCompany.setEmail(companies.get(0).getEmail());
            }
            context.put(TARGET_COMPANY_OBJECT_ID, targetCompany.getObjectId());
            referential.getSharedCompanies().put(targetCompany.getObjectId(), targetCompany);
        } else {
            log.info("No company with name {} in database, create a default company with this name",
                    parameters.getTargetNetwork());
            String targetCompanyObjectId = ObjectIdUtil.composeNeptuneObjectId(parameters.getObjectIdPrefix(), "Authority", UUID.randomUUID().toString());
            targetCompany = ObjectFactory.getCompany(referential, targetCompanyObjectId);
            targetCompany.setName(parameters.getTargetNetwork());
            targetCompany.setUrl(DEFAULT_URL);
            context.put(TARGET_COMPANY_OBJECT_ID, targetCompanyObjectId);
        }

        if (CollectionUtils.isNotEmpty(networks)) {
            log.info("Found target network with name {} in database", parameters.getTargetNetwork());
            targetNetwork = networks.get(0);
            context.put(TARGET_NETWORK_OBJECT_ID, networks.get(0).getObjectId());
            referential.getSharedPTNetworks().put(targetNetwork.getObjectId(), targetNetwork);
        } else {
            log.info("No network with name {} in database, create a default network with this name", parameters.getTargetNetwork());
            String targetNetworkObjectId = ObjectIdUtil.composeNeptuneObjectId(parameters.getObjectIdPrefix(), "Network", UUID.randomUUID().toString());
            targetNetwork = ObjectFactory.getPTNetwork(referential, targetNetworkObjectId);
            targetNetwork.setName(parameters.getTargetNetwork());
            context.put(TARGET_NETWORK_OBJECT_ID, targetNetworkObjectId);
        }

        targetNetwork.setCompany(targetCompany);
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
