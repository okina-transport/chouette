package mobi.chouette.exchange.netexprofile.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.netexprofile.importer.client.TiamatClient;

import javax.naming.InitialContext;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Log4j
public class NetexGeocodeCommand implements Command {

    private static final String NETEX_GEOCODE_COMMAND = "NetexGeocodeCommand";

    private static final String STOP_PLACE_REGISTER_MAP = "STOP_PLACE_REGISTER_MAP";

    @Override
    public boolean execute(Context context) throws Exception {
        log.debug(NETEX_GEOCODE_COMMAND);
        Map<String, String> stopPlaceMapping;
        if (context.get(STOP_PLACE_REGISTER_MAP) != null) {
            stopPlaceMapping = (Map<String, String>) context.get(STOP_PLACE_REGISTER_MAP);
            Set<String> tiamatIdentifier = new HashSet<>(stopPlaceMapping.size());
            for (Map.Entry<String, String> entry : stopPlaceMapping.entrySet()) {
                tiamatIdentifier.add(entry.getKey());
            }
            TiamatClient tiamatClient = new TiamatClient();
            tiamatClient.sendQuayNetexIdForGeocoding(tiamatIdentifier);
        }
        return true;
    }

    public static class DefaultCommandFactory extends CommandFactory {
        @Override
        protected Command create(InitialContext context) throws IOException {
            return new NetexGeocodeCommand();
        }
    }

    static {
        CommandFactory.factories.put(NetexGeocodeCommand.class.getName(),
                new NetexGeocodeCommand.DefaultCommandFactory());
    }
}
