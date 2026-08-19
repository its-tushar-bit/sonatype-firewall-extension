/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.api.v2.HasFeatureMethodInterceptor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class HasFeatureAspect
{
  private volatile HasFeatureMethodInterceptor interceptor;

  @jakarta.inject.Inject
  void setInterceptor(final HasFeatureMethodInterceptor interceptor) {
    this.interceptor = interceptor;
  }

  @Around("execution(* *(..)) && @annotation(com.sonatype.insight.brain.api.v2.HasFeature)")
  public Object checkFeatureOnMethod(final ProceedingJoinPoint joinPoint) throws Throwable {
    return SecurityAspectSupport.execute(joinPoint, interceptor);
  }

  @Around("execution(* *(..)) && @within(com.sonatype.insight.brain.api.v2.HasFeature) && "
      + "!@annotation(com.sonatype.insight.brain.api.v2.HasFeature) && !within(is(InterfaceType))")
  public Object checkFeatureOnClass(final ProceedingJoinPoint joinPoint) throws Throwable {
    return SecurityAspectSupport.execute(joinPoint, interceptor);
  }
}
