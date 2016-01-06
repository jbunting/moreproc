package io.bunting.prochelp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import io.bunting.prochelp.PipeHandler.Stream;

/**
 * TODO: Document this class
 */
class ChannelCopyMonitor implements Monitor
{
	private final ReadableByteChannel readChannel;
	private final WritableByteChannel writeChannel;
	private final Stream stream;

	public ChannelCopyMonitor(final ReadableByteChannel readChannel, final WritableByteChannel writeChannel, final Stream stream)
	{
		this.readChannel = readChannel;
		this.writeChannel = writeChannel;
		this.stream = stream;
	}

	@Override
	public void setup(final EnhancedProcess process)
	{
	}

	@Override
	public boolean update(final EnhancedProcess process)
	{
		final ByteBuffer byteBuffer = ByteBuffer.allocate(ReadableChannelPipeHandler.BUF_SIZE);
		byteBuffer.clear();
		int last = 0;
		if (readChannel.isOpen() && writeChannel.isOpen())
		{
			try
			{
				while ((last = readChannel.read(byteBuffer)) > 0 || byteBuffer.position() > 0)
				{
					byteBuffer.flip();
					writeChannel.write(byteBuffer);
					byteBuffer.compact();
				}
				if (last == -1)
				{
					return true;
				}
			}
			catch (IOException e)
			{
				throw new RuntimeException("Failed to transfer bytes for stream " + stream.name(), e);
			}
		}
		else
		{
			return true;
		}
		return false;
	}

	@Override
	public void cleanup(final EnhancedProcess process)
	{
		try
		{
			if (writeChannel.isOpen())
			{
				writeChannel.close();
			}
			if (readChannel.isOpen())
			{
				writeChannel.close();
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException("Failed to close channel for stream " + stream.name(), e);
		}
	}
}
