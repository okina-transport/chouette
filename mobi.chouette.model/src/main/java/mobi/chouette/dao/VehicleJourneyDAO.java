package mobi.chouette.dao;

import mobi.chouette.model.FirstOrLastJourneyInfo;
import mobi.chouette.model.IneoVJMapping;
import mobi.chouette.model.TheoreticalStopMonitoringInfo;
import mobi.chouette.model.VehicleJourney;
import org.joda.time.LocalDate;

import java.util.List;

public interface VehicleJourneyDAO extends GenericDAO<VehicleJourney> {
	
	long updateAccessibilityId(Long accessibilityId, List<String> objectIds);

	long updateBrandingId(Long brandingId, List<String> objectIds);

	long updateDefaultAccessibility(Long defaultAccessibilityId);

	List<IneoVJMapping> getIneoVJMappingData(LocalDate date);

	List<FirstOrLastJourneyInfo> getFirstOrLastJourneyData(LocalDate date);

	List<TheoreticalStopMonitoringInfo> getAllTheoreticalStopMonitoringInfoByDate(LocalDate date);

}
