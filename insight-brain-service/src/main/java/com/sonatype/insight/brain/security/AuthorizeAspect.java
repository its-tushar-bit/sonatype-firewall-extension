/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class AuthorizeAspect
{
  private volatile AuthorizeMethodInterceptor interceptor;

  @jakarta.inject.Inject
  void setInterceptor(final AuthorizeMethodInterceptor interceptor) {
    this.interceptor = interceptor;
  }

  @Around("@annotation(com.sonatype.insight.brain.security.Authorize)")
  public Object authorize(final ProceedingJoinPoint joinPoint) throws Throwable {
    return SecurityAspectSupport.execute(joinPoint, interceptor);
  }
}
