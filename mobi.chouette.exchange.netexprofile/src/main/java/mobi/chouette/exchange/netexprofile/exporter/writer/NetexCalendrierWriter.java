package mobi.chouette.exchange.netexprofile.exporter.writer;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.rutebanken.netex.model.DayTypeAssignment_VersionStructure;
import org.rutebanken.netex.model.ValidBetween;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.GENERAL_FRAME;
import static mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes.MEMBERS;

public class NetexCalendrierWriter extends AbstractNetexWriter {

    public static void writer(XMLStreamWriter writer, Context context, ExportableNetexData exportableNetexData, Marshaller marshaller, String timestamp, String typeNetex) throws XMLStreamException, JAXBException {
        writer.writeAttribute(ID, NetexProducerUtils.createUniqueGeneralFrameId(context, GENERAL_FRAME, typeNetex, timestamp));

        List<LocalDateTime> allDates = new ArrayList<>();

        if (MapUtils.isNotEmpty(exportableNetexData.getSharedOperatingPeriods())) {
            exportableNetexData.getSharedOperatingPeriods().values().forEach(op -> {
                if (op.getFromDate() != null) {
					allDates.add(op.getFromDate());
				}
                if (op.getToDate() != null) {
					allDates.add(op.getToDate());
				}
            });
        }

        if (CollectionUtils.isNotEmpty(exportableNetexData.getSharedDayTypeAssignments())) {
            exportableNetexData.getSharedDayTypeAssignments().stream()
                    .map(DayTypeAssignment_VersionStructure::getDate)
                    .filter(Objects::nonNull)
                    .forEach(allDates::add);
        }

        if (CollectionUtils.isNotEmpty(allDates)) {
            ValidBetween validBetween = new ValidBetween();

            LocalDateTime fromDate = null;
            LocalDateTime toDate = null;

            for (LocalDateTime date : allDates) {
                if (fromDate == null || date.isBefore(fromDate)) {
                    fromDate = date;
                }
                if (toDate == null || date.isAfter(toDate)) {
                    toDate = date;
                }
            }

            validBetween.setFromDate(fromDate);
            validBetween.setToDate(toDate);

            marshaller.marshal(validBetween, writer);
        }

        TypeOfFrameWriter.typeOfFrameWriter(writer, marshaller, typeNetex);
        writer.writeStartElement(MEMBERS);
        ServiceCalendarFrameFranceWriter.write(writer, context, exportableNetexData, marshaller);
        writer.writeEndElement();
    }
}
