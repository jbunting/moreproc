package io.bunting.prochelp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import jnr.constants.platform.WaitFlags;
import jnr.posix.POSIX;

import org.apache.commons.io.IOUtils;

/**
 * TODO: Document this class
 */
public class EnhancedProcess
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
	}

	public long getPid()
	{
		return this.pid;
	}

	public OutputStream getOutputStream()
	{
		return this.in;
	}

	public InputStream getInputStream()
	{
		return this.out;
	}

	public InputStream getErrorStream()
	{
		return this.err;
	}

	public boolean waitFor(final long timeout, final TimeUnit unit) throws InterruptedException
	{
		throw new UnsupportedOperationException("Jared hasn't implemented this yet...");
	}

	public int waitFor() throws InterruptedException
	{
		throw new UnsupportedOperationException("Jared hasn't implemented this yet...");
	}

	public EnhancedProcess destroyForcibly()
	{
		throw new UnsupportedOperationException("Jared hasn't implemented this yet...");
	}

	public void destroy()
	{
		throw new UnsupportedOperationException("Jared hasn't implemented this yet...");
	}

	public int exitValue()
	{
		checkForExit();
		if (exitValue == -1)
		{
			throw new IllegalThreadStateException("process hasn't exited");
		}
		return exitValue;
	}

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
		if (waitpid != 0)
		{
			exitValue = status[0] >> 8;
		}
	}
}
