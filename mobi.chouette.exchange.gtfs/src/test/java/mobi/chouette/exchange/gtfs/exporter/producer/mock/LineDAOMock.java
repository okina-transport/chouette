package mobi.chouette.exchange.gtfs.exporter.producer.mock;

import mobi.chouette.core.CoreException;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.model.Line;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LineDAOMock implements LineDAO {
    @Override
    public String updateStopareasForIdfmLineCommand(Long lineId) throws Exception {
        return "";
    }

    @Override
    public void mergeDuplicateJourneyPatternsOfLineAndAddSuffix(Long lineId, String lineName) {

    }

    @Override
    public List<Line> findByNetworkId(Long networkId) {
        return Collections.emptyList();
    }

    @Override
    public List<Line> findNotDeleted() {
        return Collections.emptyList();
    }

    @Override
    public List<String> findObjectIdLinesInFirstDataspace(List<Long> ids, String dataspace) {
        return Collections.emptyList();
    }

    @Override
    public String removeDeletedLines() throws CoreException {
        return "";
    }

    @Override
    public Line findByObjectIdAndInitialize(String objectId) {
        return null;
    }

    @Override
    public Map<String, String> findColorLines() {
        return Collections.emptyMap();
    }

    @Override
    public Line find(Object id) {
        return null;
    }

    @Override
    public Line findByObjectId(String id) {
        return null;
    }

    @Override
    public List<Line> findByObjectId(Collection<String> objectIds) {
        return Collections.emptyList();
    }

    @Override
    public List<Line> findAll() {
        return Collections.emptyList();
    }

    @Override
    public List<Line> findAll(Collection<Long> ids) {
        return Collections.emptyList();
    }

    @Override
    public List<Line> find(String hql, List<Object> values) {
        return Collections.emptyList();
    }

    @Override
    public int deleteAll() {
        return 0;
    }

    @Override
    public int truncate() {
        return 0;
    }

    @Override
    public void evictAll() {

    }

    @Override
    public void flush() {

    }

    @Override
    public void clear() {

    }

    @Override
    public void detach(Collection<?> list) {

    }

    @Override
    public void detach(Line entity) {

    }

    @Override
    public void delete(Line entity) {

    }

    @Override
    public Line update(Line entity) {
        return null;
    }

    @Override
    public void create(Line entity) {

    }
}
