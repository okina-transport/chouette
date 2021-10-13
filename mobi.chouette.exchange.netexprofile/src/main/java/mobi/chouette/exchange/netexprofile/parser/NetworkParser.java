package mobi.chouette.exchange.netexprofile.parser;

import java.util.List;

import javax.xml.bind.JAXBElement;

import mobi.chouette.exchange.NetexParserUtils;
import mobi.chouette.exchange.netexprofile.importer.NetexprofileImportParameters;
import mobi.chouette.exchange.netexprofile.importer.util.NetexImportUtil;
import org.rutebanken.netex.model.GroupOfLines;
import org.rutebanken.netex.model.LineRefStructure;
import org.rutebanken.netex.model.OrganisationRefStructure;

import mobi.chouette.common.Context;
import mobi.chouette.common.TimeUtil;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.ConversionUtil;
import mobi.chouette.exchange.netexprofile.util.NetexReferential;
import mobi.chouette.model.Company;
import mobi.chouette.model.GroupOfLine;
import mobi.chouette.model.Line;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;

public class NetworkParser extends NetexParser implements Parser, Constant {

    @Override
    public void parse(Context context) throws Exception {
        Referential referential = (Referential) context.get(REFERENTIAL);
		NetexReferential netexReferential = (NetexReferential) context.get(NETEX_REFERENTIAL);
        org.rutebanken.netex.model.Network netexNetwork = (org.rutebanken.netex.model.Network) context.get(NETEX_LINE_DATA_CONTEXT);
        NetexprofileImportParameters parameters = (NetexprofileImportParameters) context.get(CONFIGURATION);


        String networkId = NetexImportUtil.composeObjectIdFromNetexId(context,"Network",netexNetwork.getId());
        mobi.chouette.model.Network chouetteNetwork = ObjectFactory.getPTNetwork(referential, networkId);
        chouetteNetwork.setObjectVersion(NetexParserUtils.getVersion(netexNetwork));

        if (netexNetwork.getCreated() != null) {
            chouetteNetwork.setCreationTime(TimeUtil.toJodaLocalDateTime(netexNetwork.getCreated()));
        }
        if (netexNetwork.getChanged() != null) {
            chouetteNetwork.setVersionDate(TimeUtil.toJodaLocalDateTime(netexNetwork.getChanged()).toLocalDate());
        }

        chouetteNetwork.setName(netexNetwork.getName().getValue());

        OrganisationRefStructure authorityRefStruct = netexNetwork.getTransportOrganisationRef().getValue();
        String generatedAuthorityId = NetexImportUtil.composeObjectIdFromNetexId("Authority", parameters.getObjectIdPrefix(), authorityRefStruct.getRef());
        Company company = ObjectFactory.getCompany(referential, generatedAuthorityId);
        chouetteNetwork.setCompany(company);
        chouetteNetwork.setDescription(ConversionUtil.getValue(netexNetwork.getDescription()));
       
        if (netexNetwork.getPrivateCode() != null) {
            chouetteNetwork.setRegistrationNumber(netexNetwork.getPrivateCode().getValue());
        }
        if (netexNetwork.getMainLineRef() != null) {

            String lineId = NetexImportUtil.composeObjectIdFromNetexId(context,"Line",netexNetwork.getMainLineRef().getRef());
            Line line = ObjectFactory.getLine(referential, lineId);
            line.setNetwork(chouetteNetwork);
        }

        for (JAXBElement<? extends LineRefStructure> lineRefStructure : netexNetwork.getMembers().getLineRef()) {
            String lineRef = lineRefStructure.getValue().getRef();
            String lineId = NetexImportUtil.composeObjectIdFromNetexId(context,"Line",lineRef);
            Line line = ObjectFactory.getLine(referential, lineId);
            line.setNetwork(chouetteNetwork);
        }

        if (netexNetwork.getGroupsOfLines() != null) {
            List<GroupOfLines> groupsOfLines = netexNetwork.getGroupsOfLines().getGroupOfLines();

            for (GroupOfLines groupOfLines : groupsOfLines) {
                GroupOfLine groupOfLine = ObjectFactory.getGroupOfLine(referential, groupOfLines.getId());
                groupOfLine.setName(ConversionUtil.getValue(groupOfLines.getName()));

                if (groupOfLines.getMembers() != null) {
                    for (JAXBElement<? extends LineRefStructure> lineRefRelStruct : groupOfLines.getMembers().getLineRef()) {
                        String lineIdRef = lineRefRelStruct.getValue().getRef();
                        String lineId = NetexImportUtil.composeObjectIdFromNetexId(context,"Line",lineIdRef);
                        Line line = ObjectFactory.getLine(referential, lineId);

                        if (line != null) {
                            groupOfLine.addLine(line);
                        }
                    }
                }
             
                netexReferential.getGroupOfLinesToNetwork().put(groupOfLine.getObjectId(),chouetteNetwork.getObjectId());
                groupOfLine.setFilled(true);
            }
        }

        chouetteNetwork.setFilled(true);
    }

 
    static {
        ParserFactory.register(NetworkParser.class.getName(), new ParserFactory() {
            private NetworkParser instance = new NetworkParser();

            @Override
            protected Parser create() {
                return instance;
            }
        });
    }

}
