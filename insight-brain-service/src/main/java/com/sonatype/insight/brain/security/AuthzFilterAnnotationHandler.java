/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import org.apache.shiro.aop.AnnotationHandler;

/**
 * Supports {@link AuthzFilterMethodInterceptor} and only fulfills demands of the Shiro API.
 *
 * @since 1.7
 */
class AuthzFilterAnnotationHandler
    extends AnnotationHandler
{
  public AuthzFilterAnnotationHandler() {
    super(AuthzFilter.class);
  }
}
