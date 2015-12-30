package io.bunting.prochelp;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

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

	public <T> T invokeSynchronously(final Function<EnhancedProcess, T> completion) throws InterruptedException
	{
		return options.invokeSynchronously(completion);
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
//	public EnhancedProcessBuilder redirectInput(final Redirect source)
//	{
//		delegate.redirectInput(source);
//		return this;
//	}
//
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
//	public EnhancedProcessBuilder redirectInput(final File file)
//	{
//		delegate.redirectInput(file);
//		return this;
//	}
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
//	public EnhancedProcessBuilder redirectError(final Redirect destination)
//	{
//		delegate.redirectError(destination);
//		return this;
//	}
//
//	public List<String> command()
//	{
//		return delegate.command();
//	}
//
//	public EnhancedProcessBuilder redirectOutput(final File file)
//	{
//		delegate.redirectOutput(file);
//		return this;
//	}
//
//	public EnhancedProcessBuilder redirectOutput(final Redirect destination)
//	{
//		delegate.redirectOutput(destination);
//		return this;
//	}
//
//	public EnhancedProcessBuilder redirectError(final File file)
//	{
//		delegate.redirectError(file);
//		return this;
//	}
}
