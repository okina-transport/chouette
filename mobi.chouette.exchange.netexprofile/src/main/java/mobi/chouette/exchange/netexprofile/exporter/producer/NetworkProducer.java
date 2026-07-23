package mobi.chouette.exchange.netexprofile.exporter.producer;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.ConversionUtil;
import org.rutebanken.netex.model.AuthorityRef;
import org.rutebanken.netex.model.KeyValueStructure;
import org.rutebanken.netex.model.PrivateCodeStructure;

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
        @SuppressWarnings("unchecked")
        java.util.Map<String, java.util.List<mobi.chouette.model.Translation>> networkFieldValueTranslations =
                (java.util.Map<String, java.util.List<mobi.chouette.model.Translation>>) context.get(mobi.chouette.exchange.netexprofile.Constant.NETWORK_FIELD_VALUE_TRANSLATIONS);
        NetexProducerUtils.addAlternativeTexts(netexNetwork,
                NetexProducerUtils.getTranslations(neptuneNetwork.getTranslations(), networkFieldValueTranslations, "name", neptuneNetwork.getName()), "name", "Name");

        if (neptuneNetwork.getCompany() != null) {
            AuthorityRef authorityRef = netexFactory.createAuthorityRef();
            NetexProducerUtils.populateReference(neptuneNetwork.getCompany(), authorityRef, true);
            netexNetwork.setTransportOrganisationRef(netexFactory.createAuthorityRef(authorityRef));
        }

        if (isSet(neptuneNetwork.getRegistrationNumber())) {
            PrivateCodeStructure privateCodeStruct = netexFactory.createPrivateCodeStructure();
            privateCodeStruct.setValue(neptuneNetwork.getRegistrationNumber());
            netexNetwork.setPrivateCode(privateCodeStruct);
        }

        return netexNetwork;
    }
}
