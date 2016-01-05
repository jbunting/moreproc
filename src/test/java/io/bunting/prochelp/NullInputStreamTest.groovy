package io.bunting.prochelp

import spock.lang.Specification

/**
 * TODO: Document this class
 */
class NullInputStreamTest extends Specification
{
	def "passes requirements"()
	{
		given: "a stream"
			def uut = new NullInputStream(25)
		expect: "proper return values"
			uut.read() == -1
			uut.available() == 0
			uut.close()
			uut.read() == -1
			uut.available() == 0
	}
}
