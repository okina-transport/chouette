package mobi.chouette.exchange.netexprofile.parser;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.TimeUtil;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.importer.util.NetexImportUtil;
import mobi.chouette.model.JourneyFrequency;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.rutebanken.netex.model.FrequencyGroups_RelStructure;
import org.rutebanken.netex.model.HeadwayJourneyGroup;

import java.util.ArrayList;
import java.util.List;

@Log4j
public class HeadwayJourneyGroupParser  extends NetexParser implements Parser, Constant {

    @Override
    public void parse(Context context) throws Exception {
        Referential referential = (Referential) context.get(REFERENTIAL);
        FrequencyGroups_RelStructure frequencyGroups_relStructure = (FrequencyGroups_RelStructure) context.get(NETEX_LINE_DATA_CONTEXT);

        List<Object> listHeadwayJourneyGroup = frequencyGroups_relStructure.getHeadwayJourneyGroupRefOrHeadwayJourneyGroupOrRhythmicalJourneyGroupRef();

        for (Object array : listHeadwayJourneyGroup) {
            if(array instanceof ArrayList){
                ArrayList<Object> arrayList = (ArrayList<Object>)array;

                arrayList.stream().filter(objet -> objet instanceof HeadwayJourneyGroup)
                        .map(objet -> (HeadwayJourneyGroup)objet)
                                .forEach( headwayJourneyGroup -> {
                                    String serviceHeadwayJourneyId = NetexImportUtil.composeObjectIdFromNetexId(context,"HeadwayJourney", headwayJourneyGroup.getId());
                                    JourneyFrequency journeyFrequency = ObjectFactory.getJourneyFrequency(referential,serviceHeadwayJourneyId );

                                    journeyFrequency.setFirstDepartureTime(headwayJourneyGroup.getFirstDepartureTime());
                                    journeyFrequency.setLastDepartureTime(headwayJourneyGroup.getLastDepartureTime());
                                    journeyFrequency.setObjectId(serviceHeadwayJourneyId);


                                    journeyFrequency.setScheduledHeadwayInterval(TimeUtil.fromXmlDuration(headwayJourneyGroup.getScheduledHeadwayInterval()));

                                });
            }
        }
    }

    static {
        ParserFactory.register(HeadwayJourneyGroupParser.class.getName(), new ParserFactory() {
            private HeadwayJourneyGroupParser instance = new HeadwayJourneyGroupParser();

            @Override
            protected Parser create() {
                return instance;
            }
        });
    }
}
