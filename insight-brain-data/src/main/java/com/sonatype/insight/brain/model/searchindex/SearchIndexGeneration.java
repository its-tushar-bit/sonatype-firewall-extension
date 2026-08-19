/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.searchindex;

import java.util.Date;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "search_index_generation")
public class SearchIndexGeneration
    implements HasStringId
{
  public static final String ROLE_SERVING = "SERVING";

  public static final String ROLE_BUILDING = "BUILDING";

  public static final String ROLE_RETIRED = "RETIRED";

  public static final String ROLE_FAILED = "FAILED";

  public static final String BACKEND_LUCENE = "LUCENE";

  public static final String BACKEND_OPENSEARCH = "OPENSEARCH";

  public static final String BACKEND_HYBRID = "HYBRID";

  @Id
  @Column(name = "search_index_generation_id")
  private String id;

  @Column(name = "backend", nullable = false)
  private String backend;

  @Column(name = "role", nullable = false)
  private String role;

  @Column(name = "schema_version", nullable = false)
  private int schemaVersion;

  @Column(name = "storage_ref", nullable = false)
  private String storageRef;

  @Column(name = "doc_count")
  private Long docCount;

  @Column(name = "created_at", nullable = false)
  private Date createdAt;

  @Column(name = "serving_since")
  private Date servingSince;

  @Column(name = "retired_at")
  private Date retiredAt;

  @Column(name = "created_by_job_id")
  private String createdByJobId;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getBackend() {
    return backend;
  }

  public void setBackend(final String backend) {
    this.backend = backend;
  }

  public String getRole() {
    return role;
  }

  public void setRole(final String role) {
    this.role = role;
  }

  public int getSchemaVersion() {
    return schemaVersion;
  }

  public void setSchemaVersion(final int schemaVersion) {
    this.schemaVersion = schemaVersion;
  }

  public String getStorageRef() {
    return storageRef;
  }

  public void setStorageRef(final String storageRef) {
    this.storageRef = storageRef;
  }

  public Long getDocCount() {
    return docCount;
  }

  public void setDocCount(final Long docCount) {
    this.docCount = docCount;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(final Date createdAt) {
    this.createdAt = createdAt;
  }

  public Date getServingSince() {
    return servingSince;
  }

  public void setServingSince(final Date servingSince) {
    this.servingSince = servingSince;
  }

  public Date getRetiredAt() {
    return retiredAt;
  }

  public void setRetiredAt(final Date retiredAt) {
    this.retiredAt = retiredAt;
  }

  public String getCreatedByJobId() {
    return createdByJobId;
  }

  public void setCreatedByJobId(final String createdByJobId) {
    this.createdByJobId = createdByJobId;
  }
}
