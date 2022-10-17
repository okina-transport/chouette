package mobi.chouette.exchange.netexprofile.exporter.writer;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils;
import org.apache.commons.collections.CollectionUtils;
import org.rutebanken.netex.model.*;

import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamWriter;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.NETEX_DEFAULT_OBJECT_VERSION;
import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.netexFactory;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.*;

public class TimetableFrameWriter extends AbstractNetexWriter {

    public static void write(XMLStreamWriter writer, Context context, ExportableNetexData exportableNetexData, Marshaller marshaller) {

        String timetableFrameId = NetexProducerUtils.createUniqueId(context, TIMETABLE_FRAME);

        try {
            writer.writeStartElement(TIMETABLE_FRAME);
            writer.writeAttribute(VERSION, NETEX_DEFAULT_OBJECT_VERSION);
            writer.writeAttribute(ID, timetableFrameId);
            writeVehicleJourneysElement(writer, exportableNetexData, marshaller);
            ReusedConstructsWriter.writeNoticeAssignmentsElement(writer, exportableNetexData.getNoticeAssignmentsTimetableFrame(), marshaller);

            if (CollectionUtils.isNotEmpty(exportableNetexData.getServiceJourneyInterchanges())) {
                writeServiceJourneyInterchangesElement(writer, exportableNetexData, marshaller);
            }

            writer.writeEndElement();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeVehicleJourneysElement(XMLStreamWriter writer, ExportableNetexData exportableData, Marshaller marshaller) {
        try {
            writer.writeStartElement(VEHICLE_JOURNEYS);
            for (ServiceJourney_VersionStructure serviceJourney : exportableData.getServiceJourneys()) {
                if (serviceJourney instanceof ServiceJourney){
                    marshaller.marshal(netexFactory.createServiceJourney((ServiceJourney) serviceJourney), writer);
                }else{
                    marshaller.marshal(netexFactory.createTemplateServiceJourney((TemplateServiceJourney) serviceJourney), writer);
                }
            }
            for (DatedServiceJourney datedServiceJourney : exportableData.getDatedServiceJourneys()) {
				marshaller.marshal(netexFactory.createDatedServiceJourney(datedServiceJourney), writer);
			}
			for (DeadRun deadRun : exportableData.getDeadRuns()) {
				marshaller.marshal(netexFactory.createDeadRun(deadRun), writer);
			}writer.writeEndElement();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeServiceJourneyInterchangesElement(XMLStreamWriter writer, ExportableNetexData exportableData, Marshaller marshaller) {
        try {
            writer.writeStartElement(JOURNEY_INTERCHANGES);
            for (ServiceJourneyInterchange serviceJourneyInterchange : exportableData.getServiceJourneyInterchanges()) {
                marshaller.marshal(netexFactory.createServiceJourneyInterchange(serviceJourneyInterchange), writer);
            }
            writer.writeEndElement();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

