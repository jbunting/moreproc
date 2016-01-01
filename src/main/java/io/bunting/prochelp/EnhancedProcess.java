package io.bunting.prochelp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import jnr.constants.platform.Signal;
import jnr.constants.platform.WaitFlags;
import jnr.posix.POSIX;

/**
 * TODO: Document this class
 */
public class EnhancedProcess extends Process
{
	private final long pid;
	private final OutputStream in;
	private final InputStream out;
	private final InputStream err;

	private final POSIX posix;

	private int exitValue = -1;

	EnhancedProcess(final long pid, final OutputStream in, final InputStream out, final InputStream err, final POSIX posix)
	{
		this.pid = pid;
		this.in = in;
		this.out = out;
		this.err = err;
		this.posix = posix;
		System.out.println("pid: " + pid);
	}

	public long getPid()
	{
		return this.pid;
	}

	@Override
	public OutputStream getOutputStream()
	{
		return this.in;
	}

	@Override
	public InputStream getInputStream()
	{
		return this.out;
	}

	@Override
	public InputStream getErrorStream()
	{
		return this.err;
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
		System.out.println("waitpid: " + waitpid);
		if (waitpid != 0)
		{
			if ((status[0] & 0x000F) == 0)
			{
				// exited normally
				System.out.println("setting exit status ( " + Integer.toHexString(status[0]) + " )");
				exitValue = (status[0] >> 8) & 0x00FF;
			}
			else
			{
				// killed by signal
				System.out.println("setting exit status due to kill by signal ( " + Integer.toHexString(status[0]) + " )");
				System.out.println("-- " + Integer.toHexString(status[0] & 0x00FF));
				System.out.println("-- " + Integer.toHexString((status[0] & 0x00FF) | 0x0080));
				exitValue = (status[0] & 0x00FF) | 0x0080;
			}
		}
	}
}
