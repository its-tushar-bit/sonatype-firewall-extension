/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.Date;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;

/**
 * @since 1.17
 */
@Entity
@Table(name = "repository_policy_violation")
public class RepositoryPolicyViolation
    extends AbstractPolicyViolation
{
  @Id
  @Column(name = "repository_policy_violation_id")
  private String id;

  @Column(name = "repository_id")
  private String repositoryId;

  @Column(name = "pathname")
  private String pathname;

  @Column(name = "time")
  private Date time;

  /**
   * @since 1.12
   */
  @Column(name = "waived")
  private boolean isWaived;

  /**
   * @deprecated Since 1.90, we don't save inactive repository policy violations anymore.
   *             The existing inactive repository policy violations are cleaned up by InactiveRepositoryViolationCleaner
   *             when the server starts. Since the cleanup is done asynchronously, we still need to filter out inactive
   *             violations for some operations.
   *             See https://issues.sonatype.org/browse/CLM-14555 for more details.
   */
  @Deprecated
  @Column(name = "active")
  private boolean active = true;

  public RepositoryPolicyViolation() {
  }

  public RepositoryPolicyViolation(
      String repositoryId,
      String pathname,
      Date time,
      String policyId,
      String policyName,
      int threatLevel,
      PolicyThreatCategory threatCategory,
      String hash,
      ComponentIdentifier componentIdentifier,
      List<ConstraintFact> constraintFacts)
  {
    super(policyId, policyName, threatLevel, threatCategory, hash, componentIdentifier, constraintFacts);
    this.repositoryId = repositoryId;
    this.pathname = pathname;
    this.time = time;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getRepositoryId() {
    return repositoryId;
  }

  public void setRepositoryId(String repositoryId) {
    this.repositoryId = repositoryId;
  }

  public String getPathname() {
    return pathname;
  }

  public void setPathname(String pathname) {
    this.pathname = pathname;
  }

  public Date getTime() {
    return time;
  }

  public void setTime(Date time) {
    this.time = time;
  }

  @Override
  public boolean isWaived() {
    return isWaived;
  }

  public void setWaived(boolean isWaived) {
    if (this.isWaived && !isWaived) {
      throw new IllegalStateException("Cannot un-waive a repository policy violation.");
    }
    this.isWaived = isWaived;
  }

  @Override
  public void setWaiveTime(Date waiveTime) {
    if (getWaiveTime() != null && waiveTime == null) {
      throw new IllegalStateException("Cannot un-waive a repository policy violation.");
    }
    super.setWaiveTime(waiveTime);
  }

  @Transient
  @Override
  public String getStageTypeId() {
    return StageTypes.PROXY.getId();
  }

  @Transient
  @Override
  public Date getOpenTime() {
    return getTime();
  }

  @Transient
  @Override
  public String getOwnerId() {
    return getRepositoryId();
  }
}
