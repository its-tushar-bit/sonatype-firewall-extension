/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.tenancy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "deleted_tenant")
public class DeletedTenant
    implements HasStringId
{
  @Id
  @Column(name = "tenant_slug")
  private String tenantSlug;

  @Column(name = "delete_requested_timestamp")
  private Long deleteRequestedTimestamp;

  public DeletedTenant() {
  }

  public DeletedTenant(String tenantSlug) {
    this(tenantSlug, System.currentTimeMillis());
  }

  public DeletedTenant(String tenantSlug, Long deleteRequestedTime) {
    this.tenantSlug = tenantSlug;
    this.setDeleteRequestedTimestamp(deleteRequestedTime);
  }

  @Override
  public String getId() {
    return tenantSlug;
  }

  @Override
  public void setId(String tenantSlug) {
    this.tenantSlug = tenantSlug;
  }

  public Long getDeleteRequestedTimestamp() {
    return deleteRequestedTimestamp;
  }

  public void setDeleteRequestedTimestamp(Long deleteRequestedTimestamp) {
    this.deleteRequestedTimestamp = deleteRequestedTimestamp;
  }

  @Override
  public String toString() {
    return "DeletedTenant{" +
        "tenantSlug='" + tenantSlug + '\'' +
        ", deleteRequestedTime=" + deleteRequestedTimestamp +
        '}';
  }
}
