package io.bunting.prochelp;

import java.io.IOException;
import java.io.InputStream;

class NullInputStream extends InputStream
{
	@SuppressWarnings("unused")
	private final long pid;

	public NullInputStream(final long pid)
	{
		this.pid = pid;
	}

	@Override
	public int read() throws IOException
	{
		return -1;
	}
}
