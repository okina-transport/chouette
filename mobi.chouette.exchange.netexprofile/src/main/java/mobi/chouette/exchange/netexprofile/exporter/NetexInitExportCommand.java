package mobi.chouette.exchange.netexprofile.exporter;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.common.monitor.JamonUtils;
import mobi.chouette.dao.CodespaceDAO;
import mobi.chouette.dao.ProviderDAO;
import mobi.chouette.dao.ReferentialLastUpdateDAO;
import mobi.chouette.exchange.metadata.Metadata;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.jaxb.NetexXMLProcessingHelperFactory;
import mobi.chouette.exchange.netexprofile.util.NetexReferential;
import mobi.chouette.model.Codespace;
import mobi.chouette.model.Provider;
import mobi.chouette.model.util.Referential;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Log4j
@Stateless(name = NetexInitExportCommand.COMMAND)
public class NetexInitExportCommand implements Command, Constant {

	public static final String COMMAND = "NetexInitExportCommand";

	@Resource
	private SessionContext daoContext;

	@EJB
	private CodespaceDAO codespaceDAO;

	@EJB
	private ProviderDAO providerDAO;

	@EJB
	private ReferentialLastUpdateDAO referentialLastUpdateDAO;

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public boolean execute(Context context) throws Exception {
		boolean result = ERROR;

		Monitor monitor = MonitorFactory.start(COMMAND);

		try {
			JobData jobData = (JobData) context.get(JOB_DATA);

			String referential = jobData.getReferential();
			Boolean isSimulationExport = referential.startsWith("simulation_");
            log.info("NetexInitExportCommand.execute : ref => " + referential);

            String idSite = "";
            if (!isSimulationExport) {
				Optional<Provider> provider = providerDAO.findBySchema(referential);
				idSite = provider.orElseThrow(() -> new RuntimeException("Aucun provider trouvé pour " + referential)).getCodeIdfm();
				log.info("NetexInitExportCommand.execute : " + referential + " " + idSite);
			} else {
            	log.info("NetexInitExportCommand.execute : " + referential);
			}

			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			Date currentDate = new Date();

			NetexprofileExportParameters configuration = (NetexprofileExportParameters) context.get(CONFIGURATION);
			String exportedFileName = configuration.getExportedFileName();
			if (exportedFileName != null && !"".equals(exportedFileName)){
				jobData.setOutputFilename(exportedFileName);
			}else{
				if (isSimulationExport) {
					String prefix = referential.replace("simulation", "SIMULATION");
					jobData.setOutputFilename(prefix + "_" + sdf.format(currentDate) + "Z.zip");

				} else {
					jobData.setOutputFilename("OFFRE_" + idSite + "_" + sdf.format(currentDate) + "Z.zip");
				}

			}

			context.put(REFERENTIAL, new Referential());
			context.put(NETEX_REFERENTIAL, new NetexReferential());

			NetexprofileExportParameters parameters = (NetexprofileExportParameters) context.get(Constant.CONFIGURATION);

			if (parameters.isAddMetadata()) {
				Metadata metadata = new Metadata();
				metadata.setDate(LocalDateTime.now());
				metadata.setFormat("application/xml");
				metadata.setTitle("Export NeTEx ");
				try {
					metadata.setRelation(new URL("http://www.normes-donnees-tc.org/format-dechange/donnees-theoriques/netex/"));
				} catch (MalformedURLException e1) {
					log.error("problem with http://www.normes-donnees-tc.org/format-dechange/donnees-theoriques/netex/ url", e1);
				}

				context.put(METADATA, metadata);
			}

			Path path = Paths.get(jobData.getPathName(), OUTPUT);
			if (!Files.exists(path)) {
				Files.createDirectories(path);
			}

			List<Codespace> referentialCodespaces = codespaceDAO.findAll();
			if (referentialCodespaces.isEmpty()) {
				log.error("no valid codespaces present for referential");
				return ERROR;
			}

			Set<Codespace> validCodespaces = new HashSet<>(referentialCodespaces);
			context.put(NETEX_VALID_CODESPACES, validCodespaces);

			NetexXMLProcessingHelperFactory netexXMLFactory = new NetexXMLProcessingHelperFactory();
			context.put(MARSHALLER, netexXMLFactory.createFragmentMarshaller());

			java.time.LocalDateTime lastUpdate = referentialLastUpdateDAO.getLastUpdateTimestamp();
			context.put(REFERENTIAL_LAST_UPDATE_TIMESTAMP, lastUpdate);

			daoContext.setRollbackOnly();
			codespaceDAO.clear();

			result = SUCCESS;
		} catch (Exception e) {
			log.error(e, e);
			throw e;
		} finally {
			JamonUtils.logMagenta(log, monitor);
		}

		return result;
	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = null;
			try {
				String name = "java:app/mobi.chouette.exchange.netexprofile/" + COMMAND;
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
		CommandFactory.factories.put(NetexInitExportCommand.class.getName(), new NetexInitExportCommand.DefaultCommandFactory());
	}

}
