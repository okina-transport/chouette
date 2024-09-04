package mobi.chouette.dao;

import mobi.chouette.model.VehicleJourneyAtStop;

import java.util.List;

public interface VehicleJourneyAtStopDAO extends GenericDAO<VehicleJourneyAtStop> {

    int deleteByVehicleJourneyObjectIds(List<String> vjObjectIds);

    void copy(String data);

}
