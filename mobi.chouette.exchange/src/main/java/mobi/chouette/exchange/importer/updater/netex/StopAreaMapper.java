package mobi.chouette.exchange.importer.updater.netex;

import mobi.chouette.exchange.NetexParserUtils;
import mobi.chouette.exchange.importer.updater.NeTExStopPlaceUtil;
import mobi.chouette.model.KeyValue;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.type.LongLatTypeEnum;
import mobi.chouette.model.type.StopAreaTypeEnum;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.apache.commons.lang3.StringUtils;
import org.rutebanken.netex.model.*;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static mobi.chouette.exchange.importer.updater.NeTExStopPlaceRegisterUpdater.EXTERNAL_REF;
import static mobi.chouette.exchange.importer.updater.NeTExStopPlaceRegisterUpdater.FARE_ZONE;

/**
 * Map from NeTEx to chouette model
 */
public class StopAreaMapper {

    public StopArea mapCommercialStopPoint(Referential referential, StopArea stopArea) {
        String split[] = stopArea.getObjectId().split(":");
        String parentId = split[0] + ":StopPlace:" + split[2];

        StopArea parent = ObjectFactory.getStopArea(referential, parentId);
        parent.setAreaType(ChouetteAreaEnum.CommercialStopPoint);
        parent.setLatitude(stopArea.getLatitude());
        parent.setLongitude(stopArea.getLongitude());
        parent.setLongLatType(stopArea.getLongLatType());
        parent.setName(stopArea.getName());

        stopArea.setParent(parent);

        return parent;
    }

    public StopArea mapStopPlaceToStopArea(Referential referential, StopPlace stopPlace) {
        StopArea stopArea = mapStopArea(referential, stopPlace);

        Quays_RelStructure quays = stopPlace.getQuays();
        if (quays != null) {
            for (Object q : quays.getQuayRefOrQuay().stream().map(JAXBElement::getValue).collect(Collectors.toList())) {
                StopArea boardingPosition = mapBoardingPosition(referential, stopPlace, (Quay) q);
                boardingPosition.setParent(stopArea);
            }
        }

        return stopArea;
    }

    public void mapCentroidToChouette(Zone_VersionStructure zone, StopArea stopArea) {
        if (zone.getCentroid() != null && zone.getCentroid().getLocation() != null) {
            LocationStructure location = zone.getCentroid().getLocation();
            stopArea.setLatitude(location.getLatitude());
            stopArea.setLongitude(location.getLongitude());
            stopArea.setLongLatType(LongLatTypeEnum.WGS84);
        }
    }

    public void mapQuayName(StopPlace stopPlace, Quay quay, StopArea stopArea) {
        if (quay.getName() == null) {
            if (stopPlace.getName() != null) {
                stopArea.setName(stopPlace.getName().getValue());
            }
        } else if (quay.getName() != null) {
            if (stopPlace.getName() != null && multiLingualStringEquals(stopPlace.getName(), quay.getName())) {
                // Same as parent
                stopArea.setName(quay.getName().getValue());
            } else {
                stopArea.setName(quay.getName().getValue());
                stopArea.setRegistrationNumber(quay.getPublicCode());
            }
        }
    }

    public void mapName(Zone_VersionStructure zone, StopArea stopArea) {
        if (zone.getName() != null) {
            stopArea.setName(zone.getName().getValue());
        }
    }

    private boolean multiLingualStringEquals(MultilingualString a, MultilingualString b) {
        return a.getValue().equals(b.getValue());
    }

    private StopArea mapBoardingPosition(Referential referential, StopPlace stopPlace, Quay quay) {

        StopArea boardingPosition = ObjectFactory.getStopArea(referential, quay.getId());
        // Set default values TODO set what we get from NSR
        mapMobilityRestrictedSuitable(quay, boardingPosition);
        boardingPosition.setLiftAvailable(null);
        boardingPosition.setStairsAvailable(null);
        mapQuayDescription(quay, boardingPosition);
        boardingPosition.setAreaType(ChouetteAreaEnum.BoardingPosition);
        mapCentroidToChouette(quay, boardingPosition);
        mapQuayName(stopPlace, quay, boardingPosition);
        mapQuayUrl(quay, boardingPosition);
        mapQuayRegistrationNumber(quay, boardingPosition);
        createCompassBearing(quay, boardingPosition);
        mapOriginalStopId(quay, boardingPosition);
        boardingPosition.setTransportModeName(NetexParserUtils.toTransportModeNameEnum(quay.getTransportMode().value()));
        boardingPosition.setStopAreaType(StopAreaTypeEnum.valueOf(StringUtils.capitalize(stopPlace.getStopPlaceType().value())));
        mapKeyValuesExternalRef(quay, boardingPosition);
        mapFareZone(quay, boardingPosition);
        return boardingPosition;
    }

    private void mapQuayDescription(Quay quay, StopArea boardingPosition) {
        if(quay.getDescription() != null){
            if (StringUtils.isNotBlank(quay.getDescription().getValue())) {
                boardingPosition.setComment(quay.getDescription().getValue());
            }
        }
    }

