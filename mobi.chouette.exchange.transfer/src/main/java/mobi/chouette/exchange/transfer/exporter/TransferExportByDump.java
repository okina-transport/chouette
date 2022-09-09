package mobi.chouette.exchange.transfer.exporter;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.core.ChouetteRuntimeException;
import mobi.chouette.dao.ReferentialDAO;
import mobi.chouette.exchange.transfer.Constant;
import mobi.chouette.persistence.hibernate.ContextHolder;
import org.jboss.ejb3.annotation.TransactionTimeout;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

@Log4j
@Stateless(name = TransferExportByDump.COMMAND)
public class TransferExportByDump implements Command, Constant {

	public static final String COMMAND = "TransferExportByDump";

	@EJB
	private ReferentialDAO referentialDAO;

	private String currentSchema;
	private String superSpaceSchema;
	private String dumpFileName;
	private String connectionUrl ="postgresql://" + PROPERTY_OKINA_DATASOURCE_USER + ":" + PROPERTY_OKINA_DATASOURCE_PASSWORD + "@" + PROPERTY_OKINA_DATASOURCE_HOST + ":" + PROPERTY_OKINA_DATASOURCE_PORT + "/" + PROPERTY_OKINA_DATASOURCE_NAME;


	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@TransactionTimeout(value = 8, unit = TimeUnit.HOURS)
	public boolean execute(Context context) throws Exception {

		TransferExportParameters parameters = (TransferExportParameters) context.get(CONFIGURATION);

		currentSchema = ContextHolder.getContext();
		superSpaceSchema = parameters.getDestReferentialName();
		dumpFileName = "work_schema_dump_" + currentSchema + ".sql";


		log.info("Starting transferring data by dump method");
		log.info("connectionURL:" + connectionUrl);
		dumpWorkSchema();
		log.info("work schema dump completed successfully");
		renameWorkSchema();
		log.info("starting work schema restoration");
		restoreWorkSchema();
		log.info("Transferring by dump completed");
		deleteWorkDump();
		log.info("Deleting dump file completed");
		return true;
	}

	private void deleteWorkDump() throws IOException, InterruptedException {

		log.info("Deleting work dump file : " + dumpFileName);
		ProcessBuilder deleteCommand = new ProcessBuilder(
				"rm", dumpFileName
		);

		deleteCommand.redirectOutput(ProcessBuilder.Redirect.INHERIT);
		deleteCommand.redirectErrorStream(true);

		try {
			Process delete = deleteCommand.start();
			BufferedReader r = new BufferedReader(
					new InputStreamReader(delete.getErrorStream()));

			String line = r.readLine();
			while (line != null) {
				log.error(line);
				line = r.readLine();
			}
			r.close();
			delete.waitFor();
			log.info("Deletion return:" +  delete.exitValue());

		} catch (IOException | InterruptedException e) {
			log.error("Error while deleting work dump file ");
			throw e;
		}
	}


	private void renameWorkSchema(){
		log.info("Deleting superspace schema:" + superSpaceSchema);
		referentialDAO.dropSchema(superSpaceSchema);
		String currentSchema = ContextHolder.getContext();
		referentialDAO.renameSchemaForSimulation(currentSchema);
	}

	private void restoreWorkSchema() throws IOException, InterruptedException {

		ProcessBuilder restoreCommand = new ProcessBuilder(
				"psql",
				"--single-transaction",
				"-d", connectionUrl,
				"-f", dumpFileName
		);

		restoreCommand.redirectOutput(ProcessBuilder.Redirect.INHERIT);
		restoreCommand.redirectErrorStream(true);

		try {
			Process restore = restoreCommand.start();
			BufferedReader r = new BufferedReader(
					new InputStreamReader(restore.getErrorStream()));

			String line = r.readLine();
			while (line != null) {
				log.error(line);
				line = r.readLine();
			}
			r.close();
			restore.waitFor();
			log.info("Restoration return:" +  restore.exitValue());

		} catch (IOException | InterruptedException e) {
			log.error("Error while restoring work schema ");
			throw e;
		}
	}


	private void dumpWorkSchema() throws IOException, InterruptedException {
		ProcessBuilder dumpCommand = new ProcessBuilder(
				"pg_dump",
				connectionUrl,
				"-n", currentSchema,
				"-F", "p",
				"-f", dumpFileName
		);

		try {
			Process dump = dumpCommand.start();
			BufferedReader r = new BufferedReader(
					new InputStreamReader(dump.getErrorStream()));

			String line = r.readLine();
			while (line != null) {
				System.err.println(line);
				line = r.readLine();
			}
			r.close();
			dump.waitFor();

			if (dump.exitValue() != 0) {
				throw new ChouetteRuntimeException("Impossible de réaliser le dump de la filiale: " + currentSchema) {
					@Override
					public String getPrefix() {
						return null;
					}

					@Override
					public String getCode() {
						return null;
					}
				};
			}


		} catch (IOException | InterruptedException e) {
			log.error("Error while dumping work schema : " + currentSchema);
			throw e;
		}
	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = null;
			try {
				String name = "java:app/mobi.chouette.exchange.transfer/" + COMMAND;
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
		CommandFactory.factories.put(TransferExportByDump.class.getName(), new DefaultCommandFactory());
	}

}