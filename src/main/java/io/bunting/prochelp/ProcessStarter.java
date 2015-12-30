package io.bunting.prochelp;

import java.io.IOException;

/**
 * A functional interface that is used for physically starting a process. Used to avoid exposing the entirety of a {@link ProcessBuilder} to multiple classes.
 */
@FunctionalInterface
interface ProcessStarter
{
	EnhancedProcess invoke();
}
