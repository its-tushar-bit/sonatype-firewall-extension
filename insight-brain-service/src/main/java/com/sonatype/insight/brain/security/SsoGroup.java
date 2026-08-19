/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import com.sonatype.insight.brain.model.security.OAuth2Group;
import com.sonatype.insight.brain.model.security.SamlGroup;

/**
 * Class representing a SSO Group
 */
public class SsoGroup
{
  private String id;

  private String name;

  public SsoGroup(final String id, final String name) {
    this.id = id;
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void setId(final String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public static SamlGroup toSamlGroup(SsoGroup ssoGroup) {
    SamlGroup group = new SamlGroup(ssoGroup.name);
    group.setId(ssoGroup.id);
    return group;
  }

  public static SsoGroup fromSamlGroup(SamlGroup samlGroup) {
    SsoGroup group = new SsoGroup(samlGroup.getId(), samlGroup.getName());
    return group;
  }

  public static OAuth2Group toOAuth2Group(SsoGroup ssoGroup) {
    OAuth2Group group = new OAuth2Group(ssoGroup.name);
    group.setId(ssoGroup.id);
    return group;
  }

  public static SsoGroup fromOAuth2Group(OAuth2Group oauth2Group) {
    SsoGroup group = new SsoGroup(oauth2Group.getId(), oauth2Group.getName());
    return group;
  }
}
