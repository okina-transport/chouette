package mobi.chouette.dao;

import mobi.chouette.model.admin.GlobalExportMonitoring;

import java.util.List;

public interface GlobalExportMonitoringDAO extends GenericDAO<GlobalExportMonitoring> {

    List<GlobalExportMonitoring> findAllByJobId(Long jobId);
}
