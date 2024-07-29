package mobi.chouette.exchange.gtfs.model;

import mobi.chouette.model.type.TransportModeNameEnum;
import mobi.chouette.model.type.TransportSubModeNameEnum;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test
public class RouteTypeEnumTest {

    @DataProvider(name = "transportModeToRouteType")
    public Object[][] transportModeToRouteType() {
        return new Object[][] {
                {TransportModeNameEnum.Tram, RouteTypeEnum.Tram},
                {TransportModeNameEnum.Metro, RouteTypeEnum.Subway},
                {TransportModeNameEnum.Rail, RouteTypeEnum.Rail},
                {TransportModeNameEnum.Bus, RouteTypeEnum.Bus},
                {TransportModeNameEnum.Ferry, RouteTypeEnum.Ferry},
                {TransportModeNameEnum.Cableway, RouteTypeEnum.AerialLift},
                {TransportModeNameEnum.Funicular, RouteTypeEnum.Funicular},
                {TransportModeNameEnum.TrolleyBus, RouteTypeEnum.Trolleybus},
                {TransportModeNameEnum.Water, RouteTypeEnum.Ferry},
        };
    }

    @DataProvider(name = "notHandledTransportModes")
    public Object[][] notHandledTransportModes() {
        return new Object[][] {
                {TransportModeNameEnum.Air},
                {TransportModeNameEnum.Coach},
                {TransportModeNameEnum.Lift},
                {TransportModeNameEnum.Taxi},
                {TransportModeNameEnum.Bicycle},
                {TransportModeNameEnum.Other},
        };
    }

    @DataProvider(name = "busSubModeToRouteType")
    public Object[][] busSubModeToRouteType() {
        return new Object[][] {
                { TransportSubModeNameEnum.RegionalBus, RouteTypeEnum.RegionalBusService },
                { TransportSubModeNameEnum.ExpressBus, RouteTypeEnum.ExpressBusService },
                { TransportSubModeNameEnum.LocalBus, RouteTypeEnum.LocalBusService },
                { TransportSubModeNameEnum.NightBus, RouteTypeEnum.NightBusService },
                { TransportSubModeNameEnum.SpecialNeedsBus, RouteTypeEnum.SpecialNeedsBus },
                { TransportSubModeNameEnum.MobilityBus, RouteTypeEnum.MobilityBusService },
                { TransportSubModeNameEnum.SightseeingBus, RouteTypeEnum.SightseeingBus },
                { TransportSubModeNameEnum.ShuttleBus, RouteTypeEnum.ShuttleBus },
                { TransportSubModeNameEnum.SchoolBus, RouteTypeEnum.SchoolBus },
                { TransportSubModeNameEnum.SchoolAndPublicServiceBus, RouteTypeEnum.SchoolandPublicServiceBus },
                { TransportSubModeNameEnum.RailReplacementBus, RouteTypeEnum.RailReplacementBusService },
                { TransportSubModeNameEnum.DemandAndResponseBus, RouteTypeEnum.DemandandResponseBusService },
        };
    }

    public void test__fromTransportMode__when_mode_is_null__returns_bus() {
        Assert.assertEquals(RouteTypeEnum.fromTransportMode(null), RouteTypeEnum.Bus, "should return Bus");
    }

    @Test(dataProvider = "transportModeToRouteType")
    public void test__fromTransportMode__when_transport_mode_is_set__returns_correct_route_type(TransportModeNameEnum in, RouteTypeEnum expected) {
        Assert.assertEquals(RouteTypeEnum.fromTransportMode(in), expected, "should return " + expected);
    }

    @Test(dataProvider = "notHandledTransportModes")
    public void test__fromTransportMode__when_transport_mode_is_not_handled__returns_bus_by_default(TransportModeNameEnum notHandled) {
        Assert.assertEquals(RouteTypeEnum.fromTransportMode(notHandled), RouteTypeEnum.Bus, "should return Bus by default");
    }

    @Test
    public void test__fromTransportModeAndSubMode__when_mode_and_sub_mode_are_null__returns_bus() {
        Assert.assertEquals(RouteTypeEnum.fromTransportModeAndSubMode(null, null), RouteTypeEnum.Bus, "should return Bus");
    }

    @Test(dataProvider = "busSubModeToRouteType")
    public void test__fromTransportModeAndSubMode__when_mode_is_bus_and_sub_mode_is_set__returns_correct_route_type(TransportSubModeNameEnum in, RouteTypeEnum expected) {
        Assert.assertEquals(RouteTypeEnum.fromTransportModeAndSubMode(TransportModeNameEnum.Bus, in), expected, "should return " + expected);
    }

}
