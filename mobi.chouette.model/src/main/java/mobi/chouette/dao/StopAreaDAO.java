package mobi.chouette.dao;

import mobi.chouette.core.CoreException;
import mobi.chouette.model.SearchAddressFeatures;
import mobi.chouette.model.StopArea;

import java.util.List;

public interface StopAreaDAO extends GenericDAO<StopArea> {

    List<String> getBoardingPositionObjectIds();
    void mergeStopArea30m(Long from, Long into) throws CoreException;
    List<StopArea> findByOriginalId(String id);
    List<StopArea> findByOriginalIds(List<String> ids);
    boolean isStopAreaUsed(String stopAreaNetexId);
    int deleteEmptyStopPlaces();
    List<SearchAddressFeatures> findByNamePatternSearchAddressFeatures(String namePattern);
}
