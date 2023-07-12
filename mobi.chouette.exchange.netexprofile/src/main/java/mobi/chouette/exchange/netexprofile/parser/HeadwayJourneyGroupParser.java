package mobi.chouette.exchange.netexprofile.parser;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.TimeUtil;
import mobi.chouette.exchange.importer.Parser;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.importer.NetexprofileImportParameters;
import mobi.chouette.exchange.netexprofile.importer.util.NetexImportUtil;
import mobi.chouette.exchange.netexprofile.importer.util.NetexTimeConversionUtil;
import mobi.chouette.model.JourneyFrequency;
import mobi.chouette.model.Timeband;
import mobi.chouette.model.VehicleJourney;
import mobi.chouette.model.util.ObjectFactory;
import mobi.chouette.model.util.Referential;
import org.joda.time.Duration;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.rutebanken.netex.model.FrequencyGroups_RelStructure;
import org.rutebanken.netex.model.HeadwayJourneyGroup;
import org.rutebanken.netex.model.HeadwayJourneyGroup_VersionStructure;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
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

                                    journeyFrequency.setFirstDepartureTime(TimeUtil.toJodaLocalTime(headwayJourneyGroup.getFirstDepartureTime()));
                                    journeyFrequency.setLastDepartureTime(TimeUtil.toJodaLocalTime(headwayJourneyGroup.getLastDepartureTime()));
                                    journeyFrequency.setObjectId(serviceHeadwayJourneyId);

                                    try {
                                        journeyFrequency.setScheduledHeadwayInterval(toJodaDuration(headwayJourneyGroup.getScheduledHeadwayInterval()));
                                    } catch (DatatypeConfigurationException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
            }
        }
    }

    private static org.joda.time.Duration toJodaDuration(javax.xml.datatype.Duration duration) throws DatatypeConfigurationException {
        if (duration == null) {
            return null;
        }
        Period jodaPeriod = new Period(duration.toString());
        org.joda.time.Duration jodaDuration = jodaPeriod.toDurationFrom(null);
        return jodaDuration;
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
