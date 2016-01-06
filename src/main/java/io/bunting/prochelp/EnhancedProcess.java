package io.bunting.prochelp;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.TimeUnit;

import jnr.constants.platform.Signal;
import jnr.constants.platform.WaitFlags;
import jnr.posix.POSIX;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Document this class
 */
public class EnhancedProcess extends Process
{
	private static final Logger logger = LoggerFactory.getLogger(EnhancedProcess.class);

	private final long pid;
	private final WritableByteChannel in;
	private final ReadableByteChannel out;
	private final ReadableByteChannel err;

	private final POSIX posix;

	private int exitValue = -1;

	EnhancedProcess(final long pid, final WritableByteChannel in, final ReadableByteChannel out, final ReadableByteChannel err, final POSIX posix)
	{
		this.pid = pid;
		this.in = in;
		this.out = out;
		this.err = err;
		this.posix = posix;
		logger.debug("Created process with pid {}.", pid);
	}

	public long getPid()
	{
		return this.pid;
	}

	@Override
	public OutputStream getOutputStream()
	{
		return EnhancedProcessOptions.getOutputStream(this.pid, this.in);
	}

	@Override
	public InputStream getInputStream()
	{
		return EnhancedProcessOptions.getInputStream(this.pid, this.out);
	}

	@Override
	public InputStream getErrorStream()
	{
		return EnhancedProcessOptions.getInputStream(this.pid, this.err);
	}

	@Override
	public boolean waitFor(final long timeout, final TimeUnit unit) throws InterruptedException
	{
		long abortTime = System.currentTimeMillis() + unit.toMillis(timeout);
		checkForExit();
		while (exitValue == -1 && abortTime >= System.currentTimeMillis())
		{
			if (useDefaultSleep(timeout, unit))
			{
				TimeUnit.MILLISECONDS.sleep(100);
			}
			else
			{
				unit.sleep(timeout);
			}
			checkForExit();
		}
		return exitValue != -1;
	}

	private boolean useDefaultSleep(final long timeout, final TimeUnit unit)
	{
		return TimeUnit.MILLISECONDS.toNanos(100) < unit.toNanos(timeout);
	}

	@Override
	public int waitFor() throws InterruptedException
	{
		checkForExit();
		while (exitValue == -1)
		{
			TimeUnit.MILLISECONDS.sleep(100);
			checkForExit();
		}
		return exitValue;
	}

	@Override
	public EnhancedProcess destroyForcibly()
	{
		posix.kill((int) pid, Signal.SIGKILL.intValue());
		try
		{
			this.waitFor();
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException("Interrupted.", e);
		}
		return this;
	}

	@Override
	public void destroy()
	{
		posix.kill((int) pid, Signal.SIGTERM.intValue());
		try
		{
			this.waitFor();
		}
		catch (InterruptedException e)
		{
			throw new RuntimeException("Interrupted.", e);
		}
	}

	@Override
	public int exitValue()
	{
		checkForExit();
		if (exitValue == -1)
		{
			throw new IllegalThreadStateException("process hasn't exited");
		}
		return exitValue;
	}

	@Override
	public boolean isAlive()
	{
		checkForExit();
		return exitValue == -1;
	}

	private synchronized void checkForExit()
	{
		if (exitValue != -1)
		{
			return;
		}

		int[] status = new int[1];
		final int waitpid = posix.waitpid(pid, status, WaitFlags.WNOHANG.intValue());
		logger.trace("Process {} is running.", pid);
		if (waitpid != 0)
		{
			if ((status[0] & 0x000F) == 0)
			{
				// exited normally
				logger.debug("Received normal exit status 0x{} for process {}.", Integer.toHexString(status[0]), pid);
				exitValue = (status[0] >> 8) & 0x00FF;
			}
			else
			{
				// killed by signal
				logger.debug("Received 'killed by signal' exit status 0x{} for process {}.", Integer.toHexString(status[0]), pid);
				exitValue = (status[0] & 0x00FF) | 0x0080;
			}
			logger.debug("Process {} exited with value {}.", pid, exitValue);
		}
	}
}
