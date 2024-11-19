package mobi.chouette.exchange.importer;


import mobi.chouette.common.Context;
import mobi.chouette.dao.CompanyDAO;
import mobi.chouette.dao.NetworkDAO;
import mobi.chouette.exchange.parameters.AbstractImportParameter;
import mobi.chouette.model.Company;
import mobi.chouette.model.Network;
import mobi.chouette.model.type.OrganisationTypeEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static mobi.chouette.common.Constant.*;

public class TargetNetworkPreprocessCommandTest {

    public static final String OBJECT_ID_PREFIX = "TEST:";
    public static final String REGEX_NEW_COMPANY_OBJECT_ID = String.format("%s:Authority:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", OBJECT_ID_PREFIX);
    public static final String REGEX_NEW_NETWORK_OBJECT_ID = String.format("%s:Network:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", OBJECT_ID_PREFIX);
    private final Company dbCompanyAuthority;
    private final Company dbCompanyOperator;
    private final Network dbNetwork;
    TargetNetworkPreprocessCommand tested;
    private CompanyDAO companyDAOMock;
    private NetworkDAO networkDAOMock;


    public TargetNetworkPreprocessCommandTest() {

        dbCompanyAuthority = new Company();
        dbCompanyAuthority.setName("Target");
        dbCompanyAuthority.setObjectId("TEST:Authority:666");
        dbCompanyAuthority.setOrganisationType(OrganisationTypeEnum.Authority);

        dbCompanyOperator = new Company();
        dbCompanyOperator.setName("Target");
        dbCompanyOperator.setObjectId("TEST:Operator:666o");
        dbCompanyOperator.setOrganisationType(OrganisationTypeEnum.Operator);
        dbCompanyOperator.setRegistrationNumber("RNo");
        dbCompanyOperator.setTimeZone("Europe/Paris");
        dbCompanyOperator.setLang("FR");
        dbCompanyOperator.setUrl("https://www.url.fr");
        dbCompanyOperator.setFareUrl("https://www.fare.fr");
        dbCompanyOperator.setEmail("email@email.com");

        dbNetwork = new Network();
        dbNetwork.setName("Target");
        dbNetwork.setObjectId("TEST:Network:666");
        dbNetwork.setCompany(dbCompanyAuthority);

    }

    @BeforeMethod
    private void beforeMethod() {
        companyDAOMock = Mockito.mock(CompanyDAO.class);
        networkDAOMock = Mockito.mock(NetworkDAO.class);
        tested = new TargetNetworkPreprocessCommand(companyDAOMock, networkDAOMock);
    }

    private Context buildContext(String targetNetwork) {
        Context context = new Context();

        AbstractImportParameter parameter = new AbstractImportParameter();
        parameter.setTargetNetwork(targetNetwork);
        parameter.setUseTargetNetwork(true);
        parameter.setObjectIdPrefix(OBJECT_ID_PREFIX);

        context.put(CONFIGURATION, parameter);
        context.put(REFERENTIAL, new Referential());

        return context;
    }

    @DataProvider
    public Object[][] targetNetworkIsEmptyOrBlank() {
        return new Object[][]{
                {null},
                {""},
                {"            "},
        };
    }

    @Test(dataProvider = "targetNetworkIsEmptyOrBlank", expectedExceptions = IllegalArgumentException.class)
    public void testExecute__whenTargetNetworkIsEmptyOrBlank__throwsIllegalArgumentException(String targetNetwork) throws Exception {
        // Arrange
        Context ctx = buildContext(targetNetwork);
        Assert.assertTrue("targetNetwork should be blank", StringUtils.isBlank(targetNetwork));

        // Act
        tested.execute(ctx);
    }

    @DataProvider
    public Object[][] authorityCompanyWithNameAndNetworkWithNameExistInDatabase() {
        return new Object[][]{
                {"Target", Arrays.asList(dbCompanyAuthority, dbCompanyOperator), Collections.singletonList(dbNetwork)},
                {"Target", Arrays.asList(dbCompanyOperator, dbCompanyAuthority), Collections.singletonList(dbNetwork)},
                {"Target", Collections.singletonList(dbCompanyAuthority), Collections.singletonList(dbNetwork)},
        };
    }

    @Test(dataProvider = "authorityCompanyWithNameAndNetworkWithNameExistInDatabase")
    public void testExecute__whenAuthorityCompanyWithNameAndNetworkWithNameExistInDatabase__thenPutsTheirObjectIdInContext(String targetNetwork, List<Company> dbCompanies, List<Network> dbNetworks) throws Exception {
        // Arrange
        Context ctx = buildContext(targetNetwork);
        Mockito.when(companyDAOMock.findByName(targetNetwork)).thenReturn(dbCompanies);
        Mockito.when(networkDAOMock.findByName(targetNetwork)).thenReturn(dbNetworks);

        // Act
        tested.execute(ctx);

        // Assert
        String targetCompanyObjectId = (String) ctx.get(TARGET_COMPANY_OBJECT_ID);
        String targetNetworkObjectId = (String) ctx.get(TARGET_NETWORK_OBJECT_ID);

        Assert.assertEquals("should put Authority db company object id in ctx", dbCompanyAuthority.getObjectId(),
                targetCompanyObjectId);
        Assert.assertEquals("should put db network object id in ctx", dbNetwork.getObjectId(),
                targetNetworkObjectId);

        Referential referential = (Referential) ctx.get(REFERENTIAL);
        Company targetCompanyFromRef = ObjectFactory.getCompany(referential, targetCompanyObjectId);
        Network targetNetworkFromRef = ObjectFactory.getPTNetwork(referential, targetNetworkObjectId);

        Assert.assertEquals("should put company in referential", dbCompanyAuthority,
                targetCompanyFromRef);
        Assert.assertEquals("should put network in referential", dbNetwork,
                targetNetworkFromRef);

        Assert.assertEquals("target network from ref should belong to target company from ref", targetCompanyFromRef,
                targetNetworkFromRef.getCompany());
    }

