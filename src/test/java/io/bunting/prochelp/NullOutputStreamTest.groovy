package io.bunting.prochelp

import spock.lang.Specification

/**
 * TODO: Document this class
 */
class NullOutputStreamTest extends Specification
{
	def "passes requirements"()
	{
		given: "a stream"
			def uut = new NullOutputStream(25)
		when: "write"
			uut.write(34)
		then: "IOException thrown"
			thrown(IOException)
	}
}
