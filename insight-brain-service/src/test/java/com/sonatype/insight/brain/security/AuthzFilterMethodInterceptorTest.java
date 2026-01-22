/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;

import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;

import org.apache.shiro.aop.MethodInvocation;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthzFilterMethodInterceptorTest
    extends AbstractDataTest
{
  private MethodInvocation invoc;

  private AuthorizationChecker authzChecker;

  private Subject subject;

  private AuthzFilterMethodInterceptor interceptor;

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
  public void testInvoke_NullReturnValue() throws Throwable {
    when(invoc.getMethod()).thenReturn(getClass().getMethod("stubOrgs"));
    when(invoc.getArguments()).thenReturn(new Object[0]);
    when(invoc.proceed()).thenReturn(null);
    when(subject.getPrincipal()).thenReturn("admin");
    assertThat(interceptor.invoke(invoc)).isNull();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testInvoke_FilteredReturnValue() throws Throwable {
    Collection<Organization> entities = Collections.singletonList(tempEntity.newOrganization());
    when(invoc.getMethod()).thenReturn(getClass().getMethod("stubOrgs"));
    when(invoc.getArguments()).thenReturn(new Object[0]);
    when(invoc.proceed()).thenReturn(entities);
    UserPrincipal principal = new UserPrincipal("john", "John Smith", InternalRealm.ID);
    when(subject.getPrincipal()).thenReturn(principal);
    when(
        authzChecker.filterByPermission(eq(principal), eq(Permission.READ), eq(entities),
            eq(AuthzFilter.Context.ORGANIZATION))).thenReturn(Collections.EMPTY_LIST);
    assertThat((Collection<?>) interceptor.invoke(invoc)).isEmpty();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testInvoke_NoPrincipal() throws Throwable {
    Collection<Organization> entities = Collections.singletonList(tempEntity.newOrganization());
    when(invoc.getMethod()).thenReturn(getClass().getMethod("stubOrgs"));
    when(invoc.getArguments()).thenReturn(new Object[0]);
    when(invoc.proceed()).thenReturn(entities);
    when(authzChecker.filterByPermission(isNull(), eq(Permission.READ), eq(entities),
        eq(AuthzFilter.Context.ORGANIZATION))).thenReturn(Collections.EMPTY_LIST);
    assertThat((Collection<?>) interceptor.invoke(invoc)).isEmpty();
  }
}
