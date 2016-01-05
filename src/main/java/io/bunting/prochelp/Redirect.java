package io.bunting.prochelp;

import java.io.File;
import java.util.Objects;

/**
 * TODO: Document this class
 */
public class Redirect
{
	public static enum Type
	{
		APPEND,
		INHERIT,
		PIPE,
		READ,
		WRITE
	}

	public static final Redirect INHERIT = new Redirect(Type.INHERIT, null);
	public static final Redirect PIPE = new Redirect(Type.PIPE, null);

	public static final Redirect from(final File file)
	{
		return new Redirect(Type.READ, file);
	}

	public static final Redirect to(final File file)
	{
		return new Redirect(Type.WRITE, file);
	}

	public static final Redirect appendTo(final File file)
	{
		return new Redirect(Type.APPEND, file);
	}

	private final Type type;
	private final File file;

	private Redirect(final Type type, final File file)
	{
		this.type = type;
		this.file = file;
	}

	public File file()
	{
		return this.file;
	}

	public Type type()
	{
		return this.type;
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
