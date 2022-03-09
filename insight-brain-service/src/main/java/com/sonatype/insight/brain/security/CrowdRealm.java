/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.configuration.crowd.CrowdConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.crowd.CrowdConfiguration;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.ExperimentalFeature;

import com.atlassian.crowd.integration.rest.service.factory.RestCrowdClientFactory;
import com.atlassian.crowd.model.DirectoryEntity;
import com.atlassian.crowd.model.group.Group;
import com.atlassian.crowd.model.user.User;
import com.atlassian.crowd.service.client.CrowdClient;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.realm.AuthenticatingRealm;

@Named
@Singleton
public class CrowdRealm
    extends AuthenticatingRealm
{
  public static final String ID = "Crowd";

  private final CrowdConfigurationDAO crowdConfigurationDAO;

  private final RestCrowdClientFactory restCrowdClientFactory;

  private final PasswordHandler passwordHandler;

  private final InsightConfig insightConfig;

  @Inject
  public CrowdRealm(
      CrowdConfigurationDAO crowdConfigurationDAO,
      PasswordHandler passwordHandler,
      InsightConfig insightConfig)
  {
    super(new AllowAllCredentialsMatcher());
    this.crowdConfigurationDAO = crowdConfigurationDAO;
    this.passwordHandler = passwordHandler;
    this.insightConfig = insightConfig;
    restCrowdClientFactory = new RestCrowdClientFactory();
    setName(ID);
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken)
      throws AuthenticationException
  {
    if (!insightConfig.isExperimentalFeatureEnabled(ExperimentalFeature.CROWD_INTEGRATION)) {
      return null;
    }

    CrowdConfiguration crowdConfiguration = crowdConfigurationDAO.get();
    if (crowdConfiguration == null) {
      return null;
    }

    CrowdClient crowdClient;
    try {
      crowdClient =
          restCrowdClientFactory.newInstance(crowdConfiguration.getServerUrl(), crowdConfiguration.getApplicationName(),
              new String(passwordHandler.decryptPassword(crowdConfiguration.getApplicationPassword())));
    }
    catch (Exception e) {
      throw new RuntimeException(String.format(
          "Failed to create a Crowd REST client for serverUrl '%s', applicationName '%s', " +
              "and applicationPassword '****'. Your Crowd configuration may be invalid.",
          crowdConfiguration.getServerUrl(), crowdConfiguration.getApplicationName()), e);
    }

    UsernamePasswordToken usernamePasswordToken = (UsernamePasswordToken) authenticationToken;
    try {
      User crowdUser = crowdClient.authenticateUser(usernamePasswordToken.getUsername(),
          new String(usernamePasswordToken.getPassword()));
      List<Group> groups = crowdClient.getGroupsForNestedUser(crowdUser.getName(), 0, -1);
      Set<String> groupNames = groups.stream().filter(Group::isActive).map(DirectoryEntity::getName)
          .collect(Collectors.toCollection(LinkedHashSet::new));
      return new SimpleAuthenticationInfo(
          new UserPrincipal(crowdUser.getName(), crowdUser.getDisplayName(), ID, groupNames), null,
          getName());
    }
    catch (Exception e) {
      throw new AuthenticationException(
          "Could not authenticate user '" + usernamePasswordToken.getUsername() + "' with Crowd", e);
    }
  }
}
