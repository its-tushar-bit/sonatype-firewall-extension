/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * A single policy violation recorded against a {@link HostedDeploymentBlock}.
 * <p>
 * Intentionally a narrow projection of the internal violation model — only enough detail to
 * render an actionable error message to the developer and support the future "view blocked
 * deployment details" UI. The full internal violation structure is not duplicated here.
 * <p>
 * Foreign-key deletion CASCADEs from {@link HostedDeploymentBlock}, so the cleanup task only
 * needs to remove parent rows.
 */
@Entity
@Table(name = "hosted_deployment_block_violation")
public class HostedDeploymentBlockViolation
    implements HasStringId
{
  @Id
  @Column(name = "hosted_deployment_block_violation_id")
  private String id;

  @Column(name = "hosted_deployment_block_id", nullable = false)
  private String hostedDeploymentBlockId;

  @Column(name = "policy_name", nullable = false, length = 200)
  private String policyName;

  @Column(name = "constraint_name", length = 200)
  private String constraintName;

  @Column(name = "reason", length = 2000)
  private String reason;

  @Column(name = "component_identifier", length = 500)
  private String componentIdentifier;

  public HostedDeploymentBlockViolation() {
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(final String id) {
    this.id = id;
  }

  public String getHostedDeploymentBlockId() {
    return hostedDeploymentBlockId;
  }

  public void setHostedDeploymentBlockId(final String hostedDeploymentBlockId) {
    this.hostedDeploymentBlockId = hostedDeploymentBlockId;
  }

  public String getPolicyName() {
    return policyName;
  }

  public void setPolicyName(final String policyName) {
    this.policyName = policyName;
  }

  public String getConstraintName() {
    return constraintName;
  }

  public void setConstraintName(final String constraintName) {
    this.constraintName = constraintName;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(final String reason) {
    this.reason = reason;
  }

  public String getComponentIdentifier() {
    return componentIdentifier;
  }

  public void setComponentIdentifier(final String componentIdentifier) {
    this.componentIdentifier = componentIdentifier;
  }
}
