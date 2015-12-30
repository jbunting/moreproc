package io.bunting.prochelp

import spock.lang.Specification

import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * TODO: Document this class
 */
class AbstractSettableFutureTest extends Specification
{
	def "future test"()
	{
		given: "a future"
			def uut = new AbstractSettableFuture<String>() {
				@Override
				protected String computeValue() throws Exception {
					return "result"
				}
			}
		expect: "isCancelled is false"
			!uut.isCancelled()
		when: "cancel invoked with true"
			uut.cancel(true)
		then: "it fails"
			thrown UnsupportedOperationException
		when: "cancel invoked with false"
			uut.cancel(false)
		then: "it fails"
			thrown UnsupportedOperationException
		expect: "isCancelled is false"
			!uut.isCancelled()
		and: "isDone is false"
			!uut.isDone()
		when: "get with timeout"
			uut.get(10, TimeUnit.MILLISECONDS)
		then: "timeout exception thrown"
			thrown TimeoutException
		expect: "get returns once future is complete"
			new Timer().schedule({ uut.doCompute() }, 1000)
			!uut.isDone() // shouldn't be done yet
			uut.get() == "result"
			uut.isDone()
		and: "another get works as well"
			uut.get() == "result"
		when: "compute is called again"
			uut.doCompute()
		then: "it throws illegal state exception"
			thrown IllegalStateException

	}

	def "failing future test"()
	{
		given: "a future"
			def uut = new AbstractSettableFuture<String>() {
				@Override
				protected String computeValue() throws Exception {
					throw new RuntimeException("I failed!")
				}
			}
		expect: "isCancelled is false"
			!uut.isCancelled()
		when: "cancel invoked with true"
			uut.cancel(true)
		then: "it fails"
			thrown UnsupportedOperationException
		when: "cancel invoked with false"
			uut.cancel(false)
		then: "it fails"
			thrown UnsupportedOperationException
		expect: "isCancelled is false"
			!uut.isCancelled()
		and: "isDone is false"
			!uut.isDone()
		when: "get with timeout"
			uut.get(10, TimeUnit.MILLISECONDS)
		then: "timeout exception thrown"
			thrown TimeoutException
		when: "compute is done while blocked on get"
			new Timer().schedule({ uut.doCompute() }, 1000)
			!uut.isDone() // shouldn't be done yet
			uut.get()
		then: "get fails with execution exception"
			thrown ExecutionException
			uut.isDone()
		when: "another get is invoked"
			uut.get()
		then: "it fails again with the execution exception"
			thrown ExecutionException
		when: "compute is called again"
			uut.doCompute()
		then: "it throws illegal state exception"
			thrown IllegalStateException
	}

	def "null value future test"()
	{
		given: "a future"
			def uut = new AbstractSettableFuture<String>() {
				@Override
				protected String computeValue() throws Exception {
					return null
				}
			}
		when: "doCompute is called"
			uut.doCompute()
			uut.get()
		then: "it fails"
			ExecutionException e = thrown()
			e.getCause() instanceof IllegalArgumentException
	}
}
