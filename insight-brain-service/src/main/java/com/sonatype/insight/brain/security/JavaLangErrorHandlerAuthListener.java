/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.jaxrs.error.JavaLangErrorHandler;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationListener;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.PrincipalCollection;

@Named
class JavaLangErrorHandlerAuthListener
    implements AuthenticationListener
{
  private final JavaLangErrorHandler javaLangErrorHandler;

  @Inject
  JavaLangErrorHandlerAuthListener(JavaLangErrorHandler javaLangErrorHandler) {
    this.javaLangErrorHandler = javaLangErrorHandler;
  }

  @Override
  public void onSuccess(AuthenticationToken token, AuthenticationInfo info) {
    // Nothing to do
  }

  @Override
  public void onLogout(PrincipalCollection principals) {
    // Nothing to do
  }

  @Override
  public void onFailure(AuthenticationToken token, AuthenticationException authenticationException) {
    javaLangErrorHandler.handle(authenticationException);
  }
}
