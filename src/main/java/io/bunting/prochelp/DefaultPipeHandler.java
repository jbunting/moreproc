package io.bunting.prochelp;

import java.io.InputStream;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import jnr.enxio.channels.NativeDeviceChannel;
import jnr.enxio.channels.NativeSelectorProvider;
import jnr.posix.POSIX;
import jnr.posix.SpawnFileAction;

/**
 * TODO: Document this class
 */
// for each stream we want to create in the child process, we have to:
// 1. create a pipe
// 2. duplicate the child's side when spawning the process
// 3. close the parent's side when spawning the process
// 4. close the child's side in the parent after the process is spawned
// 5. convert the parent's side to a usable channel or stream on the parent's side
class DefaultPipeHandler implements PipeHandler
{
	private static final int PIPE_READ_SIDE = 0;
	private static final int PIPE_WRITE_SIDE = 1;

	private final int[] fds = new int[2];

	@Override
	public List<SpawnFileAction> init(final POSIX posix, final Stream stream)
	{
		posix.pipe(fds);

		return Arrays.asList(
				SpawnFileAction.dup(fds[childPipeSide(stream)], stream.getNumber()),
		        SpawnFileAction.close(fds[parentPipeSide(stream)])
		);
	}

	@Override
	public ByteChannel afterSpawn(final POSIX posix, final Stream stream, final Consumer<Monitor> monitorRegistrar)
	{
		posix.close(fds[childPipeSide(stream)]);
		return new NativeDeviceChannel(NativeSelectorProvider.getInstance(),
		                               fds[parentPipeSide(stream)],
		                               stream.isParentWriteSide() ? SelectionKey.OP_WRITE : SelectionKey.OP_READ
		);
	}

	private int childPipeSide(final Stream stream)
	{
		return stream.isParentWriteSide() ? PIPE_READ_SIDE : PIPE_WRITE_SIDE;
	}

	private int parentPipeSide(final Stream stream)
	{
		return stream.isParentWriteSide() ? PIPE_WRITE_SIDE : PIPE_READ_SIDE;
	}


}
