/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
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

  private final SamlConfigurationService samlConfigurationService;

  @Inject
  public SamlRealm(SsoUserService ssoUserService, SamlConfigurationService samlConfigurationService) {
    super(new AllowAllCredentialsMatcher());
    this.samlConfigurationService = samlConfigurationService;
    setName("SAML");
    setAuthenticationTokenClass(SamlAuthenticationToken.class);
    this.ssoUserService = ssoUserService;
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
    SamlPrincipalAttributes samlPrincipal = ((SamlAuthenticationToken) token).getSamlPrincipal();
    log.debug("Authenticated SAML principal {} with attributes {}",
        samlPrincipal.getName(), samlPrincipal.getAllAttributes());

    SamlConfiguration samlConfiguration = samlConfigurationService.get();
    if (samlConfiguration == null) {
      // SAML was deconfigured between the filter resolving the registration and this realm running.
      throw new AuthenticationException("SAML configuration is no longer available");
    }
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

    try (AuditSession auditSession = AuditData.get().recordSubEvent(AuditEvent.LOGIN, true)) {
      AuditData.get().setUsername(username);
    }

    return new SimpleAuthenticationInfo(
        new UserPrincipal(ssoUser.getUsername(), ssoUser.calculateDisplayName(), ID, ssoUser.getGroups()), null,
        getName());
  }

  private String getFirstAttribute(SamlPrincipalAttributes samlPrincipal, String attributeName) {
    String attribute = samlPrincipal.getAttribute(attributeName);
    if (attribute != null) {
      return attribute;
    }
    return samlPrincipal.getFriendlyAttribute(attributeName);
  }

  private List<String> getAllAttributes(SamlPrincipalAttributes samlPrincipal, String attributeName) {
    // Attributes keyed by formal Name and by FriendlyName are combined. Implementations must return
    // disjoint values from these two accessors to avoid duplicates; SpringSamlPrincipal does so by
    // folding FriendlyNames into the formal-name map and returning empty from getFriendlyAttributes.
    List<String> result = new ArrayList<>();
    result.addAll(samlPrincipal.getAttributes(attributeName));
    result.addAll(samlPrincipal.getFriendlyAttributes(attributeName));
    return result;
  }

}
