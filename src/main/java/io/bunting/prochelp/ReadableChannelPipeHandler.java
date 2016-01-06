package io.bunting.prochelp;

import java.nio.channels.ByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.annotation.WillClose;

import jnr.posix.POSIX;
import jnr.posix.SpawnFileAction;

/**
 * TODO: Document this class
 */
class ReadableChannelPipeHandler implements PipeHandler
{
	final static int BUF_SIZE = 1024;

	private final ReadableByteChannel readChannel;
	private final DefaultPipeHandler pipeHandler = new DefaultPipeHandler();

	public ReadableChannelPipeHandler(@WillClose final ReadableByteChannel readChannel)
	{
		this.readChannel = readChannel;
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
		WritableByteChannel writeChannel = pipeHandler.afterSpawn(posix, stream, monitorRegistrar);

		monitorRegistrar.accept(new ChannelCopyMonitor(readChannel, writeChannel, stream));
		return null;
	}

}
