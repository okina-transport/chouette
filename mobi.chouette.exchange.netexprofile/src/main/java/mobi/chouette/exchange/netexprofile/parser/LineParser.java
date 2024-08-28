package mobi.chouette.exchange.netexprofile.parser;

import java.util.*;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.TimeUtil;
import mobi.chouette.exchange.NetexParserUtils;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.ConversionUtil;
import mobi.chouette.exchange.netexprofile.importer.NetexprofileImportParameters;
import mobi.chouette.exchange.netexprofile.importer.util.NetexImportUtil;
import mobi.chouette.exchange.netexprofile.util.NetexObjectIdTypes;
import mobi.chouette.exchange.netexprofile.util.NetexReferential;
import mobi.chouette.model.*;
import mobi.chouette.model.AccessibilityLimitation;
import mobi.chouette.model.Line;
import mobi.chouette.model.Network;
import mobi.chouette.model.type.LimitationStatusEnum;
import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;

import org.rutebanken.netex.model.*;
import org.rutebanken.netex.model.AccessibilityAssessment;

import static mobi.chouette.model.util.ObjectIdTypes.ACCESSIBILITYASSESSMENT_KEY;
import static mobi.chouette.model.util.ObjectIdTypes.ACCESSIBILITYLIMITATION_KEY;

@Log4j
public class LineParser implements Parser, Constant {

	private KeyValueParser keyValueParser = new KeyValueParser();

	private ContactStructureParser contactStructureParser = new ContactStructureParser();

