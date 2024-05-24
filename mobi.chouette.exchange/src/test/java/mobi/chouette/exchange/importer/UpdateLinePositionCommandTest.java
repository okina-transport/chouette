package mobi.chouette.exchange.importer;


import mobi.chouette.common.Context;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.NetworkDAO;
import mobi.chouette.model.Line;
import mobi.chouette.model.Network;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UpdateLinePositionCommandTest {

    private LineDAO lineDAOMock;
    private NetworkDAO networkDAOMock;
    UpdateLinePositionCommand tested;

    @BeforeMethod
    private void beforeMethod() {
        lineDAOMock = Mockito.mock(LineDAO.class);
        networkDAOMock = Mockito.mock(NetworkDAO.class);
        tested = new UpdateLinePositionCommand(lineDAOMock, networkDAOMock);
    }

    @DataProvider(name="nullAndEmptyNetworks")
    private Object[][] nullAndEmptyNetworks() {
        return new Object[][]{ { null }, { new ArrayList<Network>() } };
    }

    @Test(dataProvider = "nullAndEmptyNetworks")
    public void testExecute_whenNoNetworks_updatesNoPosition(List<Network> networks) throws Exception {
        // arrange
        Mockito.when(networkDAOMock.findAll()).thenReturn(networks);

        // act
        boolean out = tested.execute(new Context());

        // assert
        Assert.assertTrue(out, "should return true");
        Mockito.verify(lineDAOMock, Mockito.never()).update(Mockito.any());
        Mockito.verify(lineDAOMock, Mockito.never()).flush();

    }

    @DataProvider(name="nullAndEmptyLines")
    private Object[][] nullAndEmptyLines() {
        return new Object[][]{ { null }, { new ArrayList<Line>() } };
    }

    @Test(dataProvider = "nullAndEmptyLines")
    public void testExecute_whenNoLinesOnNetworks__updatesNoPosition(List<Line> lines) throws Exception {
        // arrange
        Network network = new Network();
        network.setId(1L);
        network.setSupprime(false);

        Mockito.when(networkDAOMock.findAll()).thenReturn(Collections.singletonList(network));
        Mockito.when(lineDAOMock.findByNetworkIdNotDeleted(network.getId())).thenReturn(lines);

        // act
        boolean out = tested.execute(new Context());

        // assert
        Assert.assertTrue(out, "should return true");
        Mockito.verify(lineDAOMock, Mockito.never()).update(Mockito.any());
        Mockito.verify(lineDAOMock, Mockito.never()).flush();
    }

    @Test
    void testExecute_whenPositionsAreUpdated__updatesThemCorrectly() throws Exception {
        // arrange
        Network n1 = new Network();
        n1.setId(1L);
        n1.setSupprime(false);

        Network n2 = new Network();
        n2.setId(2L);
        n2.setSupprime(false);

        Line l1 = new Line();
        l1.setId(1L);
        l1.setSupprime(false);
        l1.setPublishedName("l1");
        l1.setPosition(null);

        Line l2 = new Line();
        l2.setId(2L);
        l2.setSupprime(false);
        l2.setPublishedName("l2");
        l2.setPosition(5);

        Line l3 = new Line();
        l3.setId(3L);
        l3.setSupprime(false);
        l3.setPublishedName("l3");
        l3.setPosition(1);

        Line l4 = new Line();
        l4.setId(4L);
        l4.setSupprime(false);
        l4.setPublishedName("l4");
        l4.setPosition(5);

        Line l5 = new Line();
        l5.setId(5L);
        l5.setSupprime(false);
        l5.setPublishedName("l5");
        l5.setPosition(1500);

        Line l6 = new Line();
        l6.setId(6L);
        l6.setSupprime(false);
        l6.setPublishedName("l6");
        l6.setPosition(null);

        Mockito.when(networkDAOMock.findAll()).thenReturn(Arrays.asList(n1, n2));
        Mockito.when(lineDAOMock.findByNetworkIdNotDeleted(n1.getId())).thenReturn(Arrays.asList(l1, l2, l3));
        Mockito.when(lineDAOMock.findByNetworkIdNotDeleted(n2.getId())).thenReturn(Arrays.asList(l4, l5, l6));

        // act
        boolean out = tested.execute(new Context());

        // assert
        Assert.assertTrue(out, "should return true");
        Assert.assertEquals(l1.getPosition().intValue(), 3, "position should be equal");
        Assert.assertEquals(l2.getPosition().intValue(), 2, "position should be equal");
        Assert.assertEquals(l3.getPosition().intValue(), 1, "position should be equal");

        Assert.assertEquals(l4.getPosition().intValue(), 1, "position should be equal");
        Assert.assertEquals(l5.getPosition().intValue(), 2, "position should be equal");
        Assert.assertEquals(l6.getPosition().intValue(), 3, "position should be equal");

        Mockito.verify(lineDAOMock, Mockito.times(5)).update(Mockito.any());
        Mockito.verify(lineDAOMock).flush();
    }

    @Test
    void testExecute_whenAtLeastOneLineHasNoPositionAndNoPublishedName_doNotThrowNPE() throws Exception {
        // arrange
        Network network = new Network();
        network.setId(1L);
        network.setSupprime(false);

        Line l1 = new Line();
        l1.setId(1L);
        l1.setSupprime(false);
        l1.setPublishedName(null);
        l1.setPosition(null);

        Line l2 = new Line();
        l2.setId(2L);
        l2.setSupprime(false);
        l2.setPublishedName("l2");
        l2.setPosition(5);

        Line l3 = new Line();
        l3.setId(3L);
        l3.setSupprime(false);
        l3.setPublishedName("l3");
        l3.setPosition(1);

        Line l4 = new Line();
        l4.setId(4L);
        l4.setSupprime(false);
        l4.setPublishedName("l4");
        l4.setPosition(5);

        Line l5 = new Line();
        l5.setId(5L);
        l5.setSupprime(false);
        l5.setPublishedName("l5");
        l5.setPosition(1500);

        Line l6 = new Line();
        l6.setId(6L);
        l6.setSupprime(false);
        l6.setPublishedName("l6");
        l6.setPosition(null);

        Mockito.when(networkDAOMock.findAll()).thenReturn(Collections.singletonList(network));
        Mockito.when(lineDAOMock.findByNetworkIdNotDeleted(network.getId())).thenReturn(Arrays.asList(l1, l2, l3, l4, l5, l6));

        try {
            // act
            boolean out = tested.execute(new Context());

            // assert
            Assert.assertTrue(out, "should return true");
        } catch (NullPointerException e) {
            Assert.fail("should not raise NullPointerException");
        }

        Assert.assertEquals(l1.getPosition().intValue(), 6, "position should be equal");
        Assert.assertEquals(l2.getPosition().intValue(), 2, "position should be equal");
        Assert.assertEquals(l3.getPosition().intValue(), 1, "position should be equal");
        Assert.assertEquals(l4.getPosition().intValue(), 3, "position should be equal");
        Assert.assertEquals(l5.getPosition().intValue(), 4, "position should be equal");
        Assert.assertEquals(l6.getPosition().intValue(), 5, "position should be equal");

        Mockito.verify(lineDAOMock, Mockito.times(5)).update(Mockito.any());
        Mockito.verify(lineDAOMock).flush();
    }

    @Test
    void testExecute_whenNoLinePositionIsUpdated_doNotFlush() throws Exception {
        // arrange
        Network network = new Network();
        network.setId(1L);
        network.setSupprime(false);

        Line l1 = new Line();
        l1.setId(1L);
        l1.setPosition(1);
        l1.setSupprime(false);
        l1.setPublishedName("l1");

        Line l2 = new Line();
        l2.setId(2L);
        l2.setPosition(2);
        l2.setSupprime(false);
        l2.setPublishedName("l2");

        Line l3 = new Line();
        l3.setId(3L);
        l3.setPosition(3);
        l3.setSupprime(false);
        l3.setPublishedName("l3");

        Mockito.when(networkDAOMock.findAll()).thenReturn(Collections.singletonList(network));
        Mockito.when(lineDAOMock.findByNetworkIdNotDeleted(network.getId())).thenReturn(Arrays.asList(l1, l2, l3));

        // act
        boolean out = tested.execute(new Context());

        // assert
        Assert.assertTrue(out, "should return true");

        Mockito.verify(lineDAOMock, Mockito.never()).update(Mockito.any());
        Mockito.verify(lineDAOMock, Mockito.never()).flush();
    }

}


