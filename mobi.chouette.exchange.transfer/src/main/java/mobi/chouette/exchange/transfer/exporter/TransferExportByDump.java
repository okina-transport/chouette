package mobi.chouette.exchange.transfer.exporter;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.core.ChouetteRuntimeException;
import mobi.chouette.dao.FeedInfoDAO;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.dao.ReferentialDAO;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.exchange.transfer.Constant;
import mobi.chouette.model.FeedInfo;
import mobi.chouette.model.Line;
import mobi.chouette.model.StopArea;
import mobi.chouette.persistence.hibernate.ContextHolder;
import org.jboss.ejb3.annotation.TransactionTimeout;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j
@Stateless(name = TransferExportByDump.COMMAND)
public class TransferExportByDump implements Command, Constant {

	public static final String COMMAND = "TransferExportByDump";

	@EJB
	private ReferentialDAO referentialDAO;

	private String currentSchema;
	private String superSpaceSchema;
	private String connectionUrl ="postgresql://" + PROPERTY_OKINA_DATASOURCE_USER + ":" + PROPERTY_OKINA_DATASOURCE_PASSWORD + "@" + PROPERTY_OKINA_DATASOURCE_HOST + ":" + PROPERTY_OKINA_DATASOURCE_PORT + "/" + PROPERTY_OKINA_DATASOURCE_NAME;


	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@TransactionTimeout(value = 8, unit = TimeUnit.HOURS)
	public boolean execute(Context context) throws Exception {

		TransferExportParameters parameters = (TransferExportParameters) context.get(CONFIGURATION);

		currentSchema = ContextHolder.getContext();
		superSpaceSchema = parameters.getDestReferentialName();

		log.info("Starting transferring data by dump method");
		dumpWorkSchema();
		log.info("work schema dump completed successfully");
		renameWorkSchema();
		log.info("starting work schema restoration");
		restoreWorkSchema();
		log.info("Transferring by dump completed");
		return true;
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
				"-d", connectionUrl,
				"-f", "work_schema_dump.sql"
		);

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
				"-f", "work_schema_dump.sql"
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
