/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.model.HasStringId;

/**
 * @since 1.17
 */
@Entity
@Table(name = "repository_policy_violation")
public class RepositoryPolicyViolation
    extends AbstractPolicyViolation
  implements HasStringId
{
  @Id
  @Column(name = "repository_policy_violation_id")
  private String id;

  @Column(name = "repository_id")
  private String repositoryId;

  @Column(name = "pathname")
  private String pathname;

  @Column(name = "latest_evaluation")
  private boolean latestEvaluation = true;

  public RepositoryPolicyViolation() {
  }

  public RepositoryPolicyViolation(String repositoryId, String pathname, Date time, String policyId, String policyName,
      int threatLevel, PolicyThreatCategory threatCategory, String hash, ComponentIdentifier componentIdentifier,
      String constraintFactsJson)
  {
    super(time, policyId, policyName, threatLevel, threatCategory, hash, componentIdentifier, constraintFactsJson);
    this.repositoryId = repositoryId;
    this.pathname = pathname;
  }

  public RepositoryPolicyViolation(String repositoryId, String pathname, Date time, String policyId, String policyName,
      int threatLevel, PolicyThreatCategory threatCategory, String hash, ComponentIdentifier componentIdentifier,
      List<ConstraintFact> constraintFacts)
  {
    super(time, policyId, policyName, threatLevel, threatCategory, hash, componentIdentifier, constraintFacts);
    this.repositoryId = repositoryId;
    this.pathname = pathname;
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

  public boolean isLatestEvaluation() {
    return latestEvaluation;
  }

  public void setLatestEvaluation(boolean latestEvaluation) {
    this.latestEvaluation = latestEvaluation;
  }

  public String getPathname() {
    return pathname;
  }

  public void setPathname(String pathname) {
    this.pathname = pathname;
  }
}
