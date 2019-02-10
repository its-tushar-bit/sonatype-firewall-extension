/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.filter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.11.0
 */
@Entity
@Table(name = "dashboard_filter")
public class DashboardFilter
    implements HasStringId
{
  @Id
  @Column(name = "dashboard_filter_id")
  private String id;

  @Column(name = "username")
  private String username;

  @Column(name = "name")
  private String name;

  @Column(name = "name_lowercase_no_whitespace")
  private String nameLowercaseNoWhitespace;

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

  public DashboardFilter(final String name) {
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
    this.username = username;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    nameLowercaseNoWhitespace = NameHelper.normalize(name);
    this.name = name;
  }

  public String getNameLowercaseNoWhitespace() {
    return nameLowercaseNoWhitespace;
  }

  /**
   * This method is defined here only to trick jackson into "thinking" that it de-serialized the value of the
   * nameLowercaseNoWhitespace field. If this method is not defined, jackson will set/access the
   * nameLowercaseNoWhitespace field directly via reflection, possibly setting it to an incorrect value.
   *
   * @deprecated This method should not be used explicitly.
   */
  @Deprecated
  @SuppressWarnings("unused")
  private void setNameLowercaseNoWhitespace(String nameLowercaseNoWhitespace) {
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
}
