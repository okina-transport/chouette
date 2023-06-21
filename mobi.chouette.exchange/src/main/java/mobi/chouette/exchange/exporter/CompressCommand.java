package mobi.chouette.exchange.exporter;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import javax.naming.InitialContext;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.FileUtil;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.common.file.FileStoreFactory;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import org.apache.commons.io.FileUtils;

@Log4j
public class CompressCommand implements Command, Constant {

	public static final String COMMAND = "CompressCommand";

	@Override
	public boolean execute(Context context) throws Exception {

		boolean result = ERROR;

		Monitor monitor = MonitorFactory.start(COMMAND);

		try {
			JobData jobData = (JobData) context.get(JOB_DATA);
			String path = jobData.getPathName();
			String file = jobData.getOutputFilename();
			Path target = Paths.get(path, OUTPUT);

			Path tmpFilename= Paths.get(target.toString(),file);
			File tmpFile= tmpFilename.toFile();
			if (tmpFile.exists()) tmpFile.delete();

			FileUtil.compress(target.toString(), tmpFile.toString(), jobData.getType());

			// Store file in permanent storage
			Path filename = Paths.get(path, file);
			FileStoreFactory.getFileStore().writeFile(filename, FileUtils.openInputStream(tmpFile));
			changeDirectoryPermissions(path, "rwxrwxrwx");
			tmpFile.delete();

			result = SUCCESS;
			try {
				FileUtils.deleteDirectory(target.toFile());
			} catch (Exception e) {
				log.warn("cannot purge output directory " + e.getMessage());
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
		}

		return result;
	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = new CompressCommand();
			return result;
		}
	}

	public static void changeDirectoryPermissions(String directoryPath, String permissions) throws IOException {
		Set<PosixFilePermission> posixFilePermissions = PosixFilePermissions.fromString(permissions);
		Path path = Paths.get(directoryPath);
//		Files.setPosixFilePermissions(path, posixFilePermissions);

		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.setPosixFilePermissions(file, posixFilePermissions);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				Files.setPosixFilePermissions(dir, posixFilePermissions);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	static {
		CommandFactory.factories.put(CompressCommand.class.getName(), new DefaultCommandFactory());
	}
}
