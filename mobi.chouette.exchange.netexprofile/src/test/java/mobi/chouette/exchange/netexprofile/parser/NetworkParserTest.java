package mobi.chouette.exchange.netexprofile.parser;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.importer.NetexprofileImportParameters;
import mobi.chouette.exchange.netexprofile.jaxb.NetexXMLProcessingHelperFactory;
import mobi.chouette.exchange.netexprofile.util.NetexObjectUtil;
import mobi.chouette.exchange.netexprofile.util.NetexReferential;
import mobi.chouette.model.Line;
import mobi.chouette.model.Network;
import mobi.chouette.model.util.Referential;
import org.rutebanken.netex.model.Common_VersionFrameStructure;
import org.rutebanken.netex.model.CompositeFrame;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.netex.model.ServiceFrame;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Test
public class NetworkParserTest {

    public static final String REFERENTIAL = "test";

    // referential network
    public static final long ID = 666L;
    public static final String TARGET_NETWORK = "target_network";
    public static final String OBJECT_ID = String.format("%s:Network:666", REFERENTIAL);
    public static final boolean SUPPRIME = false;

    NetworkParser tested = new NetworkParser();

    private final Network referentialNetwork;
    private final Line referentialLine;

    public NetworkParserTest() {
        referentialNetwork = new Network();
        referentialNetwork.setId(ID);
        referentialNetwork.setName(TARGET_NETWORK);
        referentialNetwork.setObjectId(OBJECT_ID);
        referentialNetwork.setSupprime(SUPPRIME);

        referentialLine = new Line();
        referentialLine.setId(1L);
        referentialLine.setNetwork(referentialNetwork);
    }

    private org.rutebanken.netex.model.Network parseNetexNetwork(Context context) throws XMLStreamException, JAXBException, IOException, SAXException {
        NetexXMLProcessingHelperFactory helperFactory = new NetexXMLProcessingHelperFactory();
        PublicationDeliveryStructure pds = helperFactory.unmarshal(new File("src/test/data/C_NETEX_1.xml"), new HashSet<>(), context);
        JAXBElement<CompositeFrame> cf =  (JAXBElement<CompositeFrame>) pds.getDataObjects().getCompositeFrameOrCommonFrame().get(0);
        List<JAXBElement<? extends Common_VersionFrameStructure>> frames = cf.getValue().getFrames().getCommonFrame();
        List<ServiceFrame> serviceFrames = NetexObjectUtil.getFrames(ServiceFrame.class, frames);
        return serviceFrames.get(0).getNetwork();
    }

    private Context buildContext(Referential referential) throws XMLStreamException, JAXBException, IOException, SAXException {
        NetexReferential netexReferential = new NetexReferential();
        NetexprofileImportParameters parameters = new NetexprofileImportParameters();

        Context context = new Context();
        context.put(Constant.REFERENTIAL, referential);
        context.put(Constant.NETEX_REFERENTIAL, netexReferential);
        context.put(Constant.CONFIGURATION, parameters);
        context.put(Constant.STREAM_TO_CLOSE, new ArrayList<>());

        org.rutebanken.netex.model.Network netexNetwork = parseNetexNetwork(context);
        context.put(Constant.NETEX_LINE_DATA_CONTEXT, netexNetwork);

        return context;
    }

    public void test_parse__when_context_target_network_object_id_is_not_null__use_referential_network() throws Exception {
        // arrange
        Referential referential = new Referential();
        referential.getSharedPTNetworks().put(referentialNetwork.getObjectId(), referentialNetwork);
        Context context = buildContext(referential);
        context.put(Constant.TARGET_NETWORK_OBJECT_ID, referentialNetwork.getObjectId());

        Assert.assertSame(referential.getSharedPTNetworks().get(OBJECT_ID), referentialNetwork, "referential should contain referential network");

        // act
        tested.parse(context);

        // assert
        Assert.assertEquals(referential.getSharedPTNetworks().size(), 1, "no new network should be added to referential");
        Assert.assertSame(referential.getSharedPTNetworks().get(OBJECT_ID), referentialNetwork, "referential should still contain referential network");
        Assert.assertEquals((long) referentialNetwork.getId(), ID, "referential network shall not be updated");
        Assert.assertEquals(referentialNetwork.getName(), TARGET_NETWORK, "referential network shall not be updated") ;
        Assert.assertEquals(referentialNetwork.getObjectId(), OBJECT_ID, "referential network shall not be updated") ;
        Assert.assertEquals((boolean) referentialNetwork.getSupprime(), SUPPRIME, "referential network shall not be updated");
        Assert.assertTrue(referentialNetwork.getLines().stream().anyMatch(referentialLine::equals), "should keep line(s) present before parsing");
        Assert.assertEquals(referentialNetwork.getLines().size(), 2, "should add parsed lines to referential network");
    }


}
