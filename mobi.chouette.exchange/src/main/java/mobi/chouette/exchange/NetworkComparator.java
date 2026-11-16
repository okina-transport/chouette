package mobi.chouette.exchange;

import mobi.chouette.model.Network;

import java.util.Comparator;

/**
 * Compare networks by their position, then by their id (creation order) and objectId so that
 * networks without position (or sharing the same position) are never considered equal
 */
public class NetworkComparator implements Comparator<Network> {

    @Override
    public int compare(Network n1, Network n2) {
        return Comparator.comparing(Network::getPosition, Comparator.nullsLast(Integer::compare))
                .thenComparing(Network::getId, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Network::getObjectId, Comparator.nullsLast(Comparator.naturalOrder()))
                .compare(n1, n2);
    }

}
