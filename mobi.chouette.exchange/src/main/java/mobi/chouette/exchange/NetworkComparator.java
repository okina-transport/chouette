package mobi.chouette.exchange;

import mobi.chouette.model.Network;

import java.util.Comparator;

public class NetworkComparator implements Comparator<Network> {

    @Override
    public int compare(Network n1, Network n2) {
        return Comparator.comparing(Network::getPosition, Comparator.nullsLast(Integer::compare)).compare(n1, n2);
    }

}
