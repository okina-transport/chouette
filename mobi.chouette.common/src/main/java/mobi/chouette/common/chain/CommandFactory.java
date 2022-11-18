package mobi.chouette.common.chain;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.naming.InitialContext;

public abstract class CommandFactory {

    public static Map<String, CommandFactory> factories = new HashMap<>();

    protected abstract Command create(InitialContext context) throws IOException;

    public static final Command create(InitialContext context, String name) throws ClassNotFoundException, IOException {
        if (!factories.containsKey(name)) {
            Class.forName(name);
            if (!factories.containsKey(name))
                throw new ClassNotFoundException(name);
        }
        return factories.get(name).create(context);
    }
}
