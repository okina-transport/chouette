package mobi.chouette.exchange.netexprofile.exporter.producer;

import mobi.chouette.common.Context;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.ConversionUtil;
import mobi.chouette.exchange.netexprofile.exporter.ExportableNetexData;
import mobi.chouette.model.BookingArrangement;
import mobi.chouette.model.FlexibleLineProperties;
import mobi.chouette.model.Line;
import org.apache.commons.collections.CollectionUtils;
import org.rutebanken.netex.model.*;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.util.stream.Collectors;

import static mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducerUtils.isSet;

public class LineFranceProducer extends NetexProducer implements NetexEntityProducer<org.rutebanken.netex.model.Line_VersionStructure, mobi.chouette.model.Line> {

    private static KeyListStructureProducer keyListStructureProducer = new KeyListStructureProducer();

    private static ContactStructureProducer contactStructureProducer = new ContactStructureProducer();

    @Override
    public org.rutebanken.netex.model.Line_VersionStructure produce(Context context, mobi.chouette.model.Line neptuneLine) {

        ExportableNetexData exportableNetexData = (ExportableNetexData) context.get(Constant.EXPORTABLE_NETEX_DATA);

        org.rutebanken.netex.model.Line_VersionStructure netexLine;
        if (Boolean.TRUE.equals(neptuneLine.getFlexibleService())) {
            netexLine = createFlexibleLine(neptuneLine);
        } else {
            netexLine = netexFactory.createLine();
        }

        NetexProducerUtils.populateIdAndVersionIDFM(neptuneLine, netexLine);
        NetexProducerUtils.populateLineAccessibilityAssessment(neptuneLine, netexLine);

        if (isSet(neptuneLine.getName())) {
            netexLine.setName(ConversionUtil.getMultiLingualString(neptuneLine.getName()));
        } else {
            if (isSet(neptuneLine.getPublishedName())) {
                netexLine.setName(ConversionUtil.getMultiLingualString(neptuneLine.getPublishedName()));
            } else if (isSet(neptuneLine.getNumber())) {
                netexLine.setName(ConversionUtil.getMultiLingualString(neptuneLine.getNumber()));
            } else {
                netexLine.setName(ConversionUtil.getMultiLingualString(neptuneLine.objectIdSuffix()));
            }
        }

        netexLine.setShortName(ConversionUtil.getMultiLingualString(neptuneLine.getPublishedName()));
        netexLine.setDescription(ConversionUtil.getMultiLingualString(neptuneLine.getComment()));

        if (isSet(neptuneLine.getTransportModeName())) {
            AllVehicleModesOfTransportEnumeration vehicleModeOfTransport = ConversionUtil.toVehicleModeOfTransportEnum(neptuneLine.getTransportModeName());
            netexLine.setTransportMode(vehicleModeOfTransport);
        }

        netexLine.setTransportSubmode(ConversionUtil.toTransportSubmodeStructure(neptuneLine.getTransportSubModeName()));
        netexLine.setPublicCode(neptuneLine.getNumber());

        if (isSet(neptuneLine.getRegistrationNumber())) {
            PrivateCodeStructure privateCodeStruct = netexFactory.createPrivateCodeStructure();
            privateCodeStruct.setValue(neptuneLine.getRegistrationNumber());
            netexLine.setPrivateCode(privateCodeStruct);
        }

        if (neptuneLine.getCompany() != null) {
            OperatorRefStructure operatorRefStruct = netexFactory.createOperatorRefStructure();
            NetexProducerUtils.populateReferenceIDFM(neptuneLine.getCompany(), operatorRefStruct);
            netexLine.setOperatorRef(operatorRefStruct);
        }

        if (neptuneLine.getColor() != null || neptuneLine.getTextColor() != null) {
            HexBinaryAdapter hexBinaryAdapter = new HexBinaryAdapter();
            PresentationStructure presentation = new PresentationStructure();
            if (neptuneLine.getColor() != null) {
                presentation.setColour(hexBinaryAdapter.unmarshal(neptuneLine.getColor()));
            }
            if (neptuneLine.getTextColor() != null) {
                presentation.setTextColour(hexBinaryAdapter.unmarshal(neptuneLine.getTextColor()));
            }
            netexLine.setPresentation(presentation);
        }

        netexLine.setKeyList(keyListStructureProducer.produce(neptuneLine.getKeyValues()));
        NoticeProducer.addNoticeAndNoticeAssignments(context, exportableNetexData, exportableNetexData.getNoticeAssignmentsTimetableFrame(), neptuneLine.getFootnotes(), neptuneLine);

        return netexLine;
    }

    FlexibleLine createFlexibleLine(Line neptuneLine) {
        if (!neptuneLine.getObjectId().contains(":FlexibleLine:")) {
            neptuneLine.setObjectId(neptuneLine.getObjectId().replace(":Line:", ":FlexibleLine:"));
        }
        FlexibleLine flexibleLine = netexFactory.createFlexibleLine();
        flexibleLine.setFlexibleLineType(FlexibleLineTypeEnumeration.FIXED);
        FlexibleLineProperties flexibleLineProperties = neptuneLine.getFlexibleLineProperties();
        if (flexibleLineProperties != null) {
            BookingArrangement bookingArrangement = flexibleLineProperties.getBookingArrangement();
//            flexibleLine.setFlexibleLineType(ConversionUtil.toFlexibleLineType(flexibleLineProperties.getFlexibleLineType()));
            if (bookingArrangement != null) {
                if (bookingArrangement.getBookingNote() != null) {
                    flexibleLine.setBookingNote(new MultilingualString().withValue(bookingArrangement.getBookingNote()));
                }
                flexibleLine.setBookingAccess(ConversionUtil.toBookingAccess(bookingArrangement.getBookingAccess()));
                flexibleLine.setBookWhen(ConversionUtil.toPurchaseWhen(bookingArrangement.getBookWhen()));
                if (!CollectionUtils.isEmpty(bookingArrangement.getBuyWhen())) {
                    flexibleLine.withBuyWhen(bookingArrangement.getBuyWhen().stream().map(ConversionUtil::toPurchaseMoment).collect(Collectors.toList()));
                }
                if (!CollectionUtils.isEmpty(bookingArrangement.getBookingMethods())) {
                    flexibleLine.withBookingMethods(bookingArrangement.getBookingMethods().stream().map(ConversionUtil::toBookingMethod).collect(Collectors.toList()));
                }
                flexibleLine.setLatestBookingTime(bookingArrangement.getLatestBookingTime());
                flexibleLine.setMinimumBookingPeriod(bookingArrangement.getMinimumBookingPeriod());

                flexibleLine.setBookingContact(contactStructureProducer.produce(bookingArrangement.getBookingContact()));
            }
        }
        return flexibleLine;
    }

}

