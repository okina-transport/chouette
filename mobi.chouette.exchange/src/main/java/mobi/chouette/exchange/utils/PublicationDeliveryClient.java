package mobi.chouette.exchange.utils;

import mobi.chouette.exchange.importer.utils.TokenService;
import org.rutebanken.netex.model.ObjectFactory;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.netex.validation.NeTExValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class PublicationDeliveryClient {
    private static final Logger logger = LoggerFactory.getLogger(PublicationDeliveryClient.class);

    private final ObjectFactory objectFactory = new ObjectFactory();

    private final String publicationDeliveryUrl;

    private final JAXBContext jaxbContext;
    private final boolean validateAgainstSchema;

    private NeTExValidator neTExValidator = null;

    private final TokenService tokenService;

    /**
     * @param publicationDeliveryUrl Url to where to POST the publication delivery XML
     * @param validateAgainstSchema  If serialized XML should be validated against the NeTEx schema.
     * @throws JAXBException
     * @throws IOException
     * @throws SAXException
     */
    public PublicationDeliveryClient(String publicationDeliveryUrl, boolean validateAgainstSchema, TokenService tokenService) throws JAXBException, IOException, SAXException {
        this.publicationDeliveryUrl = publicationDeliveryUrl;
        this.jaxbContext = JAXBContext.newInstance(PublicationDeliveryStructure.class);
        this.validateAgainstSchema = validateAgainstSchema;
        this.tokenService = tokenService;
        if (validateAgainstSchema) {
            neTExValidator = new NeTExValidator();
        }
    }

    public PublicationDeliveryClient(String publicationDeliveryUrl, TokenService tokenService) throws JAXBException, IOException, SAXException {
        this(publicationDeliveryUrl, false, tokenService);
    }

    public PublicationDeliveryStructure sendPublicationDelivery(PublicationDeliveryStructure publicationDelivery) throws JAXBException, IOException, SAXException {

        Marshaller marshaller = jaxbContext.createMarshaller();
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        if (validateAgainstSchema) {
            marshaller.setSchema(neTExValidator.getSchema());
        }

        URL url = new URL(publicationDeliveryUrl);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-type", "application/xml");
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Bearer " + tokenService.getToken());

            logger.info("About to start marshalling publication delivery to output stream {}", publicationDelivery);
            marshaller.marshal(objectFactory.createPublicationDelivery(publicationDelivery), connection.getOutputStream());
            logger.info("Done marshalling publication delivery to output stream. (Schema validation was set to {}) {}", validateAgainstSchema, publicationDelivery);

            int responseCode = connection.getResponseCode();
            logger.info("Got response code {} after posting publication delivery to URL : {}", responseCode, url);

            BufferedReader br = null;
            if (100 <= connection.getResponseCode() && connection.getResponseCode() <= 399) {
                InputStream inputStream = connection.getInputStream();
                JAXBElement<PublicationDeliveryStructure> element = (JAXBElement<PublicationDeliveryStructure>) unmarshaller.unmarshal(inputStream);
                return element.getValue();
            } else {
                br = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                String errorMsg = br.readLine();
                logger.error(errorMsg);
                throw new IOException(errorMsg);
            }


        } catch (Exception e) {
            throw new IOException("Error posting XML to " + publicationDeliveryUrl, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }


    }
}
