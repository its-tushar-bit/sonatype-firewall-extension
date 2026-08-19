/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import org.apache.shiro.aop.AnnotationHandler;

/**
 * Supports {@link AnonymousWithFeatureMethodInterceptor} and only fulfills demands of the Shiro API.
 */
class AnonymousWithFeatureAnnotationHandler
    extends AnnotationHandler
{
  public AnonymousWithFeatureAnnotationHandler() {
    super(AnonymousWithFeature.class);
  }
}
