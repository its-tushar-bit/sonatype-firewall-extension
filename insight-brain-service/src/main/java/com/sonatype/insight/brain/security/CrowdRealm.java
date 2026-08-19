/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

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

  private final CrowdClientFactory crowdClientFactory;

  @Inject
  public CrowdRealm(CrowdClientFactory crowdClientFactory) {
    super(new AllowAllCredentialsMatcher());
    this.crowdClientFactory = crowdClientFactory;
    setName(ID);
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(
      AuthenticationToken authenticationToken) throws AuthenticationException
  {
    CrowdClient crowdClient = crowdClientFactory.createCrowdClient();

    if (crowdClient == null) {
      return null;
    }

    UsernamePasswordToken usernamePasswordToken = (UsernamePasswordToken) authenticationToken;
    try {
      return new SimpleAuthenticationInfo(crowdClient.authenticateUser(usernamePasswordToken), null, getName());
    }
    catch (Exception e) {
      throw new AuthenticationException(
          String.format("Could not authenticate the '%s' Crowd user.", usernamePasswordToken.getUsername()), e);
    }
  }
}
