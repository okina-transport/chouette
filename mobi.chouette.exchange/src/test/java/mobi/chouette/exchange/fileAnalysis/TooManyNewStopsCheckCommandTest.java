package mobi.chouette.exchange.fileAnalysis;

import mobi.chouette.common.Context;
import mobi.chouette.dao.StopAreaDAO;
import mobi.chouette.exchange.report.AnalyzeReport;
import mobi.chouette.model.StopArea;
import org.junit.Assert;
import org.junit.Test;

import javax.ejb.EJB;

import static mobi.chouette.common.Constant.ANALYSIS_REPORT;

public class TooManyNewStopsCheckCommandTest {

    @EJB
    StopAreaDAO stopAreaDAO;

    //@Test
    // TODO Faire fonctionner le stopAreaDAO -> nullpointerexception
    public void testTooManyNewStopsCheckCommand() throws Exception {
        Context context = new Context();
        AnalyzeReport analyzeReport = new AnalyzeReport();

        StopArea stopArea1 = new StopArea();
        StopArea stopArea2 = new StopArea();
        StopArea stopArea3 = new StopArea();

        stopArea1.setOriginalStopId("Test1");
        stopArea1.setOriginalStopId("Test2");
        stopArea1.setOriginalStopId("Test3");

        analyzeReport.getStops().add(stopArea1);
        analyzeReport.getStops().add(stopArea2);
        analyzeReport.getStops().add(stopArea3);
        context.put(ANALYSIS_REPORT, analyzeReport);

        stopAreaDAO.create(stopArea1);
        stopAreaDAO.flush();

        TooManyNewStopsCheckCommand tooManyNewStopsCheckCommand = new TooManyNewStopsCheckCommand();
        tooManyNewStopsCheckCommand.execute(context);

        Assert.assertEquals(2, analyzeReport.getNewStops().size());
    }
}