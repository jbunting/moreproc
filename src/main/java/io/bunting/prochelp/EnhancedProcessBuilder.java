package io.bunting.prochelp;

import java.io.File;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

import io.bunting.prochelp.Redirect.Type;

/**
 * An enhanced version of {@link ProcessBuilder}. Should be a clean replacement for the JDK's builtin process builder.
 *
 * It adds several additional niceties, such as:
 *
 *  * allow passing an {@link OutputStream} for out and err.
 *  * allow passing callbacks to handle out and err data
 *  * allow passing callbacks to handle return codes
 *  * setting `/dev/null` for out and err streams
 *  * allow setting timeouts
 *  * allows treating the process invocation as a {@link Callable}
 */
public class EnhancedProcessBuilder
{
	private final EnhancedProcessOptions options;

	public EnhancedProcessBuilder(final List<String> commands)
	{
		this.options = new EnhancedProcessOptions(commands);
	}

	public EnhancedProcessBuilder(final String ... commands)
	{
		this.options = new EnhancedProcessOptions(Arrays.asList(commands));
	}

	public <T> ProcessCallable<T> create(final Function<EnhancedProcess, T> completion)
	{
		return options.create(completion);
	}

	// simple delegate methods below here
//
//	public EnhancedProcessBuilder command(final List<String> command)
//	{
//		delegate.command(command);
//		return this;
//	}
//
	public EnhancedProcessBuilder redirectInput(final Redirect source)
	{
		options.setInputHandler(fromRedirect(source));
		return this;
	}

//	public EnhancedProcessBuilder command(final String... command)
//	{
//		delegate.command(command);
//		return this;
//	}
//
//	public Redirect redirectOutput()
//	{
//		return delegate.redirectOutput();
//	}
//
//	public Redirect redirectInput()
//	{
//		return delegate.redirectInput();
//	}
//
//	public EnhancedProcessBuilder directory(final File directory)
//	{
//		delegate.directory(directory);
//		return this;
//	}
//
	public EnhancedProcessBuilder redirectInput(final File file)
	{
		return this.redirectInput(Redirect.from(file));
	}
//
//	public File directory()
//	{
//		return delegate.directory();
//	}
//
//	public Redirect redirectError()
//	{
//		return delegate.redirectError();
//	}
//
//	public boolean redirectErrorStream()
//	{
//		return delegate.redirectErrorStream();
//	}
//
//	public EnhancedProcessBuilder redirectErrorStream(final boolean redirectErrorStream)
//	{
//		delegate.redirectErrorStream(redirectErrorStream);
//		return this;
//	}
//
//	public EnhancedProcessBuilder inheritIO()
//	{
//		delegate.inheritIO();
//		return this;
//	}
//
//	public Map<String, String> environment()
//	{
//		return delegate.environment();
//	}
//
	public EnhancedProcessBuilder redirectError(final Redirect destination)
	{
		options.setErrorHandler(fromRedirect(destination));
		return this;
	}
//
//	public List<String> command()
//	{
//		return delegate.command();
//	}
//
	public EnhancedProcessBuilder redirectOutput(final File file)
	{
		return this.redirectOutput(Redirect.to(file));
	}

	public EnhancedProcessBuilder redirectOutput(final Redirect destination)
	{
		options.setOutputHandler(fromRedirect(destination));
		return this;
	}

	private Supplier<PipeHandler> fromRedirect(final Redirect redirect)
	{
		if (redirect.type() == Type.PIPE)
		{
			return DefaultPipeHandler::new;
		}
		else if (redirect.type() == Type.CHANNEL)
		{
			if (redirect.readChannel() != null)
			{
				return () -> new ReadableChannelPipeHandler(redirect.readChannel());
			}
			else
			{
				return () -> new WriteableChannelPipeHandler(redirect.writeChannel());
			}
		}
		else if (redirect.file() != null)
		{
			return () -> new FilePipeHandler(redirect.file(), redirect.type() == Type.APPEND);
		}
		else
		{
			throw new UnsupportedOperationException("Redirect type " + redirect.type() + " not supported.");
		}
	}

	public EnhancedProcessBuilder redirectError(final File file)
	{
		this.redirectError(Redirect.to(file));
		return this;
	}

	public EnhancedProcessBuilder withIn(final ReadableByteChannel channel)
	{
		this.redirectInput(Redirect.from(channel));
		return this;
	}

	public EnhancedProcessBuilder withOut(final WritableByteChannel channel)
	{
		this.redirectOutput(Redirect.to(channel));
		return this;
	}


	public EnhancedProcessBuilder withErr(final WritableByteChannel channel)
	{
		this.redirectError(Redirect.to(channel));
		return this;
	}
}
