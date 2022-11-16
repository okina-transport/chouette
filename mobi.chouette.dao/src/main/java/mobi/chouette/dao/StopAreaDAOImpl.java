package mobi.chouette.dao;

import lombok.extern.log4j.Log4j;
import mobi.chouette.core.CoreException;
import mobi.chouette.core.CoreExceptionCode;
import mobi.chouette.model.SearchAddressFeatures;
import mobi.chouette.model.SearchAddressGeometry;
import mobi.chouette.model.SearchAddressProperties;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.ChouetteAreaEnum;
import org.hibernate.Hibernate;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;

@Stateless
@Log4j
public class StopAreaDAOImpl extends GenericDAOImpl<StopArea> implements StopAreaDAO {

    public StopAreaDAOImpl() {
        super(StopArea.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    @Override
    public List<String> getBoardingPositionObjectIds() {
        return em.createQuery("select s.objectId from StopArea s where s.areaType = :areaType").setParameter("areaType", ChouetteAreaEnum.BoardingPosition).getResultList();
    }

    /**
     * Fusionner 2 stopAreas
     *
     * @param from
     * @param into
     * @throws CoreException
     */
    @Override
    public void mergeStopArea30m(Long from, Long into) throws CoreException {
        try {
            em.createNativeQuery(
                    "SELECT 1 FROM merge_duplicate_stop_area_30m( :safrom, :sainto)")
                    .setParameter("safrom", from)
                    .setParameter("sainto", into)
                    .getSingleResult();
        } catch (Exception e) {
            throw new CoreException(CoreExceptionCode.DELETE_IMPOSSIBLE,"Error while trying to merge point:" + from + " to point:" + into);
        }
    }

    public List<StopArea> findByOriginalId(String originalId) {
        return em.createQuery("SELECT s " +
                "                   FROM StopArea s " +
                "                  WHERE s.originalStopId = :originalStopId", StopArea.class)
                .setParameter("originalStopId", originalId)
                .getResultList();
    }

    @Override
    public List<StopArea> findByOriginalIds(List<String> originalIds) {
        return em.createQuery("SELECT s " +
                "                   FROM StopArea s " +
                "                  WHERE s.originalStopId IN (:originalStopIds)", StopArea.class)
                .setParameter("originalStopIds", originalIds)
                .getResultList();
    }

    /**
     * Check id a stop area is in use     *
     * @param stopAreaNetexId
     *      the netex Id of the stop area to check (e.g:MOBIITI:Quay:5486)
     * @return
     *      true : the stop area is used
     *      false : the stop area is not used
     */
    @Override
    public boolean isStopAreaUsed(String stopAreaNetexId){

        List<Object> results1 = em.createNativeQuery("SELECT 1 " +
                "                   FROM stop_areas s INNER JOIN stop_points sp ON s.id = sp.stop_area_id " +
                "                  WHERE s.objectid = (:objectId)")

                .setParameter("objectId", stopAreaNetexId)
                .getResultList();

        if (!results1.isEmpty()){
            log.error("Can't delete stop area : " + stopAreaNetexId + " because it is used by a stop_point");
            return true;
        }


        List<Object> results2 = em.createNativeQuery("SELECT 1 " +
                "                   FROM stop_areas s INNER JOIN scheduled_stop_points ssp ON s.objectid = ssp.stop_area_objectid_key " +
                "                  WHERE s.objectid = (:objectId)")

                .setParameter("objectId", stopAreaNetexId)
                .getResultList();

        if (!results2.isEmpty()){
            log.error("Can't delete stop area : " + stopAreaNetexId + " because it is used by a scheduled_stop_point");
            return true;
        }

        List<Object> results3 = em.createNativeQuery("SELECT 1 " +
                "                   FROM stop_areas s INNER JOIN access_links al ON s.id = al.stop_area_id " +
                "                  WHERE s.objectid = (:objectId)")

                .setParameter("objectId", stopAreaNetexId)
                .getResultList();

        if (!results3.isEmpty()){
            log.error("Can't delete stop area : " + stopAreaNetexId + " because it is used by an access_link");
            return true;
        }

        List<Object> results4 = em.createNativeQuery("SELECT 1 " +
                "                   FROM stop_areas s INNER JOIN access_points ap ON s.id = ap.stop_area_id " +
                "                  WHERE s.objectid = (:objectId)")

                .setParameter("objectId", stopAreaNetexId)
                .getResultList();

        if (!results4.isEmpty()){
            log.error("Can't delete stop area : " + stopAreaNetexId + " because it is used by an access_point");
            return true;
        }

        List<Object> results5 = em.createNativeQuery("SELECT 1 " +
                "                   FROM stop_areas s INNER JOIN disruption_stop_area dsa ON s.id = dsa.stop_area_id " +
                "                  WHERE s.objectid = (:objectId)")

                .setParameter("objectId", stopAreaNetexId)
                .getResultList();

        if (!results5.isEmpty()){
            log.error("Can't delete stop area : " + stopAreaNetexId + " because it is used by a disruption");
            return true;
        }

        return false;

    }

    /**
     * Removes commercial stop points that have NO children
     * @return
     */
    @Override
    public int deleteEmptyStopPlaces(){
        return em.createNativeQuery("DELETE FROM stop_areas s WHERE s.area_type = 'CommercialStopPoint' AND NOT EXISTS " +
                "           (SELECT 1 FROM stop_areas s2 WHERE s2.parent_id = s.id)").executeUpdate();

    }

    @Override
    public List<SearchAddressFeatures> findByNamePatternSearchAddressFeatures(String namePattern) {
        List<SearchAddressFeatures> searchAddressFeatures = new ArrayList<>(0);
        findByNamePatternWithLazyDepsAllAreaType(namePattern).forEach(stopArea -> {
            SearchAddressProperties searchAddressProperties = new SearchAddressProperties();
            searchAddressProperties.setId(String.valueOf(stopArea.getId()));
            searchAddressProperties.setCity(stopArea.getCityName());
            searchAddressProperties.setCitycode(stopArea.getCityCode());
            searchAddressProperties.setX(String.valueOf(stopArea.getLatitude()));
            searchAddressProperties.setY(String.valueOf(stopArea.getLongitude()));
            searchAddressProperties.setImportance("1");
            searchAddressProperties.setLabel(stopArea.getParent() != null ? String.format("[POINT D'ARRÊT] - %s", stopArea.getParent().getName()) : String.format("[POINT D'ARRÊT] - %s", stopArea.getName()));
            searchAddressProperties.setName(searchAddressProperties.getLabel());
            SearchAddressFeatures searchAddressFeaturesElement = new SearchAddressFeatures();
            SearchAddressGeometry searchAddressGeometry = new SearchAddressGeometry();
            searchAddressGeometry.setCoordinates(new String[]{String.valueOf(stopArea.getLongitude()), String.valueOf(stopArea.getLatitude())});
            searchAddressFeaturesElement.setGeometry(searchAddressGeometry);
            searchAddressFeaturesElement.setProperties(searchAddressProperties);

            boolean shouldAdd = searchAddressFeatures.stream().noneMatch(searchAddressFeature -> {
                SearchAddressProperties currentAdressProperties = searchAddressFeature.getProperties();
                // Check if any of the used properties are set
                return (currentAdressProperties.getX() != null
                        && currentAdressProperties.getY() != null
                        && currentAdressProperties.getCity() != null
                        && currentAdressProperties.getLabel() != null)
                        // Check the equality
                        && (currentAdressProperties.getX().equals(searchAddressProperties.getX())
                        && currentAdressProperties.getY().equals(searchAddressProperties.getY())
                        && currentAdressProperties.getCity().equals(searchAddressProperties.getCity())
                        && currentAdressProperties.getLabel().equals(searchAddressProperties.getLabel()));
            });

            if (shouldAdd) {
                searchAddressFeatures.add(searchAddressFeaturesElement);
            }
        });
        return searchAddressFeatures;
    }

    public List<StopArea> findByNamePatternWithLazyDepsAllAreaType(String namePattern) {
        List<StopArea> stopAreasList = em.createQuery("SELECT s FROM StopArea s"
                + " WHERE upper(s.name) LIKE upper(:namePattern)"
                + " ORDER BY s.name", StopArea.class)
                .setParameter("namePattern", "%" + namePattern + "%")
                .getResultList();

        for (StopArea stopArea : stopAreasList) {
            Hibernate.initialize(stopArea.getParent());
        }

        return stopAreasList;
    }

}
