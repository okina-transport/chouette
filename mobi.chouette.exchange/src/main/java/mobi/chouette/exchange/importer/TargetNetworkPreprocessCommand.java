package mobi.chouette.exchange.importer;

import lombok.extern.slf4j.Slf4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.ObjectIdUtil;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.CompanyDAO;
import mobi.chouette.exchange.parameters.AbstractImportParameter;
import mobi.chouette.exchange.report.AnalyzeReport;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.model.Company;
import mobi.chouette.model.Network;
import mobi.chouette.model.type.OrganisationTypeEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.ObjectIdTypes;
import mobi.chouette.model.util.Referential;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.List;

@Stateless(name = TargetNetworkPreprocessCommand.COMMAND)
@Slf4j
public class TargetNetworkPreprocessCommand implements Command {

    public static final String COMMAND = "TargetNetworkPreprocessCommand";
    public static final String DEFAULT_URL = "https://www.okina.fr";

    static {
        CommandFactory.factories.put(TargetNetworkPreprocessCommand.class.getName(), new TargetNetworkPreprocessCommand.DefaultCommandFactory());
    }

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
        ActionReporter reporter = ActionReporter.Factory.getInstance();

        if (StringUtils.isBlank(parameters.getTargetNetwork())) {
            return reportInvalidTargetNetwork(context, reporter, "Le réseau ciblé n'est pas renseigné");
        }

        Referential referential = (Referential) context.get(REFERENTIAL);

        List<Company> companies = companyDAO.findActiveCompaniesByNameAndOrganisationType(parameters.getTargetNetwork(), OrganisationTypeEnum.Operator);

        // Look for active operator company from database with name equals to target network
        // (company is required by GTFS in order to fill agency_id properly)
        Company targetOperatorCompany;
        String targetCompanyOriginalId;
        if (CollectionUtils.isNotEmpty(companies)) {
            if (companies.size() > 1) {
                return reportInvalidTargetNetwork(context, reporter, String.format("Plusieurs transporteurs actifs " +
                        "portent le nom '%s' : il ne doit y en avoir qu'un seul pour pouvoir cibler ce réseau",
                        parameters.getTargetNetwork()));
            }
            log.info("Found active operator company with name '{}' in database", parameters.getTargetNetwork());
            targetOperatorCompany = companies.get(0);
            targetCompanyOriginalId =
                    StringUtils.chomp(ObjectIdUtil.extractOriginalId(targetOperatorCompany.getObjectId()), "o");
        } else {
            return reportInvalidTargetNetwork(context, reporter, String.format("Le réseau ciblé '%s' n'existe pas : " +
                    "aucun transporteur actif ne porte ce nom", parameters.getTargetNetwork()));
        }

        log.info("Target operator company objectId: '{}'", targetOperatorCompany.getObjectId());
        context.put(TARGET_COMPANY_OBJECT_ID, targetOperatorCompany.getObjectId());

        companies = companyDAO.findActiveCompaniesByNameAndOrganisationType(parameters.getTargetNetwork(),
                OrganisationTypeEnum.Authority);

        Company targetAuthorityCompany;
        if (CollectionUtils.isNotEmpty(companies)) {
            if (companies.size() > 1) {
                return reportInvalidTargetNetwork(context, reporter, String.format("Plusieurs autorités organisatrices " +
                        "actives portent le nom '%s' : il ne doit y en avoir qu'une seule pour pouvoir cibler ce réseau",
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

    private boolean reportInvalidTargetNetwork(Context context, ActionReporter reporter, String message) {
        log.error(message);
        AnalyzeReport analyzeReport = (AnalyzeReport) context.get(ANALYSIS_REPORT);
        if (analyzeReport != null) {
            analyzeReport.setTargetNetworkError(message);
            return SUCCESS;
        }
        reporter.setActionError(context, ActionReporter.ERROR_CODE.INVALID_PARAMETERS, message);
        return ERROR;
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


}
