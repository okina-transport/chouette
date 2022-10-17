package mobi.chouette.exchange.netexprofile.importer;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.common.monitor.JamonUtils;
import mobi.chouette.exchange.importer.ParserFactory;
import mobi.chouette.exchange.netexprofile.Constant;
import mobi.chouette.exchange.netexprofile.parser.PublicationDeliveryParser;
import mobi.chouette.exchange.report.ActionReporter;
import mobi.chouette.exchange.report.ActionReporter.FILE_ERROR_CODE;
import mobi.chouette.exchange.report.IO_TYPE;
import mobi.chouette.model.Line;
import mobi.chouette.model.util.NamingUtil;
import mobi.chouette.model.util.Referential;
import org.apache.commons.lang3.StringUtils;

import javax.naming.InitialContext;
import java.io.IOException;
import java.nio.file.Path;

@Log4j
public class NetexLineParserCommand implements Command, Constant {

    public static final String COMMAND = "NetexLineParserCommand";

    @Getter
    @Setter
    private Path path;

    @Override
    public boolean execute(Context context) throws Exception {
        boolean result = ERROR;
        Monitor monitor = MonitorFactory.start(COMMAND);
 
        String fileName = path.getFileName().toString();
        
        ActionReporter reporter = ActionReporter.Factory.getInstance();
        reporter.addFileReport(context, fileName, IO_TYPE.INPUT);
        context.put(FILE_NAME, fileName);

        try {
            Referential referential = (Referential) context.get(REFERENTIAL);
            if (referential != null) {
                referential.clear(true);
            }

            PublicationDeliveryParser parser = (PublicationDeliveryParser) ParserFactory.create(PublicationDeliveryParser.class.getName());
            parser.parse(context);
            feedLineInfoWithCommonData(context);

            Context validationContext = (Context) context.get(VALIDATION_CONTEXT);
            addStats(context, reporter, validationContext, referential);
			reporter.setFileState(context, fileName, IO_TYPE.INPUT, ActionReporter.FILE_STATE.OK);
            result = SUCCESS;
        } catch (Exception e) {
            log.error("ERROR",e);
        	reporter.addFileErrorInReport(context, fileName, FILE_ERROR_CODE.INTERNAL_ERROR, e.toString());
            reporter.setActionError(context, ActionReporter.ERROR_CODE.INVALID_DATA, "Error");
            throw e;
        } finally {
            JamonUtils.logMagenta(log, monitor);
        }

        return result;
    }

    /**
     * Feed the current line with information coming from common files.
     * (Common files are parsed before line files. And each time NetexLineParser is called, referential is cleared.
     * To avoid data loss, line information are stored in context and then pulled back to current line in feedLineInfoWithCommonData)
     */
    private void feedLineInfoWithCommonData(Context context){
        Referential referential = (Referential) context.get(REFERENTIAL);


        for (Line currentLine : referential.getLines().values()) {

            String currentLineId = currentLine.getObjectId();
            if (!referential.getSharedLines().containsKey(currentLineId))
              continue;

            Line currentLineInfo = referential.getSharedLines().get(currentLine.getObjectId());

            if (StringUtils.isEmpty(currentLine.getName())){
                currentLine.setName(currentLineInfo.getName());
            }

            if (StringUtils.isEmpty(currentLine.getNumber())){
                currentLine.setNumber(currentLineInfo.getNumber());
            }

            if (StringUtils.isEmpty(currentLine.getColor())){
                currentLine.setColor(currentLineInfo.getColor());
            }

            if (StringUtils.isEmpty(currentLine.getComment())){
                currentLine.setComment(currentLineInfo.getComment());
            }

            //transport mode is never null because there is a default value (bus). No need to to a test : override must be made in all cases
            currentLine.setTransportModeName(currentLineInfo.getTransportModeName());

            if (StringUtils.isEmpty(currentLine.getCodifligne())){
                currentLine.setCodifligne(currentLineInfo.getCodifligne());
            }

            if (StringUtils.isEmpty(currentLine.getPublishedName())){
                currentLine.setPublishedName(currentLineInfo.getPublishedName());
            }

            if (StringUtils.isEmpty(currentLine.getRegistrationNumber())){
                currentLine.setRegistrationNumber(currentLineInfo.getRegistrationNumber());
            }

            if (StringUtils.isEmpty(currentLine.getTextColor())){
                currentLine.setTextColor(currentLineInfo.getTextColor());
            }

            if (StringUtils.isEmpty(currentLine.getUrl())){
                currentLine.setUrl(currentLineInfo.getUrl());
            }

            if (StringUtils.isEmpty(currentLine.getCreatorId())){
                currentLine.setCreatorId(currentLineInfo.getCreatorId());
            }

            if (currentLine.getBike() == null){
                currentLine.setBike(currentLineInfo.getBike());
            }

            if (currentLine.getCategoriesForLine() == null){
                currentLine.setCategoriesForLine(currentLineInfo.getCategoriesForLine());
            }

            if (currentLine.getCompany() == null){
                currentLine.setCompany(currentLineInfo.getCompany());
            }

            if (currentLine.getFlexibleLineProperties() == null){
                currentLine.setFlexibleLineProperties(currentLineInfo.getFlexibleLineProperties());
            }

            if (currentLine.getFlexibleService() == null){
                currentLine.setFlexibleService(currentLineInfo.getFlexibleService());
            }

            if (currentLine.getFootnotes() == null || currentLine.getFootnotes().isEmpty()){
                currentLine.setFootnotes(currentLineInfo.getFootnotes());
            }

            if (currentLine.getGroupOfLines() == null || currentLine.getGroupOfLines().isEmpty()){
                currentLine.setGroupOfLines(currentLineInfo.getGroupOfLines());
            }

            if (currentLine.getIntUserNeeds() == null){
                currentLine.setIntUserNeeds(currentLineInfo.getIntUserNeeds());
            }

            if (currentLine.getNetwork() == null){
                currentLine.setNetwork(currentLineInfo.getNetwork());
            }

            if (currentLine.getMobilityRestrictedSuitable() == null){
                currentLine.setMobilityRestrictedSuitable(currentLineInfo.getMobilityRestrictedSuitable());
            }

            if (currentLine.getPosition() == null){
                currentLine.setPosition(currentLineInfo.getPosition());
            }

            if (currentLine.getTransportSubModeName() == null  ){
                currentLine.setTransportSubModeName(currentLineInfo.getTransportSubModeName());
            }

            if (currentLine.getUserNeeds() == null  ){
                currentLine.setUserNeeds(currentLineInfo.getUserNeeds());
            }

            if (currentLine.getTad() == null  ){
                currentLine.setTad(currentLineInfo.getTad());
            }

        }

    }

    private void addStats(Context context, ActionReporter reporter, Context validationContext, Referential referential) {
        Line line = referential.getLines().values().iterator().next();
        reporter.addObjectReport(context, line.getObjectId(), ActionReporter.OBJECT_TYPE.LINE, NamingUtil.getName(line), ActionReporter.OBJECT_STATE.OK, IO_TYPE.INPUT);
        reporter.setStatToObjectReport(context, line.getObjectId(), ActionReporter.OBJECT_TYPE.LINE, ActionReporter.OBJECT_TYPE.LINE, 1);

    }

    public static class DefaultCommandFactory extends CommandFactory {

        @Override
        protected Command create(InitialContext context) throws IOException {
            Command result = new NetexLineParserCommand();
            return result;
        }
    }

    static {
        CommandFactory.factories.put(NetexLineParserCommand.class.getName(),
                new DefaultCommandFactory());
    }
}
