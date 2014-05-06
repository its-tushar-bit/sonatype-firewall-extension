/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.filter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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

  @Column(name = "filter_json")
  private String filter;

  public DashboardFilter() {
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

  public String getFilter() {
    return filter;
  }

  public void setFilter(final String filter) {
    this.filter = filter;
  }
}
