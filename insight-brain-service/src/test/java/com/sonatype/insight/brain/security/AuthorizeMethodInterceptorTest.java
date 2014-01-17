/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Map;

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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyMapOf;
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
  public String stubSomeContext(@AuthzContext(AuthzContext.Key.TYPE) String arg0,
      @AuthzContext(AuthzContext.Key.ID) String arg1, String arg2)
  {
    return arg0;
  }

  @Authorize(permission = Permission.READ)
  public String stubErrorMessage(@AuthzErrorMsg boolean forceSuccess) {
    return "test";
  }

  @Authorize(permission = Permission.READ)
  @AuthzErrorMsg
  public String stubErrorMessage() {
    return "test";
  }

  @Before
  public void init() {
    invoc = mock(MethodInvocation.class);
    when(invoc.getThis()).thenReturn(this);
    authzChecker = mock(AuthorizationChecker.class);
    interceptor = new AuthorizeMethodInterceptor(authzChecker);
    subject = mock(Subject.class);
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
    assertThat(params.entrySet(), is(empty()));
  }

  @Test
  public void testGetContextParameters_Some() throws Exception {
    when(invoc.getMethod()).thenReturn(
        getClass().getMethod("stubSomeContext", String.class, String.class, String.class));
    when(invoc.getArguments()).thenReturn(new Object[] { "app", "dev", "foo" });
    Map<AuthzContext.Key, Object> params = AuthorizeMethodInterceptor.getContextParameters(invoc);
    assertThat(params.keySet(), containsInAnyOrder(AuthzContext.Key.TYPE, AuthzContext.Key.ID));
    assertThat(params.get(AuthzContext.Key.TYPE), is((Object) "app"));
    assertThat(params.get(AuthzContext.Key.ID), is((Object) "dev"));
  }

  @Test
  public void testInvoke_Pass() throws Throwable {
    when(invoc.getMethod()).thenReturn(getClass().getMethod("stubNoContext", String.class));
    when(invoc.getArguments()).thenReturn(new Object[] { "test" });
    when(invoc.proceed()).thenReturn("test");
    when(authzChecker.isPermitted(any(UserPrincipal.class), any(Permission.class), anyMapOf(AuthzContext.Key.class, Object.class)))
        .thenReturn(true);
    when(subject.getPrincipal()).thenReturn(adminPrincipal());
    assertThat(interceptor.invoke(invoc), is((Object) "test"));
  }

  @Test
  public void testInvoke_FailWithException() throws Throwable {
    when(invoc.getMethod()).thenReturn(getClass().getMethod("stubNoContext", String.class));
    when(invoc.getArguments()).thenReturn(new Object[] { "test" });
    when(invoc.proceed()).thenReturn("test");
    when(authzChecker.isPermitted(any(UserPrincipal.class), any(Permission.class), anyListOf(String.class))).thenReturn(false);
    when(subject.getPrincipal()).thenReturn(adminPrincipal());
    try {
      interceptor.invoke(invoc);
      fail("Should have thrown UnauthorizedException");
    }
    catch (UnauthorizedException e) {
      assertThat(e.getMessage(), is("Insufficient permissions"));
    }
  }

  @Test
  public void testInvoke_FailWithMessage_ParameterAnno() throws Throwable {
    when(invoc.getMethod()).thenReturn(getClass().getMethod("stubErrorMessage", Boolean.TYPE));
    when(invoc.getArguments()).thenReturn(new Object[] { Boolean.TRUE });
    when(invoc.proceed()).thenReturn("test");
    when(authzChecker.isPermitted(any(UserPrincipal.class), any(Permission.class), anyListOf(String.class))).thenReturn(false);
    when(subject.getPrincipal()).thenReturn(adminPrincipal());
    assertThat(interceptor.invoke(invoc), is((Object) "Insufficient permissions"));

    when(invoc.getArguments()).thenReturn(new Object[] { Boolean.FALSE });
    try {
      interceptor.invoke(invoc);
      fail("Should have thrown UnauthorizedException");
    }
    catch (UnauthorizedException e) {
      assertThat(e.getMessage(), is("Insufficient permissions"));
    }
  }

  @Test
  public void testInvoke_FailWithMessage_MethodAnno() throws Throwable {
    when(invoc.getMethod()).thenReturn(getClass().getMethod("stubErrorMessage"));
    when(invoc.getArguments()).thenReturn(new Object[] {});
    when(invoc.proceed()).thenReturn("test");
    when(authzChecker.isPermitted(any(UserPrincipal.class), any(Permission.class), anyListOf(String.class))).thenReturn(false);
    when(subject.getPrincipal()).thenReturn(adminPrincipal());
    assertThat(interceptor.invoke(invoc), is((Object) "Insufficient permissions"));
  }

  @Test
  public void testInvoke_NoPrincipal() throws Throwable {
    when(invoc.getMethod()).thenReturn(getClass().getMethod("stubNoContext", String.class));
    when(invoc.getArguments()).thenReturn(new Object[] { "test" });
    when(authzChecker.isPermitted(any(UserPrincipal.class), any(Permission.class), anyListOf(String.class))).thenReturn(true);
    try {
      interceptor.invoke(invoc);
      fail("Should have thrown UnauthenticatedException");
    }
    catch (UnauthenticatedException e) {
      assertThat(e.getMessage(), is("Anonymous access forbidden"));
    }
  }

  private UserPrincipal adminPrincipal() {
    return new UserPrincipal("admin", "Admin BuiltIn", true);
  }
}
