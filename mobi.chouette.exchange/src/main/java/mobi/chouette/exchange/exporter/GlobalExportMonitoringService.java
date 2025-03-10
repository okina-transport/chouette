package mobi.chouette.exchange.exporter;

import lombok.extern.slf4j.Slf4j;
import mobi.chouette.dao.ExportTemplateDAO;
import mobi.chouette.dao.GlobalExportMonitoringDAO;
import mobi.chouette.model.admin.ExportType;
import mobi.chouette.model.admin.GlobalExportMonitoring;
import mobi.chouette.model.admin.JobStatus;
import mobi.chouette.model.type.GlobalExportAction;
import mobi.chouette.persistence.hibernate.ContextHolder;

import javax.ejb.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Stateless
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Slf4j
public class GlobalExportMonitoringService {

    public static final String BEAN_NAME = "GlobalExportMonitoringService";

    private static final String ADMIN_CONTEXT = "technique";

    @EJB
    private GlobalExportMonitoringDAO globalExportMonitoringDAO;

    @EJB
    private ExportTemplateDAO exportTemplateDAO;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void saveMonitoringAdminContext(GlobalExportMonitoring globalExportMonitoring) {
        ContextHolder.setContext(ADMIN_CONTEXT);
        save(globalExportMonitoring);
        globalExportMonitoringDAO.flush();
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Map<String, GlobalExportMonitoring> initGlobalMonitoring(String[] referentials, Long exportTemplateId, Long jobId, ExportType exportType) {
        ContextHolder.setContext(ADMIN_CONTEXT);
        GlobalExportMonitoring.GlobalExportMonitoringBuilder builder =
                initMonitoringBuilder(exportTemplateId, jobId, exportType);
        Map<String, GlobalExportMonitoring> result = new HashMap<>(referentials.length);
        GlobalExportMonitoring globalExportMonitoring;
        for (String referential : referentials) {
            globalExportMonitoring = builder.build();
            globalExportMonitoring.setReferential(referential);
            save(globalExportMonitoring);
        }
        saveMergeActionMonitoring(builder);
        List<GlobalExportMonitoring> createdExportMonitorings = globalExportMonitoringDAO.findAllByJobId(jobId);
        if (createdExportMonitorings != null) {
            for (GlobalExportMonitoring createdExportMonitoring : createdExportMonitorings) {
                result.put(createdExportMonitoring.getReferential(), createdExportMonitoring);
            }
        }
        return result;
    }

    private void saveMergeActionMonitoring(GlobalExportMonitoring.GlobalExportMonitoringBuilder builder) {
        GlobalExportMonitoring mergeActionMonitoring = builder.build();
        mergeActionMonitoring.setCommand(GlobalExportAction.MERGE.name());
        mergeActionMonitoring.setReferential(ADMIN_CONTEXT);
        save(mergeActionMonitoring);
    }

    private void save(GlobalExportMonitoring globalExportMonitoring) {
        if (globalExportMonitoring.getId() == null) {
            log.debug("Creating new global export monitoring");
            globalExportMonitoringDAO.create(globalExportMonitoring);
        } else {
            log.debug("Updating global export monitoring {}", globalExportMonitoring.getId());
            globalExportMonitoringDAO.update(globalExportMonitoring);
        }
    }

    private GlobalExportMonitoring.GlobalExportMonitoringBuilder initMonitoringBuilder(Long exportTemplateId, Long jobId, ExportType exportType) {
        return GlobalExportMonitoring.builder()
                .status(JobStatus.PENDING)
                .jobId(jobId)
                .type(exportType)
                .exportTemplate(exportTemplateDAO.find(exportTemplateId))
                .command(GlobalExportAction.PROCESS_REFERENTIAL.name());
    }


}
