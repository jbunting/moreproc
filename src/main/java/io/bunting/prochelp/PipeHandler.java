package io.bunting.prochelp;

import java.nio.channels.ByteChannel;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import jnr.posix.POSIX;
import jnr.posix.SpawnFileAction;

/**
 * TODO: Document this class
 */
interface PipeHandler
{
	List<SpawnFileAction> init(POSIX posix, Stream stream);

	@Nullable
	ByteChannel afterSpawn(POSIX posix, Stream stream, Consumer<Monitor> monitorRegistrar);

	enum Stream {
		IN(0, true),
		OUT(1, false),
		ERR(2, false);

		final int number;
		final boolean parentWriteSide;

		Stream(final int number, final boolean parentWriteSide)
		{
			this.number = number;
			this.parentWriteSide = parentWriteSide;
		}

		public int getNumber()
		{
			return number;
		}

		public boolean isParentWriteSide()
		{
			return parentWriteSide;
		}
	}
}
