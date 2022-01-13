package mobi.chouette.exchange.netexprofile.importer;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import javax.naming.InitialContext;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.type.LongLatTypeEnum;
import mobi.chouette.model.util.Referential;

@Log4j
public class NetexInitGeolocationCommand implements Command, Constant {

    public static final String COMMAND = "NetexInitGeolocationCommand";

    @Override
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;

        Monitor monitor = MonitorFactory.start(COMMAND);

        try {
            Referential referential = (Referential) context.get(REFERENTIAL);

            List<StopArea> stopAreasWithoutGeolocation = referential.getSharedStopAreas().values().stream()
                                                                                                .filter(stopArea -> !stopArea.hasCoordinates())
                                                                                                .collect(Collectors.toList());

            stopAreasWithoutGeolocation.forEach(this::initGeolocation);


            result = SUCCESS;
        } catch (Exception e) {
        	log.error("Error initializing geolocation",e);
        } finally {
            log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
        }

        return result;
    }

    private void initGeolocation(StopArea stopArea){
        stopArea.setLongitude(new BigDecimal(0));
        stopArea.setLatitude(new BigDecimal(0));
        stopArea.setLongLatType(LongLatTypeEnum.WGS84);
    }
    
  

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = new NetexInitGeolocationCommand();
            return result;
        }
    }

    static {
        CommandFactory.factories.put(NetexInitGeolocationCommand.class.getName(), new DefaultCommandFactory());
    }

}
