package mobi.chouette.exchange;

import mobi.chouette.model.Line;
import mobi.chouette.model.Network;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

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

    @Test
    public void testSorting_withAllKindOfLines_shouldSortCorrectly() {
        // arrange
        Network n1 = new Network();
        n1.setPosition(1);

        Network n2 = new Network();
        n2.setPosition(2);

        Line l100 = new Line();
        l100.setNetwork(n1);
        l100.setPosition(1);
        l100.setPublishedName("l100");
        l100.setObjectId("id100");

        Line l76 = new Line();
        l76.setNetwork(n1);
        l76.setPosition(2);
        l76.setPublishedName("l76");
        l76.setObjectId("id76");
        
        Line l3 = new Line();
        l3.setNetwork(n1);
        l3.setPosition(null);
        l3.setPublishedName("l3");
        l3.setObjectId("id3");

        Line l5 = new Line();
        l5.setNetwork(n1);
        l5.setPosition(null);
        l5.setPublishedName("l5");
        l5.setObjectId("id5");
            
        Line l4 = new Line();
        l4.setNetwork(n1);
        l4.setPosition(null);
        l4.setPublishedName(null);
        l4.setObjectId("id4");

        Line l8 = new Line();
        l8.setNetwork(n1);
        l8.setPosition(null);
        l8.setPublishedName(null);
        l8.setObjectId("id8");

        Line l1 = new Line();
        l1.setNetwork(n2);
        l1.setPosition(1);
        l1.setPublishedName("l1");
        l1.setObjectId("id1");

        List<Line> expected = Arrays.asList(l100, l76, l3, l5, l4, l8, l1);

        // act
        List<Line> output = Arrays.asList(l1, l3, l4, l5, l8, l76, l100);
        output.sort(tested);

        // assert
        Assert.assertEquals(output, expected);
    }

}


