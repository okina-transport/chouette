package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Log4j
public class PublicationDeliveryReflexService {

    public static byte[] getAll(String urlTarget) throws IOException {
        URL url = new URL(urlTarget);
        InputStream input;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String icarToken = System.getProperty("iev.icar.token");
        log.info("Token ICAR utilisé : " + icarToken);

        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestProperty("Authorization", "Bearer " + icarToken);
            connection.setRequestMethod("GET");
            input = connection.getInputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = input.read(buffer)) > -1 ) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
        } catch (Exception exception) {
            log.error("Error calling ICAR " + url.toString(), exception);
        }
        return baos.toByteArray();
    }

}
