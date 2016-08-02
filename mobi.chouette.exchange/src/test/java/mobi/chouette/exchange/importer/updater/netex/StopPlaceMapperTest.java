package mobi.chouette.exchange.importer.updater.netex;

import mobi.chouette.model.StopArea;
import mobi.chouette.model.StopPoint;
import mobi.chouette.model.type.ChouetteAreaEnum;
import no.rutebanken.netex.model.Quay;
import no.rutebanken.netex.model.StopPlace;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.testng.Assert.*;

public class StopPlaceMapperTest {

    private StopPlaceMapper stopPlaceMapper = new StopPlaceMapper();

    @Test
    public void stopPlaceWithThreeBoardingPositions() {
        StopArea stopPlace = createStopPlace("Moensletta");

        StopArea firstBoardingPosition = createBoardingPosition(stopPlace.getName());
        StopArea secondBoardingPosition = createBoardingPosition(stopPlace.getName());
        StopArea thirdBoardingPosition = createBoardingPosition(stopPlace.getName());
        stopPlace.setContainedStopAreas(Arrays.asList(firstBoardingPosition, secondBoardingPosition, thirdBoardingPosition));

        StopPlace netexStopPlace = stopPlaceMapper.mapStopAreaToStopPlace(stopPlace);

        assertNotNull(netexStopPlace);
        assertEquals(netexStopPlace.getName().getValue(), stopPlace.getName());

        assertNotNull(netexStopPlace.getQuays(), "Quays shall not be null.");
        assertEquals(netexStopPlace.getQuays().getQuayRefOrQuay().size(), 3);
        Quay firstQuay = (Quay) netexStopPlace.getQuays().getQuayRefOrQuay().get(0);
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
    public void boardingPositionWithoutStopPlace() {
        StopArea boardingPosition = createBoardingPosition("Borgen");
        StopPlace netexStopPlace = stopPlaceMapper.mapStopAreaToStopPlace(boardingPosition);

        assertNotNull(netexStopPlace);
        assertEquals(netexStopPlace.getName().getValue(), boardingPosition.getName());
    }

    private StopArea createStopPlace(String name) {
        StopArea stopPlace = new StopArea();
        stopPlace.setName(name);
        stopPlace.setAreaType(ChouetteAreaEnum.StopPlace);
        return stopPlace;
    }

    private StopArea createBoardingPosition(String name) {
        StopArea boardingPosition = new StopArea();
        boardingPosition.setName(name);
        boardingPosition.setAreaType(ChouetteAreaEnum.BoardingPosition);
        return boardingPosition;
    }

}