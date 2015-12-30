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
	private static final int PIPE_READ_SIDE = 0;
	private static final int PIPE_WRITE_SIDE = 1;

	private final POSIX posix = POSIXFactory.getPOSIX();
	private final List<String> commands;

	EnhancedProcessOptions(final List<String> commands)
	{
		this.commands = commands;
	}

	private EnhancedProcess doStart()
	{
		// for each stream we want to create in the child process, we have to:
		// 1. create a pipe
		// 2. duplicate the child's side when spawning the process
		// 3. close the parent's side when spawning the process
		// 4. close the child's side in the parent after the process is spawned
		// 5. convert the parent's side to a usable channel or stream on the parent's side

		int[] stdin = new int[2];
		int[] stdout = new int[2];
		int[] stderr = new int[2];

		// Step 1
		posix.pipe(stdin);
		posix.pipe(stdout);
		posix.pipe(stderr);

		// we have to create the environment variables manually
		List<String> childEnvVars = System.getenv().entrySet()
		                          .stream()
		                          .map(e -> e.getKey() + "=" + e.getValue())
		                          .collect(Collectors.toList());

		List<SpawnFileAction> spawnFileActions = Arrays.asList(
				// Step 2
				dup(stdin[PIPE_READ_SIDE], 0),
				dup(stdout[PIPE_WRITE_SIDE], 1),
				dup(stderr[PIPE_WRITE_SIDE], 2),
				// Step 3
				close(stdin[PIPE_WRITE_SIDE]),
				close(stdout[PIPE_READ_SIDE]),
				close(stderr[PIPE_READ_SIDE])
		);
		long pid = posix.posix_spawnp(commands.get(0),
		                              spawnFileActions,
		                              commands,
		                              childEnvVars);

		// Step 4
		posix.close(stdin[PIPE_READ_SIDE]);
		posix.close(stdout[PIPE_WRITE_SIDE]);
		posix.close(stderr[PIPE_WRITE_SIDE]);

		// Step 5
		final OutputStream in = Channels.newOutputStream(new NativeDeviceChannel(NativeSelectorProvider.getInstance(), stdin[1], SelectionKey.OP_WRITE));
		final InputStream out = Channels.newInputStream(new NativeDeviceChannel(NativeSelectorProvider.getInstance(), stdout[0], SelectionKey.OP_READ));
		final InputStream err = Channels.newInputStream(new NativeDeviceChannel(NativeSelectorProvider.getInstance(), stderr[0], SelectionKey.OP_READ));

		return new EnhancedProcess(pid, in, out, err, posix);
	}

	public <T> T invokeSynchronously(Function<EnhancedProcess, T> completion) throws InterruptedException
	{
		return this.doCreate(completion, this::doStart).call();
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
				Thread.sleep(100);
//				process.waitFor(100, TimeUnit.MILLISECONDS);
			}
			for (Monitor monitor : monitors)
			{
				monitor.cleanup(process);
			}
			return completion.apply(process);
		}
	}
}
