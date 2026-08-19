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
public class AuthzFilterAspect
{
  private volatile AuthzFilterMethodInterceptor interceptor;

  @jakarta.inject.Inject
  void setInterceptor(final AuthzFilterMethodInterceptor interceptor) {
    this.interceptor = interceptor;
  }

  @Around("@annotation(com.sonatype.insight.brain.security.AuthzFilter)")
  public Object filter(final ProceedingJoinPoint joinPoint) throws Throwable {
    return SecurityAspectSupport.execute(joinPoint, interceptor);
  }
}
