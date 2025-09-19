package mobi.chouette.exchange.transfer.exporter;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.ReferentialDAO;
import mobi.chouette.exchange.transfer.Constant;
import mobi.chouette.persistence.hibernate.ContextHolder;
import mobi.chouette.service.PostgresDumpService;
import org.jboss.ejb3.annotation.TransactionTimeout;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Log4j
@Stateless(name = TransferExportByDump.COMMAND)
public class TransferExportByDump implements Command, Constant {

	public static final String COMMAND = "TransferExportByDump";

	static {
		CommandFactory.factories.put(TransferExportByDump.class.getName(), new DefaultCommandFactory());
	}

	@EJB
	private ReferentialDAO referentialDAO;
	@EJB
	private PostgresDumpService postgresDumpService;

	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	@TransactionTimeout(value = 8, unit = TimeUnit.HOURS)
	public boolean execute(Context context) throws Exception {
		TransferExportParameters parameters = (TransferExportParameters) context.get(CONFIGURATION);
        String currentSchema = ContextHolder.getContext();
        String superSpaceSchema = parameters.getDestReferentialName();
        String dumpFileName = "work_schema_dump_" + currentSchema + ".sql";
		log.info("Starting transferring data by dump method");
		postgresDumpService.exportSQLDumpForSchema(dumpFileName, currentSchema);
		log.info("work schema dump completed successfully");
		referentialDAO.dropSchemaIfExists(superSpaceSchema);
		referentialDAO.renameSchema(currentSchema, superSpaceSchema);
		log.info("starting work schema restoration");
		postgresDumpService.importSQLDump(dumpFileName);
		log.info("Transferring by dump completed");
		postgresDumpService.deleteDumpFile(dumpFileName);
		log.info("Deleting dump file completed");
		return true;
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

}
