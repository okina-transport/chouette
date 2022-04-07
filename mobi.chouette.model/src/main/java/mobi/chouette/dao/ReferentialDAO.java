package mobi.chouette.dao;

import java.util.List;

import mobi.chouette.core.CoreException;
import mobi.chouette.model.Referential;

public interface ReferentialDAO extends GenericDAO<Referential> {

    List<String> getReferentials();

    String getReferentialNameBySlug(String slug);

    void dropSchema(String schema);

    void renameSchemaForSimulation(String schema);

}
