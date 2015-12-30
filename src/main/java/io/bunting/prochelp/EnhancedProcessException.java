package io.bunting.prochelp;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * TODO: Document this class
 */
class EnhancedProcessException extends RuntimeException
{
	public EnhancedProcessException(final String message, final InterruptedException e)
	{
		super(message, e);
	}

	public EnhancedProcessException(final String message, final ExecutionException e)
	{
		super(message, e);
	}

	public EnhancedProcessException(final String message, final IOException e)
	{
		super(message, e);
	}
}
