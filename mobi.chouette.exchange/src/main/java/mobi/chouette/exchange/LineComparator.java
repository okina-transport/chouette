package mobi.chouette.exchange;

import mobi.chouette.model.Line;

import java.util.Comparator;

/**
 * Compare line this way:
 * - by their network position
 * - if both network position are equals or both lines network or network position are null, then by their own position
 * - if both position are equals or null then by their published name
 * - if both published name are equal or null by then their objectId
 */
public class LineComparator implements Comparator<Line> {

    @Override
    public int compare(Line o1, Line o2) {
        return Comparator.comparing(Line::getNetwork, Comparator.nullsLast(new NetworkComparator()))
                .thenComparing(Line::getPosition, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Line::getPublishedName, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Line::getObjectId)
                .compare(o1, o2);
    }

}