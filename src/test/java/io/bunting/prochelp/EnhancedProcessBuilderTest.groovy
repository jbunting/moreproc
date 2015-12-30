package io.bunting.prochelp

import org.spockframework.util.IoUtil
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.Function

/**
 * TODO: Document this class
 */
class EnhancedProcessBuilderTest extends Specification
{
	def static script = "src/test/scripts/simple.sh"

	@Unroll
	def "start process using #constructor and get value from callback"()
	{
		given: "we have a script"
		expect: "it exists"
			new File(script).getAbsoluteFile().isFile()
		and: "process creates successfully"
			def builder = new EnhancedProcessBuilder(args)
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
		where:
			constructor | args
			"varargs"   | [script, "first arg"] as String[]
			"list"      | [script, "first arg"]
	}

	def "start process and kill it immediately"()
	{
		expect: "it exists"
			new File(script).getAbsoluteFile().isFile()
		and: "process creates successfully"
			def builder = new EnhancedProcessBuilder(script, "first arg")
			def output = "<no value set here yet>"
			def errout = "<no value set here yet>"
			def pid = -1
			ProcessCallable<Integer> callable = builder.create({ process ->
				println "completion called!"
				pid = process.getPid()
				println "pid: " + pid
				output = IoUtil.getText(process.getInputStream())
				errout = IoUtil.getText(process.getErrorStream())
				def value = process.exitValue()
				println "Exit value: " + Integer.toHexString(value)
				return value
			})
		when: "process gotten before launch we get a timeout exception"
			callable.get(0, TimeUnit.SECONDS)
		then:
			thrown TimeoutException
		expect: "calling the process async and then killing gives no response"
			def exitCode = -1;
			new Timer().schedule({ exitCode = callable.call(); println "done!" }, 1000)

			def process = callable.get()
			process.destroy();
			while (exitCode == -1)
			{
				TimeUnit.MILLISECONDS.sleep(100)
			}
			exitCode == 0x008F
			process.getPid() == pid
			output == ""
			errout == ""
	}

	def "start process and kill 9 it immediately"()
	{
		expect: "it exists"
			new File(script).getAbsoluteFile().isFile()
		and: "process creates successfully"
			def builder = new EnhancedProcessBuilder(script, "first arg")
			def output = "<no value set here yet>"
			def errout = "<no value set here yet>"
			def pid = -1
			ProcessCallable<Integer> callable = builder.create({ process ->
				println "completion called!"
				pid = process.getPid()
				println "pid: " + pid
				output = IoUtil.getText(process.getInputStream())
				errout = IoUtil.getText(process.getErrorStream())
				def value = process.exitValue()
				println "Exit value: " + Integer.toHexString(value)
				return value
			})
		when: "process gotten before launch we get a timeout exception"
			callable.get(0, TimeUnit.SECONDS)
		then:
			thrown TimeoutException
		expect: "calling the process async and then killing gives no response"
			def exitCode = -1;
			new Timer().schedule({ exitCode = callable.call(); println "done!" }, 1000)

			def process = callable.get()
			process.destroyForcibly();
			while (exitCode == -1)
			{
				TimeUnit.MILLISECONDS.sleep(100)
			}
			exitCode == 0x0089
			process.getPid() == pid
			output == ""
			errout == ""
	}
}
