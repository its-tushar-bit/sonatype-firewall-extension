/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import org.apache.shiro.aop.AnnotationResolver;
import org.apache.shiro.guice.aop.ShiroAopModule;

/**
 * Binds AOP interceptors to enforce authorization.
 * 
 * @since 1.7
 */
public class SecurityAopModule
    extends ShiroAopModule
{
  @Override
  protected void configureInterceptors(AnnotationResolver resolver) {
    AuthorizationChecker authzChecker = new AuthorizationChecker();
    bind(AuthorizationChecker.class).toInstance(authzChecker);
    bindShiroInterceptor(new AuthorizeMethodInterceptor(resolver, authzChecker));
    bindShiroInterceptor(new AuthzFilterMethodInterceptor(resolver, authzChecker));
  }
}
