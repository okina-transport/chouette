package mobi.chouette.exchange.netexprofile.exporter;

import mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils;
import mobi.chouette.model.Line;
import mobi.chouette.model.type.TadEnum;
import org.rutebanken.netex.model.FlexibleLineRefStructure;
import org.rutebanken.netex.model.LineRefStructure;
import org.rutebanken.netex.model.ObjectFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBElement;

import static org.testng.Assert.assertEquals;

public class NetexProducerUtilsLineRefTest {

    private final ObjectFactory netexFactory = new ObjectFactory();

    @DataProvider(name = "tadData")
    public Object[][] tadData() {
        return new Object[][]{
                {TadEnum.FULL_TAD, true, "FlexibleLineRef"},
                {TadEnum.PARTIAL_TAD, false, "FlexibleLineRef"},
                {TadEnum.NO_TAD, false, "LineRef"},
                {null, null, "LineRef"},
        };
    }

    @Test(dataProvider = "tadData")
    public void createLineIDFMRefUsesTad(TadEnum tad, Boolean flexibleService, String expectedElement) {
        Line line = new Line();
        line.setObjectId("DATASET:Line:2153:LOC");
        line.setTad(tad);
        line.setFlexibleService(flexibleService);

        JAXBElement<? extends LineRefStructure> lineRef = NetexProducerUtils.createLineIDFMRef(line, netexFactory);

        assertEquals(lineRef.getName().getLocalPart(), expectedElement);
        assertEquals(lineRef.getValue() instanceof FlexibleLineRefStructure, "FlexibleLineRef".equals(expectedElement));
        assertEquals(lineRef.getValue().getRef(), "DATASET:Line:2153:LOC");
    }
}
