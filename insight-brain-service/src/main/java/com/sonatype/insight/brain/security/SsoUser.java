/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Optional;
import java.util.Set;

import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.security.oauth2.OAuth2Realm;

/**
 * Class representing an SSO User
 */
public class SsoUser
{
  private String id;

  private String username;

  private String firstName;

  private String lastName;

  private String email;

  private String realmId;

  private Set<String> groups;

  public SsoUser(
      final String username,
      final String firstName,
      final String lastName,
      final String email)
  {
    this(null, username, firstName, lastName, email, null, null);
  }

  public SsoUser(
      final String username,
      final String firstName,
      final String lastName,
      final String email,
      final String realmId,
      final Set<String> groups)
  {
    this(null, username, firstName, lastName, email, realmId, groups);
  }

  public SsoUser(
      final String id,
      final String username,
      final String firstName,
      final String lastName,
      final String email,
      final String realmId,
      final Set<String> groups)
  {
    this.id = id;
    this.username = username;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.realmId = realmId;
    this.groups = groups;
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(final String username) {
    this.username = username;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(final String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(final String lastName) {
    this.lastName = lastName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(final String email) {
    this.email = email;
  }

  public String getRealmId() {
    return realmId;
  }

  public void setRealmId(final String realmId) {
    this.realmId = realmId;
  }

  public Set<String> getGroups() {
    return groups;
  }

  public void setGroups(final Set<String> groups) {
    this.groups = groups;
  }

  public String calculateDisplayName() {
    String displayName = Optional.ofNullable(firstName).orElse("") + " " + Optional.ofNullable(lastName).orElse("");
    displayName = displayName.trim();
    if (displayName.isEmpty()) {
      displayName = username;
    }
    return displayName;
  }

  public static SamlUser toSamlUser(SsoUser ssoUser) {
    if (ssoUser == null) {
      return null;
    }

    SamlUser user = new SamlUser(ssoUser.username, ssoUser.firstName, ssoUser.lastName, ssoUser.email, ssoUser.groups);
    user.setId(ssoUser.id);
    return user;
  }

  public static SsoUser fromSamlUser(SamlUser samlUser) {
    if (samlUser == null) {
      return null;
    }

    return new SsoUser(samlUser.getId(), samlUser.getUsername(), samlUser.getFirstName(), samlUser.getLastName(),
        samlUser.getEmail(), SamlRealm.ID, samlUser.getGroups());
  }

  public static OAuth2User toOAuth2User(SsoUser ssoUser) {
    if (ssoUser == null) {
      return null;
    }

    OAuth2User user =
        new OAuth2User(ssoUser.username, ssoUser.firstName, ssoUser.lastName, ssoUser.email, ssoUser.groups);
    user.setId(ssoUser.id);
    return user;
  }

  public static SsoUser fromOAuth2User(OAuth2User oauth2User) {
    if (oauth2User == null) {
      return null;
    }

    return new SsoUser(oauth2User.getId(), oauth2User.getUsername(), oauth2User.getFirstName(),
        oauth2User.getLastName(),
        oauth2User.getEmail(), OAuth2Realm.ID, oauth2User.getGroups());
  }
}
