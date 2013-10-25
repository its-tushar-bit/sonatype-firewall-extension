/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.sonatype.insight.brain.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;

import org.apache.shiro.aop.MethodInvocation;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthzFilterMethodInterceptorTest
{
  private MethodInvocation invoc;

  private AuthorizationChecker authzChecker;

  private Subject subject;

  private AuthzFilterMethodInterceptor interceptor;

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  @AuthzFilter(permission = Permission.READ, context = AuthzFilter.Context.ORGANIZATION)
  public Collection<Application> stubOrgs() {
    return null;
  }

  @Before
  public void init() {
    invoc = mock(MethodInvocation.class);
    when(invoc.getThis()).thenReturn(this);
    authzChecker = mock(AuthorizationChecker.class);
    interceptor = new AuthzFilterMethodInterceptor(authzChecker);
    subject = mock(Subject.class);
    ThreadContext.bind(subject);
  }

  @After
  public void exit() {
    ThreadContext.unbindSubject();
  }

  @Test
  public void testInvoke_NullReturnValue() throws Throwable {
    when(invoc.getMethod()).thenReturn(getClass().getMethod("stubOrgs"));
    when(invoc.getArguments()).thenReturn(new Object[0]);
    when(invoc.proceed()).thenReturn(null);
    when(subject.getPrincipal()).thenReturn("admin");
    assertThat(interceptor.invoke(invoc), is(nullValue()));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testInvoke_FilteredReturnValue() throws Throwable {
    Collection<Organization> entities = Arrays.asList(tempEntity.newOrganization());
    when(invoc.getMethod()).thenReturn(getClass().getMethod("stubOrgs"));
    when(invoc.getArguments()).thenReturn(new Object[0]);
    when(invoc.proceed()).thenReturn(entities);
    when(subject.getPrincipal()).thenReturn("john");
    when(
        authzChecker.filterByPermission(eq("john"), eq(Permission.READ), eq(entities),
            eq(AuthzFilter.Context.ORGANIZATION))).thenReturn(Collections.EMPTY_LIST);
    assertThat((Collection<?>) interceptor.invoke(invoc), is(empty()));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testInvoke_NoPrincipal() throws Throwable {
    Collection<Organization> entities = Arrays.asList(tempEntity.newOrganization());
    when(invoc.getMethod()).thenReturn(getClass().getMethod("stubOrgs"));
    when(invoc.getArguments()).thenReturn(new Object[0]);
    when(invoc.proceed()).thenReturn(entities);
    when(
        authzChecker.filterByPermission(isNull(String.class), eq(Permission.READ), eq(entities),
            eq(AuthzFilter.Context.ORGANIZATION))).thenReturn(Collections.EMPTY_LIST);
    assertThat((Collection<?>) interceptor.invoke(invoc), is(empty()));
  }
}
