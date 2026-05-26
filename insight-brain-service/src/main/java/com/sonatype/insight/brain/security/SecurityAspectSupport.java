/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import org.apache.shiro.aop.AnnotationMethodInterceptor;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * Shared execution logic for CTW security aspects. Each aspect delegates here to avoid
 * duplicating the enforcement-disabled check, null-interceptor guard, and Shiro dispatch.
 */
final class SecurityAspectSupport
{
  private SecurityAspectSupport() {
  }

  static Object execute(
      ProceedingJoinPoint joinPoint,
      AnnotationMethodInterceptor interceptor) throws Throwable
  {
    if (SecurityAspectControl.isEnforcementDisabled()) {
      return joinPoint.proceed();
    }
    if (interceptor == null) {
      return joinPoint.proceed();
    }
    AspectJShiroMethodInvocation invocation = new AspectJShiroMethodInvocation(joinPoint);
    if (interceptor.supports(invocation)) {
      return interceptor.invoke(invocation);
    }
    return joinPoint.proceed();
  }
}
