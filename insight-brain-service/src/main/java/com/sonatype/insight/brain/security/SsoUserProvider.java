/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.List;
import java.util.Set;

/**
 * Interface with the methods needed to integrate an SSO User implementation with the application. Look at
 * {@link SsoUserService} to see how the different implementations are used.
 */
public interface SsoUserProvider
{
  String getSsoRealm();

  boolean isSsoRealm(String realmId);

  boolean isSsoConfigured();

  void updateSsoUserAndGroups(SsoUser ssoUser, Set<String> newSamlGroupNames);

  List<SsoUser> getSsoUsersByGroupName(String groupName);

  Set<String> filterExistingSsoGroupNames(Set<String> groupNames);

  List<SsoUser> getSsoUsersByUsernames(Set<String> usernames);

  List<SsoUser> getSsoUsersByEmails(Set<String> emails);

  // A realName is firstName + " " + lastName.
  List<SsoUser> getSsoUsersByRealNames(Set<String> realNames);

  List<SsoUser> findSsoUsersByNameOrUsernameQuery(String nameQuery);

  List<SsoGroup> findSsoGroupsByNameQuery(String nameQuery);

  void deleteSsoUser(SsoUser ssoUser);

  void upsertByUsername(SsoUser ssoUser);

  List<SsoUser> getAll();

  SsoUser getByUsername(String username);

  SsoUser getByUsernameNotNull(String username);
}
