/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CurrentUserTest
    extends AbstractComponentTest
{
  @Inject
  private CurrentUser currentUser;

  @Test
  public void testGetIP() {
    HttpServletRequest request = mock(HttpServletRequest.class);

    when(request.getHeader(eq(CurrentUser.XFF_HEADER))).thenReturn(null);
    assertThat(currentUser.getIP(request), is(nullValue()));

    when(request.getHeader(eq(CurrentUser.XFF_HEADER))).thenReturn("");
    assertThat(currentUser.getIP(request), is(nullValue()));

    when(request.getHeader(eq(CurrentUser.XFF_HEADER))).thenReturn("{unknown}, 127.0.0.1, {unknown1}");
    assertThat(currentUser.getIP(request), is("127.0.0.1"));

    // IPs that start with "[" are considered IPv6, so this is a special case
    when(request.getHeader(eq(CurrentUser.XFF_HEADER))).thenReturn("[missing], 127.0.0.1, {unknown}");
    assertThat(currentUser.getIP(request), is("127.0.0.1"));

    when(request.getHeader(eq(CurrentUser.XFF_HEADER))).thenReturn(null);
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    assertThat(currentUser.getIP(request), is("127.0.0.1"));

    when(request.getHeader(eq(CurrentUser.XFF_HEADER))).thenReturn("{unknown}");
    assertThat(currentUser.getIP(request), is("127.0.0.1"));

    when(request.getHeader(eq(CurrentUser.XFF_HEADER))).thenReturn("[missing]");
    assertThat(currentUser.getIP(request), is("127.0.0.1"));
  }

  @Test
  public void testResolveIP() {
    assertThat(CurrentUser.resolveIP((String[]) null), is(nullValue()));
    assertThat(CurrentUser.resolveIP((String) null), is(nullValue()));
    assertThat(CurrentUser.resolveIP(new String[0]), is(nullValue()));
    assertThat(CurrentUser.resolveIP("{unknown}", "127.0.0.1", "{unknown1}"), is("127.0.0.1"));
    // IPs that start with "[" are considered IPv6, so this is a special case
    assertThat(CurrentUser.resolveIP("[missing]", "127.0.0.1", "{unknown}"), is("127.0.0.1"));
  }

  @Test
  public void testResolveLeftMostIP() {
    assertThat(CurrentUser.resolveIP("127.0.0.1", "127.0.0.2", "127.0.0.3"), is("127.0.0.1"));
  }

  @Test
  public void testFindLeftMostIP() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(eq(CurrentUser.XFF_HEADER))).thenReturn("127.0.0.1, 127.0.0.2, 127.0.0.3");
    assertThat(currentUser.getIP(request), is("127.0.0.1"));
  }

  @Test
  public void testGetUsername() {
    when(subject.getPrincipal()).thenReturn(null);
    assertThat(currentUser.getUsername(), is("anonymous"));
    when(subject.getPrincipal()).thenReturn(new UserPrincipal("admin", "Administrator", true));
    assertThat(currentUser.getUsername(), is("admin"));
  }
}
