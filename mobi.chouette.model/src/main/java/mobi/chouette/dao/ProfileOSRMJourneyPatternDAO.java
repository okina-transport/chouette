package mobi.chouette.dao;

import mobi.chouette.model.JourneyPattern;
import mobi.chouette.model.OSRMProfile;
import mobi.chouette.model.ProfileOSRMJourneyPattern;

public interface ProfileOSRMJourneyPatternDAO extends GenericDAO<ProfileOSRMJourneyPattern> {

    ProfileOSRMJourneyPattern findByJourneyPatternId(Long journeyPatternId, OSRMProfile profile);
}
