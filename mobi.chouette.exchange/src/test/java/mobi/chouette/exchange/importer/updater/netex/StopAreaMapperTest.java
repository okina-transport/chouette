package mobi.chouette.exchange.importer.updater.netex;

import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.util.Referential;
import org.rutebanken.netex.model.*;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class StopAreaMapperTest {

    private static final ObjectFactory netexObjectFactory = new ObjectFactory();

    private StopAreaMapper stopAreaMapper = new StopAreaMapper();

    @Test
    public void setBoardingPositionNameIfMissing() {
        StopPlace stopPlace = new StopPlace()
                .withName(new MultilingualString().withValue("Festningen"))
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay().withTransportMode(AllVehicleModesOfTransportEnumeration.BUS))));

        StopArea stopArea = stopAreaMapper.mapStopPlaceToStopArea(new Referential(), stopPlace);

        assertEquals(stopArea.getContainedStopAreas().get(0).getName(), "Festningen");
    }

    @Test
    public void keepBoardingPositionNameIfDifferent() {
        StopPlace stopPlace = new StopPlace()
                .withName(new MultilingualString().withValue("Festningen"))
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(netexObjectFactory.createQuay(new Quay().withName(new MultilingualString().withValue("A")).withTransportMode(AllVehicleModesOfTransportEnumeration.BUS))));

        StopArea stopArea = stopAreaMapper.mapStopPlaceToStopArea(new Referential(), stopPlace);

        assertEquals(stopArea.getContainedStopAreas().get(0).getName(), "A");
    }

    @Test
    public void keepBoardingPositionComment() {
        StopPlace stopPlace = new StopPlace()
                .withName(new MultilingualString().withValue("Hestehovveien"))
                .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(
                                netexObjectFactory.createQuay(new Quay()
                                        .withName(new MultilingualString().withValue("A"))
                                        .withTransportMode(AllVehicleModesOfTransportEnumeration.BUS)
                                        .withDescription(new MultilingualString().withValue("description")))));


        StopArea stopArea = stopAreaMapper.mapStopPlaceToStopArea(new Referential(), stopPlace);

        assertEquals(stopArea.getContainedStopAreas().get(0).getComment(), "description");
    }
}