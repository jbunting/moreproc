package io.bunting.prochelp

import org.spockframework.util.IoUtil
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.Function

/**
 * TODO: Document this class
 */
class EnhancedProcessBuilderTest extends Specification
{
	def static script = "src/test/scripts/simple.sh"
	def static input_script = "src/test/scripts/simple_input.sh"
	def static executor = Executors.newCachedThreadPool()
	def static delayedExecutor = Executors.newSingleThreadScheduledExecutor()

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
				pid = process.getPid()
				output = IoUtil.getText(process.getInputStream())
				errout = IoUtil.getText(process.getErrorStream())
				def value = process.exitValue()
				return value
			})
		when: "process gotten before launch we get a timeout exception"
			callable.get(0, TimeUnit.SECONDS)
		then:
			thrown TimeoutException
		expect: "calling the process async and then killing gives no response"
			def future = delayedExecutor.schedule(callable, 1, TimeUnit.SECONDS)

			def process = callable.get()
			process.destroy();
			future.get() == 0x008F
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
				pid = process.getPid()
				output = IoUtil.getText(process.getInputStream())
				errout = IoUtil.getText(process.getErrorStream())
				def value = process.exitValue()
				return value
			})
		when: "process gotten before launch we get a timeout exception"
			callable.get(0, TimeUnit.SECONDS)
		then:
			thrown TimeoutException
		expect: "calling the process async and then killing gives no response"
			def future = delayedExecutor.schedule(callable, 1, TimeUnit.SECONDS)

			def process = callable.get()
			process.destroyForcibly();
			future.get() == 0x0089
			process.getPid() == pid
			output == ""
			errout == ""
	}

	def "try to get exit value before process exits"()
	{
		expect: "it exists"
			new File(script).getAbsoluteFile().isFile()
		and: "process creates successfully"
			def builder = new EnhancedProcessBuilder(script, "first arg")
			ProcessCallable<Integer> callable = builder.create({ process ->
				def value = process.exitValue()
				return value
			})
		when: "calling the process async and then requesting exit value before it is complete"
			def future = delayedExecutor.schedule(callable, 1, TimeUnit.SECONDS)

			def process = callable.get()
			process.exitValue()
		then: "illegal thread state exception is thrown"
			thrown IllegalThreadStateException
	}

	def "wait for a process for awhile, causing the default exit pollin in waitFor to be used"()
	{
		expect: "it exists"
			new File(script).getAbsoluteFile().isFile()
		and: "process creates successfully"
			def builder = new EnhancedProcessBuilder(script, "first arg")
			ProcessCallable<Integer> callable = builder.create({ process ->
				def value = process.exitValue()
				return value
			})
		and: "calling the process async and then waiting a really long time nothing fails"
			def exitCode = -1;
			def future = delayedExecutor.schedule(callable, 1, TimeUnit.SECONDS)

			def process = callable.get()
			process.waitFor(20, TimeUnit.SECONDS)
	}

	def "run a process that requires input"()
	{
		expect: "it exists"
			new File(input_script).getAbsoluteFile().isFile()
		and: "process creates successfully"
			def builder = new EnhancedProcessBuilder(input_script)
			def output = "<no value set here yet>"
			ProcessCallable<Integer> callable = builder.create({ process ->
				output = IoUtil.getText(process.getInputStream())
				def value = process.exitValue()
				return value
			})
		when: "calling the process async and then passing input"
			def future = executor.submit(callable)
			def process = callable.get()
			process.getOutputStream().withWriter {it.write("stuff")}
		then: "we get the input back in the output"
			future.get() == 0
			output == "Hello folks...\nstuff\n"
	}

	@Ignore("This is one of the key usecases, but we need to flesh out basic process builder functionality first.")
	def "run a process and pass in streams"()
	{
		given: "streams"
			def stdin = new ByteArrayInputStream("stuff".getBytes(StandardCharsets.UTF_8))
			def stdout = new ByteArrayOutputStream()
			def stderr = new ByteArrayOutputStream()
		expect: "process creates successfully"
			def builder = new EnhancedProcessBuilder(input_script)
			ProcessCallable<Integer> callable = builder
					.withIn(stdin)
					.withOut(stdout)
					.withErr(stderr)
					.create({ process -> process.exitValue() })
		when: "process is run"
			executor.submit(callable).get()
		then: "outputs contain expected content"
			stdout.toString(StandardCharsets.UTF_8.name()) == "Hello folks...\nstuff\n"
			stderr.toString(StandardCharsets.UTF_8.name()) == "This is error text\n"
	}
}
