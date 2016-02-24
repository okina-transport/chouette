package mobi.chouette.exchange.gtfs.exporter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;

import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.dao.LineDAO;
import mobi.chouette.exchange.gtfs.Constant;
import mobi.chouette.exchange.gtfs.DummyChecker;
import mobi.chouette.exchange.gtfs.GtfsTestsUtils;
import mobi.chouette.exchange.gtfs.JobDataTest;
import mobi.chouette.exchange.neptune.importer.NeptuneImportParameters;
import mobi.chouette.exchange.neptune.importer.NeptuneImporterCommand;
import mobi.chouette.exchange.report.ActionReport;
import mobi.chouette.exchange.report.FileInfo;
import mobi.chouette.exchange.report.LineInfo;
import mobi.chouette.exchange.report.LineInfo.LINE_STATE;
import mobi.chouette.exchange.report.ReportConstant;
import mobi.chouette.exchange.validation.report.CheckPoint;
import mobi.chouette.exchange.validation.report.ValidationReport;
import mobi.chouette.model.Line;
import mobi.chouette.persistence.hibernate.ContextHolder;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.Test;

@Log4j
public class GtfsExportTests extends Arquillian implements Constant, ReportConstant
{
	@Deployment
	public static EnterpriseArchive createDeployment() {

		EnterpriseArchive result;


		File[] files = Maven.resolver().loadPomFromFile("pom.xml")
				.resolve("mobi.chouette:mobi.chouette.exchange.gtfs").withTransitivity().asFile();
		List<File> jars = new ArrayList<>();
		List<JavaArchive> modules = new ArrayList<>();
		for (File file : files) {
			if (file.getName().startsWith("mobi.chouette.exchange"))
			{
				String name = file.getName().split("\\-")[0]+".jar";
				
				JavaArchive archive = ShrinkWrap
						  .create(ZipImporter.class, name)
						  .importFrom(file)
						  .as(JavaArchive.class);
				modules.add(archive);
			}
			else
			{
				jars.add(file);
			}
		}
		
		File[] filesDao = Maven.resolver().loadPomFromFile("pom.xml")
				.resolve("mobi.chouette:mobi.chouette.dao").withTransitivity().asFile();
		if (filesDao.length == 0) 
		{
			throw new NullPointerException("no dao");
		}
		for (File file : filesDao) {
			if (file.getName().startsWith("mobi.chouette.dao"))
			{
				String name = file.getName().split("\\-")[0]+".jar";
				
				JavaArchive archive = ShrinkWrap
						  .create(ZipImporter.class, name)
						  .importFrom(file)
						  .as(JavaArchive.class);
				modules.add(archive);
				if (!modules.contains(archive))
				   modules.add(archive);
			}
			else
			{
				if (!jars.contains(file))
				   jars.add(file);
			}
		}

        

		final WebArchive testWar = ShrinkWrap.create(WebArchive.class, "test.war").addAsWebInfResource("postgres-ds.xml")
				.addClass(GtfsExportTests.class)
				.addClass(GtfsTestsUtils.class)
				.addClass(DummyChecker.class)
				.addClass(JobDataTest.class);
		
		result = ShrinkWrap.create(EnterpriseArchive.class, "test.ear")
				.addAsLibraries(jars.toArray(new File[0]))
				.addAsModules(modules.toArray(new JavaArchive[0]))
				.addAsModule(testWar)
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
		configuration.setCleanRepository(true);
		configuration.setNoSave(false);
		context.put(CONFIGURATION, configuration);
		configuration.setName("name");
		configuration.setUserName("userName");
		configuration.setNoSave(true);
		configuration.setOrganisationName("organisation");
		configuration.setReferentialName("test");
		JobDataTest test = new JobDataTest();
		context.put(JOB_DATA, test);
		test.setPathName( "target/referential/test");
		File f = new File("target/referential/test");
		if (f.exists())
			try {
				FileUtils.deleteDirectory(f);
			} catch (IOException e) {
				e.printStackTrace();
			}
		f.mkdirs();
		test.setReferential( "chouette_gui");
		test.setAction( IMPORTER);
		test.setType( "neptune");
		context.put("testng", "true");
		context.put(OPTIMIZED, Boolean.FALSE);
		return context;

	}

	protected Context initExportContext() {
		init();
		ContextHolder.setContext("chouette_gui"); // set tenant schema

		Context context = new Context();
		context.put(INITIAL_CONTEXT, initialContext);
		context.put(REPORT, new ActionReport());
		context.put(MAIN_VALIDATION_REPORT, new ValidationReport());
		GtfsExportParameters configuration = new GtfsExportParameters();
		context.put(CONFIGURATION, configuration);
		configuration.setName("name");
		configuration.setUserName("userName");
		configuration.setOrganisationName("organisation");
		configuration.setReferentialName("test");
		JobDataTest test = new JobDataTest();
		context.put(JOB_DATA, test);
		test.setPathName("target/referential/test");
		test.setFilename( "gtfs.zip");
		File f = new File("target/referential/test");
		if (f.exists())
			try {
				FileUtils.deleteDirectory(f);
			} catch (IOException e) {
				e.printStackTrace();
			}
		f.mkdirs();
		test.setReferential( "chouette_gui");
		test.setAction( EXPORTER);
		test.setType("gtfs");
		context.put("testng", "true");
		context.put(OPTIMIZED, Boolean.FALSE);
		return context;

	}

