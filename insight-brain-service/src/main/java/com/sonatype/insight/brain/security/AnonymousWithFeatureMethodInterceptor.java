/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.aop.AnnotationMethodInterceptor;
import org.apache.shiro.aop.AnnotationResolver;
import org.apache.shiro.aop.MethodInvocation;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthenticatedException;

/**
 * AOP-based method interceptor to evaluate {@link AnonymousWithFeature} annotations.
 */
class AnonymousWithFeatureMethodInterceptor
    extends AnnotationMethodInterceptor
{
  public AnonymousWithFeatureMethodInterceptor(final AnnotationResolver resolver) {
    super(new AnonymousWithFeatureAnnotationHandler(), resolver);
  }

  @Override
  protected AnonymousWithFeature getAnnotation(final MethodInvocation methodInvocation) {
    return (AnonymousWithFeature) super.getAnnotation(methodInvocation);
  }

  @Override
  public Object invoke(final MethodInvocation methodInvocation) throws Throwable {
    assertAnonymousWithFeature(getAnnotation(methodInvocation));
    return methodInvocation.proceed();
  }

  private void assertAnonymousWithFeature(
      final AnonymousWithFeature anonymousWithFeature) throws AuthorizationException
  {
    Object principal = SecurityUtils.getSubject().getPrincipal();
    if (principal != null) {
      return;
    }
    SystemConfigurationPropertyFeature value = anonymousWithFeature.value();
    if (!value.isEnabled()) {
      throw new UnauthenticatedException(
          String.format("Anonymous access requires %s to be enabled.", value.getPropertyName()));
    }
  }
}
