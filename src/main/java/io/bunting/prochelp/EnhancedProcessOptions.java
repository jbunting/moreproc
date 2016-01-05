package io.bunting.prochelp;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import io.bunting.prochelp.PipeHandler.Stream;
import jnr.enxio.channels.NativeDeviceChannel;
import jnr.enxio.channels.NativeSelectorProvider;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import jnr.posix.SpawnFileAction;

import static jnr.posix.SpawnFileAction.close;
import static jnr.posix.SpawnFileAction.dup;

/**
 * A class that holds the majority of the logic for {@link EnhancedProcessBuilder}. It exists so that this logic can be separated from the re-implementation of the JDK's
 * {@link ProcessBuilder} API.
 */
class EnhancedProcessOptions
{
	private final POSIX posix = POSIXFactory.getPOSIX();
	private final List<String> commands;

	EnhancedProcessOptions(final List<String> commands)
	{
		this.commands = commands;
	}

	private EnhancedProcess doStart()
	{
		final PipeHandler inPipeHandler = new DefaultPipeHandler();
		final PipeHandler outPipeHandler = new DefaultPipeHandler();
		final PipeHandler errPipeHandler = new DefaultPipeHandler();

		// we have to create the environment variables manually
		List<String> childEnvVars = System.getenv().entrySet()
		                          .stream()
		                          .map(e -> e.getKey() + "=" + e.getValue())
		                          .collect(Collectors.toList());

		List<SpawnFileAction> spawnFileActions = new ArrayList<>();

		spawnFileActions.addAll(inPipeHandler.init(posix, Stream.IN));
		spawnFileActions.addAll(outPipeHandler.init(posix, Stream.OUT));
		spawnFileActions.addAll(errPipeHandler.init(posix, Stream.ERR));

		long pid = posix.posix_spawnp(commands.get(0),
		                              spawnFileActions,
		                              commands,
		                              childEnvVars);

		final OutputStream in = Channels.newOutputStream(inPipeHandler.afterSpawn(posix, Stream.IN));
		final InputStream out = Channels.newInputStream(outPipeHandler.afterSpawn(posix, Stream.OUT));
		final InputStream err = Channels.newInputStream(errPipeHandler.afterSpawn(posix, Stream.ERR));

		return new EnhancedProcess(pid, in, out, err, posix);
	}

	public <T> ProcessCallable<T> create(Function<EnhancedProcess, T> completion)
	{
		return this.doCreate(completion, this::doStart);
	}

	private <T> EnhancedProcessInvoker<T> doCreate(Function<EnhancedProcess, T> completion, final ProcessStarter processStarter)
	{
		final List<Monitor> monitors = detectMonitors();

		return new EnhancedProcessInvoker<>(monitors, completion, processStarter);
	}

	private List<Monitor> detectMonitors()
	{
		return Collections.emptyList();
	}

	private class EnhancedProcessInvoker<T> extends AbstractSettableFuture<EnhancedProcess> implements ProcessCallable<T>
	{
		private final List<Monitor> monitors;
		private final Function<EnhancedProcess, T> completion;
		private final ProcessStarter processStarter;

		public EnhancedProcessInvoker(final List<Monitor> monitors, final Function<EnhancedProcess, T> completion, final ProcessStarter processStarter)
		{
			this.monitors = monitors;
			this.completion = completion;
			this.processStarter = processStarter;
		}

		@Nonnull
		@Override
		protected EnhancedProcess computeValue() throws Exception
		{
			return processStarter.invoke();
		}

		@Override
		public T call() throws InterruptedException
		{
			final EnhancedProcess process = this.doCompute();

			for (Monitor monitor : monitors)
			{
				monitor.setup(process);
			}
			while (process.isAlive())
			{
				for (Monitor monitor : monitors)
				{
					monitor.update(process);
				}
				process.waitFor(100, TimeUnit.MILLISECONDS);
			}
			for (Monitor monitor : monitors)
			{
				monitor.cleanup(process);
			}
			return completion.apply(process);
		}
	}
}
