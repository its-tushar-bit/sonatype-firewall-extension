/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.security.oauth2.OAuth2Realm;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantReference;

/**
 * This service orchestrate and manage the different SSO User implementations in the application.
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

  private TenantReference<Map<String, Boolean>> configurationMap;

  @Inject
  public SsoUserService(SamlSsoUserProvider samlSsoUserProvider, OAuth2SsoUserProvider oAuth2SsoUserProvider) {
    this.samlSsoUserProvider = samlSsoUserProvider;
    this.oAuth2SsoUserProvider = oAuth2SsoUserProvider;
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
    configMap.put(SamlRealm.ID, samlSsoUserProvider.isSsoConfigured());
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

  public List<SsoUser> getSsoByUsernames(Set<String> usernames) {
    return getEnabledSsoUserProvider().getSsoByUsernames(usernames);
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

  @Override
  public void register() {
    loadSsoConfiguration();
  }
}
