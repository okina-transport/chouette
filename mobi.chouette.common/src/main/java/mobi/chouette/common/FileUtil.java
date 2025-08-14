package mobi.chouette.common;

import lombok.extern.log4j.Log4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@Log4j
public class FileUtil {

    public static List<Path> listFiles(Path path, String glob) throws IOException {
        final PathMatcher matcher = path.getFileSystem().getPathMatcher("glob:" + glob);

        final DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {

            @Override
            public boolean accept(Path entry) throws IOException {
                return Files.isDirectory(entry) || matcher.matches(entry.getFileName());
            }
        };
        List<Path> result = new ArrayList<Path>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, filter)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    result.addAll(listFiles(entry, glob));
                    return result;
                }
                result.add(entry);
            }
        }
        return result;
    }

    public static List<Path> listFiles(Path path, String glob, String exclusionGlob) throws IOException {
        final PathMatcher matcher = path.getFileSystem().getPathMatcher("glob:" + glob);

        final PathMatcher excludeMatcher = path.getFileSystem().getPathMatcher("glob:" + exclusionGlob);

        final DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {

            @Override
            public boolean accept(Path entry) throws IOException {
                return Files.isDirectory(entry)
                        || (matcher.matches(entry.getFileName()) && !excludeMatcher.matches(entry.getFileName()));
            }
        };
        List<Path> result = new ArrayList<Path>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, filter)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    result.addAll(listFiles(entry, glob, exclusionGlob));
                    return result;
                }
                result.add(entry);
            }
        }
        return result;
    }

    public static List<Path> listZip(String directoryPath) throws IOException {
        List<Path> zipFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(directoryPath), "*.zip")) {
            for (Path entry : stream) {
                zipFiles.add(entry);
            }
        } catch (IOException | DirectoryIteratorException e) {
            log.error("Erreur lors de la lecture du dossier : " + e.getMessage());
        }
        return zipFiles;
    }

    public static void unzipAllFiles(String directoryPath) throws IOException, ArchiveException {
        List<Path> zipFiles = listZip(directoryPath);

        for (Path zipFile : zipFiles) {
            String destinationDir = directoryPath + "/" + zipFile.getFileName().toString().replace(".zip", "");
            Files.createDirectories(Paths.get(destinationDir));
            uncompress(zipFile.toString(), destinationDir);
        }
    }

    public static Set<String> getFiles(String directoryPath, String fileName) {
        Set<String> txtFiles = new HashSet<>();
        Path startPath = Paths.get(directoryPath);

        try {
            Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(fileName)) {
                        txtFiles.add(file.toAbsolutePath().toString());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Erreur lors du parcours des fichiers : " + e.getMessage());
        }

        return txtFiles;
    }

    public static Set<String> listFilesOfType(String directoryPath, String extension, boolean withAbsolutePath) {
        Set<String> foundFiles = new HashSet<>();
        Path startPath = Paths.get(directoryPath);

        try {
            Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(extension)) {
                        if (withAbsolutePath) {
                            foundFiles.add(file.toAbsolutePath().toString());
                        }else{
                            foundFiles.add(file.getFileName().toString());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
           log.error("Erreur lors du parcours des fichiers : " + e.getMessage());
        }

        return foundFiles;
    }


    public static void uncompress(String filename, String path) throws IOException, ArchiveException {
        ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream(new BufferedInputStream(
                new FileInputStream(new File(filename))));
        ArchiveEntry entry = null;
        while ((entry = in.getNextEntry()) != null) {

            String name = FilenameUtils.getName(entry.getName());
            File file = new File(path, name);
            if (entry.isDirectory()) {
                // if (!file.exists()) {
                // file.mkdirs();
                // }
            } else {
                if (file.exists()) {
                    file.delete();
                }
                file.createNewFile();
                OutputStream out = new FileOutputStream(file);
                IOUtils.copy(in, out);
                IOUtils.closeQuietly(out);
            }
        }
        IOUtils.closeQuietly(in);

    }

    public static void deleteFilesByType(String directoryPath, String extension) {
        File directory = new File(directoryPath);
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Not a directory : " + directoryPath);
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFilesByType(file.getAbsolutePath(), extension);
                } else if (file.getName().endsWith(extension)) {
                    if (file.delete()) {
                        log.info("Supprimé: " + file.getAbsolutePath());
                    } else {
                        log.error("Échec de la suppression: " + file.getAbsolutePath());
                    }
                }
            }
        }
    }

    public static void compress(String path, String filename, String type) throws IOException {

        File directoryToZip = new File(path);
        List<File> fileList = new ArrayList<File>();
        getAllFiles(directoryToZip, fileList);
        writeZipFile(directoryToZip, filename, fileList, type);

        // Path dir = Paths.get(path);
        // DirectoryStream<Path> stream = Files.newDirectoryStream(dir);
        //
        // ZipArchiveOutputStream zout = new
        // ZipArchiveOutputStream(Files.newOutputStream(Paths.get(filename)));
        // for (Path file : stream) {
        //
        // String name = file.getName(file.getNameCount() - 1).toString();
        // long size = Files.size(file);
        //
        // ZipArchiveEntry entry = new ZipArchiveEntry(name);
        // entry.setSize(size);
        // InputStream in = Files.newInputStream(file);
        //
        // zout.putArchiveEntry(entry);
        // IOUtils.copy(in, zout);
        // zout.closeArchiveEntry();
        // IOUtils.closeQuietly(in);
        //
        // }
        // IOUtils.closeQuietly(zout);

    }

    private static void getAllFiles(File dir, List<File> fileList) {

        File[] files = dir.listFiles();
        for (File file : files) {
            fileList.add(file);
            if (file.isDirectory()) {
                getAllFiles(file, fileList);
            }
        }

    }

    private static void writeZipFile(File path, String zipName, List<File> fileList, String type) {
        if (!type.equals("gtfs")) {
            // NETETX
            writeNetexZipFile(path, zipName, fileList);
        } else {
            // GTFS
            writeGTFSZipFile(path, zipName, fileList);
        }
    }

    private static void writeNetexZipFile(File path, String zipName, List<File> fileList) {

        try {
            FileOutputStream fos = new FileOutputStream(zipName);
            ZipOutputStream zos = new ZipOutputStream(fos);

            for (File file : fileList) {
                if (!file.isDirectory()) { // we only zip files, not directories
                    addNetexFileToZip(path, file, zos);
                }
            }
            zos.flush();
            fos.flush();
            zos.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addNetexFileToZip(File directoryToZip, File file, ZipOutputStream zos) throws IOException {

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);

            // we want the zipEntry's path to be a relative path that is relative
            // to the directory being zipped, so chop off the rest of the path
            String zipFilePath = file.getCanonicalPath().substring(directoryToZip.getCanonicalPath().length() + 1);

            ZipEntry zipEntry = new ZipEntry(zipFilePath);
            zos.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } finally {
            zos.flush();
            zos.closeEntry();
            if (fis != null) fis.close();
        }
    }

    private static void writeGTFSZipFile(File path, String zipName, List<File> fileList) {

        try (FileOutputStream fos = new FileOutputStream(zipName);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (File file : fileList) {
                if (!file.isDirectory()) { // we only zip files, not directories
                    addGTFSFileToZip(path, file, zos, zipName);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createZipFromFiles(Set<String> filePaths, String outputZipPath) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(outputZipPath))) {
            for (String filePath : filePaths) {
                File fileToZip = new File(filePath);
                if (!fileToZip.exists() || !fileToZip.isFile()) {
                    log.error("Skipping invalid file: " + filePath);
                    continue;
                }

                try (FileInputStream fis = new FileInputStream(fileToZip)) {
                    ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
                    zipOut.putNextEntry(zipEntry);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zipOut.write(buffer, 0, length);
                    }
                    zipOut.closeEntry();
                }
            }
        }
    }

    private static void addGTFSFileToZip(File directoryToZip, File file, ZipOutputStream zos, String zipName) throws
            IOException {

        FileInputStream fis = new FileInputStream(file);

        zipName = zipName.substring(zipName.lastIndexOf("/"));
        String folderNameInZip = zipName.replace(".zip", "");

        // we want the zipEntry's path to be a relative path that is relative
        // to the directory being zipped, so chop off the rest of the path
        String zipFilePath = file.getCanonicalPath().substring(directoryToZip.getCanonicalPath().length() + 1);

        ZipEntry zipEntry;
        zipEntry = new ZipEntry(zipFilePath);

        zos.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }

        zos.closeEntry();
        fis.close();
    }

    public static void mergeFilesInPath(String path, String filename) {
        FileUtil.mergeFilesInPath(path, filename, null, true);
    }


    public static void mergeFilesInPath(String path, String filename, String oneOccurence, boolean deleteOldFiles) {
        boolean oneOccurenceIsFound = false;
        File directoryToMerge = new File(path);
        List<File> fileList = new ArrayList<File>();
        getAllFiles(directoryToMerge, fileList);
        fileList.sort(Comparator.comparing(File::getName));
        List<File> filesToDel = new ArrayList<File>();

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(filename);
            for (File file : fileList) {
                if (!StringUtils.isEmpty(oneOccurence) && file.getName().contains(oneOccurence)) {
                    if (oneOccurenceIsFound) continue;
                    oneOccurenceIsFound = true;
                }

                if (!file.isDirectory()) { // we only merge files, not directories
                    FileInputStream fileInputStream = new FileInputStream(file);
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fileInputStream.read(buffer)) > 0) {
                        fileOutputStream.write(buffer, 0, length);
                    }
                    fileInputStream.close();
                }
                if (deleteOldFiles) filesToDel.add(file);
            }
            fileOutputStream.close();

            for (File file : filesToDel) {
                FileUtils.forceDelete(file);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static Path getTmpPath(Path originalPath) {
        String tmpFilePathStr = originalPath.toString().replace("/opt/jboss/data/referentials", "/tmp/data");
        Path tmpPath = Paths.get(tmpFilePathStr);
        File tmpFile = tmpPath.toFile();
        tmpFile.getParentFile().mkdirs();
        return tmpPath;
    }

    public static Path getTechnicalPath() {
        String tmpFilePathStr = "/opt/jboss/data/referentials/mobiiti_technique/lines";
        Path tmpPath = Paths.get(tmpFilePathStr);
        File tmpFile = tmpPath.toFile();
        tmpFile.getParentFile().mkdirs();
        tmpFile.mkdir();
        return tmpPath;
    }

    public static boolean renameFile(String filePath, String newFileName) throws IOException {
        Path source = Paths.get(filePath);
        Path target = source.resolveSibling(newFileName);
        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

}
