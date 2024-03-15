package mobi.chouette.exchange.neptune.importer;


import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Color;
import mobi.chouette.common.Context;
import mobi.chouette.common.FileUtil;
import mobi.chouette.common.JobData;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.exchange.neptune.Constant;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.exchange.validation.report.DataLocation;
import mobi.chouette.exchange.validation.report.ValidationReporter;
import mobi.chouette.model.util.Referential;
import org.trident.schema.trident.ChouettePTNetworkType;
import org.xml.sax.SAXParseException;

import javax.naming.InitialContext;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


/**
 * Command that will check that XML files in input complies with Neptune format.
 * If not, process is stopped and report is send to user
 *
 */
@Log4j
public class NeptuneComplianceCheckerCommand implements Command, Constant {

    public static final String COMMAND = "NeptuneComplianceCheckerCommand";

    private boolean result = SUCCESS;

    Unmarshaller unmarshaller;

    private ValidationReporter validationReporter;

    private static final String XML_3 = "1-NEPTUNE-XML-3";


    @Override
    public boolean execute(Context context) throws Exception {

        Monitor monitor = MonitorFactory.start(COMMAND);


        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);


        try {
            //todo switch to run neptune local
            //Schema schema = schemaFactory.newSchema(new File("/tmp/xsd/neptune.xsd"));
            Schema schema = schemaFactory.newSchema(getClass().getClassLoader().getResource("/xsd/neptune.xsd"));
            validationReporter = ValidationReporter.Factory.getInstance();
            validationReporter.addItemToValidationReport(context, XML_3, "E");

            JAXBContext jaxbContext = JAXBContext.newInstance(org.trident.schema.trident.ObjectFactory.class);
            unmarshaller = jaxbContext.createUnmarshaller();
            unmarshaller.setSchema(schema);

            context.put(REFERENTIAL, new Referential());
            JobData jobData = (JobData) context.get(JOB_DATA);
            Path path = Paths.get(jobData.getPathName(), INPUT);

            List<Path> filesToProcess = FileUtil.listFiles(path, "*.xml", "*metadata*");
            filesToProcess.forEach(file->checkNeptuneCompliance(file, context));

        } catch (Exception e) {
            log.error(e, e);
            result = ERROR;
            throw e;
        } finally {
            log.info(Color.MAGENTA + monitor.stop() + Color.NORMAL);
        }
        return result;
    }

    private void checkNeptuneCompliance(Path path,Context context){
        log.info("Checking neptune compliance for file:"+path.toAbsolutePath());
        ActionReporter reporter = ActionReporter.Factory.getInstance();
        String fileName =  path.getFileName().toString();


        try {
            JAXBElement<ChouettePTNetworkType> chouetteRoute = (JAXBElement<ChouettePTNetworkType>) unmarshaller.unmarshal(new File(path.toAbsolutePath().toString()));
            log.info("File complies with neptune format");
            reporter.setFileState(context, fileName, IO_TYPE.INPUT, ActionReporter.FILE_STATE.OK);

        } catch (JAXBException e) {
            Throwable error = e.getLinkedException();
            if (error instanceof SAXParseException) {
                SAXParseException cause = (SAXParseException) error;
                DataLocation location = new DataLocation(fileName, cause.getLineNumber(), cause.getColumnNumber());
                validationReporter.addCheckPointReportError(context, XML_3, location,cause.getMessage());

            } else {
                DataLocation location = new DataLocation(fileName, 1, 1);
                location.setName("xml-failure");
                validationReporter.addCheckPointReportError(context, XML_3, location, error.toString());
            }

            log.error("Error while checking Neptune compliance for xml file:"+path.toAbsolutePath());
            log.error(e);
            result = ERROR;
            reporter.setFileState(context, fileName, IO_TYPE.INPUT, ActionReporter.FILE_STATE.ERROR);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = new NeptuneComplianceCheckerCommand();
            return result;
        }
    }

    static {
        CommandFactory.factories.put(NeptuneComplianceCheckerCommand.class.getName(), new NeptuneComplianceCheckerCommand.DefaultCommandFactory());
    }


}
