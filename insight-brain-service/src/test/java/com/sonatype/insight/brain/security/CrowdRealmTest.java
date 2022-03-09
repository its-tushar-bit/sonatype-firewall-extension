/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.ExperimentalFeature;

import com.atlassian.crowd.integration.rest.entity.GroupEntity;
import com.atlassian.crowd.model.group.GroupType;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.google.common.collect.ImmutableMap.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class CrowdRealmTest
    extends AbstractComponentTest
{
  @Rule
  public CrowdMockServerRule crowdMockServer = new CrowdMockServerRule();

  @Inject
  private CrowdRealm crowdRealm;

  @Inject
  private PasswordHandler passwordHandler;

  @Inject
  private InsightConfig insightConfig;

  @Before
  public void before() {
    insightConfig.setExperimentalFeatures(of(ExperimentalFeature.CROWD_INTEGRATION.getFlag(), true));
  }

  @Test
  public void testCrowdRealm() {
    assertThat(crowdRealm.getCredentialsMatcher()).isInstanceOf(AllowAllCredentialsMatcher.class);
    assertThat(crowdRealm.getName()).isEqualTo(CrowdRealm.ID);
    assertThat(crowdRealm.getAuthenticationTokenClass()).isSameAs(UsernamePasswordToken.class);
  }

  @Test
  public void testDoGetAuthenticationInfo_FeatureDisabled() throws Exception {
    insightConfig.setExperimentalFeatures(of(ExperimentalFeature.CROWD_INTEGRATION.getFlag(), false));
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("username", "password");

    assertThat(crowdRealm.doGetAuthenticationInfo(usernamePasswordToken)).isNull();
  }

  @Test
  public void testDoGetAuthenticationInfo_NotConfigured() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("username", "password");

    assertThat(crowdRealm.doGetAuthenticationInfo(usernamePasswordToken)).isNull();
  }

  @Test
  public void testDoGetAuthenticationInfo_BadConfiguration() {
    tempEntity.newCrowdConfiguration("badUrl", "iq server", passwordHandler.encryptPassword("password".toCharArray()));
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("username", "password");

    assertThatExceptionOfType(RuntimeException.class).isThrownBy(
        () -> crowdRealm.doGetAuthenticationInfo(usernamePasswordToken)).withMessageContaining(
        "Failed to create a Crowd REST client for serverUrl 'badUrl', applicationName 'iq server', " +
            "and applicationPassword '****'. Your Crowd configuration may be invalid");
  }

  @Test
  public void testDoGetAuthenticationInfo_Configured() throws Exception {
    tempEntity.newCrowdConfiguration(crowdMockServer.getBaseUrl() + "/crowd", "iq server",
        passwordHandler.encryptPassword("password".toCharArray()));
    crowdMockServer.mockAuthenticateUser("username", "displayName");
    crowdMockServer.mockGetGroupsForNestedUser("username", "group1", "group2", "group3");
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("username", "password");

    AuthenticationInfo authenticationInfo = crowdRealm.doGetAuthenticationInfo(usernamePasswordToken);

    UserPrincipal userPrincipal = getUserPrincipal(authenticationInfo);
    assertThat(userPrincipal.getUsername()).isEqualTo("username");
    assertThat(userPrincipal.getDisplayName()).isEqualTo("displayName");
    assertThat(userPrincipal.getRealmId()).isEqualTo(CrowdRealm.ID);
    assertThat(userPrincipal.getMembership()).isEqualTo(
        new LinkedHashSet<>(Arrays.asList("group1", "group2", "group3", "(all-authenticated-users)")));
  }

  @Test
  public void testDoGetAuthenticationInfo_Configured_NoGroups() throws Exception {
    tempEntity.newCrowdConfiguration(crowdMockServer.getBaseUrl() + "/crowd", "iq server",
        passwordHandler.encryptPassword("password".toCharArray()));
    crowdMockServer.mockAuthenticateUser("username", "displayName");
    crowdMockServer.mockGetGroupsForNestedUser("username", new String[]{});
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("username", "password");

    AuthenticationInfo authenticationInfo = crowdRealm.doGetAuthenticationInfo(usernamePasswordToken);

    UserPrincipal userPrincipal = getUserPrincipal(authenticationInfo);
    assertThat(userPrincipal.getUsername()).isEqualTo("username");
    assertThat(userPrincipal.getDisplayName()).isEqualTo("displayName");
    assertThat(userPrincipal.getRealmId()).isEqualTo(CrowdRealm.ID);
    assertThat(userPrincipal.getMembership()).isEqualTo(
        new LinkedHashSet<>(Collections.singletonList("(all-authenticated-users)")));
  }

  @Test
  public void testDoGetAuthenticationInfo_Configured_SomeGroupsInactive() throws Exception {
    tempEntity.newCrowdConfiguration(crowdMockServer.getBaseUrl() + "/crowd", "iq server",
        passwordHandler.encryptPassword("password".toCharArray()));
    crowdMockServer.mockAuthenticateUser("username", "displayName");
    GroupEntity groupEntity1 = new GroupEntity("group1", "description", GroupType.GROUP, true);
    GroupEntity groupEntity2 = new GroupEntity("group2", "description", GroupType.GROUP, true);
    GroupEntity groupEntity3 = new GroupEntity("group3", "description", GroupType.GROUP, false);
    crowdMockServer.mockGetGroupsForNestedUser("username", groupEntity1, groupEntity2, groupEntity3);
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("username", "password");

    AuthenticationInfo authenticationInfo = crowdRealm.doGetAuthenticationInfo(usernamePasswordToken);

    UserPrincipal userPrincipal = getUserPrincipal(authenticationInfo);
    assertThat(userPrincipal.getUsername()).isEqualTo("username");
    assertThat(userPrincipal.getDisplayName()).isEqualTo("displayName");
    assertThat(userPrincipal.getRealmId()).isEqualTo(CrowdRealm.ID);
    assertThat(userPrincipal.getMembership()).isEqualTo(
        new LinkedHashSet<>(Arrays.asList("group1", "group2", "(all-authenticated-users)")));
  }

  @Test
  public void testDoGetAuthenticationInfo_Configured_UserError() {
    tempEntity.newCrowdConfiguration(crowdMockServer.getBaseUrl() + "/crowd", "iq server",
        passwordHandler.encryptPassword("password".toCharArray()));
    crowdMockServer.mockAuthenticateUserError("username", 401);
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("username", "password");

    assertThatExceptionOfType(AuthenticationException.class).isThrownBy(
        () -> crowdRealm.doGetAuthenticationInfo(usernamePasswordToken))
        .withMessageContaining("Could not authenticate user 'username' with Crowd");
  }

  @Test
  public void testDoGetAuthenticationInfo_Configured_GroupError() {
    tempEntity.newCrowdConfiguration(crowdMockServer.getBaseUrl() + "/crowd", "iq server",
        passwordHandler.encryptPassword("password".toCharArray()));
    crowdMockServer.mockGetGroupsForNestedUserError("username", 401);
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("username", "password");

    assertThatExceptionOfType(AuthenticationException.class).isThrownBy(
        () -> crowdRealm.doGetAuthenticationInfo(usernamePasswordToken))
        .withMessageContaining("Could not authenticate user 'username' with Crowd");
  }

  private UserPrincipal getUserPrincipal(AuthenticationInfo authenticationInfo) {
    assertThat(authenticationInfo).isInstanceOf(SimpleAuthenticationInfo.class);
    SimpleAuthenticationInfo simpleAuthenticationInfo = (SimpleAuthenticationInfo) authenticationInfo;
    assertThat(simpleAuthenticationInfo.getCredentials()).isNull();
    assertThat(simpleAuthenticationInfo.getPrincipals()).isNotEmpty();
    assertThat(simpleAuthenticationInfo.getPrincipals().getRealmNames()).containsExactly(CrowdRealm.ID);
    Object primaryPrincipal = simpleAuthenticationInfo.getPrincipals().getPrimaryPrincipal();
    assertThat(primaryPrincipal).isInstanceOf(UserPrincipal.class);
    UserPrincipal userPrincipal = (UserPrincipal) primaryPrincipal;
    assertThat(userPrincipal.getRealmId()).isEqualTo(CrowdRealm.ID);
    return userPrincipal;
  }
}
