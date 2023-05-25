package mobi.chouette.exchange.netexprofile.parser;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.NetexParserUtils;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.importer.ParserUtils;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.ConversionUtil;
import mobi.chouette.exchange.netexprofile.importer.NetexprofileImportParameters;
import mobi.chouette.exchange.netexprofile.importer.util.NetexImportUtil;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.type.LongLatTypeEnum;
import mobi.chouette.model.type.StopAreaTypeEnum;
import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.type.TransportSubModeNameEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import net.opengis.gml._3.DirectPositionType;
import org.apache.commons.lang3.StringUtils;
import org.rutebanken.netex.model.*;

import javax.xml.bind.JAXBElement;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Log4j
public class StopPlaceParser implements Parser, Constant {

    private Map<String, Properties> tariffZoneProperties;

    private KeyValueParser keyValueParser = new KeyValueParser();

    public static final String FARE_ZONE = "fare-zone";

    @Override
    public void parse(Context context) throws Exception {
        Referential referential = (Referential) context.get(REFERENTIAL);
        RelationshipStructure relationshipStruct = (RelationshipStructure) context.get(NETEX_LINE_DATA_CONTEXT);

        if (relationshipStruct instanceof TariffZonesInFrame_RelStructure) {
            tariffZoneProperties = new HashMap<>();

            TariffZonesInFrame_RelStructure tariffZonesStruct = (TariffZonesInFrame_RelStructure) relationshipStruct;
            List<JAXBElement<? extends Zone_VersionStructure>> tariffZones = tariffZonesStruct.getTariffZone_();

            String zoneStrucId = tariffZonesStruct.getId();
            Properties properties;

            if (this.tariffZoneProperties.containsKey(zoneStrucId)){
                properties = this.tariffZoneProperties.get(zoneStrucId);
            }else{
                properties = new Properties();
                this.tariffZoneProperties.put(zoneStrucId, properties);
            }


            for (JAXBElement<? extends Zone_VersionStructure> zone : tariffZones) {
                properties.put(NAME, zone.getName());
            }
        } else if (relationshipStruct instanceof StopPlacesInFrame_RelStructure) {
            StopPlacesInFrame_RelStructure stopPlacesStruct = (StopPlacesInFrame_RelStructure) relationshipStruct;
            List<StopPlace> stopPlaces = stopPlacesStruct.getStopPlace_().stream().map(sp -> (StopPlace) sp.getValue()).collect(Collectors.toList());
            Map<String, String> parentZoneMap = new HashMap<>();
            Map<String, String> parentSiteMap = new HashMap<>();
            for (StopPlace stopPlace : stopPlaces) {
                parseStopPlace(context, stopPlace, parentZoneMap, parentSiteMap);
            }


            updateParentAndChildRefs(referential, parentZoneMap, ChouetteAreaEnum.StopPlace);
            updateParentAndChildRefs(referential, parentSiteMap, ChouetteAreaEnum.CommercialStopPoint);

        }
    }

    private void updateParentAndChildRefs(Referential referential, Map<String, String> childMappedAgainstParent, ChouetteAreaEnum parentAreaType) {
        for (Map.Entry<String, String> item : childMappedAgainstParent.entrySet()) {
            StopArea child = ObjectFactory.getStopArea(referential, item.getKey());
            StopArea parent = ObjectFactory.getStopArea(referential, item.getValue());
            if (parent != null) {
                parent.setAreaType(parentAreaType);
                child.setParent(parent);
                copyNameIfMissingRecursively(parent);
            }
        }
    }

    private void copyNameIfMissingRecursively(StopArea parent){
        for(StopArea child:parent.getContainedStopAreas()){
            if (child.getName()==null){
                child.setName(parent.getName());
            }
            copyNameIfMissingRecursively(child);
        }
    }


