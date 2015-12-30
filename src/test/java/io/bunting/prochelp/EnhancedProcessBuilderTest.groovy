package io.bunting.prochelp

import org.spockframework.util.IoUtil
import spock.lang.Specification

import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.Function

/**
 * TODO: Document this class
 */
class EnhancedProcessBuilderTest extends Specification
{

	def "start process and get value from callback"()
	{
		given: "we have a script"
			def script = "src/test/scripts/simple.sh"
		expect: "it exists"
			new File(script).getAbsoluteFile().isFile()
		and: "process creates successfully"
			def builder = new EnhancedProcessBuilder(script, "first arg")
			def output = "<no value set here yet>"
			def errout = "<no value set here yet>"
			def pid = -1
			ProcessCallable<Integer> callable = builder.create({ process ->
				pid = process.getPid()
				output = IoUtil.getText(process.getInputStream())
				errout = IoUtil.getText(process.getErrorStream())
				def value = process.exitValue()
				println "Exit value: " + Integer.toBinaryString(value)
				return value
			})
		when: "process gotten before launch we get a timeout exception"
			callable.get(0, TimeUnit.SECONDS)
		then:
			thrown TimeoutException
		expect: "calling the process gives the proper response"
			callable.call() == 0
			output == "Hello folks...\nArg first arg\n"
			errout == "This is error text\n"
			pid > 0
	}
}
