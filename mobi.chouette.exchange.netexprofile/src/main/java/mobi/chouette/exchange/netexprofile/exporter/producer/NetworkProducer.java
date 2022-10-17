package mobi.chouette.exchange.netexprofile.exporter.producer;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.ConversionUtil;
import org.rutebanken.netex.model.AuthorityRefStructure;
import org.rutebanken.netex.model.KeyValueStructure;
import org.rutebanken.netex.model.PrivateCodeStructure;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.time.LocalDateTime;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils.isSet;

public class NetworkProducer extends NetexProducer implements NetexEntityProducer<org.rutebanken.netex.model.Network, mobi.chouette.model.Network> {

    @Override
    public org.rutebanken.netex.model.Network produce(Context context, mobi.chouette.model.Network neptuneNetwork) {
        org.rutebanken.netex.model.Network netexNetwork = netexFactory.createNetwork();
        
        NetexProducerUtils.populateId(neptuneNetwork, netexNetwork);

        if (isSet(neptuneNetwork.getVersionDate())) {
            LocalDateTime changedDateTime = neptuneNetwork.getVersionDate().atStartOfDay();
            netexNetwork.setChanged(changedDateTime);
        }

        if (isSet(neptuneNetwork.getComment())) {
            KeyValueStructure keyValueStruct = netexFactory.createKeyValueStructure()
                    .withKey("Comment")
                    .withValue(neptuneNetwork.getComment());
            netexNetwork.setKeyList(netexFactory.createKeyListStructure().withKeyValue(keyValueStruct));
        }

        netexNetwork.setName(ConversionUtil.getMultiLingualString(neptuneNetwork.getName()));
        netexNetwork.setDescription(ConversionUtil.getMultiLingualString(neptuneNetwork.getDescription()));

        if(neptuneNetwork.getCompany() != null) {
            AuthorityRefStructure authorityRef = netexFactory.createAuthorityRefStructure();
        	NetexProducerUtils.populateReference(neptuneNetwork.getCompany(), authorityRef, true);

            // TODO : Check merge entur : J'ai récupéré le code de tuyauterie du netexFactory.createAuthorityRef(authorityRefStruct) a virer une fois la récup du netex-java-model faite
            JAXBElement<AuthorityRefStructure> authorityRefJaxb = new JAXBElement<AuthorityRefStructure>(new QName("http://www.netex.org.uk/netex", "AuthorityRef"), AuthorityRefStructure.class, (Class) null, authorityRef);
            netexNetwork.setTransportOrganisationRef(authorityRefJaxb);
        }

        if (isSet(neptuneNetwork.getRegistrationNumber())) {
            PrivateCodeStructure privateCodeStruct = netexFactory.createPrivateCodeStructure();
            privateCodeStruct.setValue(neptuneNetwork.getRegistrationNumber());
            netexNetwork.setPrivateCode(privateCodeStruct);
        }

        return netexNetwork;
    }
}
