/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.testing.BrainInjectedTest;
import com.sonatype.insight.jaxrs.error.JavaLangErrorHandler;

import com.google.inject.Binder;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.lang.util.LifecycleUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
    extends BrainInjectedTest
{
  @Inject
  private SecurityManager securityManager;

  @Inject
  private JavaLangErrorHandler javaLangErrorHandler;

  private Realm mockRealm;

  private Subject subject;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    InsightConfig config = new InsightConfig();
    binder.bind(InsightConfig.class).toInstance(config);

    mockRealm = mock(Realm.class);
    SecurityModule securityModule = new SecurityModule()
    {
      @Override
      protected void configureShiro() {
        super.configureShiro();
        bindRealm().toInstance(mockRealm);
      }
    };
    binder.install(securityModule);
  }

  @Before
  public void setUpSecurity() {
    javaLangErrorHandler.setExitOnFatalErrorSupplier(() -> false);
    ThreadContext.bind(securityManager);
    subject = (new Subject.Builder()).buildSubject();
    ThreadContext.bind(subject);
  }

  @After
  public void tearDownSecurity() {
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