    void parseStopPlace(Context context, StopPlace stopPlace, Map<String, String> parentZoneMap, Map<String, String> parentSiteMap) throws Exception {
        Referential referential = (Referential) context.get(REFERENTIAL);
        NetexprofileImportParameters parameters = (NetexprofileImportParameters) context.get(CONFIGURATION);
        String stopPlaceId;

        if (stopPlace.getQuays() == null) {
            return;
        }

        if (parameters != null){
            //Netex file import by application : parameters are available
            stopPlaceId = NetexImportUtil.composeObjectId("StopPlace", parameters.getObjectIdPrefix(), stopPlace.getId());
        }else{
            //Irkalla synchronization case : no parameters are defined.
            stopPlaceId = stopPlace.getId();
        }


        StopArea stopArea = ObjectFactory.getStopArea(referential, stopPlaceId);
        stopArea.setAreaType(ChouetteAreaEnum.CommercialStopPoint);
        stopArea.setObjectVersion(NetexParserUtils.getVersion(stopPlace));
        stopArea.setName(ConversionUtil.getValue(stopPlace.getName()));
        stopArea.setStopAreaType(mapStopAreaType(stopPlace.getStopPlaceType()));
        stopArea.setTransportModeName(mapTransportModeName(stopPlace.getTransportMode()));
        stopArea.setTransportSubMode(mapTransportSubMode(stopPlace));

        if(stopPlace.getKeyList() != null && !stopPlace.getKeyList().getKeyValue().isEmpty()){
            stopPlace.getKeyList().getKeyValue().stream().filter(key -> key.getKey().equals("RAIL-UIC"))
                    .forEach(key -> {
                        stopArea.setRailUic(key.getValue());
                    });
            ;
        }
        if (stopPlace.getDescription() != null) {
            stopArea.setComment(stopPlace.getDescription().getValue());
        }
        if (stopPlace.getLandmark() != null) {
            stopArea.setNearestTopicName(stopPlace.getLandmark().getValue());
        }

        PrivateCodeStructure privateCodeStruct = stopPlace.getPrivateCode();
        if (privateCodeStruct != null) {
            stopArea.setRegistrationNumber(privateCodeStruct.getValue());
        } else {
            if (stopPlace.getShortName() != null) {
                stopArea.setRegistrationNumber(stopPlace.getShortName().getValue());
            }
        }

        SimplePoint_VersionStructure centroidStruct = stopPlace.getCentroid();
        if (centroidStruct != null) {
            parseCentroid(centroidStruct.getLocation(), stopArea);
        }

        ZoneRefStructure parentZoneRefStruct = stopPlace.getParentZoneRef();
        if (parentZoneRefStruct != null) {
            parentZoneMap.put(stopArea.getObjectId(), parentZoneRefStruct.getRef());
        }

        PostalAddress postalAddress = stopPlace.getPostalAddress();
        if (postalAddress != null) {
            if (postalAddress.getPostCode() != null) {
                stopArea.setCountryCode(postalAddress.getPostCode());
            }
            if (postalAddress.getAddressLine1() != null && postalAddress.getAddressLine1().getValue() != null) {
                stopArea.setStreetName(postalAddress.getAddressLine1().getValue());
            }
        }

        TariffZoneRefs_RelStructure tariffZonesStruct = stopPlace.getTariffZones();
        if (tariffZonesStruct != null) {
            parseTariffZoneRefs(tariffZonesStruct, stopArea);
        }

        Quays_RelStructure quaysStruct = stopPlace.getQuays();
        if (quaysStruct != null) {
            List<Object> quayObjects = quaysStruct.getQuayRefOrQuay().stream().map(JAXBElement::getValue).collect(Collectors.toList());
            for (Object quayObject : quayObjects) {
                parseQuay(context, stopArea, (Quay) quayObject);
            }
        }

        stopArea.setFilled(true);
        stopArea.setKeyValues(keyValueParser.parse(stopPlace.getKeyList()));

        mapAccessibilityAssesssment(stopArea, stopPlace);

        if(stopPlace.getKeyList() != null && !stopPlace.getKeyList().getKeyValue().isEmpty()){
            stopPlace.getKeyList().getKeyValue().stream().filter(key -> key.getKey().equals(FARE_ZONE))
                    .forEach(key -> stopArea.setZoneId(key.getValue()));
        }
    }

    /**
     * Read incoming site and set mobilityrestrictedSuitable to true or false by reading  accessibility limitations
     * @param stopArea
     *  the stopArea for which the mobilityRestrictedSuitable must be set
     * @param site
     *  the incoming stopPlace or quay which will be read to search the limitations
     */
    private void mapAccessibilityAssesssment(StopArea stopArea, SiteElement_VersionStructure site) {
        if (site.getAccessibilityAssessment() == null || site.getAccessibilityAssessment().getLimitations() == null ||
                site.getAccessibilityAssessment().getLimitations().getAccessibilityLimitation() == null || site.getAccessibilityAssessment().getLimitations().getAccessibilityLimitation().getWheelchairAccess() == null){
            return;
        }
        LimitationStatusEnumeration wheelchairAccess = site.getAccessibilityAssessment().getLimitations().getAccessibilityLimitation().getWheelchairAccess();
        stopArea.setMobilityRestrictedSuitable(LimitationStatusEnumeration.TRUE.equals(wheelchairAccess));

    }

