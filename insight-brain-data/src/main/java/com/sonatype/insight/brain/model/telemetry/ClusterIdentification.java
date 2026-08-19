/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.telemetry;

import java.util.Date;

import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cluster_identification")
public class ClusterIdentification
    implements HasStringId
{
  @Id
  @Column(name = "cluster_identification_id")
  private String id;

  @Column(name = "assigned_cluster_id")
  private String assignedClusterId;

  @Column(name = "assigned_telemetry_id")
  private String assignedTelemetryId;

  @Column(name = "tamper_code")
  private String tamperCode;

  @Column(name = "base_url_hash")
  private String baseUrlHash;

  @Column(name = "last_calculated_cluster_id")
  private String lastCalculatedClusterId;

  @Column(name = "created")
  private Date created;

  @Column(name = "last_updated")
  private Date lastUpdated;

  // Getters and Setters

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getAssignedClusterId() {
    return assignedClusterId;
  }

  public void setAssignedClusterId(String assignedClusterId) {
    this.assignedClusterId = assignedClusterId;
  }

  public String getAssignedTelemetryId() {
    return assignedTelemetryId;
  }

  public void setAssignedTelemetryId(String assignedTelemetryId) {
    this.assignedTelemetryId = assignedTelemetryId;
  }

  public String getTamperCode() {
    return tamperCode;
  }

  public void setTamperCode(String tamperCode) {
    this.tamperCode = tamperCode;
  }

  public String getBaseUrlHash() {
    return baseUrlHash;
  }

  public void setBaseUrlHash(String baseUrlHash) {
    this.baseUrlHash = baseUrlHash;
  }

  public String getLastCalculatedClusterId() {
    return lastCalculatedClusterId;
  }

  public void setLastCalculatedClusterId(String lastCalculatedClusterId) {
    this.lastCalculatedClusterId = lastCalculatedClusterId;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public Date getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(Date lastUpdated) {
    this.lastUpdated = lastUpdated;
  }
}
