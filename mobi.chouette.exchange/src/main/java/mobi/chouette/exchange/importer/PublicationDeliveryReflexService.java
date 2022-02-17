package mobi.chouette.exchange.importer;

import lombok.extern.log4j.Log4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

@Log4j
public class PublicationDeliveryReflexService {

    public static byte[] getAll(String urlTarget) throws MalformedURLException, IOException {
        URL url = new URL(urlTarget);
        InputStream input = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String icarToken = System.getProperty("iev.icar.token");
        log.info("Token ICAR utilisé : " + icarToken);

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozaic");
            connection.setRequestProperty("Authorization","Bearer "+icarToken);
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            input = connection.getInputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = input.read(buffer)) > -1 ) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
        } catch (Exception var13) {
            throw new IOException("Error posting XML to " + url.toString(), var13);
        }
        return baos.toByteArray();
    }

}
