package mobi.chouette.exchange.netexprofile.exporter.producer;

import mobi.chouette.common.Context;
import mobi.chouette.common.TimeUtil;
import mobi.chouette.exchange.netexprofile.ConversionUtil;
import mobi.chouette.model.Line;
import org.rutebanken.netex.model.AuthorityRefStructure;
import org.rutebanken.netex.model.KeyValueStructure;
import org.rutebanken.netex.model.LineRefStructure;
import org.rutebanken.netex.model.LineRefs_RelStructure;
import org.rutebanken.netex.model.PrivateCodeStructure;

import javax.xml.bind.JAXBElement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils.isSet;

public class NetworkFranceProducer extends NetexProducer implements NetexEntityProducer<org.rutebanken.netex.model.Network, mobi.chouette.model.Network> {

    @Override
    public org.rutebanken.netex.model.Network produce(Context context, mobi.chouette.model.Network neptuneNetwork) {
        org.rutebanken.netex.model.Network netexNetwork = netexFactory.createNetwork();

        NetexProducerUtils.populateIdAndVersionIDFM(neptuneNetwork, netexNetwork);

        if (isSet(neptuneNetwork.getVersionDate())) {
            LocalDateTime changedDateTime = TimeUtil.toLocalDateFromJoda(neptuneNetwork.getVersionDate()).atStartOfDay();
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

        if (neptuneNetwork.getCompany() != null) {
            AuthorityRefStructure authorityRefStruct = netexFactory.createAuthorityRefStructure();
            NetexProducerUtils.populateReferenceIDFM(neptuneNetwork.getCompany(), authorityRefStruct);
            netexNetwork.setTransportOrganisationRef(netexFactory.createAuthorityRef(authorityRefStruct));
        }

        if (isSet(neptuneNetwork.getRegistrationNumber())) {
            PrivateCodeStructure privateCodeStruct = netexFactory.createPrivateCodeStructure();
            privateCodeStruct.setValue(neptuneNetwork.getRegistrationNumber());
            netexNetwork.setPrivateCode(privateCodeStruct);
        }

        LineRefs_RelStructure lineRefs_relStructure = netexFactory.createLineRefs_RelStructure();
        List<JAXBElement<? extends LineRefStructure>> jaxbElementsLineRefStructure = new ArrayList<>();
        for(Line line : neptuneNetwork.getLines()){
            JAXBElement<? extends LineRefStructure> jaxbElementLineRefStructure = NetexProducerUtils.createLineIDFMRef(line, netexFactory);
            jaxbElementsLineRefStructure.add(jaxbElementLineRefStructure);
        }

        lineRefs_relStructure.withLineRef(jaxbElementsLineRefStructure);
        netexNetwork.withMembers(lineRefs_relStructure);

        return netexNetwork;
    }
}

