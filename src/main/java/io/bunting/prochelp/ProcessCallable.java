package io.bunting.prochelp;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * TODO: Document this class
 */
public interface ProcessCallable<T> extends Callable<T>, Future<EnhancedProcess>
{
}
