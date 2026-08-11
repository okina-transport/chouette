package mobi.chouette.exchange.netexprofile.importer;


import com.google.common.collect.Lists;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.BrandingDAO;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.VehicleJourneyDAO;
import mobi.chouette.model.Branding;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Log4j
@Stateless(name = BrandingRefCommand.COMMAND)
public class BrandingRefCommand implements Command, Constant {

    @EJB
    LineDAO lineDAO;

    @EJB
    VehicleJourneyDAO vehicleJourneyDAO;


    @EJB
    BrandingDAO brandingDAO;

    public static final String COMMAND = "BrandingRefCommand";

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean execute(Context context) throws Exception {
        LocalDateTime start = LocalDateTime.now();
        Map<String, Set<String>> brandingRefLineMap = (Map<String, Set<String>>) context.get(BRANDING_REF_LINE_MAP);
        Map<String, Set<String>> brandingRefServiceJourneyMap = (Map<String, Set<String>>) context.get(BRANDING_REF_SERVICE_JOURNEY_MAP);
        NetexprofileImportParameters parameters = (NetexprofileImportParameters) context.get(CONFIGURATION);
        String objectPrefix = parameters.getObjectIdPrefix();
        createBrandingRefs(brandingRefLineMap, objectPrefix);
        createBrandingRefs(brandingRefServiceJourneyMap, objectPrefix);

        for (Map.Entry<String, Set<String>> brandingEntry : brandingRefLineMap.entrySet()) {
            String brandingObjectId = objectPrefix + ":Branding:" + brandingEntry.getKey();
            Branding branding = brandingDAO.findByObjectId(brandingObjectId);
            updateLineBrandings(branding.getId(), brandingEntry.getValue());
        }

        for (Map.Entry<String, Set<String>> brandingEntry : brandingRefServiceJourneyMap.entrySet()) {
            String brandingObjectId = objectPrefix + ":Branding:" + brandingEntry.getKey();
            Branding branding = brandingDAO.findByObjectId(brandingObjectId);
            updateVehicleJourneyBrandings(branding.getId(), brandingEntry.getValue());
        }

        LocalDateTime end = LocalDateTime.now();
        Duration duration = Duration.between(start, end);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        log.info("BrandingRefCommand duration:" + " - " + hours + " hours, " + minutes + " minutes, " + seconds + " seconds");
        return SUCCESS;
    }

    private void updateLineBrandings(Long id, Set<String> objectIdSet) {
        lineDAO.updateBrandingId(id, objectIdSet);
        log.info("Branding - updated lines:" + objectIdSet.size());
    }

    private void updateVehicleJourneyBrandings(Long id, Set<String> objectIdSet) {
        for (List<String> batch : Lists.partition(new ArrayList<>(objectIdSet), 30000)) {
            long updatedlines = vehicleJourneyDAO.updateBrandingId(id, batch);
            log.info("Branding - updated vehicle journeys:" + updatedlines);
        }
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private void createBrandingRefs(Map<String, Set<String>> brandingRefMap, String objectIdPrefix) {
        for (String brandingRef : brandingRefMap.keySet()) {
            String objectId = objectIdPrefix + ":Branding:" + brandingRef;
            Branding existingBrand = brandingDAO.findByObjectId(objectId);
            if (existingBrand == null){
                Branding newBranding = new Branding();
                newBranding.setObjectId(objectId);
                newBranding.setName(brandingRef);
                brandingDAO.create(newBranding);
            }
        }
        brandingDAO.flush();
    }



    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = null;
            try {
                String name = "java:app/mobi.chouette.exchange.netexprofile/" + COMMAND;
                result = (Command) context.lookup(name);
            } catch (NamingException e) {
                // try another way on test context
                String name = "java:module/" + COMMAND;
                try {
                    result = (Command) context.lookup(name);
                } catch (NamingException e1) {
                    log.error(e);
                }
            }
            return result;
        }
    }


    static {
        CommandFactory.factories.put(BrandingRefCommand.class.getName(), new BrandingRefCommand.DefaultCommandFactory());
    }



}
