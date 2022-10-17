package mobi.chouette.exchange.netexprofile.validator;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.common.monitor.JamonUtils;
import mobi.chouette.exchange.netexprofile.importer.NetexprofileImporterCommand;
import mobi.chouette.exchange.parameters.AbstractImportParameter;

import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;

@Log4j
@Stateless(name = NetexprofileValidatorCommand.COMMAND)
public class NetexprofileValidatorCommand extends NetexprofileImporterCommand {

	public static final String COMMAND = "NetexprofileValidatorCommand";

	@Override
	public boolean execute(Context context) throws Exception {
		Monitor monitor = MonitorFactory.start(COMMAND);

		try {
			// set default parameters
			AbstractImportParameter parameters = (AbstractImportParameter) context.get(CONFIGURATION);
			parameters.setNoSave(true);
			parameters.setCleanMode("purge");

			return super.execute(context);
		} finally {
			JamonUtils.logYellow(log, monitor);
		}
	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = null;
			try {
				String name = "java:app/mobi.chouette.exchange.validator/" + COMMAND;
				result = (Command) context.lookup(name);
			} catch (NamingException e) {
				// try another way on test context
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
		CommandFactory.factories.put(NetexprofileValidatorCommand.class.getName(), new DefaultCommandFactory());
	}
}
