package mobi.chouette.exchange.netexprofile.importer;

import mobi.chouette.common.Context;
import mobi.chouette.dao.CodespaceDAO;
import mobi.chouette.dao.NetworkDAO;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.model.Network;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.apache.commons.lang.StringUtils;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collections;

@Test
public class NetexInitImportCommandTest {

    public static final String REFERENTIAL = "test";
    public static final String TARGET_NETWORK = "target_network";
    public static final String OBJECT_ID = String.format("%s:Network:666", REFERENTIAL);

    NetexInitImportCommand tested;
    CodespaceDAO codespaceDAOMock;
    NetworkDAO networkDAOMock;

    private final Network databaseNetwork;

    public NetexInitImportCommandTest() {
        databaseNetwork = new Network();
        databaseNetwork.setId(1L);
        databaseNetwork.setName(TARGET_NETWORK);
        databaseNetwork.setObjectId(OBJECT_ID);
        databaseNetwork.setSupprime(false);
    }

    @BeforeMethod
    public void beforeMethod() {
        // (re)create mock and tested command before each test
        codespaceDAOMock = Mockito.mock(CodespaceDAO.class);
        networkDAOMock = Mockito.mock(NetworkDAO.class);
        tested = new NetexInitImportCommand(codespaceDAOMock, networkDAOMock);
    }

    @DataProvider
    public Object[][] blankTargetNetwork() {
        return new Object[][]{
                { null },
                { "" },
                { "            " },
        };
    }

    @Test(dataProvider = "blankTargetNetwork", expectedExceptions = IllegalArgumentException.class)
    public void test_execute__when_context_target_network_is_blank__throws_illegal_argument_exception(String targetNetwork) throws Exception {
        // arrange
        Context context = new Context();
        NetexprofileImportParameters parameters = new NetexprofileImportParameters();
        parameters.setUseTargetNetwork(true);
        parameters.setTargetNetwork(targetNetwork);
        context.put(mobi.chouette.common.Constant.CONFIGURATION, parameters);

        Assert.assertTrue(StringUtils.isBlank(parameters.getTargetNetwork()), "targetNetwork should be blank");

        // act
        tested.execute(context);
    }

    @Test
    public void test_execute__when_context_target_network_is_set_and_an_active_network_named_target_network_exists__add_network_to_content() throws Exception {
        // arrange
        Context context = new Context();
        NetexprofileImportParameters parameters = new NetexprofileImportParameters();
        parameters.setTargetNetwork(TARGET_NETWORK);
        parameters.setUseTargetNetwork(true);


        context.put(Constant.CONFIGURATION, parameters);
        Mockito.when(networkDAOMock.findByNameAndNotSupprime(TARGET_NETWORK)).thenReturn(Collections.singletonList(databaseNetwork));

        // act
        tested.execute(context);

        // assert
        Referential referential = (Referential) context.get(Constant.REFERENTIAL);
        String objectId = (String) context.get(Constant.TARGET_NETWORK_OBJECT_ID);
        Assert.assertNotNull(objectId, String.format("%s should be added to context", Constant.TARGET_NETWORK_OBJECT_ID));

        Network newNetworkFromReferential = ObjectFactory.getPTNetwork(referential, objectId);
        Assert.assertEquals(newNetworkFromReferential, databaseNetwork, "network from database should be added to referential");
    }

    @Test
    public void test_execute__when_context_target_network_is_set_and_no_active_network_named_target_network_exists__add_network_to_content() throws Exception {
        // arrange
        Context context = new Context();
        NetexprofileImportParameters parameters = new NetexprofileImportParameters();
        parameters.setTargetNetwork(TARGET_NETWORK);
        parameters.setUseTargetNetwork(true);


        context.put(Constant.CONFIGURATION, parameters);
        Mockito.when(networkDAOMock.findByNameAndNotSupprime(TARGET_NETWORK)).thenReturn(null);

        // act
        tested.execute(context);

        // assert
        Referential referential = (Referential) context.get(Constant.REFERENTIAL);
        String objectId = (String) context.get(Constant.TARGET_NETWORK_OBJECT_ID);
        Assert.assertNotNull(objectId, String.format("%s should be added to context", Constant.TARGET_NETWORK_OBJECT_ID));

        Network newNetworkFromReferential = ObjectFactory.getPTNetwork(referential, objectId);
        Assert.assertNotNull(newNetworkFromReferential, "new network should be added to referential");
        Assert.assertNotEquals(newNetworkFromReferential, databaseNetwork, "new network should be added to referential");
        Assert.assertEquals(newNetworkFromReferential.getName(), parameters.getTargetNetwork(), "new network name should be equal to import parameter targetNetwork");
        Assert.assertEquals(newNetworkFromReferential.getObjectId(), objectId, "new network objectid should be equal to context " + Constant.TARGET_NETWORK_OBJECT_ID);
    }

}
