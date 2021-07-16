package mobi.chouette.exchange.gtfs.importer;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.exchange.gtfs.model.importer.StopById;

import javax.naming.InitialContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

@Log4j
public class GtfsPrefixRemovalChecker implements Command{


    private int quayColumnIndex;
    private int stopAreaColumnIndex;

    @Override
    public boolean execute(Context context) throws Exception {
        JobData jobData = (JobData) context.get(JOB_DATA);
        String path = jobData.getPathName();
        String fileName = path + "/" + Constant.GTFS_STOPS_FILE;

//        List<String> lines = Collections.emptyList();
//        try
//        {
//            lines = Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);
//            initIndexes(lines);
//
//        }
//
//        catch (IOException e)
//        {
//            log.error("Error while checking prefix removal");
//            // do something
//            e.printStackTrace();
//        }




        return true;
    }

    private void initIndexes(List<String> lines){
        String[] headers = lines.get(0).split(",");

        for (int i = 0 ; i < headers.length ; i++){
            if (StopById.FIELDS.stop_id.name().equals(headers[i])){
                quayColumnIndex = i;
            }else if (StopById.FIELDS.parent_station.name().equals(headers[i])){
                stopAreaColumnIndex = i;
            }
        }
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = new GtfsPrefixRemovalChecker();
            return result;
        }
    }

    static {
        CommandFactory.factories.put(GtfsPrefixRemovalChecker.class.getName(), new GtfsPrefixRemovalChecker.DefaultCommandFactory());
    }


}
