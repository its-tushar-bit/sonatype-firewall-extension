/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.test;

import java.lang.reflect.Method;

/**
 * Tracks the currently executing Spring-injected JUnit test so fixture-backed Spring bean
 * initialization can resolve the correct test-level annotations during context startup.
 */
public final class SpringTestExecutionContext
{
  private static final ThreadLocal<Class<?>> TEST_CLASS = new ThreadLocal<>();

  private static final ThreadLocal<Method> TEST_METHOD = new ThreadLocal<>();

  private static final ThreadLocal<Object> TEST_INSTANCE = new ThreadLocal<>();

  private SpringTestExecutionContext() {
    // utility class
  }

  public static void setCurrentTestClass(final Class<?> testClass) {
    TEST_CLASS.set(testClass);
  }

  public static Class<?> getCurrentTestClass() {
    return TEST_CLASS.get();
  }

  public static void clearCurrentTestClass() {
    TEST_CLASS.remove();
  }

  public static void setCurrentTestMethod(final Method testMethod) {
    TEST_METHOD.set(testMethod);
  }

  public static Method getCurrentTestMethod() {
    return TEST_METHOD.get();
  }

  public static void clearCurrentTestMethod() {
    TEST_METHOD.remove();
  }

  public static void setCurrentTestInstance(final Object testInstance) {
    TEST_INSTANCE.set(testInstance);
  }

  public static Object getCurrentTestInstance() {
    return TEST_INSTANCE.get();
  }

  public static void clearCurrentTestInstance() {
    TEST_INSTANCE.remove();
  }
}
