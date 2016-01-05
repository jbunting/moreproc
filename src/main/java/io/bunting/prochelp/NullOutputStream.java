package io.bunting.prochelp;

import java.io.IOException;
import java.io.OutputStream;

class NullOutputStream extends OutputStream
{
	private final long pid;

	public NullOutputStream(final long pid)
	{
		this.pid = pid;
	}

	@Override
	public void write(final int b) throws IOException
	{
		throw new IOException("This process [" + pid + "] does not allow writing to this stream.");
	}
}
