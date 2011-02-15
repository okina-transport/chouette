/**
 * Projet CHOUETTE
 *
 * ce projet est sous license libre
 * voir LICENSE.txt pour plus de details
 *
 */
package fr.certu.chouette.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import fr.certu.chouette.common.ChouetteException;
import fr.certu.chouette.filter.DetailLevelEnum;
import fr.certu.chouette.filter.Filter;
import fr.certu.chouette.filter.FilterOrder;
import fr.certu.chouette.manager.INeptuneManager;
import fr.certu.chouette.model.neptune.NeptuneIdentifiedObject;
import fr.certu.chouette.model.neptune.NeptuneObject;
import fr.certu.chouette.plugin.exchange.FormatDescription;
import fr.certu.chouette.plugin.exchange.ListParameterValue;
import fr.certu.chouette.plugin.exchange.ParameterDescription;
import fr.certu.chouette.plugin.exchange.ParameterValue;
import fr.certu.chouette.plugin.exchange.SimpleParameterValue;
import fr.certu.chouette.plugin.report.Report;
import fr.certu.chouette.plugin.report.Report.STATE;
import fr.certu.chouette.plugin.report.ReportHolder;
import fr.certu.chouette.plugin.report.ReportItem;
import fr.certu.chouette.plugin.validation.ValidationParameters;

/**
 *
 */
@NoArgsConstructor
public class Command
{

	private static ClassPathXmlApplicationContext applicationContext;

	@Setter private Map<String,INeptuneManager<NeptuneIdentifiedObject>> managers;

	@Setter private ValidationParameters validationParameters;

	private Map<String,List<String>> globals = new HashMap<String, List<String>>();;

	private static Map<String,String> shortCuts ;

	static
	{
		shortCuts = new HashMap<String, String>();
		shortCuts.put("c", "command");
		shortCuts.put("h", "help");
		shortCuts.put("o", "object");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		// pattern partially work
		String[] context = {"classpath*:/chouetteContext.xml"};

		if (args.length >= 1) 
		{
			if (args[0].equalsIgnoreCase("-help") ||  args[0].equalsIgnoreCase("-h") )
			{
				printHelp();
				System.exit(0);
			}

			if (args[0].equalsIgnoreCase("-noDao"))
			{
				List<String> newContext = new ArrayList<String>();
				PathMatchingResourcePatternResolver test = new PathMatchingResourcePatternResolver();
				try
				{
					Resource[] re = test.getResources("classpath*:/chouetteContext.xml");
					for (Resource resource : re)
					{
						if (! resource.getURL().toString().contains("dao"))
						{
							newContext.add(resource.getURL().toString());
						}
					}
					context = newContext.toArray(new String[0]);

				} 
				catch (Exception e) 
				{

					System.err.println("cannot remove dao : "+e.getLocalizedMessage());
				}
			}
			applicationContext = new ClassPathXmlApplicationContext(context);
			ConfigurableBeanFactory factory = applicationContext.getBeanFactory();
			Command command = (Command) factory.getBean("Command");
			command.execute(args);
		}
		else
		{
			printHelp();
		}
	}

