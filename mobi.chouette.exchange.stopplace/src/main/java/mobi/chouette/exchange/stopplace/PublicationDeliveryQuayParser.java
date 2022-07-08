package mobi.chouette.exchange.stopplace;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.model.util.Referential;
import org.rutebanken.netex.model.Common_VersionFrameStructure;
import org.rutebanken.netex.model.EntityStructure;
import org.rutebanken.netex.model.GeneralFrame;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.netex.model.Quay;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;


import static javax.xml.bind.JAXBContext.newInstance;


@Log4j
public class PublicationDeliveryQuayParser {

    private InputStream inputStream;
    private Instant now;


    @Getter
    private StopAreaUpdateContext updateContext;

    public PublicationDeliveryQuayParser(InputStream inputStream) {
        this.inputStream = inputStream;
        now = Instant.now();
        updateContext = new StopAreaUpdateContext();
        parseQuays();
    }



    public void parseQuays() {
        try {
            PublicationDeliveryStructure incomingPublicationDelivery = unmarshal(inputStream);

            convertToStopAreas(incomingPublicationDelivery);
        } catch (Exception e) {
            throw new RuntimeException("Failed to unmarshall delivery publication structure: " + e.getMessage(), e);
        }
    }

    private void convertToStopAreas(PublicationDeliveryStructure incomingPublicationDelivery) throws Exception {

        Context context = new Context();
        Referential referential = new Referential();
        context.put(Constant.REFERENTIAL, referential);

        for (JAXBElement<? extends Common_VersionFrameStructure> frameStructureElmt : incomingPublicationDelivery.getDataObjects().getCompositeFrameOrCommonFrame()) {
            Common_VersionFrameStructure frameStructure = frameStructureElmt.getValue();

            if (frameStructure instanceof GeneralFrame) {
                GeneralFrame generalFrame = (GeneralFrame) frameStructure;

                List<JAXBElement<? extends EntityStructure>> members = generalFrame.getMembers().getGeneralFrameMemberOrDataManagedObjectOrEntity_Entity();

                for (JAXBElement<? extends EntityStructure> member : members) {

                    if (member.getValue() instanceof Quay){
                        Quay currentQuay = (Quay)member.getValue();
                        updateContext.getInactiveStopAreaIds().add(currentQuay.getId());
                    }
                }
            }
        }
    }


    private PublicationDeliveryStructure unmarshal(InputStream inputStream) throws JAXBException {
        JAXBContext jaxbContext = newInstance(PublicationDeliveryStructure.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        JAXBElement<PublicationDeliveryStructure> jaxbElement = jaxbUnmarshaller.unmarshal(new StreamSource(inputStream), PublicationDeliveryStructure.class);
        PublicationDeliveryStructure publicationDeliveryStructure = jaxbElement.getValue();

        return publicationDeliveryStructure;

    }

}
