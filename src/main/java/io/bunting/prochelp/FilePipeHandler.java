package io.bunting.prochelp;

import java.io.File;
import java.nio.channels.ByteChannel;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import jnr.constants.platform.Errno;
import jnr.constants.platform.OpenFlags;
import jnr.posix.POSIX;
import jnr.posix.SpawnFileAction;

/**
 * TODO: Document this class
 */
class FilePipeHandler implements PipeHandler
{
	private final File file;
	private final boolean append;
	private int fd = -1;

	public FilePipeHandler(final File file, final boolean append)
	{
		this.file = file;
		this.append = append;
	}

	@Override
	public List<SpawnFileAction> init(final POSIX posix, final Stream stream)
	{
		final int flags;
		if (stream.isParentWriteSide())
		{
			flags = OpenFlags.O_RDONLY.intValue();
		}
		else if (append)
		{
			flags = OpenFlags.O_WRONLY.intValue() | OpenFlags.O_APPEND.intValue() | OpenFlags.O_CREAT.intValue();
		}
		else
		{
			flags = OpenFlags.O_WRONLY.intValue() | OpenFlags.O_CREAT.intValue();
		}
		fd = posix.open(file.getAbsolutePath(), flags, 0x777);
		if (fd < 0)
		{
			int errno = posix.errno();
			throw new RuntimeException("Failed to open file " + file + ". " + Errno.valueOf(errno).description());
		}
		return Arrays.asList(
				SpawnFileAction.dup(fd, stream.getNumber())
//				SpawnFileAction.close(fd)
		);
	}

	@Override
	public ByteChannel afterSpawn(final POSIX posix, final Stream stream, final Consumer<Monitor> monitorRegistrar)
	{
		posix.close(fd);
		return null;
	}
}
