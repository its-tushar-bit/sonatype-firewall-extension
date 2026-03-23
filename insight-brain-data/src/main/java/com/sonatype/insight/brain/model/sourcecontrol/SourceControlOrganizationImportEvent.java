/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sourcecontrol;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.162
 */
@Entity
@Table(name = "source_control_organization_import_event")
public class SourceControlOrganizationImportEvent
    implements HasStringId
{
  public enum ImportStatus
  {
    IN_PROGRESS,
    COMPLETE,
    ERROR
  }

  public static final int DEFAULT_DESIRED_SUB_ORGANIZATION_COUNT = 0;

  public static final int DEFAULT_IMPORT_LIMIT = -1;

  @Id
  @Column(name = "source_control_organization_import_event_id")
  private String id;

  @Column(name = "organization_id")
  private String organizationId;

  @Column(name = "source_control_host_url")
  private String scmHostUrl;

  @Column(name = "desired_sub_organization_count")
  private int desiredSubOrganizationCount = DEFAULT_DESIRED_SUB_ORGANIZATION_COUNT;

  @Column(name = "import_limit")
  private int importLimit = DEFAULT_IMPORT_LIMIT;

  @Column(name = "import_status")
  @Enumerated(EnumType.STRING)
  private ImportStatus importStatus = ImportStatus.IN_PROGRESS;

  @Column(name = "import_success_count")
  private int importSuccessCount = 0;

  @Column(name = "import_failure_count")
  private int importFailureCount = 0;

  @Column(name = "start_time")
  private Date startTime = new Date();

  @Column(name = "last_updated_time")
  private Date lastUpdatedTime = new Date();

  @Column(name = "import_errors")
  private String importErrors;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public SourceControlOrganizationImportEvent setOrganizationId(final String organizationId) {
    this.organizationId = organizationId;
    return this;
  }

  public String getScmHostUrl() {
    return scmHostUrl;
  }

  public SourceControlOrganizationImportEvent setScmHostUrl(final String scmHostUrl) {
    this.scmHostUrl = scmHostUrl;
    return this;
  }

  public int getDesiredSubOrganizationCount() {
    return desiredSubOrganizationCount;
  }

  public SourceControlOrganizationImportEvent setDesiredSubOrganizationCount(
      final int desiredSubOrganizationCount)
  {
    this.desiredSubOrganizationCount = desiredSubOrganizationCount;
    return this;
  }

  public int getImportLimit() {
    return importLimit;
  }

  public SourceControlOrganizationImportEvent setImportLimit(final int importLimit) {
    this.importLimit = importLimit;
    return this;
  }

  public ImportStatus getImportStatus() {
    return importStatus;
  }

  public SourceControlOrganizationImportEvent setImportStatus(final ImportStatus importStatus) {
    this.importStatus = importStatus;
    return this;
  }

  /**
   * Setter for jOOQ record mapping when reading from database.
   * jOOQ may pass the string value from the database directly.
   */
  public void setImportStatus(final String importStatusString) {
    this.importStatus = importStatusString != null ? ImportStatus.valueOf(importStatusString) : null;
  }

  public int getImportSuccessCount() {
    return importSuccessCount;
  }

  public SourceControlOrganizationImportEvent setImportSuccessCount(final int importSuccessCount) {
    this.importSuccessCount = importSuccessCount;
    return this;
  }

  public int getImportFailureCount() {
    return importFailureCount;
  }

  public SourceControlOrganizationImportEvent setImportFailureCount(final int importFailureCount) {
    this.importFailureCount = importFailureCount;
    return this;
  }

  public Date getStartTime() {
    return startTime;
  }

  public SourceControlOrganizationImportEvent setStartTime(final Date startTime) {
    this.startTime = startTime;
    return this;
  }

  /**
   * Setter for jOOQ record mapping when reading from database.
   * The database stores LocalDateTime which jOOQ may pass directly.
   */
  public void setStartTime(final LocalDateTime startTime) {
    this.startTime = startTime != null ? new Date(Timestamp.valueOf(startTime).getTime()) : null;
  }

  public Date getLastUpdatedTime() {
    return lastUpdatedTime;
  }

  public SourceControlOrganizationImportEvent setLastUpdatedTime(final Date lastUpdatedTime) {
    this.lastUpdatedTime = lastUpdatedTime;
    return this;
  }

  /**
   * Setter for jOOQ record mapping when reading from database.
   * The database stores LocalDateTime which jOOQ may pass directly.
   */
  public void setLastUpdatedTime(final LocalDateTime lastUpdatedTime) {
    this.lastUpdatedTime = lastUpdatedTime != null ? new Date(Timestamp.valueOf(lastUpdatedTime).getTime()) : null;
  }

  public String getImportErrors() {
    return importErrors;
  }

  public SourceControlOrganizationImportEvent setImportErrors(final String importErrors) {
    this.importErrors = importErrors;
    return this;
  }
}
