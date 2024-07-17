/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import org.apache.shiro.aop.AnnotationHandler;

/**
 * Supports {@link HasFeatureMethodInterceptor} and only fulfills demands of the Shiro API.
 *
 * @since saas-next
 */
class HasFeatureAnnotationHandler
    extends AnnotationHandler
{
  public HasFeatureAnnotationHandler() {
    super(HasFeature.class);
  }
}