	@Override
	public void parse(Context context) throws Exception {
		Referential referential = (Referential) context.get(REFERENTIAL);
		NetexReferential netexReferential = (NetexReferential) context.get(NETEX_REFERENTIAL);
		LinesInFrame_RelStructure linesInFrameStruct = (LinesInFrame_RelStructure) context.get(NETEX_LINE_DATA_CONTEXT);
		NetexprofileImportParameters parameters = (NetexprofileImportParameters) context.get(CONFIGURATION);

		List incomingLineList = (List) context.get(INCOMING_LINE_LIST);

		for (JAXBElement<? extends DataManagedObjectStructure> lineElement : linesInFrameStruct.getLine_()) {
			org.rutebanken.netex.model.Line_VersionStructure netexLine = (org.rutebanken.netex.model.Line_VersionStructure) lineElement.getValue();
			String lineId = NetexImportUtil.composeObjectIdFromNetexId(context,"Line",netexLine.getId());

			mobi.chouette.model.Line chouetteLine = ObjectFactory.getLine(referential, lineId);
			incomingLineList.add(lineId);
			chouetteLine.setObjectVersion(NetexParserUtils.getVersion(netexLine));

			if (netexLine.getRepresentedByGroupRef() != null) {
				GroupOfLinesRefStructure representedByGroupRef = netexLine.getRepresentedByGroupRef();
				String groupIdRef = representedByGroupRef.getRef();
				String dataTypeName = groupIdRef.split(":")[1];

				if (dataTypeName.equals(NetexObjectIdTypes.NETWORK)) {
					String networkId = NetexImportUtil.composeObjectIdFromNetexId(context,"Network",groupIdRef);
					Network ptNetwork = ObjectFactory.getPTNetwork(referential, networkId);
					chouetteLine.setNetwork(ptNetwork);
				} else if (dataTypeName.equals(NetexObjectIdTypes.GROUP_OF_LINES)) {
					GroupOfLine group = ObjectFactory.getGroupOfLine(referential, groupIdRef);
					group.addLine(chouetteLine);
					String networkId = NetexImportUtil.composeObjectIdFromNetexId(context,"Network", netexReferential.getGroupOfLinesToNetwork().get(groupIdRef));
					if (networkId != null) {
						Network ptNetwork = ObjectFactory.getPTNetwork(referential, networkId);
						chouetteLine.setNetwork(ptNetwork);
					}
				}
			}else{
				Optional<Network> networkOpt = findNetworkFromReferential(referential, chouetteLine);
				networkOpt.ifPresent(chouetteLine::setNetwork);
			}

			// TODO find out how to handle in chouette? can be: new, delete, revise or delta
			// ModificationEnumeration modification = netexLine.getModification();

			chouetteLine.setName(ConversionUtil.getValue(netexLine.getName()));
			if (netexLine.getShortName() != null){
				chouetteLine.setPublishedName(ConversionUtil.getValue(netexLine.getShortName()));
			}else{
				chouetteLine.setPublishedName(ConversionUtil.getValue(netexLine.getName()));
			}

			chouetteLine.setComment(ConversionUtil.getValue(netexLine.getDescription()));

			AllVehicleModesOfTransportEnumeration transportMode = netexLine.getTransportMode();
			TransportModeNameEnum transportModeName = NetexParserUtils.toTransportModeNameEnum(transportMode.value());
			chouetteLine.setTransportModeName(transportModeName);
			chouetteLine.setTransportSubModeName(NetexParserUtils.toTransportSubModeNameEnum(netexLine.getTransportSubmode()));
			chouetteLine.setUrl(netexLine.getUrl());
			chouetteLine.setNumber(netexLine.getPublicCode());

			PrivateCodeStructure privateCode = netexLine.getPrivateCode();
			if (privateCode != null) {
				chouetteLine.setRegistrationNumber(privateCode.getValue());
			}

			if (netexLine.getOperatorRef() != null) {
				String operatorRefValue = netexLine.getOperatorRef().getRef();
				String generatedOrganisationId = NetexImportUtil.composeOperatorIdFromNetexId(parameters.getObjectIdPrefix(), operatorRefValue);
				Company company = ObjectFactory.getCompany(referential, generatedOrganisationId);
				chouetteLine.setCompany(company);
			}

			if (netexLine.getPresentation() != null) {
				PresentationStructure presentation = netexLine.getPresentation();
				HexBinaryAdapter hexBinaryAdapter = new HexBinaryAdapter();
				if (parameters.isNetexImportColors()) {
					if (presentation.getColour() != null) {
						chouetteLine.setColor(hexBinaryAdapter.marshal(presentation.getColour()));
					}
					if (presentation.getTextColour() != null) {
						chouetteLine.setTextColor(hexBinaryAdapter.marshal(presentation.getTextColour()));
					}
				}
			}

			chouetteLine.setKeyValues(keyValueParser.parse(netexLine.getKeyList()));

			chouetteLine.setFilled(true);

			if (((Line_VersionStructure) lineElement.getValue()).getAccessibilityAssessment() != null) {
				AccessibilityAssessment accessibilityAssessment = ((Line_VersionStructure) lineElement.getValue()).getAccessibilityAssessment();
				mobi.chouette.model.AccessibilityAssessment newAccess = new mobi.chouette.model.AccessibilityAssessment();

				String accessibilityAssessmentId = NetexImportUtil.composeObjectIdFromNetexId(context, ACCESSIBILITYASSESSMENT_KEY, accessibilityAssessment.getId());
				newAccess.setObjectId(accessibilityAssessmentId);

				newAccess.setMobilityImpairedAccess(LimitationStatusEnum.fromValue(accessibilityAssessment.getMobilityImpairedAccess().value()));
				if (accessibilityAssessment.getLimitations() != null && accessibilityAssessment.getLimitations().getAccessibilityLimitation() != null) {
					newAccess.setAccessibilityLimitation(convertToChouetteAccessibilityLimitation(accessibilityAssessment, context));
				}
				chouetteLine.setAccessibilityAssessment(newAccess);
			}

			if (netexLine instanceof FlexibleLine) {
				chouetteLine.setFlexibleService(true);
				FlexibleLine flexibleLine = (FlexibleLine) netexLine;
				FlexibleLineProperties flexibleLineProperties = new FlexibleLineProperties();

				flexibleLineProperties.setFlexibleLineType(NetexParserUtils.toFlexibleLineType(flexibleLine.getFlexibleLineType()));
				BookingArrangement bookingArrangement=new BookingArrangement();
				if (flexibleLine.getBookingNote() != null) {
					bookingArrangement.setBookingNote(flexibleLine.getBookingNote().getValue());
				}
				bookingArrangement.setBookingAccess(NetexParserUtils.toBookingAccess(flexibleLine.getBookingAccess()));
				bookingArrangement.setBookWhen(NetexParserUtils.toPurchaseWhen(flexibleLine.getBookWhen()));
				bookingArrangement.setBuyWhen(flexibleLine.getBuyWhen().stream().map(NetexParserUtils::toPurchaseMoment).collect(Collectors.toList()));
				bookingArrangement.setBookingMethods(flexibleLine.getBookingMethods().stream().map(NetexParserUtils::toBookingMethod).collect(Collectors.toList()));
				bookingArrangement.setLatestBookingTime(TimeUtil.toJodaLocalTime(flexibleLine.getLatestBookingTime()));
				bookingArrangement.setMinimumBookingPeriod(TimeUtil.toJodaDuration(flexibleLine.getMinimumBookingPeriod()));

				bookingArrangement.setBookingContact(contactStructureParser.parse(flexibleLine.getBookingContact()));

				flexibleLineProperties.setBookingArrangement(bookingArrangement);
				chouetteLine.setFlexibleLineProperties(flexibleLineProperties);
			}

		}
	}

