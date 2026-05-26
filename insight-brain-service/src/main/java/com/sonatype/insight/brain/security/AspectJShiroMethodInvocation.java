/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.lang.reflect.Method;

import org.apache.shiro.aop.MethodInvocation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * Bridges an AspectJ {@link ProceedingJoinPoint} to Shiro's {@link MethodInvocation} interface,
 * allowing the existing Shiro-based interceptors to work with compile-time woven aspects.
 */
final class AspectJShiroMethodInvocation
    implements MethodInvocation
{
  private final ProceedingJoinPoint joinPoint;

  private final Method method;

  AspectJShiroMethodInvocation(final ProceedingJoinPoint joinPoint) {
    this.joinPoint = joinPoint;
    this.method = ((MethodSignature) joinPoint.getSignature()).getMethod();
  }

  @Override
  public Object proceed() throws Throwable {
    return joinPoint.proceed();
  }

  @Override
  public Method getMethod() {
    return method;
  }

  @Override
  public Object[] getArguments() {
    return joinPoint.getArgs();
  }

  @Override
  public Object getThis() {
    return joinPoint.getThis();
  }
}
