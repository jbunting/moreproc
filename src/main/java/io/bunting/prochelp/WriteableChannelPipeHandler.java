package io.bunting.prochelp;

import java.io.OutputStream;
import java.nio.channels.ByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import jnr.posix.POSIX;
import jnr.posix.SpawnFileAction;

/**
 * TODO: Document this class
 */
class WriteableChannelPipeHandler implements PipeHandler
{
	private final WritableByteChannel writeChannel;
	private final DefaultPipeHandler pipeHandler = new DefaultPipeHandler();

	public WriteableChannelPipeHandler(final WritableByteChannel writeChannel)
	{
		this.writeChannel = writeChannel;
	}

	@Override
	public List<SpawnFileAction> init(final POSIX posix, final Stream stream)
	{
		return pipeHandler.init(posix, stream);
	}

	@Nullable
	@Override
	public ByteChannel afterSpawn(final POSIX posix, final Stream stream, final Consumer<Monitor> monitorRegistrar)
	{
		ReadableByteChannel readChannel = pipeHandler.afterSpawn(posix, stream, monitorRegistrar);

		monitorRegistrar.accept(new ChannelCopyMonitor(readChannel, writeChannel, stream));
		return null;
	}
}
