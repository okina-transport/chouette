package mobi.chouette.exchange.metadata;

import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.generic.EscapeTool;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class TemplateFileWriter {

	public static final String LOGGER_NAME = "velocityiev";

	private static final Logger logger = Logger.getLogger(TemplateFileWriter.class);
	@Getter
	private VelocityEngine velocityEngine;
	// Prepare the model for velocity
	@Getter
	protected Map<String, Object> model = new HashMap<>();

	public TemplateFileWriter() {
		Logger.getLogger( LOGGER_NAME );
		velocityEngine = new VelocityEngine();
		velocityEngine.addProperty("resource.loader", "classpath");
		velocityEngine.addProperty("classpath.resource.loader.class",
				"org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
	}

	protected ZipEntry writeZipEntry(String entryName, String templateName, ZipOutputStream zipFile)
			throws IOException {
		// Prepare the model for velocity

		StringWriter output = new StringWriter();

		VelocityContext velocityContext = new VelocityContext(model);
		velocityContext.put("esc", new EscapeTool());

		velocityEngine.mergeTemplate(templateName, StandardCharsets.UTF_8.name(), velocityContext, output);

		// Add ZIP entry to zipFileput stream.
		ZipEntry entry = new ZipEntry(entryName);
		zipFile.putNextEntry(entry);

		zipFile.write(output.toString().getBytes(StandardCharsets.UTF_8));

		// Complete the entry
		zipFile.closeEntry();

		return entry;
	}

	protected File writePlainFile(File directory, String filename, String templateName) throws IOException {

		StringWriter output = new StringWriter();
		VelocityContext velocityContext = new VelocityContext(model);
		velocityContext.put("esc", new EscapeTool());

		velocityEngine.mergeTemplate(templateName, StandardCharsets.UTF_8.name(), velocityContext, output);

		File file = new File(directory, filename);
		FileUtils.write(file, output.toString(), StandardCharsets.UTF_8);

		logger.debug("File : " + filename + "created");

		return file;
	}

}
