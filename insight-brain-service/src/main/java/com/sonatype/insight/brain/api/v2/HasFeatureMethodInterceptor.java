/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.shiro.aop.AnnotationHandler;
import org.apache.shiro.aop.AnnotationMethodInterceptor;
import org.apache.shiro.aop.AnnotationResolver;
import org.apache.shiro.aop.MethodInvocation;

/**
 * AOP-based method interceptor to evaluate {@link HasFeature} annotations used to enforce feature flag checks.
 * Example use: @HasFeature(SystemConfigurationPropertyFeature.CODE_INSIGHTS)
 * Can be used on method or class declarations, intended for resource classes
 */
public class HasFeatureMethodInterceptor
    extends AnnotationMethodInterceptor
{
  public HasFeatureMethodInterceptor() {
    this(null);
  }

  public HasFeatureMethodInterceptor(AnnotationResolver resolver) {
    this(new HasFeatureAnnotationHandler(), resolver);
  }

  public HasFeatureMethodInterceptor(AnnotationHandler handler, AnnotationResolver resolver) {
    super(handler, resolver);
  }

  @Override
  protected HasFeature getAnnotation(MethodInvocation methodInvocation) {
    HasFeature feature = (HasFeature) super.getAnnotation(methodInvocation);
    if (feature != null) {
      return feature;
    }
    Class<?> clazz = methodInvocation.getMethod().getDeclaringClass();
    return clazz.getDeclaredAnnotation(HasFeature.class);
  }

  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    HasFeature annotation = getAnnotation(methodInvocation);
    assertHasFeature(annotation);
    return methodInvocation.proceed();
  }

  private void assertHasFeature(HasFeature annotation) throws NotFoundException {
    if (annotation == null) {
      // No feature flag to check
      return;
    }
    SystemConfigurationPropertyFeature feature = annotation.value();
    if (!feature.isEnabled()) {
      throw new NotFoundException("Feature not supported.");
    }
  }
}
