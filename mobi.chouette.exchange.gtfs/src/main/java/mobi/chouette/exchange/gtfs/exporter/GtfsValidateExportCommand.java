package mobi.chouette.exchange.gtfs.exporter;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.FileUtil;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.exchange.gtfs.importer.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;

import javax.naming.InitialContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@Log4j
public class GtfsValidateExportCommand implements Command, Constant {

	public static final String COMMAND = "GtfsValidateExportCommand";

	@Override
	public boolean execute(Context context) throws Exception {
		boolean result = ERROR;

		Monitor monitor = MonitorFactory.start(COMMAND);

		try {
			// create specific context
			Context validateContext = new Context();
			validateContext.putAll(context);
            ExportableData exportableData = (ExportableData) context.get(EXPORTABLE_DATA);
			// build parameter
			GtfsImportParameters parameters = new GtfsImportParameters();
			GtfsExportParameters configuration = (GtfsExportParameters) context.get(CONFIGURATION);
			parameters.setOrganisationName(configuration.getOrganisationName());
			parameters.setUserName(configuration.getUserName());
			parameters.setName(configuration.getName());
			parameters.setNoSave(true);
			parameters.setReferentialName(configuration.getReferentialName());
			parameters.setReferencesType(configuration.getReferencesType());
			parameters.setObjectIdPrefix(configuration.getObjectIdPrefix());
            parameters.setImportFareFiles(CollectionUtils.isNotEmpty(exportableData.getFareRules()) || CollectionUtils.isNotEmpty(exportableData.getFareAttributes()));
			validateContext.put(CONFIGURATION, parameters);
			validateContext.put(REPORT, context.get(REPORT));
			// rename output folder to input folder
			JobData jobData = (JobData) context.get(JOB_DATA);
			String path = jobData.getPathName();
			File output = new File(path, OUTPUT);
			File input = new File(path, INPUT);

			Path inputPath = FileUtil.getTmpPath(input.toPath());
			FileUtils.copyDirectory(output, inputPath.toFile());
			// run gtfs validation preparation
			InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
			try {
				Command c = CommandFactory.create(initialContext, GtfsValidationRulesCommand.class.getName());
				c.execute(validateContext);
				// run gtfs init command
				c = CommandFactory.create(initialContext, GtfsInitImportCommand.class.getName());
				c.execute(validateContext);
				// run gtfs validate command
				c = CommandFactory.create(initialContext, GtfsValidationCommand.class.getName());
				c.execute(validateContext);

				FileUtils.deleteDirectory(inputPath.toFile());

			} catch (Exception ex) {
				log.error("problem in validation" + ex);
				throw ex;
			} finally {
				// terminate validation
				Command c = CommandFactory.create(initialContext, GtfsDisposeImportCommand.class.getName());
				c.execute(validateContext);
			}
			// save report in folder
			context.put(VALIDATION_REPORT, validateContext.get(VALIDATION_REPORT));
			result = SUCCESS;

		} catch (Exception e) {
			log.error(e, e);
			throw e;
		} finally {
			log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
		}

		return result;
	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			return new GtfsValidateExportCommand();
		}
	}

	static {
		CommandFactory.factories.put(GtfsValidateExportCommand.class.getName(), new DefaultCommandFactory());
	}

}