	public AccessibilityLimitation convertToChouetteAccessibilityLimitation(org.rutebanken.netex.model.AccessibilityAssessment accessibilityAssessment,
																			Context context) {
		AccessibilityLimitation chouetteLimitation = new AccessibilityLimitation();
		org.rutebanken.netex.model.AccessibilityLimitation netexLimitation = accessibilityAssessment.getLimitations().getAccessibilityLimitation();

		String limitationId;
		if (netexLimitation.getId() != null) {
			limitationId = NetexImportUtil.composeObjectIdFromNetexId(context, ACCESSIBILITYLIMITATION_KEY, netexLimitation.getId());
		} else {
			limitationId = NetexImportUtil.composeObjectIdFromNetexId(context, ACCESSIBILITYLIMITATION_KEY, accessibilityAssessment.getId());
		}

		chouetteLimitation.setObjectId(limitationId);

		chouetteLimitation.setWheelchairAccess(netexLimitation.getWheelchairAccess());
		chouetteLimitation.setStepFreeAccess(netexLimitation.getStepFreeAccess());
		chouetteLimitation.setEscalatorFreeAccess(netexLimitation.getEscalatorFreeAccess());
		chouetteLimitation.setLiftFreeAccess(netexLimitation.getLiftFreeAccess());
		chouetteLimitation.setAudibleSignalsAvailable(netexLimitation.getAudibleSignalsAvailable());
		chouetteLimitation.setVisualSignsAvailable(netexLimitation.getVisualSignsAvailable());
		return chouetteLimitation;
	}


	/**
	 * Read referential and try to find which network is associated to a line
	 * @param referential
	 * 	the referential containing all data
	 * @param chouetteLine
	 * 	the line for which we need to find a network association
	 * @return
	 * 	the associated network, if it exists
	 */
	private Optional<Network> findNetworkFromReferential(Referential referential, Line chouetteLine) {
		for (Network network : referential.getSharedPTNetworks().values()) {
			for (Line line : network.getLines()) {
				if (line.getObjectId().equals(chouetteLine.getObjectId())){
					return Optional.of(network);
				}
			}
		}
		return Optional.empty();
	}

	static {
		ParserFactory.register(LineParser.class.getName(), new ParserFactory() {
			private LineParser instance = new LineParser();

			@Override
			protected Parser create() {
				return instance;
			}
		});
	}

}