    StopAreaTypeEnum mapStopAreaType(StopTypeEnumeration netexType) {
        if (netexType == null) {
            return null;
        }

        try {
            return StopAreaTypeEnum.valueOf(StringUtils.capitalize(netexType.value()));
        } catch (IllegalArgumentException iae) {
            log.warn("Unable to map unknown StopTypeEnumeration value: " + netexType);
            return StopAreaTypeEnum.Other;
        }

    }

    TransportSubModeNameEnum mapTransportSubMode(StopPlace netexStop) {
        if (netexStop.getTransportMode() == null) {
            return null;
        }

        if (netexStop.getWaterSubmode() != null) {
            return mapTransportSubMode(netexStop.getWaterSubmode().value());
        } else if (netexStop.getTramSubmode() != null) {
            return mapTransportSubMode(netexStop.getTramSubmode().value());
        } else if (netexStop.getMetroSubmode() != null) {
            return mapTransportSubMode(netexStop.getMetroSubmode().value());
        } else if (netexStop.getRailSubmode() != null) {
            return mapTransportSubMode(netexStop.getRailSubmode().value());
        } else if (netexStop.getBusSubmode() != null) {
            return mapTransportSubMode(netexStop.getBusSubmode().value());
        } else if (netexStop.getAirSubmode() != null) {
            return mapTransportSubMode(netexStop.getAirSubmode().value());
        } else if (netexStop.getCoachSubmode() != null) {
            return mapTransportSubMode(netexStop.getCoachSubmode().value());
        } else if (netexStop.getFunicularSubmode() != null) {
            return mapTransportSubMode(netexStop.getFunicularSubmode().value());
        } else if (netexStop.getTelecabinSubmode() != null) {
            return mapTransportSubMode(netexStop.getTelecabinSubmode().value());
        }

        return null;
    }

    TransportSubModeNameEnum mapTransportSubMode(String netexValue) {
        try {
            return TransportSubModeNameEnum.valueOf(StringUtils.capitalize(netexValue));
        } catch (IllegalArgumentException iae) {
            log.warn("Unable to map unknown TransportModeNameEnum value: " + netexValue);
            return null;
        }
    }


    TransportModeNameEnum mapTransportModeName(AllVehicleModesOfTransportEnumeration netexMode) {
        if (netexMode == null) {
            return null;
        }

        switch (netexMode) {
            case AIR:
                return TransportModeNameEnum.Air;
            case BUS:
                return TransportModeNameEnum.Bus;
            case RAIL:
                return TransportModeNameEnum.Rail;
            case TRAM:
                return TransportModeNameEnum.Tram;
            case COACH:
                return TransportModeNameEnum.Coach;
            case FERRY:
                return TransportModeNameEnum.Ferry;
            case METRO:
                return TransportModeNameEnum.Metro;
            case WATER:
                return TransportModeNameEnum.Water;
            case CABLEWAY:
                return TransportModeNameEnum.Cableway;
            case FUNICULAR:
                return TransportModeNameEnum.Funicular;
            case TROLLEY_BUS:
                return TransportModeNameEnum.TrolleyBus;
            case LIFT:
            case OTHER:
                return TransportModeNameEnum.Other;
        }

        return TransportModeNameEnum.Other;
    }


