/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.saml.SamlConfiguration;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.UserPrincipal;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.AllowAllCredentialsMatcher;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.keycloak.adapters.saml.SamlPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class SamlRealm
    extends AuthenticatingRealm
{
  private static final Logger log = LoggerFactory.getLogger(SamlRealm.class);

  public static final String ID = SamlUser.SAML_REALM_ID;

  private final SsoUserService ssoUserService;

  private final SamlConfigurationDAO samlConfigurationDAO;

  @Inject
  public SamlRealm(SsoUserService ssoUserService, SamlConfigurationDAO samlConfigurationDAO) {
    super(new AllowAllCredentialsMatcher());
    this.samlConfigurationDAO = samlConfigurationDAO;
    setName("SAML");
    setAuthenticationTokenClass(SamlAuthenticationToken.class);
    this.ssoUserService = ssoUserService;
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    SamlPrincipal samlPrincipal = ((SamlAuthenticationToken) token).getSamlPrincipal();
    log.debug("Authenticated SAML principal {} with attributes {} and friendly attributes {}",
        samlPrincipal.getName(), samlPrincipal.getAttributes(), getFriendlyAttributes(samlPrincipal));

    SamlConfiguration samlConfiguration = samlConfigurationDAO.get();
    String username =
        Optional.ofNullable(getFirstAttribute(samlPrincipal, samlConfiguration.getUsernameAttributeName()))
            .orElse(samlPrincipal.getName());
    if (StringUtils.isBlank(username)) {
      throw new AuthenticationException(
          "A username is required either from a " + samlConfiguration.getUsernameAttributeName() +
              " basic attribute or a SAML NameID.");
    }
    String firstName = getFirstAttribute(samlPrincipal, samlConfiguration.getFirstNameAttributeName());
    String lastName = getFirstAttribute(samlPrincipal, samlConfiguration.getLastNameAttributeName());
    String email = getFirstAttribute(samlPrincipal, samlConfiguration.getEmailAttributeName());
    Set<String> groups =
        new LinkedHashSet<>(getAllAttributes(samlPrincipal, samlConfiguration.getGroupsAttributeName()));
    groups.removeIf(StringUtils::isBlank);
    SsoUser ssoUser = new SsoUser(username, firstName, lastName, email, ID, groups);

    ssoUserService.updateSsoUserAndGroups(ssoUser, groups);

    return new SimpleAuthenticationInfo(
        new UserPrincipal(ssoUser.getUsername(), ssoUser.calculateDisplayName(), ID, ssoUser.getGroups()), null,
        getName());
  }

  private String getFirstAttribute(SamlPrincipal samlPrincipal, String attributeName) {
    String attribute = samlPrincipal.getAttribute(attributeName);
    if (attribute != null) {
      return attribute;
    }
    return samlPrincipal.getFriendlyAttribute(attributeName);
  }

  private List<String> getAllAttributes(SamlPrincipal samlPrincipal, String attributeName) {
    List<String> result = new ArrayList<>();
    result.addAll(samlPrincipal.getAttributes(attributeName));
    result.addAll(samlPrincipal.getFriendlyAttributes(attributeName));
    return result;
  }

  private Map<String, List<String>> getFriendlyAttributes(SamlPrincipal samlPrincipal) {
    Map<String, List<String>> friendlyAttributes = new HashMap<>();
    for (String friendlyAttributeName : samlPrincipal.getFriendlyNames()) {
      friendlyAttributes.put(friendlyAttributeName, samlPrincipal.getFriendlyAttributes(friendlyAttributeName));
    }
    return friendlyAttributes;
  }
}
