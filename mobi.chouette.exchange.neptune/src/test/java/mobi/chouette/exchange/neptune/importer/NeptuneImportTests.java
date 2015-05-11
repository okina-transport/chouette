package mobi.chouette.exchange.neptune.importer;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.ejb.EJB;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.exchange.neptune.Constant;
import mobi.chouette.exchange.neptune.JobDataTest;
import mobi.chouette.exchange.neptune.NeptuneTestsUtils;
import mobi.chouette.exchange.neptune.importer.NeptuneImportParameters;
import mobi.chouette.exchange.neptune.importer.NeptuneImporterCommand;
import mobi.chouette.exchange.report.ActionReport;
import mobi.chouette.exchange.report.ReportConstant;
import mobi.chouette.exchange.report.FileInfo.FILE_STATE;
import mobi.chouette.exchange.report.LineInfo;
import mobi.chouette.exchange.report.LineInfo.LINE_STATE;
import mobi.chouette.exchange.validation.report.ValidationReport;
import mobi.chouette.model.Line;
import mobi.chouette.model.StopArea;
import mobi.chouette.model.api.Job;
import mobi.chouette.model.type.ChouetteAreaEnum;
import mobi.chouette.model.util.Referential;
import mobi.chouette.persistence.hibernate.ContextHolder;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.Test;

@Log4j
public class NeptuneImportTests extends Arquillian implements Constant, ReportConstant {

	@EJB
	LineDAO lineDao;


	@Deployment
	public static WebArchive createDeployment() {

		WebArchive result;

		File[] files = Maven.resolver().loadPomFromFile("pom.xml")
				.resolve("mobi.chouette:mobi.chouette.exchange.neptune:3.0.0").withTransitivity().asFile();

		result = ShrinkWrap.create(WebArchive.class, "test.war")
				.addAsWebInfResource("postgres-ds.xml")
				.addAsLibraries(files)
				.addClass(NeptuneTestsUtils.class)
				.addClass(JobDataTest.class)
				.addAsResource(EmptyAsset.INSTANCE, "beans.xml");
		return result;

	}

	protected static InitialContext initialContext;
	

