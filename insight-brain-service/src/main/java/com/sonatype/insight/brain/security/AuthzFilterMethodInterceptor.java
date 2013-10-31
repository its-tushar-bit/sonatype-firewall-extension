/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import org.apache.shiro.aop.AnnotationMethodInterceptor;
import org.apache.shiro.aop.AnnotationResolver;
import org.apache.shiro.aop.MethodInvocation;

/**
 * AOP-based method interceptor to evaluate {@link AuthzFilter} annotations.
 * 
 * @since 1.7
 */
class AuthzFilterMethodInterceptor
    extends AnnotationMethodInterceptor
{
  private final AuthorizationChecker authzChecker;

  public AuthzFilterMethodInterceptor(AnnotationResolver resolver) {
    this(new AuthzFilterAnnotationHandler(), resolver, new AuthorizationChecker());
  }

  AuthzFilterMethodInterceptor(AuthorizationChecker authzChecker) {
    this(new AuthzFilterAnnotationHandler(), null, authzChecker);
  }

  private AuthzFilterMethodInterceptor(AuthzFilterAnnotationHandler handler, AnnotationResolver resolver,
      AuthorizationChecker authzChecker)
  {
    super(handler, resolver);
    this.authzChecker = authzChecker;
  }

  @Override
  protected AuthzFilter getAnnotation(MethodInvocation mi) {
    return (AuthzFilter) super.getAnnotation(mi);
  }

  @Override
  public Object invoke(MethodInvocation mi) throws Throwable {
    Object result = mi.proceed();
    if (result != null) {
      AuthzFilter anno = getAnnotation(mi);
      if (anno != null) {
        Object principal = getSubject().getPrincipal();
        String username = (principal != null) ? principal.toString() : null;
        result = authzChecker.filterByPermission(username, anno.permission(), result, anno.context());
      }
    }
    return result;
  }
}
