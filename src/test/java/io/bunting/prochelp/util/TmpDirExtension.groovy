package io.bunting.prochelp.util

import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension
import org.spockframework.runtime.extension.AbstractMethodInterceptor
import org.spockframework.runtime.extension.IAnnotationDrivenExtension
import org.spockframework.runtime.extension.IMethodInvocation
import org.spockframework.runtime.model.FieldInfo
import org.spockframework.runtime.model.SpecInfo

/**
 * TODO: Document this class
 */
class TmpDirExtension extends AbstractAnnotationDrivenExtension<TmpDir>
{
	@Override
	void visitFieldAnnotation(final TmpDir annotation, final FieldInfo field)
	{
		final File baseDir = new File(annotation.base() ?: defaultBase())
		final specInfo = field.getParent().getTopSpec()
		final interceptor = new AbstractMethodInterceptor() {
			@Override
			void interceptSetupMethod(final IMethodInvocation invocation) throws Throwable
			{
				def specInstance = field.shared ? invocation.sharedInstance : invocation.instance
				final testName = invocation.feature ? invocation.feature.name.replaceAll(/\W+/, '-') : field.name
				final testDirName = "${ specInstance.class.name }/${testName}"
				File testDir = new File(baseDir, testDirName).canonicalFile
				if (testDir.isDirectory())
				{
					int counter = 0;
					while (testDir.isDirectory())
					{
						counter++
						testDir = new File(baseDir, testDirName + "_" + counter).canonicalFile
					}
				}
				assert testDir.with { (!directory) && mkdirs() }, "Failed to create test directory [$testDir]"
				specInstance."$field.name" = testDir
				invocation.proceed()
			}

			@Override
			void interceptCleanupMethod(final IMethodInvocation invocation) throws Throwable
			{
				def specInstance = field.shared ? invocation.sharedInstance : invocation.instance
				try
				{
					invocation.proceed()
				}
				finally
				{
					File testDir = specInstance."$field.name"
					if (annotation.clean())
					{
						assert (testDir.deleteDir() && !testDir.isDirectory())
					}
				}
			}

			@Override
			void interceptSetupSpecMethod(final IMethodInvocation invocation) throws Throwable
			{
				this.interceptSetupMethod(invocation)
			}

			@Override
			void interceptCleanupSpecMethod(final IMethodInvocation invocation) throws Throwable
			{
				this.interceptCleanupMethod(invocation)
			}
		}
		if (field.isShared())
		{
			specInfo.addSetupSpecInterceptor(interceptor)
			specInfo.addCleanupSpecInterceptor(interceptor)
		}
		else
		{
			specInfo.addSetupInterceptor(interceptor)
			specInfo.addCleanupInterceptor(interceptor)
		}
	}

	static def defaultBase()
	{
		if (new File("target").exists())
		{
			return "target/test-temp"
		}
		else
		{
			return "build/test-temp"
		}
	}
}
