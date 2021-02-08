/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */

package mobi.chouette.exchange.concerto.exporter;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.model.Line;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;

/**
 *
 */
@Log4j
@Stateless(name = DaoConcertoLineProducerCommand.COMMAND)
public class DaoConcertoLineProducerCommand implements Command, Constant
{
	public static final String COMMAND = "DaoConcertoLineProducerCommand";

	@EJB
	private LineDAO lineDAO;

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
	public boolean execute(Context context) {
		boolean result = ERROR;
		Monitor monitor = MonitorFactory.start(COMMAND);

		try {

			Long lineId = (Long) context.get(LINE_ID);
			Line line = lineDAO.find(lineId);
			InitialContext initialContext = (InitialContext) context.get(INITIAL_CONTEXT);
			Command export = CommandFactory.create(initialContext, ConcertoLineProducerCommand.class.getName());
			
			context.put(LINE, line);
			result = export.execute(context);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
		}

		return result;
	}



	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = null;
			try {
				String name = "java:app/mobi.chouette.exchange.concerto/"
						+ COMMAND;
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
		CommandFactory.factories.put(DaoConcertoLineProducerCommand.class.getName(),
				new DefaultCommandFactory());
	}


}
