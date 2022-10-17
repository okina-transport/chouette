package mobi.chouette.exchange.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.common.monitor.JamonUtils;
import mobi.chouette.dao.*;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.time.LocalDateTime;

@Log4j
@Stateless(name = CleanRepositoryCommand.COMMAND)
public class CleanRepositoryCommand implements Command {

	public static final String COMMAND = "CleanRepositoryCommand";

	@EJB
	private CompanyDAO companyDAO;

	@EJB
	private GroupOfLineDAO groupOfLineDAO;

	@EJB
	private JourneyFrequencyDAO journeyFrequencyDAO;

	@EJB
	private JourneyPatternDAO journeyPatternDAO;

	@EJB
	private LineDAO lineDAO;

	@EJB
	private NetworkDAO networkDAO;

	@EJB
	private RouteDAO routeDAO;

	@EJB
	private RouteSectionDAO routeSectionDAO;

	@EJB
	private StopPointDAO stopPointDAO;

	@EJB
	private ScheduledStopPointDAO scheduledStopPointDAO;

	@EJB
	private TimetableDAO timetableDAO;

	@EJB
	private TimebandDAO timebandDAO;

	@EJB
	private VehicleJourneyDAO vehicleJourneyDAO;

	@EJB
	private VehicleJourneyAtStopDAO vehicleJourneyAtStopDAO;

	@EJB
	private DeadRunDAO deadRunDAO;

	@EJB
	private DeadRunAtStopDAO deadRunAtStopDAO;

	@EJB
	private DatedServiceJourneyDAO datedServiceJourneyDAO;

	@EJB
	private BlockDAO blockDAO;

	@EJB
	private DestinationDisplayDAO destinationDisplayDAO;

	@EJB
	private FootnoteDAO footnoteDAO;

	@EJB
	private FootnoteAlternativeTextDAO footNoteAlternativeTextDAO;

	@EJB
	private BrandingDAO brandingDAO;

	@EJB
	private InterchangeDAO interchangeDAO;

	@EJB
	private RoutePointDAO routePointDAO;

	@EJB
	private ContactStructureDAO contactStructureDAO;

	@EJB
	private BookingArrangementDAO bookingArrangementDAO;

	@EJB
	private FlexibleServicePropertiesDAO flexibleServicePropertiesDAO;

	@EJB
	private ReferentialLastUpdateDAO referentialLastUpdateDAO;

	@EJB
	private StopAreaDAO stopAreaDAO;

	@EJB
	AccessLinkDAO accessLinkDao;

	@EJB
	AccessPointDAO accessPointDAO;

	@EJB
	ConnectionLinkDAO connectionLinkDAO;

	@EJB
	CategoriesForLinesDAO categoriesForLinesDAO;

	@EJB
	FeedInfoDAO feedInfoDAO;

	@Override
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public boolean execute(Context context) throws Exception {
		// TODO : Check merge entur
		boolean result = ERROR;
		Monitor monitor = MonitorFactory.start(COMMAND);

		try {
			journeyFrequencyDAO.truncate();
			journeyPatternDAO.truncate();
			routeDAO.truncate();
			routeSectionDAO.truncate();
			footNoteAlternativeTextDAO.truncate();
			footnoteDAO.truncate();
			brandingDAO.truncate();
			stopPointDAO.truncate();
			scheduledStopPointDAO.truncate();
			timetableDAO.truncate();
			timebandDAO.truncate();
			vehicleJourneyDAO.truncate();
			vehicleJourneyAtStopDAO.truncate();
			deadRunDAO.truncate();
			deadRunAtStopDAO.truncate();
			datedServiceJourneyDAO.truncate();
			blockDAO.truncate();
			destinationDisplayDAO.truncate();
			interchangeDAO.truncate();
			routePointDAO.truncate();
			flexibleServicePropertiesDAO.truncate();
			bookingArrangementDAO.truncate();
			contactStructureDAO.truncate();
			referentialLastUpdateDAO.setLastUpdateTimestamp(LocalDateTime.now());
			//useless in MOSAIC
			accessLinkDao.truncate();
			accessPointDAO.truncate();
			connectionLinkDAO.truncate();

			// si pas import et ( transfert ou clean admin )
			if(context == null || !context.containsKey(CLEAR_FOR_IMPORT) || context.get(CLEAR_FOR_IMPORT) != Boolean.TRUE) {
				// si clean pour transfert
				if(context != null && context.containsKey(CLEAR_TABLE_CATEGORIES_FOR_LINES) && context.get(CLEAR_TABLE_CATEGORIES_FOR_LINES) == Boolean.TRUE) {
					categoriesForLinesDAO.truncate();
					feedInfoDAO.truncate();
				}
				// lignes
				contactStructureDAO.truncate();
				groupOfLineDAO.truncate();
				footnoteDAO.truncate();
				lineDAO.truncate();
				bookingArrangementDAO.truncate();
				networkDAO.truncate();
				companyDAO.truncate();

				// arrêts
				stopAreaDAO.truncate();
			} else {
				// si import on conserve lignes et arrêts
				context.put(CLEAR_FOR_IMPORT, false);
			}
			result = SUCCESS;
		} catch (Exception e) {
			log.error(e);
			throw e;
		}
		JamonUtils.logMagenta(log, monitor);
		return result;
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
		CommandFactory.factories.put(CleanRepositoryCommand.class.getName(), new DefaultCommandFactory());
	}
}
