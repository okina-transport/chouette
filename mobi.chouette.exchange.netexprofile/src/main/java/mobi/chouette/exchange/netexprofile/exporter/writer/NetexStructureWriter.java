package mobi.chouette.exchange.netexprofile.exporter.writer;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils;
import mobi.chouette.model.ConnectionLink;
import org.rutebanken.netex.model.*;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.time.Duration;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.netexFactory;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.MEMBERS;

public class NetexStructureWriter extends AbstractNetexWriter {

    public static void writer(XMLStreamWriter writer, Context context, ExportableNetexData exportableNetexData, Marshaller marshaller, String timestamp, String typeNetex) throws XMLStreamException, JAXBException {
        writer.writeAttribute(ID, NetexProducerUtils.createUniqueGeneralFrameInLineId(context, typeNetex, timestamp));

        TypeOfFrameWriter.typeOfFrameWriter(writer, marshaller, typeNetex);

        writer.writeStartElement(MEMBERS);

        writeRouteLinksElement(writer, exportableNetexData, marshaller);
        writeRoutesElement(writer, exportableNetexData, marshaller);
        writeDirectionsElement(writer, exportableNetexData, marshaller);
        writeServiceJourneyPatternsElement(writer, exportableNetexData, marshaller);
        writeScheduledStopPointsElement(writer, exportableNetexData, marshaller);
        writePassengerStopAssignmentsElement(writer, exportableNetexData, marshaller);
        writeDestinationDisplaysElement(writer, exportableNetexData, marshaller);
        writeSiteConnectionElement(writer, exportableNetexData, marshaller);

        writer.writeEndElement();
    }

    static void writeRouteLinksElement(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller) {
        try {
            for (org.rutebanken.netex.model.RouteLink routeLink : exportableNetexData.getRouteLinks()) {
                marshaller.marshal(netexFactory.createRouteLink(routeLink), writer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void writeRoutesElement(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller) {
        try {
            for (org.rutebanken.netex.model.Route route : exportableNetexData.getRoutes()) {
                marshaller.marshal(netexFactory.createRoute(route), writer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void writeDirectionsElement(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller){
        try {
            for (org.rutebanken.netex.model.Direction direction : exportableNetexData.getDirections()) {
                marshaller.marshal(netexFactory.createDirection(direction), writer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void writeServiceJourneyPatternsElement(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller){
        try {
            for (org.rutebanken.netex.model.ServiceJourneyPattern serviceJourneyPattern : exportableNetexData.getServiceJourneyPatterns()) {
                marshaller.marshal(netexFactory.createServiceJourneyPattern(serviceJourneyPattern), writer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void writeScheduledStopPointsElement(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller) {
        try {
            for (ScheduledStopPoint scheduledStopPoint : exportableNetexData.getScheduledStopPoints().values()) {
                marshaller.marshal(netexFactory.createScheduledStopPoint(scheduledStopPoint), writer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void writePassengerStopAssignmentsElement(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller) {
        try {
            for (PassengerStopAssignment stopAssignment : exportableNetexData.getStopAssignments().values()) {
                marshaller.marshal(netexFactory.createPassengerStopAssignment(stopAssignment), writer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void writeDestinationDisplaysElement(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller) {
        try {
            if (exportableNetexData.getDestinationDisplays().values().size() > 0) {
                for (DestinationDisplay destinationDisplay : exportableNetexData.getDestinationDisplays().values()) {
                    marshaller.marshal(netexFactory.createDestinationDisplay(destinationDisplay), writer);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void writeSiteConnectionElement(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller) {

        if (exportableNetexData.getConnectionLinks().isEmpty())
            return ;

        for (ConnectionLink connectionLink : exportableNetexData.getConnectionLinks()) {
            JAXBElement<? extends SiteConnection_VersionStructure> jaxbElement = null;
            SiteConnection siteConnection = new SiteConnection();

            SiteConnectionEndStructure fromArea = netexFactory.createSiteConnectionEndStructure();
            QuayRefStructure fromQuayRef = netexFactory.createQuayRefStructure();
            StopPlaceRefStructure fromStopPlaceRef = netexFactory.createStopPlaceRefStructure();
            setTransportMode(fromArea, connectionLink.getStartOfLink());
            fromStopPlaceRef.withRef(connectionLink.getStartOfLink().getParent().getObjectId());
            fromArea.setStopPlaceRef(netexFactory.createStopPlaceRef(fromStopPlaceRef));
            fromQuayRef.withRef(connectionLink.getStartOfLink().getObjectId());
            fromArea.setQuayRef(netexFactory.createQuayRef(fromQuayRef));
            siteConnection.withFrom(fromArea);


            SiteConnectionEndStructure toArea = netexFactory.createSiteConnectionEndStructure();
            QuayRefStructure toQuayRef = netexFactory.createQuayRefStructure();
            StopPlaceRefStructure toStopPlaceRef = netexFactory.createStopPlaceRefStructure();
            setTransportMode(toArea, connectionLink.getEndOfLink());
            toStopPlaceRef.withRef(connectionLink.getEndOfLink().getParent().getObjectId());
            toArea.setStopPlaceRef(netexFactory.createStopPlaceRef(toStopPlaceRef));
            toQuayRef.withRef(connectionLink.getEndOfLink().getObjectId());
            toArea.setQuayRef(netexFactory.createQuayRef(toQuayRef));
            siteConnection.withTo(toArea);

            MultilingualString name = new MultilingualString();
            name.setLang("fr");
            name.setValue(connectionLink.getName());
            siteConnection.withName(name);

            siteConnection.withDistance(connectionLink.getLinkDistance());


            TransferDurationStructure transfertDuration = netexFactory.createTransferDurationStructure();

            if (connectionLink.getDefaultDuration() != null ){
                Duration duration = Duration.parse(connectionLink.getDefaultDuration().toString());
                transfertDuration.setDefaultDuration(duration);
                siteConnection.setWalkTransferDuration(transfertDuration);
            }


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

    private static void setTransportMode(SiteConnectionEndStructure area, mobi.chouette.model.StopArea link) {
        switch (link.getParent().getTransportModeName()) {
            case Tram:
                area.setTransportMode(AllVehicleModesOfTransportEnumeration.TRAM);
                break;
            case Metro:
                area.setTransportMode(AllVehicleModesOfTransportEnumeration.METRO);
                break;
            case Rail:
                area.setTransportMode(AllVehicleModesOfTransportEnumeration.RAIL);
                break;
            case Water:
            case Ferry:
                area.setTransportMode(AllVehicleModesOfTransportEnumeration.WATER);
                break;
            case Funicular:
                area.setTransportMode(AllVehicleModesOfTransportEnumeration.FUNICULAR);
            case Cableway:
                area.setTransportMode(AllVehicleModesOfTransportEnumeration.CABLEWAY);
            case TrolleyBus:
                area.setTransportMode(AllVehicleModesOfTransportEnumeration.TROLLEY_BUS);
            case Coach:
                area.setTransportMode(AllVehicleModesOfTransportEnumeration.COACH);
            case Bus:
            default:
                area.setTransportMode(AllVehicleModesOfTransportEnumeration.BUS);
        }
    }

}
