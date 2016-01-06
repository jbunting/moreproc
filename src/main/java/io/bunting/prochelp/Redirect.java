package io.bunting.prochelp;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

/**
 * TODO: Document this class
 */
public class Redirect
{
	public enum Type
	{
		APPEND,
		INHERIT,
		PIPE,
		READ,
		WRITE,
		CHANNEL
	}

	public static final Redirect INHERIT = new Redirect(Type.INHERIT, null, null, null);
	public static final Redirect PIPE = new Redirect(Type.PIPE, null, null, null);

	public static final Redirect from(final File file)
	{
		return new Redirect(Type.READ, file, null, null);
	}

	public static final Redirect to(final File file)
	{
		return new Redirect(Type.WRITE, file, null, null);
	}

	public static final Redirect appendTo(final File file)
	{
		return new Redirect(Type.APPEND, file, null, null);
	}

	public static Redirect from(final ReadableByteChannel readChannel)
	{
		return new Redirect(Type.CHANNEL, null, readChannel, null);
	}

	public static Redirect to(final WritableByteChannel writeChannel)
	{
		return new Redirect(Type.CHANNEL, null, null, writeChannel);
	}

	private final Type type;
	private final File file;
	private final ReadableByteChannel readChannel;
	private final WritableByteChannel writeChannel;

	private Redirect(final Type type, final File file, final ReadableByteChannel readChannel, final WritableByteChannel writeChannel)
	{
		this.type = type;
		this.file = file;
		this.readChannel = readChannel;
		this.writeChannel = writeChannel;
	}

	public File file()
	{
		return this.file;
	}

	public Type type()
	{
		return this.type;
	}

	public ReadableByteChannel readChannel()
	{
		return this.readChannel;
	}

	public WritableByteChannel writeChannel()
	{
		return this.writeChannel;
	}

	@Override
	public boolean equals(final Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof Redirect))
		{
			return false;
		}
		final Redirect redirect = (Redirect) o;
		return type == redirect.type &&
		       Objects.equals(file, redirect.file);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(type, file);
	}
}
