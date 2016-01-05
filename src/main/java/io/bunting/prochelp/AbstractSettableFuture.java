package io.bunting.prochelp;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

/**
 * A separation of the notion of a simple settable future. This is separated so that the lock wait/notify code is self-contained.
 *
 * By default this future does not allow cancellation.
 */
abstract class AbstractSettableFuture<V> implements Future<V>
{
	// any code that needs to write or *safely* read the "value" field MUST synchronize on this lock
	// any code writing the value field MUST notify on this lock
	// any code waiting for the value field to be written SHOULD wait on this lock
	private final Object _lock = new Object();
	private volatile V value = null;
	private volatile Throwable failureCause = null;

	/**
	 * Computes the value for this future in a safe way.
	 *
	 * Guarantees to invoke {@link #computeValue()} exactly once. If attempted to be invoked more than once, will throw an
	 * {@link IllegalStateException}.
	 */
	protected final void doCompute()
	{
		synchronized (_lock)
		{
			if (this.isDone())
			{
				throw new IllegalStateException("This process has already been executed. It may not be executed more than once.");
			}
			try
			{
				value = this.computeValue();
				if (value == null)
				{
					throw new IllegalArgumentException("Future value computed as null. This indicates a programming error.");
				}
			}
			catch (Throwable e)
			{
				this.failureCause = e;
			}
			_lock.notifyAll();
		}
	}

	/**
	 * Invoked by {@link #doCompute()} exactly once. This method is guaranteed to never be invoked more than once for a given instance of this
	 * future.
	 *
	 * @return the value to set this future to
	 */
	@Nonnull
	protected abstract V computeValue() throws Exception;

	@Override
	public boolean cancel(final boolean mayInterruptIfRunning)
	{
		throw new UnsupportedOperationException("This future is not cancellable.");
	}

	@Override
	public boolean isCancelled()
	{
		return false;
	}

	@Override
	public boolean isDone()
	{
		return value != null || failureCause != null;
	}

	@Override
	public V get() throws InterruptedException, ExecutionException
	{
		return this.doGet(Object::wait, () -> new IllegalStateException("Future not fulfilled for unknown reason. This should not be possible."));
	}

	@Override
	public V get(final long timeout, @Nonnull final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
	{
		return this.doGet(lock -> unit.timedWait(lock, timeout),
		                  () -> new TimeoutException("Future not fulfilled within " + timeout + " " + unit.name()));
	}

	private <R extends Exception> V doGet(Waiter waiter, Supplier<R> unfulfilledExceptionSupplier) throws ExecutionException, InterruptedException, R
	{
		if (!this.isDone())
		{
			synchronized (_lock)
			{
				if (!this.isDone())
				{
					waiter.doWait(_lock);
				}
			}
		}
		if (this.value != null)
		{
			return this.value;
		}
		else if (this.failureCause != null)
		{
			throw new ExecutionException(this.failureCause);
		}
		else
		{
			throw unfulfilledExceptionSupplier.get();
		}
	}

	@FunctionalInterface
	private interface Waiter
	{
		void doWait(Object lock) throws InterruptedException;
	}
}
