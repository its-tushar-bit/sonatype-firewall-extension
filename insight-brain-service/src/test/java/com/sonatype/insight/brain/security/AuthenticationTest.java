/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collections;
import java.util.Set;

import com.sonatype.insight.jaxrs.error.JavaLangErrorHandler;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationListener;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.lang.util.LifecycleUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for authentication aspects that are not limited or specific to a single class.
 */
public class AuthenticationTest
{
  private SecurityManager securityManager;

  private JavaLangErrorHandler javaLangErrorHandler;

  private Realm mockRealm;

  private Subject subject;

  @BeforeEach
  public void setUp() {
    javaLangErrorHandler = new JavaLangErrorHandler();
    javaLangErrorHandler.setExitOnFatalErrorSupplier(() -> false);

    mockRealm = mock(Realm.class);

    // Set up authentication listener that handles java.lang.Error
    JavaLangErrorHandlerAuthListener authListener = new JavaLangErrorHandlerAuthListener(javaLangErrorHandler);
    Set<AuthenticationListener> listeners = Collections.singleton(authListener);
    Set<Realm> realms = Collections.singleton(mockRealm);

    // Create the authenticator used in production
    FirstSuccessfulRealmAuthenticator authenticator = new FirstSuccessfulRealmAuthenticator(realms, listeners);

    // Create security manager — realms are not set on the manager directly; they reach it
    // through FirstSuccessfulRealmAuthenticator, matching production wiring.
    DefaultWebSecurityManager webSecurityManager = new DefaultWebSecurityManager();
    webSecurityManager.setAuthenticator(authenticator);
    webSecurityManager.setRememberMeManager(null);
    this.securityManager = webSecurityManager;

    ThreadContext.bind(securityManager);
    subject = new Subject.Builder(securityManager).buildSubject();
    ThreadContext.bind(subject);
  }

  @AfterEach
  public void tearDown() {
    ThreadContext.unbindSecurityManager();
    ThreadContext.unbindSubject();
    LifecycleUtils.destroy(securityManager);
  }

  @Test
  public void testJavaLangErrorThrownDuringAuthentication() {
    Error error = new OutOfMemoryError("Test");
    when(mockRealm.supports(any(AuthenticationToken.class))).thenThrow(new RuntimeException(error));

    assertThatExceptionOfType(AuthenticationException.class).isThrownBy(
        () -> subject.login(new UsernamePasswordToken("username", "password")));
    // The java.lang.Error must get to the handler for Errors,
    // which in a real system will (probably) terminate the JVM.
    assertThat(javaLangErrorHandler.getLastFatalError()).isEqualTo(error);
  }

  @Test
  public void testHandleExit_whenExitOnFatalErrorIsFalse_doesNotCallRuntimeExit() {
    javaLangErrorHandler.setExitOnFatalErrorSupplier(() -> false);
    Runtime mockRuntime = mock(Runtime.class);
    javaLangErrorHandler.handleExit(mockRuntime);
    verify(mockRuntime, never()).exit(1);
  }
}
