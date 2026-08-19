/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.filter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.brain.model.Nameable;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.11.0
 */
@Entity
@Table(name = "dashboard_filter")
public class DashboardFilter
    extends Nameable
    implements HasStringId
{
  @Id
  @Column(name = "dashboard_filter_id")
  private String id;

  @Column(name = "username")
  private String username;

  /**
   * @since 1.80
   */
  @Column(name = "username_lowercase")
  private String usernameLowercase;

  /**
   * @since 1.80
   */
  @Column(name = "realm_id")
  private String realmId;

  @Column(name = "filter_json")
  private String filter;

  @Column(name = "based_on_filter_name")
  private String basedOnFilterName;

  /**
   * If true, the filter was acknowledged by the user while the needsAcknowledgementOfInitialDashboardFilter config
   * option is enabled.
   *
   * @since 1.29
   */
  @Column(name = "acknowledged")
  private boolean acknowledged;

  public DashboardFilter() {
  }

  public DashboardFilter(String username, String realmId, String name) {
    setUsername(username);
    setRealmId(realmId);
    setName(name);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(final String username) {
    usernameLowercase = User.normalizeUsername(username);
    this.username = username;
  }

  public String getFilter() {
    return filter;
  }

  public void setFilter(final String filter) {
    this.filter = filter;
  }

  public String getBasedOnFilterName() {
    return basedOnFilterName;
  }

  public void setBasedOnFilterName(final String basedOnFilterName) {
    this.basedOnFilterName = basedOnFilterName;
  }

  public boolean isAcknowledged() {
    return acknowledged;
  }

  public void setAcknowledged(boolean acknowledged) {
    this.acknowledged = acknowledged;
  }

  public String getUsernameLowercase() {
    return usernameLowercase;
  }

  /**
   * This method is defined here only to trick jackson into "thinking" that it de-serialized the value of the
   * usernameLowercase field. If this method is not defined, jackson will set/access the
   * usernameLowercase field directly via reflection, possibly setting it to an incorrect value.
   *
   * @deprecated This method should not be used explicitly.
   */
  @Deprecated
  @SuppressWarnings("unused")
  private void setUsernameLowercase(String usernameLowercase) {
  }

  public String getRealmId() {
    return realmId;
  }

  public void setRealmId(String realmId) {
    this.realmId = realmId;
  }
}
