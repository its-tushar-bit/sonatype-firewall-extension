/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature.SAML_ENABLED;

import com.sonatype.insight.brain.dataaccess.security.OAuth2UserDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.security.oauth2.OAuth2Realm;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantReference;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This service orchestrates and manages the different SSO User implementations in the application.
 * <p>
 * It dynamically selects the SSO user implementation to use on the application based on the existing configuration. All
 * the business rules to select the proper SSO implementation should live here.
 */
@Named
@Singleton
public class SsoUserService
    implements TenantManaged
{
  private final SamlSsoUserProvider samlSsoUserProvider;

  private final OAuth2SsoUserProvider oAuth2SsoUserProvider;

  private final SamlUserDAO samlUserDAO;

  private final OAuth2UserDAO oAuth2UserDAO;

  private TenantReference<Map<String, Boolean>> configurationMap;

  @Inject
  public SsoUserService(
      final SamlSsoUserProvider samlSsoUserProvider,
      final OAuth2SsoUserProvider oAuth2SsoUserProvider,
      final SamlUserDAO samlUserDAO,
      final OAuth2UserDAO oAuth2UserDAO)
  {
    this.samlSsoUserProvider = samlSsoUserProvider;
    this.oAuth2SsoUserProvider = oAuth2SsoUserProvider;
    this.samlUserDAO = samlUserDAO;
    this.oAuth2UserDAO = oAuth2UserDAO;
    configurationMap = new TenantReference<>(HashMap::new);
  }

  protected SsoUserProvider getEnabledSsoUserProvider() {

    if (isConfigurationEnabled(OAuth2Realm.ID) && SystemConfigurationPropertyFeature.OAUTH2_ENABLED.isEnabled()) {
      return oAuth2SsoUserProvider;
    }
    else {
      return samlSsoUserProvider;
    }
  }

  private boolean isConfigurationEnabled(String realmId) {
    Map<String, Boolean> config = configurationMap.get();
    return config != null && config.get(realmId) != null && config.get(OAuth2Realm.ID);
  }

  private String getEnabledSsoRealm() {
    return getEnabledSsoUserProvider().getSsoRealm();
  }

  public void loadSsoConfiguration() {
    Map<String, Boolean> configMap = new HashMap<>();
    configMap.put(OAuth2Realm.ID, oAuth2SsoUserProvider.isSsoConfigured());
    configMap.put(SamlRealm.ID, SAML_ENABLED.isEnabled() && samlSsoUserProvider.isSsoConfigured());
    configurationMap.set(configMap);
  }

  public boolean isSsoRealm(String realmId) {
    return samlSsoUserProvider.isSsoRealm(realmId) || oAuth2SsoUserProvider.isSsoRealm(realmId);
  }

  public String normalizeRealmId(String realmId) {
    if (samlSsoUserProvider.isSsoRealm(realmId)) {
      return samlSsoUserProvider.getSsoRealm();
    }
    if (oAuth2SsoUserProvider.isSsoRealm(realmId)) {
      return oAuth2SsoUserProvider.getSsoRealm();
    }
    return realmId;
  }

  public boolean isSsoConfigured() {
    loadSsoConfiguration();
    return configurationMap.get().get(SamlRealm.ID) || configurationMap.get().get(OAuth2Realm.ID);
  }

  public void updateSsoUserAndGroups(SsoUser ssoUser, Set<String> newSamlGroupNames) {
    getEnabledSsoUserProvider().updateSsoUserAndGroups(ssoUser, newSamlGroupNames);
  }

  public List<SsoUser> getSsoUsersByGroupName(String groupName) {
    return getEnabledSsoUserProvider().getSsoUsersByGroupName(groupName);
  }

  public Set<String> filterExistingSsoGroupNames(Set<String> groupNames) {
    return getEnabledSsoUserProvider().filterExistingSsoGroupNames(groupNames);
  }

  public List<Member> getSsoGroupMembers(Set<String> groupNames) {
    Set<String> existingSamlGroupNames = filterExistingSsoGroupNames(groupNames);
    return existingSamlGroupNames.stream()
        .map(group -> new Member(MemberType.GROUP, group, group, null, getEnabledSsoRealm()))
        .collect(Collectors.toList());
  }

  public List<SsoUser> getSsoUsersByUsernames(Set<String> usernames) {
    return getEnabledSsoUserProvider().getSsoUsersByUsernames(usernames);
  }

  public List<SsoUser> getSsoUsersByEmails(Set<String> emails) {
    return getEnabledSsoUserProvider().getSsoUsersByEmails(emails);
  }

  // A realName is firstName + " " + lastName.
  public List<SsoUser> getSsoUsersByRealNames(Set<String> realNames) {
    return getEnabledSsoUserProvider().getSsoUsersByRealNames(realNames);
  }

  public List<SsoUser> findSsoUsersByNameOrUsernameQuery(String nameQuery) {
    return getEnabledSsoUserProvider().findSsoUsersByNameOrUsernameQuery(nameQuery);
  }

  public List<SsoGroup> findSsoGroupsByNameQuery(String nameQuery) {
    return getEnabledSsoUserProvider().findSsoGroupsByNameQuery(nameQuery);
  }

  public List<Member> getSsoGroupMembersByNameQuery(String nameQuery) {
    return findSsoGroupsByNameQuery(nameQuery).stream()
        .map(group -> new Member(MemberType.GROUP, group.getName(), group.getName(), null, getEnabledSsoRealm()))
        .collect(Collectors.toList());
  }

  public void deleteSsoUser(SsoUser ssoUser) {
    getEnabledSsoUserProvider().deleteSsoUser(ssoUser);
  }

  public List<SsoUser> getAll() {
    return getEnabledSsoUserProvider().getAll();
  }

  public SsoUser getByUsernameNotNull(final String username) {
    return getEnabledSsoUserProvider().getByUsernameNotNull(username);
  }

  public SsoUser getByUsername(final String username) {
    return getEnabledSsoUserProvider().getByUsername(username);
  }

  public void syncSsoProviderDataSources() {
    samlUserDAO.withAllUsersWithGroups((SamlUser samlUser) -> {
      SsoUser ssoUser = SsoUser.fromSamlUser(samlUser);
      oAuth2SsoUserProvider.updateSsoUserAndGroups(ssoUser, ssoUser.getGroups());
    });

    oAuth2UserDAO.withAllUsersWithGroups((OAuth2User oAuth2User) -> {
      SsoUser ssoUser = SsoUser.fromOAuth2User(oAuth2User);
      samlSsoUserProvider.updateSsoUserAndGroups(ssoUser, ssoUser.getGroups());
    });
  }

  @Override
  public void register() {
    loadSsoConfiguration();
  }
}
