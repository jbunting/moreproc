package io.bunting.prochelp.util

import org.spockframework.runtime.extension.ExtensionAnnotation

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Creates a temporary directory.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@ExtensionAnnotation(TmpDirExtension.class)
@interface TmpDir {
	String base() default ""
	boolean clean() default false
}
