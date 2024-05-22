/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OAuth2ConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.oauth2.OAuth2Configuration;
import com.sonatype.insight.brain.model.security.UserPrincipal;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class JwtRealm
    extends AuthenticatingRealm
{
  private static final Logger log = LoggerFactory.getLogger(JwtRealm.class);

  public static final String ID = "JWT";

  private final OAuth2ConfigurationDAO oAuth2ConfigurationDAO;

  @Inject
  public JwtRealm(
      final OAuth2ConfigurationDAO oAuth2ConfigurationDAO,
      final ShiroJsonWebTokenValidator shiroJsonWebTokenValidator)
  {
    this.oAuth2ConfigurationDAO = oAuth2ConfigurationDAO;
    this.setCredentialsMatcher(new JwtCredentialsMatcher(shiroJsonWebTokenValidator));
  }

  @Override
  public String getName() {
    return ID;
  }

  @Override
  public Class<?> getAuthenticationTokenClass() {
    return ShiroJsonWebToken.class;
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    ShiroJsonWebToken jwtToken = (ShiroJsonWebToken) token;

    log.debug("Authenticated JWT with next payload: {}", jwtToken.getPrincipal().getClaims().keySet());

    UserPrincipal userPrincipal = buildPrincipal(jwtToken);

    return new SimpleAuthenticationInfo(userPrincipal, jwtToken.getCredentials(), getName());
  }

  private UserPrincipal buildPrincipal(ShiroJsonWebToken jwtToken) {
    OAuth2Configuration configuration = oAuth2ConfigurationDAO.getById(jwtToken.getPrincipal().getIssuer());

    String sub = jwtToken.getPrincipal().getSubject();

    if (configuration == null) {
      // No configuration available, we use the subject of the JWT to generate the User Principal
      return new UserPrincipal(sub, sub, ID, null);
    }

    String username = jwtToken.getClaimValue(configuration.getUsernameClaim());
    String firstName = jwtToken.getClaimValue(configuration.getFirstNameClaim());
    String lastName = jwtToken.getClaimValue(configuration.getLastNameClaim());
    String email = jwtToken.getClaimValue(configuration.getEmailClaim());

    // Calculate the final username, name and groups
    String principalUsername = calculatePrincipalUserName(username, email, sub);
    String name = calculateDisplayName(firstName, lastName, principalUsername);
    Set<String> groups = new LinkedHashSet<>(jwtToken.getClaimValueAsList(configuration.getGroupsClaim()));

    return new UserPrincipal(principalUsername, name, ID, groups);
  }

  public String calculatePrincipalUserName(String username, String email, String subject) {
    if (StringUtils.isNotBlank(username)) {
      return username;
    }
    else {
      return StringUtils.isNotBlank(email) ? email : subject;
    }
  }

  public String calculateDisplayName(String firstName, String lastName, String username) {
    String displayName = Optional.ofNullable(firstName).orElse("") + " " + Optional.ofNullable(lastName).orElse("");
    displayName = displayName.trim();
    if (displayName.isEmpty()) {
      displayName = username;
    }
    return displayName;
  }
}
