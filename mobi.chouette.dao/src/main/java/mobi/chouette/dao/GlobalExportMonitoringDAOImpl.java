package mobi.chouette.dao;

import lombok.extern.log4j.Log4j;
import mobi.chouette.model.admin.GlobalExportMonitoring;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

@Stateless(name = "GlobalExportMonitoringDAO")
@Log4j
public class GlobalExportMonitoringDAOImpl extends GenericDAOImpl<GlobalExportMonitoring> implements GlobalExportMonitoringDAO {

    public GlobalExportMonitoringDAOImpl() {
        super(GlobalExportMonitoring.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<GlobalExportMonitoring> findAllByJobId(Long jobId) {
        List<GlobalExportMonitoring> globalExportMonitorings = new ArrayList<>();
        Query findAllByJobId = em.createNativeQuery("SELECT * FROM global_export_monitoring where job_id = :jobId", GlobalExportMonitoring.class)
                .setParameter("jobId", jobId);
        try {
            globalExportMonitorings = findAllByJobId.getResultList();
        } catch (Exception e) {
            log.error(e);
        }
        return globalExportMonitorings;
    }
}
