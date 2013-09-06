/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Locale;

import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.codehaus.plexus.util.StringUtils;

/**
 * Security Shiro realm backed by the CLM ODS database.
 * 
 * @since 1.7
 */
@Named
public class CLMRealm
    extends AuthorizingRealm
{
  public CLMRealm() {
    setName("CLMRealm");
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    UsernamePasswordToken usernamePasswordToken = (UsernamePasswordToken) token;

    String username = usernamePasswordToken.getUsername();
    if (StringUtils.isEmpty(username)) {
      throw new BadRequestException("The username is required");
    }

    User user = new UserDAO().getByUsernameLowercase(username.toLowerCase(Locale.ENGLISH));
    if (user != null) {
      // TODO check the password
      return new SimpleAuthenticationInfo(username, usernamePasswordToken.getPassword(), getName());
    }

    return null;
  }

  @Override
  protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    // TODO Auto-generated method stub
    return null;
  }
}
