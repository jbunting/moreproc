package io.bunting.prochelp;

/**
 * Handles a particular function of the enhanced process.
 */
interface Monitor
{
	void setup(EnhancedProcess process);

	void update(EnhancedProcess process);

	void cleanup(EnhancedProcess process);
}

