/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

import com.sonatype.insight.brain.NetworkingHelper;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentH2Test
public class CurrentUserTest
    extends AbstractComponentH2Test
{
  @Inject
  private CurrentUser currentUser;

  @Test
  public void testGetIP() {
    HttpServletRequest request = mock(HttpServletRequest.class);

    when(request.getHeader(eq(CurrentUser.XFF_HEADER))).thenReturn(null);
    assertThat(currentUser.getIP(request)).isNull();

    when(request.getHeader(eq(CurrentUser.XFF_HEADER))).thenReturn("");
    assertThat(currentUser.getIP(request)).isNull();

    when(request.getHeader(eq(CurrentUser.XFF_HEADER))).thenReturn(null);
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    assertThat(currentUser.getIP(request)).isEqualTo("127.0.0.1");

    NetworkingHelper.assumeDnsResolutionIsNormal();

    when(request.getHeader(eq(CurrentUser.XFF_HEADER))).thenReturn("{unknown}");
    assertThat(currentUser.getIP(request)).isEqualTo("127.0.0.1");

    when(request.getHeader(eq(CurrentUser.XFF_HEADER))).thenReturn("[missing]");
    assertThat(currentUser.getIP(request)).isEqualTo("127.0.0.1");

    when(request.getHeader(eq(CurrentUser.XFF_HEADER))).thenReturn("{unknown}, 127.0.0.1, {unknown1}");
    assertThat(currentUser.getIP(request)).isEqualTo("127.0.0.1");

    // IPs that start with "[" are considered IPv6, so this is a special case
    when(request.getHeader(eq(CurrentUser.XFF_HEADER))).thenReturn("[missing], 127.0.0.1, {unknown}");
    assertThat(currentUser.getIP(request)).isEqualTo("127.0.0.1");

    when(request.getHeader(eq(CurrentUser.XFF_HEADER))).thenReturn("[missing], [0:0:0:0:0:0:0:1], {unknown}");
    assertThat(currentUser.getIP(request)).isEqualTo("[0:0:0:0:0:0:0:1]");
  }

  @Test
  public void testResolveIP() {
    assertThat(CurrentUser.resolveIP((String[]) null)).isNull();
    assertThat(CurrentUser.resolveIP((String) null)).isNull();
    assertThat(CurrentUser.resolveIP(new String[0])).isNull();

    NetworkingHelper.assumeDnsResolutionIsNormal();

    assertThat(CurrentUser.resolveIP("{unknown}", "127.0.0.1", "{unknown1}")).isEqualTo("127.0.0.1");
    // IPs that start with "[" are considered IPv6, so this is a special case
    assertThat(CurrentUser.resolveIP("[missing]", "127.0.0.1", "{unknown}")).isEqualTo("127.0.0.1");
  }

  @Test
  public void testResolveLeftMostIP() {
    assertThat(CurrentUser.resolveIP("127.0.0.1", "127.0.0.2", "127.0.0.3")).isEqualTo("127.0.0.1");
  }

  @Test
  public void testFindLeftMostIP() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getHeader(eq(CurrentUser.XFF_HEADER))).thenReturn("127.0.0.1, 127.0.0.2, 127.0.0.3");
    assertThat(currentUser.getIP(request)).isEqualTo("127.0.0.1");
  }

  @Test
  public void testGetUsername() {
    when(subject.getPrincipal()).thenReturn(null);
    assertThat(currentUser.getUsername()).isEqualTo("anonymous");
    when(subject.getPrincipal()).thenReturn(new UserPrincipal("admin", "Administrator", InternalRealm.ID));
    assertThat(currentUser.getUsername()).isEqualTo("admin");
  }

  @Test
  public void testGetUsernameOrSystem_Anonymous() {
    when(subject.getPrincipal()).thenReturn(null);
    assertThat(currentUser.getUsernameOrSystem()).isEqualTo("anonymous");
  }

  @Test
  public void testGetUsernameOrSystem_Username() {
    when(subject.getPrincipal()).thenReturn(new UserPrincipal("admin", "Administrator", InternalRealm.ID));
    assertThat(currentUser.getUsernameOrSystem()).isEqualTo("admin");
  }

  @Test
  public void testGetUsernameOrSystem_System() {
    ThreadContext.unbindSubject();
    ThreadContext.unbindSecurityManager();
    assertThat(currentUser.getUsernameOrSystem()).isEqualTo(CurrentUser.SYSTEM);
  }

  @Test
  public void testGetDisplayNameOrUsername_Anonymous() {
    when(subject.getPrincipal()).thenReturn(null);
    assertThat(currentUser.getDisplayNameOrUsername()).isEqualTo("anonymous");
  }

  @Test
  public void testGetDisplayNameOrUsername_System() {
    ThreadContext.unbindSubject();
    ThreadContext.unbindSecurityManager();
    assertThat(currentUser.getDisplayNameOrUsername()).isEqualTo(CurrentUser.SYSTEM);
  }

  @Test
  public void testGetDisplayNameOrUsername_DisplayName_Null() {
    when(subject.getPrincipal()).thenReturn(new UserPrincipal("username", null, "realmId"));
    assertThat(currentUser.getDisplayNameOrUsername()).isEqualTo("username");
  }

  @Test
  public void testGetDisplayNameOrUsername_DisplayName_Empty() {
    when(subject.getPrincipal()).thenReturn(new UserPrincipal("username", "", "realmId"));
    assertThat(currentUser.getDisplayNameOrUsername()).isEqualTo("username");
  }

  @Test
  public void testGetDisplayNameOrUsername_DisplayName_OnlyWhitespace() {
    when(subject.getPrincipal()).thenReturn(new UserPrincipal("username", " ", "realmId"));
    assertThat(currentUser.getDisplayNameOrUsername()).isEqualTo("username");
  }

  @Test
  public void testGetDisplayNameOrUsername_DisplayName() {
    when(subject.getPrincipal()).thenReturn(new UserPrincipal("username", "Bob Smith", "realmId"));
    assertThat(currentUser.getDisplayNameOrUsername()).isEqualTo("Bob Smith");
  }
}
