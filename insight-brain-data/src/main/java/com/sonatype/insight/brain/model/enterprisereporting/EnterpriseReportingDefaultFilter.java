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
@Table(name = "enterprise_reporting_default_filter")
public class EnterpriseReportingDefaultFilter implements HasStringId
{
  @Id
  @Column(name = "user_id")
  private String id;

  @Column(name = "enterprise_reporting_filter_id")
  private String filterId;

  public EnterpriseReportingDefaultFilter() {
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String userId) {
    this.id = userId;
  }

  public String getFilterId() {
    return filterId;
  }

  public void setFilterId(final String filterId) {
    this.filterId = filterId;
  }
}
