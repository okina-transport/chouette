package mobi.chouette.dao;

import mobi.chouette.core.CoreException;
import mobi.chouette.dao.GenericDAO;
import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.Line;
import org.wololo.geojson.GeoJSON;

import java.util.List;

public interface JourneyPatternDAO extends GenericDAO<JourneyPattern> {

    String removeDeletedJourneyPatterns() throws CoreException;

    JourneyPattern findByIdMapMatchingLazyDeps(Long id);

    List<JourneyPattern> getEnabledJourneyOfLine(Line line);

    JourneyPattern updateGeoJson(JourneyPattern journeyPattern, GeoJSON geoJson);
}
