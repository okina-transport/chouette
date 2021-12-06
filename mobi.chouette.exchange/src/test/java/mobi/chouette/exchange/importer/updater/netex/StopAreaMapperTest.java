package mobi.chouette.exchange.importer.updater.netex;

import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.util.Referential;
import org.rutebanken.netex.model.MultilingualString;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.Quays_RelStructure;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.StopTypeEnumeration;
import org.rutebanken.netex.model.TramSubmodeEnumeration;
import org.rutebanken.netex.model.VehicleModeEnumeration;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class StopAreaMapperTest {
    private StopAreaMapper stopAreaMapper = new StopAreaMapper();

    @Test
    public void setBoardingPositionNameIfMissing() {
        StopPlace stopPlace = new StopPlace()
                .withName(new MultilingualString().withValue("Festningen"))
                .withTransportMode(VehicleModeEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(new Quay().withTransportMode(VehicleModeEnumeration.BUS)));

        StopArea stopArea = stopAreaMapper.mapStopPlaceToStopArea(new Referential(), stopPlace);

        assertEquals(stopArea.getContainedStopAreas().get(0).getName(), "Festningen");
    }

    @Test
    public void keepBoardingPositionNameIfDifferent() {
        StopPlace stopPlace = new StopPlace()
                .withName(new MultilingualString().withValue("Festningen"))
                .withTransportMode(VehicleModeEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(new Quay().withName(new MultilingualString().withValue("A")).withTransportMode(VehicleModeEnumeration.BUS)));

        StopArea stopArea = stopAreaMapper.mapStopPlaceToStopArea(new Referential(), stopPlace);

        assertEquals(stopArea.getContainedStopAreas().get(0).getName(), "A");
    }

    @Test
    public void keepBoardingPositionComment() {
        StopPlace stopPlace = new StopPlace()
                .withName(new MultilingualString().withValue("Hestehovveien"))
                .withTransportMode(VehicleModeEnumeration.BUS)
                .withStopPlaceType(StopTypeEnumeration.BUS_STATION)
                .withQuays(new Quays_RelStructure()
                        .withQuayRefOrQuay(
                                new Quay()
                                        .withName(new MultilingualString().withValue("A"))
                                        .withTransportMode(VehicleModeEnumeration.BUS)
                                        .withDescription(new MultilingualString().withValue("description"))));


        StopArea stopArea = stopAreaMapper.mapStopPlaceToStopArea(new Referential(), stopPlace);

        assertEquals(stopArea.getContainedStopAreas().get(0).getComment(), "description");
    }
}