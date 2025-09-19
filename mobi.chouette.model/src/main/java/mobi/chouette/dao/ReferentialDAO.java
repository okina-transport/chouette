package mobi.chouette.dao;

import mobi.chouette.model.Referential;

import java.util.List;

public interface ReferentialDAO extends GenericDAO<Referential> {

    List<String> getReferentials();

    String getReferentialNameBySlug(String slug);

    void dropSchemaIfExists(String schema);

    void renameSchema(String from, String to);

    Boolean checkSchemaExists(String schema);

}
