package mobi.chouette.exchange.importer.updater.netex;

import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopAreaTranslation;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.type.TransportModeNameEnum;
import org.rutebanken.netex.model.AlternativeText;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.StopTypeEnumeration;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

public class StopPlaceMapperTest {

    private final StopPlaceMapper stopPlaceMapper = new StopPlaceMapper();

    @Test
    public void stopPlaceWithThreeBoardingPositions() {
        StopArea stopPlace = createStopPlace("Moensletta");

        StopArea firstBoardingPosition = createBoardingPosition(stopPlace.getName());
        firstBoardingPosition.setObjectId("1");
        StopArea secondBoardingPosition = createBoardingPosition(stopPlace.getName());
        secondBoardingPosition.setObjectId("2");
        StopArea thirdBoardingPosition = createBoardingPosition(stopPlace.getName());
        thirdBoardingPosition.setObjectId("3");
        stopPlace.setContainedStopAreas(Arrays.asList(firstBoardingPosition, secondBoardingPosition, thirdBoardingPosition));

        StopPlace netexStopPlace = stopPlaceMapper.mapStopAreaToStopPlace(stopPlace);

        assertNotNull(netexStopPlace);
        assertEquals(netexStopPlace.getName().getValue(), stopPlace.getName());

        assertNotNull(netexStopPlace.getQuays(), "Quays shall not be null.");
        assertEquals(netexStopPlace.getQuays().getQuayRefOrQuay().size(), 3);
        Quay firstQuay = (Quay) netexStopPlace.getQuays().getQuayRefOrQuay().get(0).getValue();
        assertEquals(firstQuay.getName().getValue(), stopPlace.getName());
    }

    @Test
    public void stopPlaceWithoutBoardingPositions() {
        StopArea stopPlace = createStopPlace("Klavestadhaugen");

        StopPlace netexStopPlace = stopPlaceMapper.mapStopAreaToStopPlace(stopPlace);

        assertNotNull(netexStopPlace);
        assertEquals(netexStopPlace.getName().getValue(), stopPlace.getName());
    }

    @Test
    public void stopPlaceWithId() {
        StopArea stopPlace = createStopPlace("Hestehaugveien");
        stopPlace.setObjectId("id");
        StopPlace netexStopPlace = stopPlaceMapper.mapStopAreaToStopPlace(stopPlace);

        assertEquals(netexStopPlace.getId(), String.valueOf(stopPlace.getObjectId()));
    }

    @Test
    public void boardingPositionWithoutStopPlace() {
        StopArea boardingPosition = createBoardingPosition("Borgen");
        StopPlace netexStopPlace = stopPlaceMapper.mapStopAreaToStopPlace(boardingPosition);

        assertNotNull(netexStopPlace);
        assertEquals(netexStopPlace.getName().getValue(), boardingPosition.getName());
    }

    @Test
    public void createQuayWithDescription() {
        StopArea stopArea = new StopArea();
        stopArea.setAreaType(ChouetteAreaEnum.BoardingPosition);
        stopArea.setComment("Comment text");
        Quay quay = stopPlaceMapper.mapQuay(stopArea);
        assertNotNull(quay.getDescription(), "description should not be null for quay");
        assertEquals(quay.getDescription().getValue(), stopArea.getComment());
    }

    @Test
    public void mapFerryToFerryStop() {
        StopPlace stopPlace = new StopPlace();
        TransportModeNameEnum transportModeNameEnum = TransportModeNameEnum.Ferry;

        stopPlaceMapper.mapTransportMode(stopPlace, transportModeNameEnum);
        assertEquals(stopPlace.getStopPlaceType(), StopTypeEnumeration.FERRY_STOP);
    }

    @Test
    public void stopPlaceWithTranslationsGetsAlternativeText() {
        StopArea stopPlace = createStopPlace("Moensletta");
        stopPlace.setObjectId("SP1");

        StopAreaTranslation translation = new StopAreaTranslation();
        translation.setFieldName("stopName");
        translation.setLanguage("en");
        translation.setTranslation("Moensletta EN");

        Map<String, List<StopAreaTranslation>> translationsByObjectId = Collections.singletonMap("SP1", Collections.singletonList(translation));

        StopPlace netexStopPlace = stopPlaceMapper.mapStopAreaToStopPlace(stopPlace, translationsByObjectId);

        assertNotNull(netexStopPlace.getAlternativeTexts(), "alternativeTexts should not be null");
        assertEquals(netexStopPlace.getAlternativeTexts().getAlternativeText().size(), 1);
        AlternativeText alternativeText = netexStopPlace.getAlternativeTexts().getAlternativeText().get(0);
        assertEquals(alternativeText.getAttributeName(), "Name");
        assertEquals(alternativeText.getUseForLanguage(), "en");
        assertEquals(alternativeText.getText().getValue(), "Moensletta EN");
    }

    @Test
    public void stopPlaceWithoutMatchingTranslationsGetsNoAlternativeText() {
        StopArea stopPlace = createStopPlace("Klavestadhaugen");
        stopPlace.setObjectId("SP2");

        StopPlace netexStopPlace = stopPlaceMapper.mapStopAreaToStopPlace(stopPlace, Collections.emptyMap());

        assertNull(netexStopPlace.getAlternativeTexts());
    }

    @Test
    public void mapPublicCode() {
        Quay quay = new Quay();
        StopArea stopArea = new StopArea();
        stopArea.setRegistrationNumber("A");

        stopPlaceMapper.mapPublicCode(stopArea, quay);
        assertEquals(quay.getPublicCode(), "A");
    }

    private StopArea createStopPlace(String name) {
        return createStopArea(name, ChouetteAreaEnum.StopPlace);
    }

    private StopArea createStopArea(String name, ChouetteAreaEnum chouetteAreaEnum) {
        StopArea stopPlace = new StopArea();
        stopPlace.setName(name);
        stopPlace.setAreaType(chouetteAreaEnum);
        return stopPlace;
    }


    private StopArea createBoardingPosition(String name) {
        StopArea boardingPosition = new StopArea();
        boardingPosition.setName(name);
        boardingPosition.setAreaType(ChouetteAreaEnum.BoardingPosition);
        return boardingPosition;
    }

}