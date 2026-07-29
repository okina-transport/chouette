package mobi.chouette.dao;

import mobi.chouette.model.StopAreaTranslation;

import java.util.Collection;

public interface StopAreaTranslationDAO extends GenericDAO<StopAreaTranslation> {

    Collection<StopAreaTranslation> findAllNewTransaction();

}
