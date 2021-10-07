package mobi.chouette.exchange.netexprofile.exporter.writer;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils;
import mobi.chouette.model.ConnectionLink;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.FlexibleLine;
import org.rutebanken.netex.model.GeneralOrganisation;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Line_VersionStructure;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Network;
import org.rutebanken.netex.model.Notice;
import org.rutebanken.netex.model.Operator;
import org.rutebanken.netex.model.Organisation_VersionStructure;
import org.rutebanken.netex.model.SiteConnection;
import org.rutebanken.netex.model.SiteConnectionEndStructure;
import org.rutebanken.netex.model.SiteConnection_VersionStructure;
import org.rutebanken.netex.model.StopAreaRefStructure;
import org.rutebanken.netex.model.TransferDurationStructure;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.time.Duration;
import java.util.Collection;
import java.util.Iterator;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.netexFactory;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.ADDITIONAL_NETWORKS;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.GENERAL_FRAME;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.MEMBERS;

public class NetexCommunWriter extends AbstractNetexWriter {

    public static void writer(XMLStreamWriter writer, Context context, ExportableNetexData exportableNetexData, Marshaller marshaller, String timestamp, String typeNetex) throws XMLStreamException, JAXBException {
        writer.writeAttribute(ID, NetexProducerUtils.createUniqueGeneralFrameId(context, GENERAL_FRAME, typeNetex, timestamp));

        TypeOfFrameWriter.typeOfFrameWriter(writer, marshaller, typeNetex);

        writer.writeStartElement(MEMBERS);

        writeNetworks(writer, exportableNetexData, marshaller);
        writeLinesElement(writer, exportableNetexData, marshaller);
        writeOrganisationsElement(writer, exportableNetexData, marshaller);
        writeNoticesElement(writer, exportableNetexData.getSharedNotices().values(), marshaller);
        writeSiteConnectionElement(writer, exportableNetexData, marshaller);

        writer.writeEndElement();

    }



    private static void writeNoticesElement(XMLStreamWriter writer, Collection<Notice> notices, Marshaller marshaller) {
        try {
            if (!notices.isEmpty()) {
                for (Notice notice : notices) {
                    marshaller.marshal(netexFactory.createNotice(notice), writer);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeNetworks(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller) throws XMLStreamException {
        if (!exportableNetexData.getSharedNetworks().isEmpty()) {
            Iterator<Network> networkIterator = exportableNetexData.getSharedNetworks().values().iterator();
            writeNetworkElement(writer, networkIterator.next(), marshaller);

            if (networkIterator.hasNext()){
                while(networkIterator.hasNext()) {
                    writeNetworkElement(writer, networkIterator.next(), marshaller);
                }
            }
        }
    }

    private static void writeNetworkElement(XMLStreamWriter writer, Network network, Marshaller marshaller) {
        try {
            marshaller.marshal(netexFactory.createNetwork(network), writer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeOrganisationsElement(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller) {
        try {
            if (!exportableNetexData.getSharedOrganisations().isEmpty()) {
                for (Organisation_VersionStructure organisation : exportableNetexData.getSharedOrganisations().values()) {
                    if (organisation instanceof Operator) {
                        marshaller.marshal(netexFactory.createOperator((Operator) organisation), writer);
                    } else if (organisation instanceof Authority) {
                        marshaller.marshal(netexFactory.createAuthority((Authority) organisation), writer);
                    } else {
                        marshaller.marshal(netexFactory.createGeneralOrganisation((GeneralOrganisation) organisation), writer);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void writeLinesElement(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller) {
        if (!exportableNetexData.getSharedLines().isEmpty()) {
            Iterator<Line_VersionStructure> lineIterator = exportableNetexData.getSharedLines().values().iterator();
            if (lineIterator.hasNext()) {
                while (lineIterator.hasNext()) {
                    try {
                        Line_VersionStructure line = lineIterator.next();
                        JAXBElement<? extends Line_VersionStructure> jaxbElement = null;
                        if (line instanceof Line) {
                            jaxbElement = netexFactory.createLine((Line) line);
                        } else if (line instanceof FlexibleLine) {
                            jaxbElement = netexFactory.createFlexibleLine((FlexibleLine) line);
                        }
                        marshaller.marshal(jaxbElement, writer);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    static void writeSiteConnectionElement(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller) {

        if (exportableNetexData.getConnectionLinks().isEmpty())
            return ;

        for (ConnectionLink connectionLink : exportableNetexData.getConnectionLinks()) {
            JAXBElement<? extends SiteConnection_VersionStructure> jaxbElement = null;
            SiteConnection siteConnection = new SiteConnection();

            SiteConnectionEndStructure fromArea = netexFactory.createSiteConnectionEndStructure();
            StopAreaRefStructure fromStopAreaRef = netexFactory.createStopAreaRefStructure();
            fromStopAreaRef.withRef(connectionLink.getStartOfLink().getObjectId());
            fromArea.setStopAreaRef(fromStopAreaRef);
            siteConnection.withFrom(fromArea);

            SiteConnectionEndStructure toArea = netexFactory.createSiteConnectionEndStructure();
            StopAreaRefStructure toStopAreaRef = netexFactory.createStopAreaRefStructure();
            toStopAreaRef.withRef(connectionLink.getEndOfLink().getObjectId());
            toArea.setStopAreaRef(toStopAreaRef);
            siteConnection.withTo(toArea);

            MultilingualString name = new MultilingualString();
            name.setLang("fr");
            name.setValue(connectionLink.getName());
            siteConnection.withName(name);

            siteConnection.withDistance(connectionLink.getLinkDistance());


            TransferDurationStructure transfertDuration = netexFactory.createTransferDurationStructure();

            Duration duration = Duration.parse(connectionLink.getDefaultDuration().toString());
            transfertDuration.setDefaultDuration(duration);
            siteConnection.setWalkTransferDuration(transfertDuration);

            siteConnection.setId(connectionLink.getObjectId());
            siteConnection.setVersion(String.valueOf(connectionLink.getObjectVersion()));

            jaxbElement = netexFactory.createSiteConnection(siteConnection);
            try {
                marshaller.marshal(jaxbElement, writer);
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        }

    }
}