    @DataProvider
    public static Object[][] noCompanyAndNoNetworkWithNameExistInDatabase() {
        return new Object[][]{
                {"Target", null, null},
                {"Target", null, Collections.emptyList()},
                {"Target", Collections.emptyList(), null},
                {"Target", Collections.emptyList(), Collections.emptyList()},
        };
    }

    @Test(dataProvider = "noCompanyAndNoNetworkWithNameExistInDatabase")
    public void testExecute__whenNoCompanyAndNoNetworkWithNameExistInDatabase__thenCreateNewObjectIdsAndEntities(String targetNetwork,
                                                                                                                 List<Company> dbCompanies, List<Network> dbNetworks) throws Exception {
        // Arrange
        Context ctx = buildContext(targetNetwork);
        Mockito.when(companyDAOMock.findByName(targetNetwork)).thenReturn(dbCompanies);
        Mockito.when(networkDAOMock.findByName(targetNetwork)).thenReturn(dbNetworks);

        // Act
        tested.execute(ctx);

        // Assert
        String targetCompanyObjectId = (String) ctx.get(TARGET_COMPANY_OBJECT_ID);
        String targetNetworkObjectId = (String) ctx.get(TARGET_NETWORK_OBJECT_ID);

        Assert.assertTrue("object id should match pattern",
                targetCompanyObjectId.matches(REGEX_NEW_COMPANY_OBJECT_ID));
        Assert.assertTrue("object id should match pattern",
                targetNetworkObjectId.matches(REGEX_NEW_NETWORK_OBJECT_ID));

        Referential referential = (Referential) ctx.get(REFERENTIAL);
        Company targetCompanyFromRef = ObjectFactory.getCompany(referential, targetCompanyObjectId);
        Network targetNetworkFromRef = ObjectFactory.getPTNetwork(referential, targetNetworkObjectId);

        Assert.assertNull("should create a new company", targetCompanyFromRef.getId());
        Assert.assertEquals("company from ref should have correct object id", targetCompanyObjectId,
                targetCompanyFromRef.getObjectId());
        Assert.assertEquals("company name from ref should be target network", targetNetwork, targetCompanyFromRef.getName());

        Assert.assertNull("should create a new network", targetNetworkFromRef.getId());
        Assert.assertEquals("network from ref should have correct object id", targetNetworkObjectId,
                targetNetworkFromRef.getObjectId());
        Assert.assertEquals("network name from ref should be target network", targetNetwork, targetNetworkFromRef.getName());

        Assert.assertEquals("target network from ref should belong to target company from ref", targetCompanyFromRef,
                targetNetworkFromRef.getCompany());

    }

    @DataProvider
    public Object[][] operatorCompanyWithNameExistInDatabase() {
        return new Object[][]{
                {"Target", Collections.singletonList(dbCompanyOperator), Collections.singletonList(dbNetwork)},
                {"Target", Collections.singletonList(dbCompanyOperator), null},
        };
    }

    @Test(dataProvider = "operatorCompanyWithNameExistInDatabase")
    public void testExecute__whenOperatorCompanyWithNameExistInDatabase__thenCreateAuthorityCompanyFromOperator(String targetNetwork, List<Company> dbCompanies, List<Network> dbNetworks) throws Exception {
        // Arrange
        Context ctx = buildContext(targetNetwork);
        Mockito.when(companyDAOMock.findByName(targetNetwork)).thenReturn(dbCompanies);
        Mockito.when(networkDAOMock.findByName(targetNetwork)).thenReturn(dbNetworks);

        // Act
        tested.execute(ctx);

        // Assert
        String targetCompanyObjectId = (String) ctx.get(TARGET_COMPANY_OBJECT_ID);

        Referential referential = (Referential) ctx.get(REFERENTIAL);
        Company targetCompanyFromRef = ObjectFactory.getCompany(referential, targetCompanyObjectId);

        Assert.assertEquals("should put company in referential", dbCompanyAuthority,
                targetCompanyFromRef);
        Assert.assertNull("should be a new company", targetCompanyFromRef.getId());
        Assert.assertSame("should be an authority company", OrganisationTypeEnum.Authority,
                targetCompanyFromRef.getOrganisationType());
        Assert.assertEquals("name should be target network", targetNetwork,
                targetCompanyFromRef.getName());
        Assert.assertEquals("object id should be valid", "TEST:Authority:666",
                targetCompanyFromRef.getObjectId());
        Assert.assertEquals("registration number should be valid", "RN", targetCompanyFromRef.getRegistrationNumber());
        Assert.assertEquals("should copy timezone from operator company", dbCompanyOperator.getTimeZone(),
                targetCompanyFromRef.getTimeZone());
        Assert.assertEquals("should copy lang from operator company", dbCompanyOperator.getLang(),
                targetCompanyFromRef.getLang());
        Assert.assertEquals("should copy phone from operator company", dbCompanyOperator.getPhone(),
                targetCompanyFromRef.getPhone());
        Assert.assertEquals("should copy url from operator company", dbCompanyOperator.getUrl(),
                targetCompanyFromRef.getUrl());
        Assert.assertEquals("should copy fare url from operator company", dbCompanyOperator.getFareUrl(),
                targetCompanyFromRef.getFareUrl());
        Assert.assertEquals("should copy email from operator company", dbCompanyOperator.getEmail(),
                targetCompanyFromRef.getEmail());


    }


}


