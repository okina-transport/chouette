package mobi.chouette.exchange.netexprofile.exporter.writer;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import org.apache.commons.collections.MapUtils;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.OperatingPeriod;

import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLStreamWriter;
import java.util.stream.Collectors;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer.netexFactory;

public class ServiceCalendarFrameFranceWriter extends AbstractNetexWriter {

    public static void write(XMLStreamWriter writer, Context context, ExportableNetexData exportableNetexData, Marshaller marshaller) {

        try {
            writeDayTypesElement(writer, exportableNetexData, marshaller);

            if (MapUtils.isNotEmpty(exportableNetexData.getSharedOperatingPeriods())) {
                writeOperatingPeriodsElement(writer, exportableNetexData, marshaller);
            }

            writeDayTypeAssignmentsElement(writer, exportableNetexData, marshaller);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeDayTypesElement(XMLStreamWriter writer, ExportableNetexData exportableData, Marshaller marshaller) {
        try {
            for (DayType dayType : exportableData.getSharedDayTypes().values()) {
                marshaller.marshal(netexFactory.createDayType(dayType), writer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeDayTypeAssignmentsElement(XMLStreamWriter writer, ExportableNetexData exportableData, Marshaller marshaller) {
        try {
            for (DayTypeAssignment dayTypeAssignment : exportableData.getSharedDayTypeAssignments().stream().sorted(new DayTypeAssignmentExportComparator()).collect(Collectors.toList())) {
                marshaller.marshal(netexFactory.createDayTypeAssignment(dayTypeAssignment), writer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeOperatingPeriodsElement(XMLStreamWriter writer, ExportableNetexData exportableNetexData, Marshaller marshaller) {
        try {
            int count = 0;
            for (OperatingPeriod operatingPeriod : exportableNetexData.getSharedOperatingPeriods().values()) {
                if(count != 0){
                    String id = operatingPeriod.getId();
                    operatingPeriod.setId(id+"-"+count);
                }
                marshaller.marshal(netexFactory.createOperatingPeriod(operatingPeriod), writer);
                count++;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