    private void mapMobilityRestrictedSuitable(SiteElement_VersionStructure siteElt, StopArea boardingPosition) {
        if(siteElt.getAccessibilityAssessment() != null){
            if(siteElt.getAccessibilityAssessment().getLimitations() != null){
                if(siteElt.getAccessibilityAssessment().getLimitations().getAccessibilityLimitation() != null){
                    if(siteElt.getAccessibilityAssessment().getLimitations().getAccessibilityLimitation().getWheelchairAccess() != null){
                        if(siteElt.getAccessibilityAssessment().getLimitations().getAccessibilityLimitation().getWheelchairAccess().equals(LimitationStatusEnumeration.TRUE)){
                            boardingPosition.setMobilityRestrictedSuitable(true);
                        }
                        if(siteElt.getAccessibilityAssessment().getLimitations().getAccessibilityLimitation().getWheelchairAccess().equals(LimitationStatusEnumeration.FALSE)){
                            boardingPosition.setMobilityRestrictedSuitable(false);
                        }
                        if(siteElt.getAccessibilityAssessment().getLimitations().getAccessibilityLimitation().getWheelchairAccess().equals(LimitationStatusEnumeration.UNKNOWN)){
                            boardingPosition.setMobilityRestrictedSuitable(null);
                        }
                    }
                }
            }
        }
    }

    private StopArea mapStopArea(Referential referential, StopPlace stopPlace) {
        StopArea stopArea = ObjectFactory.getStopArea(referential, stopPlace.getId());
        stopArea.setAreaType(ChouetteAreaEnum.CommercialStopPoint);

        mapMobilityRestrictedSuitable(stopPlace, stopArea);

        stopArea.setLiftAvailable(null);
        stopArea.setStairsAvailable(null);
        stopArea.setTransportModeName(NetexParserUtils.toTransportModeNameEnum(stopPlace.getTransportMode().value()));
        stopArea.setStopAreaType(StopAreaTypeEnum.valueOf(StringUtils.capitalize(stopPlace.getStopPlaceType().value())));
        mapCentroidToChouette(stopPlace, stopArea);
        mapName(stopPlace, stopArea);
        mapOriginalStopId(stopPlace, stopArea);
        mapKeyValuesExternalRef(stopPlace, stopArea);
        mapFareZone(stopPlace, stopArea);
        return stopArea;
    }


    /**
     * Read netex stop place key values to find an id to set in original_stop_id
     * - If a selected-id exists : it is set in original_stop_id
     * - if a selected-id does not exist : the search is made in imported-ids
     * @param srcZone
     *   the original zone with imported-id and selected-ids
     * @param createdStopArea
     *   the stopArea for which original_stop_id must be set
     */
    private void mapOriginalStopId(Zone_VersionStructure srcZone, StopArea createdStopArea){
        Optional<String> selectedIdOpt = NeTExStopPlaceUtil.getSelectedId(srcZone);
        if (selectedIdOpt.isPresent()){
            createdStopArea.setOriginalStopId(NeTExStopPlaceUtil.extractIdPostfix(selectedIdOpt.get()));
            //selected-id has higher priority than imported-id. If it exists, it is set in original_stop_id
            return;
        }

        Optional<String> importedIdOpt = NeTExStopPlaceUtil.getImportedId(srcZone);
        importedIdOpt.ifPresent(importedId->createdStopArea.setOriginalStopId(NeTExStopPlaceUtil.extractIdPostfix(importedId)));
    }




    private void createCompassBearing(Quay quay, StopArea boardingPosition) {
        if (quay.getCompassBearing() != null) {
            boardingPosition.setCompassBearing(quay.getCompassBearing().intValue());
        }
    }

    private void mapQuayUrl(Quay quay, StopArea boardingPosition){
        if(StringUtils.isNotBlank(quay.getUrl())){
            boardingPosition.setUrl(quay.getUrl());
        }
    }

    private void mapQuayRegistrationNumber(Quay quay, StopArea boardingPosition){
        if(StringUtils.isNotBlank(quay.getPublicCode())){
            boardingPosition.setRegistrationNumber(quay.getPublicCode());
        }
    }

    public void mapKeyValuesExternalRef(Zone_VersionStructure srcZone, StopArea createdStopArea) {
        if(srcZone.getKeyList() != null){
            for (KeyValueStructure keyValueStructure : srcZone.getKeyList().getKeyValue()) {
                if(org.apache.commons.lang.StringUtils.equals(keyValueStructure.getKey(), EXTERNAL_REF) && org.apache.commons.lang.StringUtils.isNotEmpty(keyValueStructure.getValue())){
                    KeyValue keyValue = new KeyValue();
                    keyValue.setKey(EXTERNAL_REF);
                    keyValue.setValue(keyValueStructure.getValue());
                    List<KeyValue> keyValues = new ArrayList<>();
                    keyValues.add(keyValue);
                    createdStopArea.setKeyValues(keyValues);
                }
            }
        }
    }

    private void mapFareZone(Zone_VersionStructure srcZone, StopArea createdStopArea){
        if(srcZone.getKeyList() != null){
            for (KeyValueStructure keyValueStructure : srcZone.getKeyList().getKeyValue()) {
                if(org.apache.commons.lang.StringUtils.equals(keyValueStructure.getKey(), FARE_ZONE) && org.apache.commons.lang.StringUtils.isNotEmpty(keyValueStructure.getValue())){
                    createdStopArea.setZoneId(keyValueStructure.getValue());
                }
            }
        }
    }
}
