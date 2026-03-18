/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.lang.annotation.Annotation;
import java.util.EnumMap;
import java.util.Map;

import com.sonatype.insight.brain.model.security.UserPrincipal;

import org.apache.shiro.aop.AnnotationMethodInterceptor;
import org.apache.shiro.aop.AnnotationResolver;
import org.apache.shiro.aop.MethodInvocation;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;

/**
 * AOP-based method interceptor to evaluate {@link Authorize} annotations.
 *
 * @since 1.7
 */
class AuthorizeMethodInterceptor
    extends AnnotationMethodInterceptor
{
  private final AuthorizationChecker authzChecker;

  public AuthorizeMethodInterceptor(AnnotationResolver resolver, AuthorizationChecker authzChecker) {
    this(new AuthorizeAnnotationHandler(), resolver, authzChecker);
  }

  AuthorizeMethodInterceptor(AuthorizationChecker authzChecker) {
    this(new AuthorizeAnnotationHandler(), null, authzChecker);
  }

  private AuthorizeMethodInterceptor(
      AuthorizeAnnotationHandler handler,
      AnnotationResolver resolver,
      AuthorizationChecker authzChecker)
  {
    super(handler, resolver);
    this.authzChecker = authzChecker;
  }

  @Override
  protected Authorize getAnnotation(MethodInvocation mi) {
    return (Authorize) super.getAnnotation(mi);
  }

  @Override
  public Object invoke(MethodInvocation mi) throws Throwable {
    Authorize anno = getAnnotation(mi);
    assertAuthorized(mi, anno);
    return mi.proceed();
  }

  static Map<AuthzContext.Key, Object> getContextParameters(MethodInvocation mi) {
    Map<AuthzContext.Key, Object> parameters = new EnumMap<>(AuthzContext.Key.class);
    Annotation[][] paramAnnos = mi.getMethod().getParameterAnnotations();
    for (int i = 0; i < paramAnnos.length; i++) {
      for (Annotation anno : paramAnnos[i]) {
        if (anno instanceof AuthzContext) {
          AuthzContext ac = (AuthzContext) anno;
          parameters.put(ac.value(), mi.getArguments()[i]);
          break;
        }
      }
    }
    return parameters;
  }

  private void assertAuthorized(MethodInvocation mi, Authorize anno) throws AuthorizationException {
    Object principal = getSubject().getPrincipal();
    if (principal == null) {
      throw new UnauthenticatedException("Anonymous access forbidden", newAuthzException(mi));
    }
    UserPrincipal user = (UserPrincipal) principal;
    Map<AuthzContext.Key, Object> contextParameters = getContextParameters(mi);
    if (!authzChecker.isPermitted(user, anno.permission(), contextParameters)) {
      throw new UnauthorizedException("Insufficient permissions", newAuthzException(mi));
    }
  }

  private static Throwable newAuthzException(MethodInvocation mi) {
    return new AuthorizationException("Not authorized to invoke method: " + mi.getMethod());
  }
}