	/**
	 * @param args
	 */
	private void execute(String[] args)
	{
		List<CommandArgument> commands = parseArgs(args);

		for (String key : globals.keySet())
		{
			System.out.println("global parameters "+key+" : "+ Arrays.toString(globals.get(key).toArray()));
		}

		try
		{
			if (getBoolean(globals,"help"))
			{
				printHelp();
				return;
			}
			List<NeptuneIdentifiedObject> beans = null;
			for (CommandArgument command : commands) 
			{
				String name = command.getName();
				Map<String, List<String>> parameters = command.getParameters();
				System.out.println("Command "+name);
				for (String key : parameters.keySet())
				{
					System.out.println("    parameters "+key+" : "+ Arrays.toString(parameters.get(key).toArray()));
				}

				INeptuneManager<NeptuneIdentifiedObject> manager = getManager(parameters);

				if (name.equals("get"))
				{
					beans = executeGet(manager,parameters);
				}
				else if (name.equals("getImportFormats"))
				{
					executeGetImportFormats(manager,parameters);
				}
				else if (name.equals("import"))
				{
					beans = executeImport(manager,parameters);
				}
				else if (name.equals("print"))
				{
					if (beans == null) throw new Exception("Invalid command sequence : print must follow a loading command");
					executePrint(beans,parameters);
				}
				else if (name.equals("validate"))
				{
					if (beans == null) throw new Exception("Invalid command sequence : validate must follow a loading command");
					executeValidate(beans,manager,parameters);
				}
				else if (name.equals("getExportFormats"))
				{
					executeGetExportFormats(manager,parameters);
				}
				else if (name.equals("export"))
				{
					if (beans == null) throw new Exception("Invalid command sequence : export must follow a loading command");
					executeExport(beans,manager,parameters);
				}
				else
				{
					System.out.println("invalid command :" +command);
				}
			}

		}
		catch (Exception e)
		{
			if (getBoolean(globals,"help"))
			{
				printHelp();
			}
			else
			{
				System.err.println(e.getMessage());
				System.err.println(e.getLocalizedMessage());
				e.printStackTrace();
			}
		}

	}

	private void executeExport(List<NeptuneIdentifiedObject> beans,
			INeptuneManager<NeptuneIdentifiedObject> manager,
			Map<String, List<String>> parameters) 
	{
		String format = getSimpleString(parameters,"format");
		try
		{
			List<FormatDescription> formats = manager.getExportFormats(null);
			FormatDescription description = null;

			for (FormatDescription formatDescription : formats)
			{
				if (formatDescription.getName().equalsIgnoreCase(format))
				{
					description=formatDescription;
					break;
				}
			}
			if (description == null)
			{
				throw new IllegalArgumentException("format "+format+" unavailable, check command getImportFormats for list ");
			}


			List<ParameterValue> values = new ArrayList<ParameterValue>();
			for (ParameterDescription desc : description.getParameterDescriptions())
			{
				String name = desc.getName();
				String key = name.toLowerCase();
				List<String> vals = parameters.get(key);
				if (vals == null)
				{
					if (desc.isMandatory())
					{
						throw new IllegalArgumentException("parameter -"+name+" is required, check command getImportFormats for list ");
					}
				}
				else
				{
					if (desc.isCollection())
					{
						ListParameterValue val = new ListParameterValue(name);
						switch (desc.getType())
						{
						case FILEPATH : val.setFilepathList(vals); break;
						case STRING : val.setStringList(vals); break;
						case FILENAME : val.setFilenameList(vals); break;
						}
						values.add(val);
					}
					else
					{
						if (vals.size() != 1)
						{
							throw new IllegalArgumentException("parameter -"+name+" must be unique, check command getImportFormats for list ");
						}
						String simpleval = vals.get(0);

						SimpleParameterValue val = new SimpleParameterValue(name);
						switch (desc.getType())
						{
						case FILEPATH : val.setFilepathValue(simpleval); break;
						case STRING : val.setStringValue(simpleval); break;
						case FILENAME : val.setFilenameValue(simpleval); break;
						case BOOLEAN : val.setBooleanValue(Boolean.parseBoolean(simpleval)); break;
						case INTEGER : val.setIntegerValue(Long.parseLong(simpleval)); break;
						}
						values.add(val);
					}
				}
			}

			ReportHolder holder = new ReportHolder();
			manager.doExport(null, beans, format, values, holder );
			if (holder.getReport() != null)
			{
				Report r = holder.getReport();
				System.out.println(r.getLocalizedMessage());
				printItems("",r.getItems());
			}
		}
		catch (ChouetteException e)
		{
			System.err.println(e.getMessage());

			Throwable caused = e.getCause();
			while (caused != null)
			{
				System.err.println("caused by "+ caused.getMessage());
				caused = caused.getCause();
			}
			throw new RuntimeException("export failed");
		}
	}

	private void executeGetExportFormats(
			INeptuneManager<NeptuneIdentifiedObject> manager,
			Map<String, List<String>> parameters) 
	throws ChouetteException 
	{

		List<FormatDescription> formats = manager.getExportFormats(null);
		for (FormatDescription formatDescription : formats)
		{
			System.out.println(formatDescription);
		}


	}