	protected void init() {
		Locale.setDefault(Locale.ENGLISH);
		if (initialContext == null) {
			try {
				initialContext = new InitialContext();
			} catch (NamingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
	}
	protected Context initImportContext() {
		init();
		ContextHolder.setContext("chouette_gui"); // set tenant schema

		Context context = new Context();
		context.put(INITIAL_CONTEXT, initialContext);
		context.put(REPORT, new ActionReport());
		context.put(MAIN_VALIDATION_REPORT, new ValidationReport());
		NeptuneImportParameters configuration = new NeptuneImportParameters();
		context.put(CONFIGURATION, configuration);
		configuration.setName("name");
		configuration.setUserName("userName");
		configuration.setNoSave(true);
		configuration.setOrganisationName("organisation");
		configuration.setReferentialName("test");
		JobDataTest jobData = new JobDataTest();
		context.put(JOB_DATA,jobData);
		jobData.setPathName("target/referential/test");
		File f = new File("target/referential/test");
		if (f.exists())
			try {
				FileUtils.deleteDirectory(f);
			} catch (IOException e) {
				e.printStackTrace();
			}
		f.mkdirs();
		jobData.setReferential("chouette_gui");
		jobData.setAction(IMPORTER);
		jobData.setType( "neptune");
		context.put("testng", "true");
		context.put(OPTIMIZED, Boolean.FALSE);
		return context;

	}

	@Test(groups = { "CheckParameters" }, description = "Import Plugin should reject file not found")
	public void verifyCheckInputFileExists() throws Exception {
		// TODO test à passer aussi sur la commande uncompress du module
		// mobi.chouette.exchange
		Context context = initImportContext();
		NeptuneImporterCommand command = (NeptuneImporterCommand) CommandFactory.create(initialContext,
				NeptuneImporterCommand.class.getName());
		try {
			command.execute(context);
		} catch (Exception ex) {
			log.error("test failed", ex);
			throw ex;
		}
		ActionReport report = (ActionReport) context.get(REPORT);
		Assert.assertEquals(report.getResult(), STATUS_ERROR, "result");
		Assert.assertTrue(report.getFailure().getDescription().startsWith("Missing"), "error message " + report.getFailure());
		System.out.println("error message = " + report.getFailure());

	}

	@Test(groups = { "ImportLineUtf8" }, description = "Import Plugin should detect file encoding")
	public void verifyCheckGoodEncoding() throws Exception {
		Context context = initImportContext();
		NeptuneImporterCommand command = (NeptuneImporterCommand) CommandFactory.create(initialContext,
				NeptuneImporterCommand.class.getName());
		NeptuneTestsUtils.copyFile("C_NEPTUNE_1_utf8.xml");
		JobDataTest jobData = (JobDataTest) context.get(JOB_DATA);
		jobData.setFilename("C_NEPTUNE_1_utf8.xml");
		try {
			command.execute(context);
		} catch (Exception ex) {
			log.error("test failed", ex);
			throw ex;
		}
		ActionReport report = (ActionReport) context.get(REPORT);
		Assert.assertEquals(report.getResult(), STATUS_OK, "result");
		Assert.assertEquals(report.getFiles().size(), 1, "file reported");
		Assert.assertEquals(report.getLines().size(), 1, "line reported");
		Assert.assertTrue(report.getLines().get(0).getName().endsWith("é"), "character conversion");
	}

	@Test(groups = { "ImportLineUtf8Bom" }, description = "Import Plugin should detect bom in file encoding")
	public void verifyCheckGoodEncodingWithBom() throws Exception {
		Context context = initImportContext();
		NeptuneImporterCommand command = (NeptuneImporterCommand) CommandFactory.create(initialContext,
				NeptuneImporterCommand.class.getName());
		NeptuneTestsUtils.copyFile("C_NEPTUNE_1_utf8_bom.xml");
		JobDataTest jobData = (JobDataTest) context.get(JOB_DATA);
		jobData.setFilename("C_NEPTUNE_1_utf8_bom.xml");
		try {
			command.execute(context);
		} catch (Exception ex) {
			log.error("test failed", ex);
			throw ex;
		}
		ActionReport report = (ActionReport) context.get(REPORT);
		Assert.assertEquals(report.getResult(), STATUS_OK, "result");
		Assert.assertEquals(report.getFiles().size(), 1, "file reported");
		Assert.assertEquals(report.getLines().size(), 1, "line reported");
		Assert.assertTrue(report.getLines().get(0).getName().endsWith("é"), "character conversion");
	}

	@Test(groups = { "ImportLineBadEnc" }, description = "Import Plugin should detect file encoding")
	public void verifyCheckBadEncoding() throws Exception {
		Context context = initImportContext();
		NeptuneImporterCommand command = (NeptuneImporterCommand) CommandFactory.create(initialContext,
				NeptuneImporterCommand.class.getName());
		NeptuneTestsUtils.copyFile("C_NEPTUNE_1_bad_enc.xml");
		JobDataTest jobData = (JobDataTest) context.get(JOB_DATA);
		jobData.setFilename("C_NEPTUNE_1_bad_enc.xml");
		try {
			command.execute(context);
		} catch (Exception ex) {
			log.error("test failed", ex);
			throw ex;
		}
		ActionReport report = (ActionReport) context.get(REPORT);
		Assert.assertEquals(report.getResult(), STATUS_ERROR, "result");
		Assert.assertEquals(report.getFiles().size(), 1, "file reported");
		Assert.assertEquals(report.getFiles().get(0).getStatus(), FILE_STATE.ERROR, "file status");
		Assert.assertEquals(report.getFiles().get(0).getErrors().size(), 1, "file errors");
		Assert.assertTrue(report.getFiles().get(0).getErrors().get(0).getDescription().startsWith("invalid encoding"),
				"file error message " + report.getFiles().get(0).getErrors().get(0));
		System.out.println("file error message = " + report.getFiles().get(0).getErrors().get(0));
	}

	@Test(groups = { "ImportLine" }, description = "Import Plugin should import file")
	public void verifyImportLine() throws Exception {
		Context context = initImportContext();
		NeptuneImporterCommand command = (NeptuneImporterCommand) CommandFactory.create(initialContext,
				NeptuneImporterCommand.class.getName());
		NeptuneTestsUtils.copyFile("C_NEPTUNE_1.xml");
		JobDataTest jobData = (JobDataTest) context.get(JOB_DATA);
		jobData.setFilename("C_NEPTUNE_1.xml");
		NeptuneImportParameters configuration = (NeptuneImportParameters) context.get(CONFIGURATION);
		configuration.setNoSave(false);
		configuration.setCleanRepository(true);
		try {
			command.execute(context);
		} catch (Exception ex) {
			log.error("test failed", ex);
			throw ex;
		}
		ActionReport report = (ActionReport) context.get(REPORT);
		Assert.assertEquals(report.getResult(), STATUS_OK, "result");
		Assert.assertEquals(report.getFiles().size(), 1, "file reported");
		Assert.assertEquals(report.getLines().size(), 1, "line reported");
		Reporter.log("report line :" + report.getLines().get(0).toString(), true);
		Assert.assertEquals(report.getLines().get(0).getStatus(), LINE_STATE.OK, "line status");

		// line should be saved
		Line line = lineDao.findByObjectId("NINOXE:Line:15574334");
		Assert.assertNotNull(line, "line");

		NeptuneTestsUtils.checkLine(context);
	}
	

	@Test(groups = { "ImportRCLine" }, description = "Import Plugin should import file with ITL")
	public void verifyImportRCLine() throws Exception {
		Context context = initImportContext();
		NeptuneImporterCommand command = (NeptuneImporterCommand) CommandFactory.create(initialContext,
				NeptuneImporterCommand.class.getName());
		NeptuneTestsUtils.copyFile("C_CHOUETTE_52.xml");
		JobDataTest jobData = (JobDataTest) context.get(JOB_DATA);
		jobData.setFilename("C_CHOUETTE_52.xml");
		NeptuneImportParameters configuration = (NeptuneImportParameters) context.get(CONFIGURATION);
		configuration.setNoSave(true);
		try {
			command.execute(context);
		} catch (Exception ex) {
			log.error("test failed", ex);
			throw ex;
		}
		ActionReport report = (ActionReport) context.get(REPORT);
		Reporter.log("report :" + report.toString(), true);
		ValidationReport valreport = (ValidationReport) context.get(MAIN_VALIDATION_REPORT);
		Reporter.log("valreport :" + valreport.toString(), true);
		Assert.assertEquals(report.getResult(), STATUS_OK, "result");
		Assert.assertEquals(report.getFiles().size(), 1, "file reported");
		Assert.assertEquals(report.getLines().size(), 1, "line reported");
		Assert.assertEquals(report.getLines().get(0).getStatus(), LINE_STATE.OK, "line status");

		Referential referential = (Referential) context.get(REFERENTIAL);
		Assert.assertNotNull(referential, "referential");
		Assert.assertEquals(referential.getLines().size(), 1, "lines size");
		Line line = referential.getLines().get("Hastustoto:Line:52");
		Assert.assertNotNull(line, "line");

		Assert.assertNotNull(line.getRoutingConstraints(), "line must have routing constraints");
		Assert.assertEquals(line.getRoutingConstraints().size(), 1, "line must have 1 routing constraint");
		StopArea area = line.getRoutingConstraints().get(0);
		Assert.assertEquals(area.getAreaType(), ChouetteAreaEnum.ITL, "routing constraint area must be of "
				+ ChouetteAreaEnum.ITL + " type");
		Assert.assertNotNull(area.getRoutingConstraintAreas(),
				"routing constraint area must have stopArea children as routing constraints");
		Assert.assertEquals(area.getContainedStopAreas().size(),0, "routing constraint area must not have stopArea children");
		Assert.assertNull(area.getParent(), "routing constraint area must not have stopArea parent");
		Assert.assertTrue(area.getRoutingConstraintAreas().size() > 0,
				"routing constraint area must have stopArea children as routing constraints");
	}

	@Test(groups = { "ImportZipLines" }, description = "Import Plugin should import zip file")
	public void verifyImportZipLines() throws Exception {
		Context context = initImportContext();
		NeptuneImporterCommand command = (NeptuneImporterCommand) CommandFactory.create(initialContext,
				NeptuneImporterCommand.class.getName());
		NeptuneTestsUtils.copyFile("lignes_neptune.zip");
		JobDataTest jobData = (JobDataTest) context.get(JOB_DATA);
		jobData.setFilename("lignes_neptune.zip");
		NeptuneImportParameters configuration = (NeptuneImportParameters) context.get(CONFIGURATION);
		configuration.setNoSave(true);
		try {
			command.execute(context);
		} catch (Exception ex) {
			log.error("test failed", ex);
			throw ex;
		}
		ActionReport report = (ActionReport) context.get(REPORT);
		Assert.assertEquals(report.getResult(), STATUS_OK, "result");
		Assert.assertEquals(report.getFiles().size(), 7, "file reported");
		Assert.assertEquals(report.getLines().size(), 6, "line reported");
		for (LineInfo line : report.getLines()) {
			Reporter.log("report line :" + line.toString(), true);
			Assert.assertEquals(line.getStatus(), LINE_STATE.OK, "line status");

		}

	}

}
