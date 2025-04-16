package mobi.chouette.exchange.importer.utils;

import javax.ejb.Stateless;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Stateless
public class FileUtils {

    public void buildFolderIfNotExist(Path folder) throws IOException {
        if (!Files.exists(folder) && !folder.toFile().mkdirs()) {
            throw new IOException("Error creating directory " + folder.toAbsolutePath());
        }
    }
}
