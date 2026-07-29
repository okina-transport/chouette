package mobi.chouette.dao;

import mobi.chouette.model.LineTranslation;

import java.util.Collection;

public interface LineTranslationDAO extends GenericDAO<LineTranslation> {

    Collection<LineTranslation> findAllNewTransaction();

}
