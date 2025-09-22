package mobi.chouette.dao;

import mobi.chouette.model.Storage;

import java.util.Optional;

public interface StorageDAO extends GenericDAO<Storage> {

    Optional<Storage> findByMaxStoredAt();

    Optional<Storage> findByMaxStoredAtAndRestoredAtNotNull();

}
