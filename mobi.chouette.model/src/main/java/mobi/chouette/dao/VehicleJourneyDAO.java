package mobi.chouette.dao;

import mobi.chouette.model.VehicleJourney;

import java.util.List;

public interface VehicleJourneyDAO extends GenericDAO<VehicleJourney> {
	
	long updateAccessibilityId(Long accessibilityId, List<String> objectIds);

	long updateBrandingId(Long brandingId, List<String> objectIds);

	long updateDefaultAccessibility(Long defaultAccessibilityId);

}