	/**
	 * @param parameters
	 * @return
	 */
	private INeptuneManager<NeptuneIdentifiedObject> getManager(Map<String, List<String>> parameters) 
	{
		String object = null;
		try
		{
			object = getSimpleString(parameters,"object");
		}
		catch (IllegalArgumentException e)
		{
			object = getSimpleString(globals,"object");
		}
		INeptuneManager<NeptuneIdentifiedObject> manager = managers.get(object);
		if (manager == null)
		{
			throw new IllegalArgumentException("unknown object "+object+ ", only "+Arrays.toString(managers.keySet().toArray())+" are managed");
		}
		return manager;
	}

	private List<NeptuneIdentifiedObject> executeImport(INeptuneManager<NeptuneIdentifiedObject> manager, Map<String, List<String>> parameters)
	{
		String format = getSimpleString(parameters,"format");
		try
		{
			List<FormatDescription> formats = manager.getImportFormats(null);
			FormatDescription description = null;

			for (FormatDescription formatDescription : formats)
			{
				if (formatDescription.getName().equalsIgnoreCase(format))
				{
					description=formatDescription;
					break;
				}
			}
			if (description == null)
			{
				throw new IllegalArgumentException("format "+format+" unavailable, check command getImportFormats for list ");
			}


			List<ParameterValue> values = new ArrayList<ParameterValue>();
			for (ParameterDescription desc : description.getParameterDescriptions())
			{
				String name = desc.getName();
				String key = name.toLowerCase();
				List<String> vals = parameters.get(key);
				if (vals == null)
				{
					if (desc.isMandatory())
					{
						throw new IllegalArgumentException("parameter -"+name+" is required, check command getImportFormats for list ");
					}
				}
				else
				{
					if (desc.isCollection())
					{
						ListParameterValue val = new ListParameterValue(name);
						switch (desc.getType())
						{
						case FILEPATH : val.setFilepathList(vals); break;
						case STRING : val.setStringList(vals); break;
						case FILENAME : val.setFilenameList(vals); break;
						}
						values.add(val);
					}
					else
					{
						if (vals.size() != 1)
						{
							throw new IllegalArgumentException("parameter -"+name+" must be unique, check command getImportFormats for list ");
						}
						String simpleval = vals.get(0);

						SimpleParameterValue val = new SimpleParameterValue(name);
						switch (desc.getType())
						{
						case FILEPATH : val.setFilepathValue(simpleval); break;
						case STRING : val.setStringValue(simpleval); break;
						case FILENAME : val.setFilenameValue(simpleval); break;
						case BOOLEAN : val.setBooleanValue(Boolean.parseBoolean(simpleval)); break;
						case INTEGER : val.setIntegerValue(Long.parseLong(simpleval)); break;
						}
						values.add(val);
					}
				}
			}

			ReportHolder holder = new ReportHolder();
			List<NeptuneIdentifiedObject> beans = manager.doImport(null, format, values,holder);
			if (holder.getReport() != null)
			{
				Report r = holder.getReport();
				System.out.println(r.getLocalizedMessage());
				printItems("",r.getItems());

			}
			if (beans == null )
			{
				System.out.println("import failed");
			}

			else
			{
				System.out.println("beans count = "+beans.size());

				if (getBoolean(parameters,"validate"))
				{
					executeValidate(beans, manager,parameters);


				}
			}

			return beans;

		}
		catch (ChouetteException e)
		{
			System.err.println(e.getMessage());

			Throwable caused = e.getCause();
			while (caused != null)
			{
				System.err.println("caused by "+ caused.getMessage());
				caused = caused.getCause();
			}
			throw new RuntimeException("import failed");
		}


	}

