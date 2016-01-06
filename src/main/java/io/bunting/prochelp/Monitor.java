package io.bunting.prochelp;

/**
 * Handles a particular function of the enhanced process.
 */
interface Monitor
{
	void setup(EnhancedProcess process);

	/**
	 * Returns {@code true} if this monitor has done everything that it can with this process. Once this method returns
	 * {@code true}, it should not be called anymore.
	 */
	boolean update(EnhancedProcess process);

	void cleanup(EnhancedProcess process);
}

