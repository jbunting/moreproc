package io.bunting.prochelp

import org.spockframework.util.IoUtil
import spock.lang.Specification

import java.util.concurrent.Callable
import java.util.function.Function

/**
 * TODO: Document this class
 */
class EnhancedProcessBuilderTest extends Specification
{

	def "start process"()
	{
		given: "we have a script"
			def script = "src/test/scripts/simple.sh"
		expect: "it exists"
			new File(script).getAbsoluteFile().isFile()
		and: "process exits successfully"
			def builder = new EnhancedProcessBuilder(script, "first arg")
			def output
			Callable<Integer> callable = builder.create({ process ->
				output = IoUtil.getText(process.getInputStream())
				def value = process.exitValue()
				println "Exit value: " + Integer.toBinaryString(value)
				return value
			})
			callable.call() == 0
			output == "Hello folks...\nArg first arg\n"
	}
}