	/**
	 * @param beans
	 * @param manager
	 * @param parameters 
	 * @throws ChouetteException
	 */
	private void executeValidate(List<NeptuneIdentifiedObject> beans,
			INeptuneManager<NeptuneIdentifiedObject> manager, 
			Map<String, List<String>> parameters)
	throws ChouetteException 
	{
		Report valReport = manager.validate(null, beans, validationParameters);
		System.out.println(valReport.getLocalizedMessage());
		printItems("",valReport.getItems());
		int nbUNCHECK = 0;
		int nbOK = 0;
		int nbWARN = 0;
		int nbERROR = 0;
		int nbFATAL = 0;
		for (ReportItem item1  : valReport.getItems()) // Categorie
		{
			for (ReportItem item2 : item1.getItems()) // fiche
			{
				for (ReportItem item3 : item2.getItems()) //test
				{
					STATE status = item3.getStatus();
					switch (status)
					{
					case UNCHECK : nbUNCHECK++; break;
					case OK : nbOK++; break;
					case WARNING : nbWARN++; break;
					case ERROR : nbERROR++; break;
					case FATAL : nbFATAL++; break;
					}

				}

			}
		}
		System.out.println("Bilan : "+nbOK+" tests ok, "+nbWARN+" warnings, "+nbERROR+" erreurs, "+nbUNCHECK+" non effectués");
	}

	private void printItems(String indent,List<ReportItem> items) 
	{
		if (items == null) return;
		for (ReportItem item : items) 
		{
			System.out.println(indent+item.getStatus().name()+" : "+item.getLocalizedMessage());
			printItems(indent+"   ",item.getItems());
		}

	}

	private void executeGetImportFormats(INeptuneManager<NeptuneIdentifiedObject> manager, Map<String, List<String>> parameters) throws ChouetteException
	{

		List<FormatDescription> formats = manager.getImportFormats(null);
		for (FormatDescription formatDescription : formats)
		{
			System.out.println(formatDescription);
		}


	}

	/**
	 * @param manager
	 * @param parameters 
	 * @return 
	 * @throws ChouetteException
	 */
	private List<NeptuneIdentifiedObject> executeGet(INeptuneManager<NeptuneIdentifiedObject> manager, Map<String, List<String>> parameters)
	throws ChouetteException
	{

		Filter filter = null;
		if (parameters.containsKey("id"))
		{
			List<String> sids = parameters.get("id");
			List<Long> ids = new ArrayList<Long>();

			for (String id : sids)
			{
				// Filter filter = Filter.getNewEqualsFilter("id", Long.valueOf(id));
				ids.add(Long.valueOf(id));
				// System.out.println("search for id "+Long.valueOf(id)+ "("+id+")");
				// NeptuneBean bean = manager.get(null, filter, NeptuneBeanManager.DETAIL_LEVEL.ATTRIBUTE);
				// System.out.println(bean);
			}
			filter = Filter.getNewInFilter("id", ids);
		}
		else if (parameters.containsKey("objectid"))
		{
			List<String> sids = parameters.get("objectid");
			filter = Filter.getNewInFilter("objectId", sids);
		}
		else
		{
			filter = Filter.getNewEmptyFilter();
		}

		if (parameters.containsKey("orderby"))
		{
			List<String> orderFields = parameters.get("orderby");

			boolean desc = getBoolean(parameters,"desc");

			if (desc)
			{
				for (String field : orderFields)
				{
					filter.addOrder(FilterOrder.desc(field));
				}
			}
			else
			{
				for (String field : orderFields)
				{
					filter.addOrder(FilterOrder.asc(field));
				}
			}
		}

		DetailLevelEnum level = DetailLevelEnum.ATTRIBUTE;
		if (parameters.containsKey("level"))
		{
			String slevel = getSimpleString(parameters,"level");
			if (slevel.equalsIgnoreCase("narrow"))
			{
				level = DetailLevelEnum.NARROW_DEPENDENCIES;
			}
			else if (slevel.equalsIgnoreCase("structure"))
			{
				level = DetailLevelEnum.STRUCTURAL_DEPENDENCIES;
			}
		}

		List<NeptuneIdentifiedObject> beans = manager.getAll(null, filter, level);

		System.out.println("beans count = "+beans.size());
		return beans;
	}

	/**
	 * @param beans
	 * @param parameters 
	 */
	private void executePrint(List<NeptuneIdentifiedObject> beans, Map<String, List<String>> parameters) 
	{
		String slevel = getSimpleString(parameters, "level", "99");
		int level = Integer.parseInt(slevel);
		for (NeptuneObject bean : beans)
		{
			System.out.println(bean.toString("", level));
		}
	}


