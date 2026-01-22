/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security.oauth2;

import java.util.LinkedHashSet;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.configuration.oauth2.OAuth2ConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.oauth2.OAuth2Configuration;
import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.security.SsoUser;
import com.sonatype.insight.brain.security.SsoUserService;

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
public class OAuth2Realm
    extends AuthenticatingRealm
{
  private static final Logger log = LoggerFactory.getLogger(OAuth2Realm.class);

  public static final String ID = OAuth2User.OAUTH2_REALM_ID;

  public static final String NICKNAME_CLAIM = "nickname";

  public static final String GIVEN_NAME_CLAIM = "given_name";

  public static final String FAMILY_NAME_CLAIM = "family_name";

  public static final String EMAIL_CLAIM = "email";

  public static final String GROUPS_CLAIM = "groups";

  private final OAuth2ConfigurationDAO oAuth2ConfigurationDAO;

  private final SsoUserService ssoUserService;

  @Inject
  public OAuth2Realm(
      final OAuth2ConfigurationDAO oAuth2ConfigurationDAO,
      final ShiroJsonWebTokenValidator shiroJsonWebTokenValidator,
      final SsoUserService ssoUserService)
  {
    this.oAuth2ConfigurationDAO = oAuth2ConfigurationDAO;
    this.ssoUserService = ssoUserService;
    this.setCredentialsMatcher(new JwtCredentialsMatcher(shiroJsonWebTokenValidator));
  }

  @Override
  public String getName() {
    return ID;
  }

  @Override
  public Class<? extends AuthenticationToken> getAuthenticationTokenClass() {
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

    String username = jwtToken.getValueFromClaimOrDefaultClaim(configuration.getUsernameClaim(), NICKNAME_CLAIM);
    String firstName = jwtToken.getValueFromClaimOrDefaultClaim(configuration.getFirstNameClaim(), GIVEN_NAME_CLAIM);
    String lastName = jwtToken.getValueFromClaimOrDefaultClaim(configuration.getLastNameClaim(), FAMILY_NAME_CLAIM);
    String email = jwtToken.getValueFromClaimOrDefaultClaim(configuration.getEmailClaim(), EMAIL_CLAIM);

    // Calculate the principal username and groups
    String principalUsername = getPrincipalUsername(username, email, sub);
    Set<String> groups =
        new LinkedHashSet<>(jwtToken.getValueAsListFromClaimOrDefaultClaim(configuration.getGroupsClaim(),
            GROUPS_CLAIM));

    // Creating the oauth2 user
    SsoUser ssoUser = new SsoUser(principalUsername, firstName, lastName, email, ID, groups);
    updateSsoUserAndGroups(ssoUser);

    return new UserPrincipal(ssoUser.getUsername(), ssoUser.calculateDisplayName(), ID, groups);
  }

  private void updateSsoUserAndGroups(final SsoUser ssoUser) {
    try {
      // This should not fail, but in case it fails, we should continue with the authentication.
      ssoUserService.updateSsoUserAndGroups(ssoUser, ssoUser.getGroups());
    }
    catch (Exception e) {
      // Adding warning to log the error and check details to fix issue
      log.warn("Unexpected error updating SSO user and groups", e);
    }
  }

  public String getPrincipalUsername(String username, String email, String subject) {
    if (StringUtils.isNotBlank(username)) {
      return username;
    }
    else {
      return StringUtils.isNotBlank(email) ? email : subject;
    }
  }
}
