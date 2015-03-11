/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.model.security.UserPrincipal;

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

  private final boolean anonymousClientAccessAllowed;

  public AuthzFilterMethodInterceptor(AnnotationResolver resolver, boolean anonymousClientAccessAllowed) {
    this(new AuthzFilterAnnotationHandler(), resolver, new AuthorizationChecker(), anonymousClientAccessAllowed);
  }

  AuthzFilterMethodInterceptor(AuthorizationChecker authzChecker) {
    this(new AuthzFilterAnnotationHandler(), null, authzChecker, true);
  }

  private AuthzFilterMethodInterceptor(AuthzFilterAnnotationHandler handler, AnnotationResolver resolver,
      AuthorizationChecker authzChecker, boolean anonymousClientAccessAllowed)
  {
    super(handler, resolver);
    this.authzChecker = authzChecker;
    this.anonymousClientAccessAllowed = anonymousClientAccessAllowed;
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
        UserPrincipal user = (UserPrincipal)((principal != null) ? principal : null);
        if (!isAnonymous(user, anno)) {
          result = authzChecker.filterByPermission(user, anno.permission(), result, anno.context());
        }
      }
    }
    return result;
  }

  private boolean isAnonymous(UserPrincipal user, AuthzFilter anno) {
    return user == null && anno.anonymousAllowed() && anonymousClientAccessAllowed;
  }
}
