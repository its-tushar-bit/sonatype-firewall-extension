/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CrowdRealmTest
    extends AbstractComponentTest
{
  @Rule
  public CrowdMockServerRule crowdMockServer = new CrowdMockServerRule();

  @Mock
  private CrowdClientFactory mockCrowdClientFactory;

  @InjectMocks
  private CrowdRealm crowdRealm;

  @Test
  public void testCrowdRealm() {
    assertThat(crowdRealm.getCredentialsMatcher()).isInstanceOf(AllowAllCredentialsMatcher.class);
    assertThat(crowdRealm.getName()).isEqualTo(CrowdRealm.ID);
    assertThat(crowdRealm.getAuthenticationTokenClass()).isSameAs(UsernamePasswordToken.class);
  }

  @Test
  public void testDoGetAuthenticationInfo_NullCrowdClient() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("username", "password");

    assertThat(crowdRealm.doGetAuthenticationInfo(usernamePasswordToken)).isNull();
    verify(mockCrowdClientFactory).createCrowdClient();
  }

  @Test
  public void testDoGetAuthenticationInfo() throws Exception {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("username", "password");
    CrowdClient mockCrowdClient = mock(CrowdClient.class);
    UserPrincipal mockUserPrincipal = mock(UserPrincipal.class);
    when(mockCrowdClient.authenticateUser(any(UsernamePasswordToken.class))).thenReturn(mockUserPrincipal);
    when(mockCrowdClientFactory.createCrowdClient()).thenReturn(mockCrowdClient);

    UserPrincipal userPrincipal = getUserPrincipal(crowdRealm.doGetAuthenticationInfo(usernamePasswordToken));

    assertThat(userPrincipal).isEqualTo(mockUserPrincipal);
    verify(mockCrowdClientFactory).createCrowdClient();
    verify(mockCrowdClient).authenticateUser(usernamePasswordToken);
  }

  @Test
  public void testDoGetAuthenticationInfo_Error() throws Exception {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("username", "password");
    CrowdClient mockCrowdClient = mock(CrowdClient.class);
    when(mockCrowdClient.authenticateUser(any(UsernamePasswordToken.class))).thenThrow(new RuntimeException());
    when(mockCrowdClientFactory.createCrowdClient()).thenReturn(mockCrowdClient);

    assertThatExceptionOfType(AuthenticationException.class).isThrownBy(
        () -> crowdRealm.doGetAuthenticationInfo(usernamePasswordToken))
        .withMessageContaining(
            String.format("Could not authenticate the '%s' Crowd user.", usernamePasswordToken.getUsername()));
  }

  private UserPrincipal getUserPrincipal(AuthenticationInfo authenticationInfo) {
    assertThat(authenticationInfo).isInstanceOf(SimpleAuthenticationInfo.class);
    SimpleAuthenticationInfo simpleAuthenticationInfo = (SimpleAuthenticationInfo) authenticationInfo;
    assertThat(simpleAuthenticationInfo.getCredentials()).isNull();
    assertThat(simpleAuthenticationInfo.getPrincipals()).isNotEmpty();
    assertThat(simpleAuthenticationInfo.getPrincipals().getRealmNames()).containsExactly(CrowdRealm.ID);
    Object primaryPrincipal = simpleAuthenticationInfo.getPrincipals().getPrimaryPrincipal();
    assertThat(primaryPrincipal).isInstanceOf(UserPrincipal.class);
    return (UserPrincipal) primaryPrincipal;
  }
}
