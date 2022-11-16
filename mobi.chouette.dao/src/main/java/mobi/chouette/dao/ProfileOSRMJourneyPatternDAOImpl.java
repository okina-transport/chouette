package mobi.chouette.dao;

import mobi.chouette.model.OSRMProfile;
import mobi.chouette.model.ProfileOSRMJourneyPattern;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless (name="ProfileOSRMJourneyPatternDAO")
public class ProfileOSRMJourneyPatternDAOImpl extends GenericDAOImpl<ProfileOSRMJourneyPattern> implements ProfileOSRMJourneyPatternDAO {

    public ProfileOSRMJourneyPatternDAOImpl() {
        super(ProfileOSRMJourneyPattern.class);
    }

    @PersistenceContext(unitName = "referential")
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

    public ProfileOSRMJourneyPattern findByJourneyPatternId(Long journeyPatternId, OSRMProfile profile) {
        return em.createQuery("SELECT p from ProfileOSRMJourneyPattern p " +
                "WHERE p.journeyPattern.id = :journeyPatternId " +
                "AND p.profile = :profile ", ProfileOSRMJourneyPattern.class)
                .setParameter("journeyPatternId", journeyPatternId)
                .setParameter("profile", profile)
                .getSingleResult();
    }}