	/**
	 *
	 */
	private static void printHelp()
	{
		System.out.println("Arguments : ");
		System.out.println("  -h(elp) for general syntax ");
		System.out.println("  -noDao to invalidate database access (MUST BE FIRST ARGUMENT) ");
		System.out.println("  -o(bject) neptuneObjectName (default object for commands)");
		System.out.println("  -c(ommand) [commandName] : get, getImportFormats, import, validate, getExportFormats, export, print");
		System.out.println("     get : ");
		System.out.println("        -id [value+] : object technical id ");
		System.out.println("        -objectId [value+] : object neptune id ");
		System.out.println("        -level [attribute|narrow|full] : detail level (default = attribute)");
		System.out.println("        -orderBy [value+] : sort fields ");
		System.out.println("        -asc|-desc sort order (default = asc) ");
		System.out.println("     import|export : ");
		System.out.println("        -format formatName : format name");
		System.out.println("        launch getImportFormats or getExportFormats for other parameters");
		System.out.println("     print : ");
		System.out.println("        -level level : deep level for recursive print (default = 99)");
		System.out.println("Notes: ");
		System.out.println("    -c(ommand) can be chained : new occurence of command must be followed by it's specific argument");
		System.out.println("               commands are executed in argument order ");
		System.out.println("               last returned objects of reading commands are send to command wich needs objects as imput");
		System.out.println("    -o(bject) argument may be added for each command to switch object types");
	}

	/**
	 * @param string
	 * @return
	 */
	private String getSimpleString(Map<String, List<String>> parameters,String key)
	{
		List<String> values = parameters.get(key);
		if (values == null) throw new IllegalArgumentException("parameter -"+key+" of String type is required");
		if (values.size() > 1) throw new IllegalArgumentException("parameter -"+key+" of String type must be unique");
		return values.get(0);
	}

	/**
	 * @param string
	 * @return
	 */
	private String getSimpleString(Map<String, List<String>> parameters,String key,String defaultValue)
	{
		List<String> values = parameters.get(key);
		if (values == null) return defaultValue;
		if (values.size() > 1) throw new IllegalArgumentException("parameter -"+key+" of String type must be unique");
		return values.get(0);
	}

	/**
	 * @param string
	 * @return
	 */
	private boolean getBoolean(Map<String, List<String>> parameters,String key)
	{
		List<String> values = parameters.get(key);
		if (values == null) return false;
		if (values.size() > 1) throw new IllegalArgumentException("parameter -"+key+" of boolean type must be unique");
		return Boolean.parseBoolean(values.get(0));
	}

	private List<CommandArgument> parseArgs(String[] args)
	{
		Map<String, List<String>> parameters = globals;
		List<CommandArgument> commands = new ArrayList<CommandArgument>();
		CommandArgument command = null;
		if (args.length == 0)
		{
			List<String> list = new ArrayList<String>();
			list.add("true");
			parameters.put("help", list);
		}
		for (int i = 0; i < args.length; i++)
		{
			if (args[i].startsWith("-"))
			{
				String key = args[i].substring(1).toLowerCase();
				if (key.length() == 1) 
				{
					String alias = shortCuts.get(key);
					if (alias != null) key = alias;
				}
				if (key.equals("command")) 
				{
					if (i == args.length -1) 
					{
						System.err.println("missing command name");
						System.exit(2);
					}
					String name = args[++i];
					if (name.startsWith("-"))
					{
						System.err.println("missing command name before "+name);
						System.exit(2);						
					}
					command = new CommandArgument(name);
					parameters = command.getParameters();
					commands.add(command);
				}
				else
				{
					if (parameters.containsKey(key))
					{
						System.err.println("duplicate parameter : -"+key);
						System.exit(2);
					}
					List<String> list = new ArrayList<String>();

					if (i == args.length -1 || args[i+1].startsWith("-"))
					{
						list.add("true");
					}
					else
					{
						while ((i+1 < args.length && !args[i+1].startsWith("-")))
						{
							list.add(args[++i]);
						}
					}
					parameters.put(key,list);
				}
			}
		}

		return commands;
	}

}
