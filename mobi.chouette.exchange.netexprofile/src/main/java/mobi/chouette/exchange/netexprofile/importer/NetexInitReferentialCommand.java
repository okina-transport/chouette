package mobi.chouette.exchange.netexprofile.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.common.monitor.JamonUtils;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.importer.validation.AbstractNetexProfileValidator;
import mobi.chouette.exchange.netexprofile.importer.validation.NetexProfileValidator;
import mobi.chouette.exchange.netexprofile.jaxb.NetexXMLProcessingHelperFactory;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.exchange.validation.report.ValidationReporter;
import net.sf.saxon.s9api.XdmNode;
import org.rutebanken.netex.model.PublicationDeliveryStructure;

import javax.naming.InitialContext;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Log4j
public class NetexInitReferentialCommand implements Command, Constant {

	public static final String COMMAND = "NetexInitReferentialCommand";

	@Getter
	//@Setter
	private Path path;
	
	public void setPath(Path p) {
		this.path = p;
	}

	@Getter
	@Setter
	private boolean lineFile;

	@Override
	public boolean execute(Context context) throws Exception {
		boolean result = SUCCESS;
		Monitor monitor = MonitorFactory.start(COMMAND);
		NetexprofileImportParameters parameters = (NetexprofileImportParameters) context.get(CONFIGURATION);

		String fileName = path.getFileName().toString();
		
		ActionReporter reporter = ActionReporter.Factory.getInstance();
		ValidationReporter validationReporter = ValidationReporter.Factory.getInstance();
		validationReporter.addItemToValidationReport(context, AbstractNetexProfileValidator._1_NETEX_UNKNOWN_PROFILE, "E");

		File file = path.toFile();
		reporter.addFileReport(context, fileName, IO_TYPE.INPUT);
		context.put(FILE_NAME, fileName);

		try {
			Set<QName> elementsToSkip = new HashSet<>();
			if (!parameters.isParseSiteFrames()) {
				// Do not parse SiteFrames at all
				elementsToSkip.add(new QName(Constant.NETEX_NAMESPACE, "SiteFrame"));
			}

			NetexXMLProcessingHelperFactory importer = (NetexXMLProcessingHelperFactory) context.get(IMPORTER);

			if(parameters.isValidateAgainstProfile()) {
				XdmNode netexDom = importer.parseFileToXdmNode(file, elementsToSkip);
				context.put(NETEX_DATA_DOM, netexDom);
			}

			PublicationDeliveryStructure netexJava = importer.unmarshal(file,elementsToSkip);

			context.put(NETEX_DATA_JAVA, netexJava);

			List incomingLineList = (List) context.get(INCOMING_LINE_LIST);
			if (incomingLineList == null){
				context.put(INCOMING_LINE_LIST, new ArrayList());
			}


			if (lineFile) {
				context.put(NETEX_WITH_COMMON_DATA, Boolean.FALSE);
			} else {
				context.put(NETEX_WITH_COMMON_DATA, Boolean.TRUE);
			}

			Map<String, NetexProfileValidator> availableProfileValidators = (Map<String, NetexProfileValidator>) context.get(NETEX_PROFILE_VALIDATORS);

			String profileVersion = netexJava.getVersion();
			// TODO Validation propre à l'appli désactivée pour le moment (voir NetexSchemaValidationCommand pour la validation xsd)
//			if (!lineFile) {
//				profileVersion += "-common";
//			}

//			NetexProfileValidator profileValidator = availableProfileValidators.get(profileVersion);
//			if (profileValidator != null) {
//				profileValidator.initializeCheckPoints(context);
//				context.put(NETEX_PROFILE_VALIDATOR, profileValidator);
//				validationReporter.reportSuccess(context, AbstractNetexProfileValidator._1_NETEX_UNKNOWN_PROFILE);
//			} else {
//				log.error("Unsupported NeTEx profile in PublicationDelivery/@version: " + profileVersion);
//				// TODO fix reporting with lineNumber etc
//				validationReporter.addCheckPointReportError(context, AbstractNetexProfileValidator._1_NETEX_UNKNOWN_PROFILE, null, new DataLocation(fileName),
//						profileVersion);
//				result = ERROR;
//			}

		} catch (Exception e) {
			reporter.addFileErrorInReport(context, fileName, ActionReporter.FILE_ERROR_CODE.INTERNAL_ERROR, e.toString());
			log.error("Netex referential initialization failed ", e);
			throw e;
		} finally {
			JamonUtils.logMagenta(log, monitor);
		}
		return result;
	}

	public static class DefaultCommandFactory extends CommandFactory {
		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = new NetexInitReferentialCommand();
			return result;
		}
	}

	static {
		CommandFactory.factories.put(NetexInitReferentialCommand.class.getName(), new NetexInitReferentialCommand.DefaultCommandFactory());
	}

}
