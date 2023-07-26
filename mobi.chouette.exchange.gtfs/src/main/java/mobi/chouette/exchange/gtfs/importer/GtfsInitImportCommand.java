package mobi.chouette.exchange.gtfs.importer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Optional;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.FileUtil;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.ProviderDAO;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.exchange.gtfs.model.importer.FactoryParameters;
import mobi.chouette.exchange.gtfs.model.importer.GtfsImporter;
import mobi.chouette.exchange.validation.ValidationData;
import mobi.chouette.model.Provider;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.util.Referential;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import org.apache.commons.lang3.StringUtils;

@Log4j
@Stateless(name = GtfsInitImportCommand.COMMAND)
public class GtfsInitImportCommand implements Command, Constant {

	public static final String COMMAND = "GtfsInitImportCommand";

	@EJB
	ProviderDAO providerDAO;

	@Override
	public boolean execute(Context context) throws Exception {
		boolean result = ERROR;

		Monitor monitor = MonitorFactory.start(COMMAND);

		try {
			JobData jobData = (JobData) context.get(JOB_DATA);
			context.put(REFERENTIAL, new Referential());
			// prepare importer
			GtfsImporter importer = (GtfsImporter) context.get(PARSER);
			GtfsImportParameters parameters = (GtfsImportParameters) context.get(CONFIGURATION);
			context.put(TIAMAT_ERROR_CODE_CONVERTER, new GtfsErrorCodeConverter());
			if (importer == null) {
				Path path = Paths.get(jobData.getPathName(), INPUT);
				if ("exporter".equals(jobData.getAction())){
					//After export, files are moved to tmp directory before validation to avoid .nfs files creation
					path = FileUtil.getTmpPath(path);
				}

				FactoryParameters factoryParameters = new FactoryParameters();
				factoryParameters.setSplitCharacter(parameters.getSplitCharacter());
				factoryParameters.setLinePrefixToRemove(parameters.getLinePrefixToRemove());
				factoryParameters.setCommercialPointIdPrefixToRemove(parameters.getCommercialPointIdPrefixToRemove());
				importer = new GtfsImporter(path.toString(),factoryParameters);
				context.put(PARSER, importer);
			}

			context.put(StopArea.IMPORT_MODE, parameters.getStopAreaImportMode());

			if (parameters.getReferencesType() == null || parameters.getReferencesType().isEmpty()) {
				parameters.setReferencesType("line");
			}
			context.put(VALIDATION_DATA, new ValidationData());
			context.put(FILE_TO_REFERENTIAL_STOP_ID_MAP, new HashMap<String, String>());

			String referential = parameters.getReferentialName();
			Optional<Provider> providerOpt = providerDAO.findBySchema(referential);
			String regExp = providerOpt.isPresent() && StringUtils.isNotEmpty(providerOpt.get().getRailUICregexp()) ? providerOpt.get().getRailUICregexp() : " ";
			context.put(RAIL_UIC_REGEXP, regExp);


			result = SUCCESS;

		} catch (Exception e) {
			log.error(e, e);
			throw e;
		} finally {
			log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
		}

		return result;
	}

	public void setProviderDAO(ProviderDAO providerDAO) {
		this.providerDAO = providerDAO;
	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = null;
			try {
				String name = "java:app/mobi.chouette.exchange.gtfs/" + COMMAND;
				result = (Command) context.lookup(name);
			} catch (NamingException e) {
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
		CommandFactory.factories.put(GtfsInitImportCommand.class.getName(), new GtfsInitImportCommand.DefaultCommandFactory());
	}

}
