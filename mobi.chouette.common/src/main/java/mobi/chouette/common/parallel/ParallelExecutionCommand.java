package mobi.chouette.common.parallel;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.Context;
import mobi.chouette.common.Pair;
import mobi.chouette.common.chain.Command;
import mobi.chouette.common.chain.CommandFactory;
import mobi.chouette.common.monitor.JamonUtils;

import javax.naming.InitialContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Log4j
public class ParallelExecutionCommand implements Command {

	public static final String COMMAND = "ParallelExecutionCommand";

	private List<Pair<Command, Function<Context, Context>>> commands = new ArrayList<>();

	private static final int DEFAULT_TIMEOUT_SECONDS = 3600;
	@Getter
	@Setter
	private int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

	public void add(Command command, Function<Context, Context> contextIntializer) {
		commands.add(Pair.of(command, contextIntializer));
	}

	@Override
	public boolean execute(Context context) throws Exception {

		if (context == null) {
			throw new IllegalArgumentException("Context is null");
		}
		if (commands.size() == 0) {
			throw new IllegalStateException("No command to execute");
		}
		boolean result = SUCCESS;
		Monitor monitor = MonitorFactory.start(COMMAND);

		final AtomicInteger counter = new AtomicInteger(0);
		ThreadFactory threadFactory = r -> {
			Thread t = new Thread(r);
			t.setName("parallel-execution-command-thread-" + (counter.incrementAndGet()));
			t.setPriority(Thread.MIN_PRIORITY);
			return t;
		};
		int processors = Math.min(commands.size(), Runtime.getRuntime().availableProcessors());
		ExecutorService executor = Executors.newFixedThreadPool(processors, threadFactory);

		try {
			List<Future<Boolean>> commandExecutionResults = new ArrayList<>();

			for (Pair<Command, Function<Context, Context>> commandWithContext : commands) {
				Command command = commandWithContext.getLeft();
				Function<Context, Context> contextInitializer = commandWithContext.getRight();
				Context commandContext = contextInitializer.apply(context);
				commandExecutionResults.add(executor.submit(new CommandTask(command, commandContext)));
			}

			executor.shutdown();

			boolean completed = executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS);

			if (completed) {
				for (Future<Boolean> commandResult : commandExecutionResults) {
					if (commandResult.get() == ERROR) {
						result = ERROR;
					}
				}
			} else {
				log.warn(COMMAND + " failed to complete within " + timeoutSeconds + " seconds");
				executor.shutdownNow();
				result = ERROR;
			}
		} catch (Exception e) {
			log.error("Parallel command execution failed ", e);
			result = ERROR;
		} finally {
			executor.shutdown();
			JamonUtils.logMagenta(log, monitor);
		}


		return result;
	}

	private static class CommandTask implements Callable<Boolean> {

		private Command command;

		private Context context;

		public CommandTask(Command command, Context context) {
			this.command = command;
			this.context = context;
		}

		@Override
		public Boolean call() throws Exception {
			try {
				return command.execute(context);
			} catch (Exception e) {
				log.warn("Command executed as a part of ParallelExecutionCommand failed: " + e.getMessage(), e);
				return false;
			}
		}
	}

	public static class DefaultCommandFactory extends CommandFactory {

		@Override
		protected Command create(InitialContext context) throws IOException {
			Command result = new ParallelExecutionCommand();
			return result;
		}
	}

	static {
		CommandFactory.factories.put(ParallelExecutionCommand.class.getName(),
				new ParallelExecutionCommand.DefaultCommandFactory());
	}
}
