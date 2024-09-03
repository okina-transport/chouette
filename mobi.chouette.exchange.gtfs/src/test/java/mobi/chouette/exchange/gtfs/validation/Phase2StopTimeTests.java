package mobi.chouette.exchange.gtfs.validation;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.exchange.gtfs.importer.GtfsImportParameters;
import mobi.chouette.exchange.report.AnalyzeReport;
import mobi.chouette.exchange.validation.report.CheckPointReport.SEVERITY;
import mobi.chouette.exchange.validation.report.ValidationReporter.RESULT;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

@Log4j
public class Phase2StopTimeTests extends AbstractPhase2Tests {


    @BeforeSuite
    public void init() {
        super.init();
    }

    @Test(groups = {"Phase 2 StopTime"}, description = "identical consecutive stops", priority = 350)
    public void verifyTest_2_1() throws Exception {
        log.info(Color.GREEN + "StopTime_1 : identical consecutive stops" + Color.NORMAL);
        Context context = new Context();
        GtfsImportParameters configuration = new GtfsImportParameters();
        configuration.setRoutesReorganization(true);
        context.put(CONFIGURATION, configuration);
        context.put(ANALYSIS_REPORT, new AnalyzeReport());

        verifyValidation(log, context, "stoptime_1", GTFS_2_GTFS_StopTime_1, SEVERITY.ERROR, RESULT.NOK, true);
    }

}
