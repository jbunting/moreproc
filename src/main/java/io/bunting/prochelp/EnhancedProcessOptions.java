package io.bunting.prochelp;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.bunting.prochelp.PipeHandler.Stream;
import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import jnr.posix.SpawnFileAction;

/**
 * A class that holds the majority of the logic for {@link EnhancedProcessBuilder}. It exists so that this logic can be separated from the re-implementation of the JDK's
 * {@link ProcessBuilder} API.
 */
class EnhancedProcessOptions
{
	private final POSIX posix = POSIXFactory.getPOSIX();
	private final List<String> commands;
	private Supplier<PipeHandler> inPipeHandlerSupplier = DefaultPipeHandler::new;
	private Supplier<PipeHandler> outPipeHandlerSupplier = DefaultPipeHandler::new;
	private Supplier<PipeHandler> errPipeHandlerSupplier = DefaultPipeHandler::new;

	EnhancedProcessOptions(final List<String> commands)
	{
		this.commands = commands;
	}

	void setInputHandler(final Supplier<PipeHandler> inPipeHandlerSupplier)
	{
		this.inPipeHandlerSupplier = inPipeHandlerSupplier;
	}

	void setOutputHandler(final Supplier<PipeHandler> outputHandlerSupplier)
	{
		this.outPipeHandlerSupplier = outputHandlerSupplier;
	}

	void setErrorHandler(final Supplier<PipeHandler> errPipeHandlerSupplier)
	{
		this.errPipeHandlerSupplier = errPipeHandlerSupplier;
	}

	private EnhancedProcess doStart()
	{
		final PipeHandler inPipeHandler = inPipeHandlerSupplier.get();
		final PipeHandler outPipeHandler = outPipeHandlerSupplier.get();
		final PipeHandler errPipeHandler = errPipeHandlerSupplier.get();

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

		final OutputStream in = getOutputStream(pid, inPipeHandler.afterSpawn(posix, Stream.IN));
		final InputStream out = getInputStream(pid, outPipeHandler.afterSpawn(posix, Stream.OUT));
		final InputStream err = getInputStream(pid, errPipeHandler.afterSpawn(posix, Stream.ERR));

		return new EnhancedProcess(pid, in, out, err, posix);
	}

	private OutputStream getOutputStream(final long pid, @Nullable final ByteChannel byteChannel)
	{
		if (byteChannel == null)
		{
			return new NullOutputStream(pid);
		}
		else
		{
			return Channels.newOutputStream(byteChannel);
		}
	}

	private InputStream getInputStream(final long pid, @Nullable final ByteChannel byteChannel)
	{
		if (byteChannel == null)
		{
			return new NullInputStream(pid);
		}
		else
		{
			return Channels.newInputStream(byteChannel);
		}
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
		public T call() throws Exception
		{
			this.doCompute();

			final EnhancedProcess process = this.get();

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