   @Test(groups = { "export" }, description = "test export GTFS Line")
   public void verifyExportLines() throws Exception
   {
		// save data
		importLines("test_neptune.zip",6,6);

		// export data
		Context context = initExportContext();
		GtfsExportParameters configuration = (GtfsExportParameters) context.get(CONFIGURATION);
		configuration.setAddMetadata(true);
		configuration.setReferencesType("line");
		configuration.setObjectIdPrefix("CITURA");
		configuration.setTimeZone("Europe/Paris");
		Command command = (Command) CommandFactory.create(initialContext,
				GtfsExporterCommand.class.getName());

		try {
			command.execute(context);
		} catch (Exception ex) {
			log.error("test failed", ex);
			throw ex;
		}
		
		ActionReport report = (ActionReport) context.get(REPORT);
		Assert.assertEquals(report.getResult(), STATUS_OK, "result");
		for (FileInfo info : report.getFiles()) {
			Reporter.log(info.toString(),true);
		}
		Assert.assertEquals(report.getFiles().size(), 6, "file reported");
		for (LineInfo info : report.getLines()) {
			Reporter.log(info.toString(),true);
		}
		Assert.assertEquals(report.getLines().size(), 6, "line reported");
		Reporter.log("report line :" + report.getLines().get(0).toString(), true);
		Assert.assertEquals(report.getLines().get(0).getStatus(), LINE_STATE.OK, "line status");

   }

   @Test(groups = { "export" }, description = "test export GTFS StopAreas")
   public void verifyExportStopAreas() throws Exception
   {
		// save data
		importLines("test_neptune.zip",6,6);

		// export data
		Context context = initExportContext();
		GtfsExportParameters configuration = (GtfsExportParameters) context.get(CONFIGURATION);
		configuration.setAddMetadata(true);
		configuration.setReferencesType("stop_area");
		configuration.setObjectIdPrefix("CITURA");
		configuration.setTimeZone("Europe/Paris");
		Command command = (Command) CommandFactory.create(initialContext,
				GtfsExporterCommand.class.getName());

		try {
			command.execute(context);
		} catch (Exception ex) {
			log.error("test failed", ex);
			throw ex;
		}
		
		ActionReport report = (ActionReport) context.get(REPORT);
		Assert.assertEquals(report.getResult(), STATUS_OK, "result");
		for (FileInfo info : report.getFiles()) {
			Reporter.log(info.toString(),true);
		}
		Assert.assertEquals(report.getFiles().size(), 1, "file reported");
		
		Assert.assertEquals(report.getLines().size(), 0, "line reported");


   }

    @EJB 
    protected LineDAO lineDAO;
   
    @PersistenceContext(unitName = "referential")
    EntityManager em;

    @Inject
    UserTransaction utx;
   
    @Test(groups = { "export" }, description = "test not export GTFS Line than has no Company")
    public void verifyNotExportLineWithNoCompany() throws Exception
    {
	// save data
	importLines("test_neptune.zip",6,6);
	// export data
	Context context = initExportContext();

               
	utx.begin();
	em.joinTransaction();
	Line myLine = lineDAO.findByObjectId("CITURA:Line:01");
	myLine.setCompany(null);
	String myLineName = myLine.getName() + " (01)";
	utx.commit();
               
	GtfsExportParameters configuration = (GtfsExportParameters) context.get(CONFIGURATION);
	configuration.setAddMetadata(true);
	configuration.setReferencesType("line");
	configuration.setObjectIdPrefix("CITURA");
	configuration.setTimeZone("Europe/Paris");
	Command command = (Command) CommandFactory.create(initialContext,
							  GtfsExporterCommand.class.getName());

	try {
	    command.execute(context);
	} catch (Exception ex) {
	    log.error("test failed", ex);
	    throw ex;
	}
               
	ActionReport report = (ActionReport) context.get(REPORT);
               
	Assert.assertEquals(report.getResult(), STATUS_OK, "result");
	for (FileInfo info : report.getFiles()) {
	    Reporter.log(info.toString(),true);
	}
	Assert.assertEquals(report.getFiles().size(), 6, "file reported");
	for (LineInfo info : report.getLines()) {
	    Reporter.log(info.toString(),true);
	}
	Assert.assertEquals(report.getLines().size(), 6, "line reported");
	for (int i = 0; i < 6; i++) {
	    Reporter.log("report line :" + report.getLines().get(i).toString(), true);
	    if (myLineName.equals(report.getLines().get(i).getName()))
		Assert.assertEquals(report.getLines().get(i).getStatus(), LINE_STATE.ERROR, "no company for this line");
	    else
		Assert.assertEquals(report.getLines().get(i).getStatus(), LINE_STATE.OK, "line status");
	}

    }



	private void importLines(String file, int fileCount, int lineCount) throws Exception
	{
		Context context = initImportContext();


		NeptuneImporterCommand command = (NeptuneImporterCommand) CommandFactory.create(initialContext,
				NeptuneImporterCommand.class.getName());
		GtfsTestsUtils.copyFile(file);
		JobDataTest test = (JobDataTest) context.get(JOB_DATA);
		test.setFilename( file);
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
		Reporter.log(report.toString(),true);
		ValidationReport valReport = (ValidationReport) context.get(MAIN_VALIDATION_REPORT);
		for (CheckPoint cp : valReport.getCheckPoints()) 
		{
			if (cp.getState().equals(CheckPoint.RESULT.NOK))
			{
				Reporter.log(cp.toString(),true);
			}
		}
		Assert.assertEquals(report.getResult(), STATUS_OK, "result");
		Assert.assertEquals(report.getFiles().size(), fileCount, "file reported");
		Assert.assertEquals(report.getLines().size(), lineCount, "line reported");
		for (LineInfo info : report.getLines()) {
			Assert.assertEquals(info.getStatus(), LINE_STATE.OK, "line status");
		}


	}

}
