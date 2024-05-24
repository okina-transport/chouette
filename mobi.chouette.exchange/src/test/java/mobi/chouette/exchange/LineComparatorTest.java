package mobi.chouette.exchange;

import mobi.chouette.model.Line;
import mobi.chouette.model.Network;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class LineComparatorTest {

    private final LineComparator tested = new LineComparator();

    @Test
    public void testCompare_whenSameNetworkSamePositionSamePublishedNameSameObjectId_returnsZero() {
        Line o1 = new Line();
        o1.setNetwork(new Network());
        o1.getNetwork().setPosition(1);
        o1.setPosition(1);
        o1.setPublishedName("A");
        o1.setObjectId("id1");

        Line o2 = new Line();
        o2.setNetwork(new Network());
        o2.getNetwork().setPosition(1);
        o2.setPosition(1);
        o2.setPublishedName("A");
        o2.setObjectId("id1");

        assertEquals(tested.compare(o1, o2), 0,"Both lines are equal in all fields, should return 0");
    }

    @Test
    public void testCompare_whenDifferentNetwork_returnsNetworkComparison() {
        Line o1 = new Line();
        o1.setNetwork(new Network());
        o1.getNetwork().setPosition(1);
        o1.setPosition(1);
        o1.setPublishedName("A");
        o1.setObjectId("id1");

        Line o2 = new Line();
        o2.setNetwork(new Network());
        o2.getNetwork().setPosition(2);
        o2.setPosition(1);
        o2.setPublishedName("A");
        o2.setObjectId("id1");

        assertEquals(tested.compare(o1, o2), -1, "Networks are different, should return network comparison result");
    }

    @Test
    public void testCompare_whenSameNetworkDifferentPosition_returnsPositionComparison() {
        Line o1 = new Line();
        o1.setNetwork(new Network());
        o1.getNetwork().setPosition(1);
        o1.setPosition(1);
        o1.setPublishedName("A");
        o1.setObjectId("id1");

        Line o2 = new Line();
        o2.setNetwork(new Network());
        o2.getNetwork().setPosition(1);
        o2.setPosition(2);
        o2.setPublishedName("A");
        o2.setObjectId("id1");

        assertEquals(tested.compare(o1, o2), -1, "Positions are different, should return position comparison result");
    }

    @Test
    public void testCompare_whenSameNetworkSamePositionDifferentPublishedName_returnsPublishedNameComparison() {
        Line o1 = new Line();
        o1.setNetwork(new Network());
        o1.getNetwork().setPosition(1);
        o1.setPosition(1);
        o1.setPublishedName("A");
        o1.setObjectId("id1");

        Line o2 = new Line();
        o2.setNetwork(new Network());
        o2.getNetwork().setPosition(1);
        o2.setPosition(1);
        o2.setPublishedName("B");
        o2.setObjectId("id1");

        assertTrue(tested.compare(o1, o2) < 0, "Published names are different, should return published name comparison result");
    }

    @Test
    public void testCompare_whenNetworkNull_returnsNetworkComparison() {
        Line o1 = new Line();
        o1.setNetwork(null);
        o1.setPosition(1);
        o1.setPublishedName("A");
        o1.setObjectId("id1");

        Line o2 = new Line();
        o2.setNetwork(new Network());
        o2.getNetwork().setPosition(1);
        o2.setPosition(1);
        o2.setPublishedName("A");
        o2.setObjectId("id1");

        assertEquals(tested.compare(o1, o2),  1, "Networks are different, should return network comparison result");
    }

    @Test
    public void testCompare_whenSameNetworkAndPositionNull_returnsPositionComparison() {
        Line o1 = new Line();
        o1.setNetwork(new Network());
        o1.getNetwork().setPosition(1);
        o1.setPosition(1);
        o1.setPublishedName("A");
        o1.setObjectId("id1");

        Line o2 = new Line();
        o2.setNetwork(new Network());
        o2.getNetwork().setPosition(1);
        o2.setPosition(null);
        o2.setPublishedName("A");
        o2.setObjectId("id1");

        assertEquals(tested.compare(o1, o2), -1, "Positions are different, should return position comparison result");
    }

    @Test
    public void testCompare_whenPublishedNameNull_returnsPublishedNameComparison() {
        Line o1 = new Line();
        o1.setNetwork(new Network());
        o1.getNetwork().setPosition(1);
        o1.setPosition(1);
        o1.setPublishedName(null);
        o1.setObjectId("id1");

        Line o2 = new Line();
        o2.setNetwork(new Network());
        o2.getNetwork().setPosition(1);
        o2.setPosition(1);
        o2.setPublishedName("A");
        o2.setObjectId("id1");

        assertEquals(tested.compare(o1, o2), 1, "Published names are different, should return published name comparison result");
    }

    @Test
    public void testCompare_whenAllNullExceptObjectId_returnsObjectIdComparison() {
        Line o1 = new Line();
        o1.setObjectId("id1");

        Line o2 = new Line();
        o2.setObjectId("id2");

        assertTrue(tested.compare(o1, o2) < 0, "Object IDs are different, should return object ID comparison result");
    }

}

