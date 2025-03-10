package mobi.chouette.dao;

import mobi.chouette.model.admin.ExportTemplate;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless(name="ExportTemplateDAO")
public class ExportTemplateDAOImpl extends GenericDAOImpl<ExportTemplate> implements ExportTemplateDAO {

    public ExportTemplateDAOImpl() {
        super(ExportTemplate.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }
}
