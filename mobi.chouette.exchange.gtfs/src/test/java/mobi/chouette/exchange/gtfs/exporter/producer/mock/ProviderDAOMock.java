package mobi.chouette.exchange.gtfs.exporter.producer.mock;

import mobi.chouette.dao.ProviderDAO;
import mobi.chouette.model.Provider;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class ProviderDAOMock implements ProviderDAO {
    @Override
    public Provider find(Object id) {
        return null;
    }

    @Override
    public Provider findByObjectId(String id) {
        return null;
    }

    @Override
    public List<Provider> findByObjectId(Collection<String> objectIds) {
        return null;
    }

    @Override
    public List<Provider> findAll() {
        return null;
    }

    @Override
    public List<Provider> findAll(Collection<Long> ids) {
        return null;
    }

    @Override
    public List<Provider> find(String hql, List<Object> values) {
        return null;
    }

    @Override
    public void create(Provider entity) {

    }

    @Override
    public Provider update(Provider entity) {
        return null;
    }

    @Override
    public void delete(Provider entity) {

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
    public void detach(Provider entity) {

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
    public Optional<Provider> findBySchema(String name) {
        return Optional.empty();
    }

    @Override
    public List<Provider> getAllProviders() {
        return null;
    }

    @Override
    public List<String> getAllWorkingSchemas() {
        return null;
    }
}
