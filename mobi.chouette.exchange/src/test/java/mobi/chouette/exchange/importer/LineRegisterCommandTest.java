package mobi.chouette.exchange.importer;

import mobi.chouette.common.Constant;
import mobi.chouette.common.Context;
import mobi.chouette.model.*;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.naming.InitialContext;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNull;

public class LineRegisterCommandTest implements Constant {
	
	
	private LineRegisterCommand lineRegister = null;
	
	@Test (groups = { "write" }, description = "write command")
	public void testLineRegisterWrite() throws Exception 
	{
		InitialContext initialContext = new InitialContext();
		Context context = new Context();
		context.put(INITIAL_CONTEXT, initialContext);
		
		StringWriter buffer = new StringWriter(); 
		VehicleJourney neptuneObject = new VehicleJourney();
		neptuneObject.setObjectId("4321L");
	    neptuneObject.setId(4321L);
	    StopPoint sp = new StopPoint();
	    sp.setId(1001L);
	    
	    VehicleJourneyAtStop vjas = new VehicleJourneyAtStop();
	    vjas.setObjectId("ObjectID");
	    vjas.setObjectVersion(1);
	    vjas.setCreationTime(new LocalDateTime(2000,2,1,0,0));
	    vjas.setCreatorId("creatorId");
        vjas.setStopPoint(sp);
	    
        vjas.setArrivalTime(new LocalTime(23,59,0));
        
        vjas.setDepartureTime(new LocalTime(0,5,0));
        vjas.setArrivalDayOffset(0);
        
        vjas.setDepartureDayOffset(1);
        
		lineRegister = new LineRegisterCommand();
		
		lineRegister.writeVjas(buffer, neptuneObject, sp, vjas,false);
		
		
		Assert.assertEquals(buffer.toString(), "ObjectID|1|2000-02-01T00:00:00|creatorId|4321|1001|23:59:00|00:05:00|0|1|\\N\n", "Invalid data entry for buffer");

	}

	@Test
	public void setEmptyDestinationDisplayRefByLineIfNullTest() {
		Line line = new Line();
		Route route1 = new Route();
		JourneyPattern journey1 = new JourneyPattern();
		DestinationDisplay destinationDisplay1 = new DestinationDisplay();

		JourneyPattern journey11 = new JourneyPattern();
		DestinationDisplay destinationDisplay11 = new DestinationDisplay();

		journey1.setDestinationDisplay(destinationDisplay1);
		journey11.setDestinationDisplay(destinationDisplay11);

		List<JourneyPattern> journeyPatterns = new ArrayList<>(2);
		journeyPatterns.add(journey1);
		journeyPatterns.add(journey11);
		route1.setJourneyPatterns(journeyPatterns);


		Route route2 = new Route();
		JourneyPattern journey2 = new JourneyPattern();
		DestinationDisplay destinationDisplay2 = new DestinationDisplay();

		JourneyPattern journey21 = new JourneyPattern();
		DestinationDisplay destinationDisplay21 = new DestinationDisplay();

		journey2.setDestinationDisplay(destinationDisplay2);
		journey21.setDestinationDisplay(destinationDisplay21);

		List<JourneyPattern> journeyPatterns2 = new ArrayList<>(2);
		journeyPatterns2.add(journey2);
		journeyPatterns2.add(journey21);
		route2.setJourneyPatterns(journeyPatterns2);

		List<Route> routes = new ArrayList<>(2);
		routes.add(route1);
		routes.add(route2);

		line.setRoutes(routes);

		lineRegister = new LineRegisterCommand();

		lineRegister.setEmptyDestinationDisplayRefByLineIfNull(line);

		for (Route route : line.getRoutes()) {
			for (JourneyPattern journeyPattern : route.getJourneyPatterns()) {
				assertNull(journeyPattern.getDestinationDisplay());
			}
		}
	}
}
