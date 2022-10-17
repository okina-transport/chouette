package mobi.chouette.exchange.neptune.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.common.monitor.JamonUtils;
import mobi.chouette.exchange.CommandCancelledException;
import mobi.chouette.exchange.ProcessingCommands;
import mobi.chouette.exchange.ProcessingCommandsFactory;
import mobi.chouette.exchange.ProgressionCommand;
import mobi.chouette.exchange.importer.AbstractImporterCommand;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ActionReporter.ERROR_CODE;
import mobi.chouette.exchange.report.ReportConstant;

import javax.naming.InitialContext;
import java.io.IOException;
import java.util.ArrayList;

/**
 * execute use in context : 
 * <ul>
 * <li>INITIAL_CONTEXT</li>
 * <li>REPORT</li>
 * <li>CONFIGURATION</li>
 * </ul>
 * 
 * @author michel
 *
 */
@Log4j
public class NeptuneImporterCommand extends AbstractImporterCommand implements Command, Constant, ReportConstant {

	public static final String COMMAND = "NeptuneImporterCommand";

	@Override
	public boolean execute(Context context) throws Exception {
		boolean result = ERROR;
		Monitor monitor = MonitorFactory.start(COMMAND);

		InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
		context.put(INCOMING_LINE_LIST, new ArrayList());

		ActionReporter reporter = ActionReporter.Factory.getInstance();
		
		// initialize reporting and progression
		ProgressionCommand progression = (ProgressionCommand) CommandFactory.create(initialContext,
				ProgressionCommand.class.getName());

        try {
		// read parameters
		Object configuration = context.get(CONFIGURATION);
		if (!(configuration instanceof NeptuneImportParameters)) {
			// fatal wrong parameters

			log.error("invalid parameters for neptune import " + configuration.getClass().getName());
			reporter.setActionError(context, ERROR_CODE.INVALID_PARAMETERS,"invalid parameters for neptune import " + configuration.getClass().getName());
			return ERROR;
		}


		NeptuneImportParameters parameters = (NeptuneImportParameters) configuration;


		String closeOldCalendarsPropStr = System.getProperty("iev.close.old.calendars");
		if (closeOldCalendarsPropStr != null ){
			context.put(CLOSE_OLD_CALENDARS, Boolean.parseBoolean(closeOldCalendarsPropStr));
		}

		context.put(KEEP_STOP_GEOLOCALISATION, Boolean.valueOf(parameters.isKeepStopGeolocalisation()));
		context.put(KEEP_STOP_NAMES, Boolean.valueOf(parameters.isKeepStopNames()));
		
		ProcessingCommands commands = ProcessingCommandsFactory.create(NeptuneImporterProcessingCommands.class.getName());
		result = process(context, commands, progression, true, Mode.line);
		

		} catch (CommandCancelledException e) {
			reporter.setActionError(context, ERROR_CODE.INTERNAL_ERROR, "Command cancelled");
			log.error(e.getMessage());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			reporter.setActionError(context, ERROR_CODE.INTERNAL_ERROR,"Fatal :" + e);

		} finally {
			progression.dispose(context);
			JamonUtils.logYellow(log, monitor);
		}

		return result;
	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = new NeptuneImporterCommand();
			return result;
		}
	}

	static {
		CommandFactory.factories.put(NeptuneImporterCommand.class.getName(), new DefaultCommandFactory());
	}
}
