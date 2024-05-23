package mobi.chouette.exchange;

import mobi.chouette.model.Network;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class NetworkComparatorTest {

    private final NetworkComparator tested = new NetworkComparator();

    @Test
    public void testCompare_whenBothNonNullAndPosEqual_returnsZero() {
        Network n1 = new Network();
        n1.setPosition(1);
        Network n2 = new Network();
        n2.setPosition(1);
        assertEquals(0, tested.compare(n1, n2), "Both networks have the same position, should return 0");
    }

    @Test
    public void testCompare_whenBothNonNullAndFirstPosLesser_returnsMinusOne() {
        Network n1 = new Network();
        n1.setPosition(1);
        Network n2 = new Network();
        n2.setPosition(2);
        assertEquals(-1, tested.compare(n1, n2), "First network has a lower position, should return -1");
    }

    @Test
    public void testCompare_whenBothNonNullAndSecondPosLesser_returnsOne() {
        Network n1 = new Network();
        n1.setPosition(2);
        Network n2 = new Network();
        n2.setPosition(1);
        assertEquals(1, tested.compare(n1, n2), "Second network has a lower position, should return 1");
    }

    @Test
    public void testCompare_whenBothNonNullAndFirstPosNull_returnsOne() {
        Network n1 = new Network();
        n1.setPosition(null);
        Network n2 = new Network();
        n2.setPosition(1);
        assertEquals(1, tested.compare(n1, n2), "First network has a null position, should return 1");
    }

    @Test
    public void testCompare_whenBothNonNullAndSecondPosNull_returnsMinusOne() {
        Network n1 = new Network();
        n1.setPosition(2);
        Network n2 = new Network();
        n2.setPosition(null);
        assertEquals(-1, tested.compare(n1, n2), "Second network has a null position, should return -1");
    }



}