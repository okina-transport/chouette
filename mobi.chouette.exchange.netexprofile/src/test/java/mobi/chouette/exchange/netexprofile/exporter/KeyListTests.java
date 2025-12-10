package mobi.chouette.exchange.netexprofile.exporter;

import mobi.chouette.exchange.netexprofile.exporter.producer.KeyListStructureProducer;
import mobi.chouette.model.KeyValue;
import org.rutebanken.netex.model.KeyListStructure;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static mobi.chouette.common.Constant.EXTERNAL_REF;

public class KeyListTests {

    @Test
    public void checkOnEmptyKeyList() throws Exception {
        KeyListStructureProducer producer = new KeyListStructureProducer();
        KeyListStructure result = producer.produce(new ArrayList<>(), true);
        Assert.assertNull(result);
        result = producer.produce(new ArrayList<>(), false);
        Assert.assertNull(result);
    }

    @Test
    public void checkOnExternalIdKeyList() throws Exception {
        KeyListStructureProducer producer = new KeyListStructureProducer();
        List<KeyValue> chouetteKeyValues = new ArrayList<>();
        KeyValue kv = new KeyValue();
        kv.setKey(EXTERNAL_REF);
        kv.setValue("toto");
        kv.setTypeOfKey("ALTERNATIVE_IDENTIFIER");
        chouetteKeyValues.add(kv);
        KeyListStructure result = producer.produce(chouetteKeyValues, true);
        Assert.assertNotNull(result);
        Assert.assertEquals(1,result.getKeyValue().size());
        result = producer.produce(chouetteKeyValues, false);
        Assert.assertNull(result);
    }


}
