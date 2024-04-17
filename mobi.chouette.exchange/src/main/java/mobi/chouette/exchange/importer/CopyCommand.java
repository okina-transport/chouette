package mobi.chouette.exchange.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.*;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.VehicleJourneyDAO;
import mobi.chouette.persistence.hibernate.ContextHolder;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

@Log4j
@Stateless(name = CopyCommand.COMMAND)
public class CopyCommand implements Command {

	public static final String COMMAND = "CopyCommand";

	@EJB 
	private VehicleJourneyDAO vehicleJourneyDAO;

	@EJB 
	private ContenerChecker checker;

	@Resource(lookup = "java:comp/DefaultManagedExecutorService")
	ManagedExecutorService executor;

	@SuppressWarnings("unchecked")
	@Override
	public boolean execute(Context context) throws Exception {

		JobData jobData = (JobData) context.get(JOB_DATA);



		String sMaxCopy = System.getProperty(checker.getContext()+ PropertyNames.MAX_COPY_BY_JOB, "5");
		int maxCopy = Integer.parseInt(sMaxCopy);

		boolean result = ERROR;

		try {

			Boolean optimized = (Boolean) context.get(OPTIMIZED);
			if (optimized) {
				List<Future<Void>> futures = (List<Future<Void>>) context.get(COPY_IN_PROGRESS);
				if (futures == null) {
					futures = new ArrayList<>();
					context.put(COPY_IN_PROGRESS, futures);
				}
				while (futures.size() >= maxCopy)
				{
					for (Iterator<Future<Void>> iterator = futures.iterator(); iterator.hasNext();) {
						Future<Void> future = iterator.next();
						if (future.isDone()) iterator.remove();
					}
					if (futures.size() >= maxCopy)
					{
						for (Iterator<Future<Void>> iterator = futures.iterator(); iterator.hasNext();) {
							Future<Void> future = iterator.next();
							if (future.isDone()) iterator.remove();
							else
							{
								log.info("too many copy in progress, waiting ...");
								future.get();
								break;
							}
						}						
					}
				}
				CommandCallable callable = new CommandCallable();
				callable.bufferVjas = (String) context.remove(BUFFER_VJAS);

				callable.schema = ContextHolder.getContext();
				Future<Void> future = executor.submit(callable);
				futures.add(future);
			}


			result = SUCCESS;
		} catch (Exception e) {
			log.error(e);
			throw e;
		}

		return result;
	}

	private class CommandCallable implements Callable<Void> {
		private String bufferVjas;

		private String schema;

		@Override
		@TransactionAttribute(TransactionAttributeType.REQUIRED)
		public Void call() {
			LocalDateTime start = LocalDateTime.now();
			Monitor monitor = MonitorFactory.start(COMMAND);
			ContextHolder.setContext(schema);
			vehicleJourneyDAO.copy(bufferVjas);
			log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
			ContextHolder.setContext(null);
			log.info("VehicleJourneyAtStop copy ended successfully");
			LocalDateTime end = LocalDateTime.now();
			Duration duration = Duration.between(start, end);
			long hours = duration.toHours();
			long minutes = duration.toMinutes() % 60;
			long seconds = duration.getSeconds() % 60;
			log.info("CopyCommand duration:" + " - " + hours + " hours, " + minutes + " minutes, " + seconds + " seconds");
			return null;
		}

	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {

			Command result = null;
			try {
				String name = "java:app/mobi.chouette.exchange/" + COMMAND;
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
		CommandFactory.factories.put(CopyCommand.class.getName(), new DefaultCommandFactory());
	}
}
