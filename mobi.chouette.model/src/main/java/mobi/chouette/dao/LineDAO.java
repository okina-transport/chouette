package mobi.chouette.dao;

import mobi.chouette.core.CoreException;
import mobi.chouette.model.Line;

import java.util.List;
import java.util.Map;

public interface LineDAO extends GenericDAO<Line> {

    String updateStopareasForIdfmLineCommand(Long lineId) throws Exception;

    void mergeDuplicateJourneyPatternsOfLineAndAddSuffix(Long lineId, String lineName);

    List<Line> findByNetworkId(Long networkId);

    List<Line> findByNetworkIdNotDeleted(Long networkId);

    List<Line> findNotDeleted();

    List<String> findObjectIdLinesInFirstDataspace(List<Long> ids, String dataspace);

    String removeDeletedLines() throws CoreException;

    Line findByObjectIdAndInitialize(String objectId);

    Map<String, String> findColorLines();
}
