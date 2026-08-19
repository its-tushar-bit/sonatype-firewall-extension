/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.enterprisereporting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "enterprise_reporting_filter")
public class EnterpriseReportingFilter
    implements HasStringId
{
  @Id
  @Column(name = "enterprise_reporting_filter_id")
  private String id;

  @Column(name = "filter_name")
  private String filterName;

  @Column(name = "filter_json")
  private String filter;

  @Column(name = "user_id")
  private String userId;

  public EnterpriseReportingFilter() {
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getFilterName() {
    return filterName;
  }

  public void setFilterName(final String filterName) {
    this.filterName = filterName;
  }

  public String getFilter() {
    return filter;
  }

  public void setFilter(final String filter) {
    this.filter = filter;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(final String userId) {
    this.userId = userId;
  }
}