    private void parseQuay(Context context, StopArea parentStopArea, Quay quay) throws Exception {
        Referential referential = (Referential) context.get(REFERENTIAL);
        NetexprofileImportParameters parameters = (NetexprofileImportParameters) context.get(CONFIGURATION);
        String quayId;
        if (parameters != null){
            //Netex file import by application : parameters are available
            quayId = NetexImportUtil.composeObjectId("Quay", parameters.getObjectIdPrefix(), quay.getId());
        }else{
            //Irkalla synchronization case : no parameters are defined.
            quayId = quay.getId();
        }

        StopArea boardingPosition = ObjectFactory.getStopArea(referential, quayId);
        boardingPosition.setAreaType(ChouetteAreaEnum.BoardingPosition);

        boardingPosition.setObjectVersion(NetexParserUtils.getVersion(quay));
        if (quay.getName() == null) {
            boardingPosition.setName(parentStopArea.getName());
        } else {
            boardingPosition.setName(quay.getName().getValue());
        }
        boardingPosition.setParent(parentStopArea);

        if (quay.getDescription() != null) {
            boardingPosition.setComment(quay.getDescription().getValue());
        }
        if (quay.getLandmark() != null) {
            boardingPosition.setNearestTopicName(quay.getLandmark().getValue());
        }

        String publicCode = quay.getPublicCode();
        boardingPosition.setRegistrationNumber(publicCode);
        

        SimplePoint_VersionStructure centroidStruct = quay.getCentroid();
        if (centroidStruct != null) {
            parseCentroid(centroidStruct.getLocation(), boardingPosition);
        }

        PostalAddress postalAddress = quay.getPostalAddress();
        if (postalAddress != null) {
            boardingPosition.setCountryCode(postalAddress.getPostCode());

            if (postalAddress.getAddressLine1() != null){
                boardingPosition.setStreetName(postalAddress.getAddressLine1().getValue());
            }

        }

        TariffZoneRefs_RelStructure tariffZonesStruct = quay.getTariffZones();
        if (tariffZonesStruct != null) {
            parseTariffZoneRefs(tariffZonesStruct, boardingPosition);
        }

        boardingPosition.setStopAreaType(parentStopArea.getStopAreaType());
        boardingPosition.setTransportModeName(parentStopArea.getTransportModeName());
        boardingPosition.setTransportSubMode(parentStopArea.getTransportSubMode());

        boardingPosition.setFilled(true);

        boardingPosition.setKeyValues(keyValueParser.parse(quay.getKeyList()));
        mapAccessibilityAssesssment(boardingPosition, quay);

        if(quay.getKeyList() != null && !quay.getKeyList().getKeyValue().isEmpty()){
            quay.getKeyList().getKeyValue().stream().filter(key -> key.getKey().equals(FARE_ZONE))
                    .forEach(key -> boardingPosition.setZoneId(key.getValue()));
        }
    }

    private void parseCentroid(LocationStructure locationStruct, StopArea stopArea) throws Exception {
        BigDecimal latitude = locationStruct.getLatitude();
        if (latitude != null) {
            stopArea.setLatitude(latitude);
        }
        BigDecimal longitude = locationStruct.getLongitude();
        if (longitude != null) {
            stopArea.setLongitude(longitude);
        }

        DirectPositionType positionType = locationStruct.getPos();
        if (positionType != null) {
            String projectedType = locationStruct.getSrsName();
            BigDecimal x = ParserUtils.getX(String.valueOf(positionType.getValue().get(0)));
            BigDecimal y = ParserUtils.getY(String.valueOf(positionType.getValue().get(1)));

            if (projectedType != null && x != null && y != null) {
                stopArea.setProjectionType(projectedType);
                stopArea.setX(x);
                stopArea.setY(y);
            }
        }

        if (stopArea.getLongitude() != null && stopArea.getLatitude() != null) {
            stopArea.setLongLatType(LongLatTypeEnum.WGS84);
        } else {
            stopArea.setLongitude(null);
            stopArea.setLatitude(null);
        }
    }

    private void parseTariffZoneRefs(TariffZoneRefs_RelStructure tariffZonesStruct, StopArea stopArea) throws Exception {
        List<TariffZoneRef> tariffZoneRefs = tariffZonesStruct.getTariffZoneRef_().stream().map(tzr -> (TariffZoneRef) tzr.getValue()).collect(Collectors.toList());

        for (TariffZoneRef tariffZoneRef : tariffZoneRefs) {
            Properties properties = tariffZoneProperties.get(tariffZoneRef.getRef());

            if (properties != null) {
                String tariffName = properties.getProperty(NAME);
                if (tariffName != null) {
                    try {
                        stopArea.setFareCode(Integer.parseInt(tariffName));
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    static {
        ParserFactory.register(StopPlaceParser.class.getName(), new ParserFactory() {
            private StopPlaceParser instance = new StopPlaceParser();

            @Override
            protected Parser create() {
                return instance;
            }
        });
    }

}
