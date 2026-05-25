/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.repository;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * Record of a synchronous hosted-repository deployment that was blocked by policy evaluation.
 * <p>
 * Each blocked deployment attempt gets its own row — there is no unique constraint on
 * {@code (repository_id, pathname)}, so retries by the same developer preserve history rather
 * than overwriting each other. Rows are deleted by a periodic cleanup task once
 * {@code blocked_time} is older than the configured retention TTL.
 * <p>
 * Associated policy-violation details are stored in
 * {@link HostedDeploymentBlockViolation}, referenced by {@code hosted_deployment_block_id}.
 */
@Entity
@Table(name = "hosted_deployment_block")
public class HostedDeploymentBlock
    implements HasStringId
{
  @Id
  @Column(name = "hosted_deployment_block_id")
  private String id;

  @Column(name = "repository_id", nullable = false)
  private String repositoryId;

  @Column(name = "pathname", nullable = false, length = 1000)
  private String pathname;

  @Column(name = "hash", length = 100)
  private String hash;

  @Column(name = "component_id_format", length = 10)
  private String componentIdFormat;

  @Column(name = "component_id_coordinates_json", length = 1000)
  private String componentIdCoordinatesJson;

  @Column(name = "display_name", length = 1000)
  private String displayName;

  @Column(name = "policy_action", nullable = false, length = 20)
  private String policyAction;

  @Column(name = "highest_threat_level", nullable = false)
  private int highestThreatLevel;

  @Column(name = "evaluation_url", length = 2000)
  private String evaluationUrl;

  @Column(name = "correlation_id", length = 100)
  private String correlationId;

  @Column(name = "requested_by", length = 200)
  private String requestedBy;

  @Column(name = "blocked_time", nullable = false)
  private Date blockedTime;

  public HostedDeploymentBlock() {
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(final String repositoryId) {
    this.repositoryId = repositoryId;
  }

  public String getPathname() {
    return pathname;
  }

  public void setPathname(final String pathname) {
    this.pathname = pathname;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(final String hash) {
    this.hash = hash;
  }

  public String getComponentIdFormat() {
    return componentIdFormat;
  }

  public void setComponentIdFormat(final String componentIdFormat) {
    this.componentIdFormat = componentIdFormat;
  }

  public String getComponentIdCoordinatesJson() {
    return componentIdCoordinatesJson;
  }

  public void setComponentIdCoordinatesJson(final String componentIdCoordinatesJson) {
    this.componentIdCoordinatesJson = componentIdCoordinatesJson;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(final String displayName) {
    this.displayName = displayName;
  }

  public String getPolicyAction() {
    return policyAction;
  }

  public void setPolicyAction(final String policyAction) {
    this.policyAction = policyAction;
  }

  public int getHighestThreatLevel() {
    return highestThreatLevel;
  }

  public void setHighestThreatLevel(final int highestThreatLevel) {
    this.highestThreatLevel = highestThreatLevel;
  }

  public String getEvaluationUrl() {
    return evaluationUrl;
  }

  public void setEvaluationUrl(final String evaluationUrl) {
    this.evaluationUrl = evaluationUrl;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public void setCorrelationId(final String correlationId) {
    this.correlationId = correlationId;
  }

  public String getRequestedBy() {
    return requestedBy;
  }

  public void setRequestedBy(final String requestedBy) {
    this.requestedBy = requestedBy;
  }

  public Date getBlockedTime() {
    return blockedTime;
  }

  public void setBlockedTime(final Date blockedTime) {
    this.blockedTime = blockedTime;
  }
}
