/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.servlet.http.HttpServletRequest;

import com.sonatype.insight.brain.model.security.UserPrincipal;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuditUtilsTest
{
  private Subject subject;

  @Before
  public void init() {
    subject = mock(Subject.class);
    ThreadContext.bind(subject);
  }

  @After
  public void exit() {
    ThreadContext.unbindSubject();
  }

  @Test
  public void testFindIP() {
    HttpServletRequest request = mock(HttpServletRequest.class);

    when(request.getHeader(eq(AuditUtils.XFF_HEADER))).thenReturn(null);
    assertThat(AuditUtils.findIP(request), is(nullValue()));

    when(request.getHeader(eq(AuditUtils.XFF_HEADER))).thenReturn("");
    assertThat(AuditUtils.findIP(request), is(nullValue()));

    when(request.getHeader(eq(AuditUtils.XFF_HEADER))).thenReturn("{unknown}, 127.0.0.1, {unknown1}");
    assertThat(AuditUtils.findIP(request), is("127.0.0.1"));

    // IPs that start with "[" are considered IPv6, so this is a special case
    when(request.getHeader(eq(AuditUtils.XFF_HEADER))).thenReturn("[missing], 127.0.0.1, {unknown}");
    assertThat(AuditUtils.findIP(request), is("127.0.0.1"));

    when(request.getHeader(eq(AuditUtils.XFF_HEADER))).thenReturn(null);
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    assertThat(AuditUtils.findIP(request), is("127.0.0.1"));

    when(request.getHeader(eq(AuditUtils.XFF_HEADER))).thenReturn("{unknown}");
    assertThat(AuditUtils.findIP(request), is("127.0.0.1"));

    when(request.getHeader(eq(AuditUtils.XFF_HEADER))).thenReturn("[missing]");
    assertThat(AuditUtils.findIP(request), is("127.0.0.1"));
  }

  @Test
  public void testResolveIP() {
    assertThat(AuditUtils.resolveIP((String[]) null), is(nullValue()));
    assertThat(AuditUtils.resolveIP((String) null), is(nullValue()));
    assertThat(AuditUtils.resolveIP(new String[0]), is(nullValue()));
    assertThat(AuditUtils.resolveIP("{unknown}", "127.0.0.1", "{unknown1}"), is("127.0.0.1"));
    // IPs that start with "[" are considered IPv6, so this is a special case
    assertThat(AuditUtils.resolveIP("[missing]", "127.0.0.1", "{unknown}"), is("127.0.0.1"));
  }

  @Test
  public void testResolveLeftMostIP() {
    assertThat(AuditUtils.resolveIP("127.0.0.1", "127.0.0.2", "127.0.0.3"), is("127.0.0.1"));
  }

  @Test
  public void testFindLeftMostIP() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(eq(AuditUtils.XFF_HEADER))).thenReturn("127.0.0.1, 127.0.0.2, 127.0.0.3");
    assertThat(AuditUtils.findIP(request), is("127.0.0.1"));
  }

  @Test
  public void testFindUser() {
    assertThat(AuditUtils.findUser(), is("anonymous"));
    when(subject.getPrincipal()).thenReturn(new UserPrincipal("admin", "Administrator", true));
    assertThat(AuditUtils.findUser(), is("admin"));
  }
}
