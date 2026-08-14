package mobi.chouette.exchange.netexprofile.exporter;

import mobi.chouette.common.Context;
import mobi.chouette.common.FileUtil;
import mobi.chouette.common.JobData;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.exporter.producer.NetexProducer;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_STATE;
import mobi.chouette.exchange.report.ActionReporter.OBJECT_TYPE;
import mobi.chouette.exchange.report.IO_TYPE;

import javax.xml.bind.Marshaller;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;


public class NetexCommonDataProducer extends NetexProducer implements Constant {

    public void produce(Context context) throws Exception {
        ActionReporter reporter = ActionReporter.Factory.getInstance();
        JobData jobData = (JobData) context.get(JOB_DATA);
        Path outputPath = Paths.get(jobData.getPathName(), OUTPUT);
        ExportableData exportableData = (ExportableData) context.get(EXPORTABLE_DATA);
        ExportableNetexData exportableNetexData = (ExportableNetexData) context.get(EXPORTABLE_NETEX_DATA);
        exportableNetexData.getConnectionLinks().addAll(exportableData.getConnectionLinks());

        String fileName = ExportedFilenamer.createNetexFranceCommonFilename(context);
        reporter.addFileReport(context, fileName, IO_TYPE.OUTPUT);
        Path filePath = new File(outputPath.toFile(), fileName).toPath();

        Marshaller marshaller = (Marshaller) context.get(MARSHALLER);
        NetexFileWriter writer = new NetexFileWriter();

        Path tmpPath = FileUtil.getTmpPath(filePath);
        writer.writeXmlFile(context, tmpPath, exportableData, exportableNetexData, NetexFragmentMode.COMMON, marshaller);
        Files.copy(tmpPath, filePath, StandardCopyOption.REPLACE_EXISTING);

        reporter.addObjectReport(context, "merged", OBJECT_TYPE.NETWORK, "networks", OBJECT_STATE.OK, IO_TYPE.OUTPUT);
        reporter.setStatToObjectReport(context, "merged", OBJECT_TYPE.NETWORK, OBJECT_TYPE.NETWORK, exportableData.getNetworks().size());
        reporter.addObjectReport(context, "merged", OBJECT_TYPE.COMPANY, "companies", OBJECT_STATE.OK, IO_TYPE.OUTPUT);
        reporter.setStatToObjectReport(context, "merged", OBJECT_TYPE.COMPANY, OBJECT_TYPE.COMPANY, exportableData.getCompanies().size());
        reporter.addObjectReport(context, "merged", OBJECT_TYPE.CONNECTION_LINK, "connection links", OBJECT_STATE.OK, IO_TYPE.OUTPUT);
        reporter.setStatToObjectReport(context, "merged", OBJECT_TYPE.CONNECTION_LINK, OBJECT_TYPE.CONNECTION_LINK, exportableData.getConnectionLinks().size());
        reporter.addObjectReport(context, "merged", OBJECT_TYPE.ACCESS_POINT, "access points", OBJECT_STATE.OK, IO_TYPE.OUTPUT);
        reporter.setStatToObjectReport(context, "merged", OBJECT_TYPE.ACCESS_POINT, OBJECT_TYPE.ACCESS_POINT, exportableData.getAccessPoints().size());
        reporter.addObjectReport(context, "merged", OBJECT_TYPE.STOP_AREA, "stop areas", OBJECT_STATE.OK, IO_TYPE.OUTPUT);
        reporter.setStatToObjectReport(context, "merged", OBJECT_TYPE.STOP_AREA, OBJECT_TYPE.STOP_AREA, exportableData.getCommercialStops().size() + exportableData.getPhysicalStops().size());
        reporter.addObjectReport(context, "merged", OBJECT_TYPE.TIMETABLE, "calendars", OBJECT_STATE.OK, IO_TYPE.OUTPUT);
        reporter.setStatToObjectReport(context, "merged", OBJECT_TYPE.TIMETABLE, OBJECT_TYPE.TIMETABLE, exportableData.getTimetables().size());
    }

}

