/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.tenancy;

import java.util.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

@Entity
@Table(name = "deleted_tenant")
public class DeletedTenant
    implements HasStringId
{
  @Id
  @Column(name = "tenant_slug")
  private String tenantSlug;

  @Column(name = "created")
  private Date created;

  @Column(name = "last_updated")
  private Date lastUpdated;

  @Column(name = "delete_completed_date")
  private Date deleteCompletedDate;

  public DeletedTenant() {
  }

  public DeletedTenant(String tenantSlug) {
    this(tenantSlug, new Date());
  }

  public DeletedTenant(String tenantSlug, Date deleteRequestedTime) {
    this.tenantSlug = tenantSlug;
    this.setCreated(deleteRequestedTime);
  }

  @Override
  public String getId() {
    return tenantSlug;
  }

  @Override
  public void setId(String tenantSlug) {
    this.tenantSlug = tenantSlug;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(final Date created) {
    this.created = created;
  }

  public Date getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(final Date lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  public Date getDeleteCompletedDate() {
    return deleteCompletedDate;
  }

  public void setDeleteCompletedDate(final Date deleteCompletedDate) {
    this.deleteCompletedDate = deleteCompletedDate;
  }

  @Override
  public String toString() {
    return "DeletedTenant{" +
        "tenantSlug='" + tenantSlug + '\'' +
        ", created=" + created + '\'' +
        ", lastUpdated=" + lastUpdated + '\'' +
        ", deleteCompletedDate=" + deleteCompletedDate +
        '}';
  }
}
