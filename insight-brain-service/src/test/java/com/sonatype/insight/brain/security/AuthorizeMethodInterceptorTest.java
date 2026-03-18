/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Map;
import java.util.concurrent.Callable;

import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;

import org.apache.shiro.aop.MethodInvocation;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthorizeMethodInterceptorTest
{
  private MethodInvocation invoc;

  private AuthorizationChecker authzChecker;

  private Subject subject;

  private AuthorizeMethodInterceptor interceptor;

  @Authorize(permission = Permission.READ)
  public String stubNoContext(String arg0) {
    return arg0;
  }

  @Authorize(permission = Permission.READ)
  public String stubSomeContext(
      @AuthzContext(AuthzContext.Key.TYPE) String arg0,
      @SuppressWarnings("unused") @AuthzContext(AuthzContext.Key.ID) String arg1,
      @SuppressWarnings("unused") String arg2)
  {
    return arg0;
  }

  @Before
  public void init() {
    invoc = mock(MethodInvocation.class);
    when(invoc.getThis()).thenReturn(this);
    authzChecker = mock(AuthorizationChecker.class);
    interceptor = new AuthorizeMethodInterceptor(authzChecker);
    subject = mock(Subject.class);
    // Support for TenantAwareOneTimeRunnable which uses subject.associateWith() to propagate security context
    // to worker threads (required since Shiro 2.0.4+ removed InheritableThreadLocal from ThreadContext)
    lenient().when(subject.associateWith(any(Runnable.class))).thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(subject.associateWith(any(Callable.class))).thenAnswer(invocation -> invocation.getArgument(0));
    ThreadContext.bind(subject);
  }

  @After
  public void exit() {
    ThreadContext.unbindSubject();
  }

  @Test
  public void testGetContextParameters_None() throws Exception {
    when(invoc.getMethod()).thenReturn(getClass().getMethod("stubNoContext", String.class));
    Map<AuthzContext.Key, Object> params = AuthorizeMethodInterceptor.getContextParameters(invoc);
    assertThat(params).isEmpty();
  }

  @Test
  public void testGetContextParameters_Some() throws Exception {
    when(invoc.getMethod()).thenReturn(
        getClass().getMethod("stubSomeContext", String.class, String.class, String.class));
    when(invoc.getArguments()).thenReturn(new Object[]{"app", "dev", "foo"});
    Map<AuthzContext.Key, Object> params = AuthorizeMethodInterceptor.getContextParameters(invoc);
    assertThat(params).containsOnlyKeys(AuthzContext.Key.TYPE, AuthzContext.Key.ID)
        .containsEntry(AuthzContext.Key.TYPE, "app")
        .containsEntry(AuthzContext.Key.ID, "dev");
  }

  @Test
  public void testInvoke_Pass() throws Throwable {
    when(invoc.getMethod()).thenReturn(getClass().getMethod("stubNoContext", String.class));
    when(invoc.getArguments()).thenReturn(new Object[]{"test"});
    when(invoc.proceed()).thenReturn("test");
    when(authzChecker.isPermitted(any(UserPrincipal.class), any(Permission.class), anyMap())).thenReturn(true);
    when(subject.getPrincipal()).thenReturn(adminPrincipal());
    assertThat(interceptor.invoke(invoc)).isEqualTo("test");
  }

  @Test
  public void testInvoke_FailWithException() throws Throwable {
    when(invoc.getMethod()).thenReturn(getClass().getMethod("stubNoContext", String.class));
    when(invoc.getArguments()).thenReturn(new Object[]{"test"});
    when(invoc.proceed()).thenReturn("test");
    when(authzChecker.isPermitted(any(UserPrincipal.class), any(Permission.class), anyList())).thenReturn(false);
    when(subject.getPrincipal()).thenReturn(adminPrincipal());
    assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(() -> interceptor.invoke(invoc))
        .withMessage("Insufficient permissions");
  }

  @Test
  public void testInvoke_NoPrincipal() throws Throwable {
    when(invoc.getMethod()).thenReturn(getClass().getMethod("stubNoContext", String.class));
    when(invoc.getArguments()).thenReturn(new Object[]{"test"});
    when(authzChecker.isPermitted(any(UserPrincipal.class), any(Permission.class), anyList())).thenReturn(true);
    assertThatExceptionOfType(UnauthenticatedException.class).isThrownBy(() -> interceptor.invoke(invoc))
        .withMessage("Anonymous access forbidden");
  }

  private UserPrincipal adminPrincipal() {
    return new UserPrincipal("admin", "Admin BuiltIn", InternalRealm.ID);
  }
}
