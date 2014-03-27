/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

import org.codehaus.plexus.util.StringUtils;

/**
 * @since 1.11
 */
@Entity
@Table(name = "policy_violation")
public class PolicyViolation
    implements HasStringId
{
  @Id
  @Column(name = "policy_violation_id")
  private String id;

  @Column(name = "policy_evaluation_id")
  private String policyEvaluationId;

  @Column(name = "policy_id")
  private String policyId;

  @Column(name = "threat_level")
  private int threatLevel;

  @Column(name = "threat_category")
  @Enumerated(EnumType.STRING)
  private PolicyThreatCategory threatCategory;

  @Column(name = "hash")
  private String hash;

  @Column(name = "group_id")
  private String groupId;

  @Column(name = "artifact_id")
  private String artifactId;

  @Column(name = "version")
  private String version;

  @Column(name = "constraint_facts_json")
  private String constraintFactsJson;

  private List<ConstraintFact> constraintFacts;

  public PolicyViolation() {
  }

  public PolicyViolation(String policyEvaluationId, String policyId, int threatLevel,
      PolicyThreatCategory threatCategory, String hash, String groupId, String artifactId, String version,
      String constraintFactsJson)
  {
    this.policyEvaluationId = policyEvaluationId;
    this.policyId = policyId;
    this.threatLevel = threatLevel;
    this.threatCategory = threatCategory;
    this.hash = hash;
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    setConstraintFactsJson(constraintFactsJson);
  }

  public PolicyViolation(String policyEvaluationId, String policyId, int threatLevel,
      PolicyThreatCategory threatCategory, String hash, String groupId, String artifactId, String version,
      List<ConstraintFact> constraintFacts)
  {
    this.policyEvaluationId = policyEvaluationId;
    this.policyId = policyId;
    this.threatLevel = threatLevel;
    this.threatCategory = threatCategory;
    this.hash = hash;
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    setConstraintFacts(constraintFacts);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getPolicyEvaluationId() {
    return policyEvaluationId;
  }

  public void setPolicyEvaluationId(String policyEvaluationId) {
    this.policyEvaluationId = policyEvaluationId;
  }

  public String getPolicyId() {
    return policyId;
  }

  public void setPolicyId(String policyId) {
    this.policyId = policyId;
  }

  public int getThreatLevel() {
    return threatLevel;
  }

  public void setThreatLevel(int threatLevel) {
    this.threatLevel = threatLevel;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public PolicyThreatCategory getThreatCategory() {
    return threatCategory;
  }

  public void setThreatCategory(PolicyThreatCategory threatCategory) {
    this.threatCategory = threatCategory;
  }

  public String getConstraintFactsJson() {
    return constraintFactsJson;
  }

  public void setConstraintFactsJson(String constraintFactsJson) {
    if (StringUtils.isEmpty(constraintFactsJson)) {
      throw new IllegalArgumentException("ConstraintFactsJson cannot be null or empty");
    }
    this.constraintFactsJson = constraintFactsJson;
    constraintFacts = null;
  }

  private void setConstraintFacts(List<ConstraintFact> constraintFacts) {
    if (constraintFacts == null || constraintFacts.isEmpty()) {
      throw new IllegalArgumentException("ConstraintFacts cannot be null or empty");
    }

    this.constraintFacts = constraintFacts;
    constraintFactsJson = JsonUtils.format(constraintFacts);
  }

  public List<ConstraintFact> getConstraintFacts() {
    if (constraintFacts == null && !StringUtils.isEmpty(constraintFactsJson)) {
      try {
        constraintFacts = Arrays.asList(JsonUtils.parse(constraintFactsJson, ConstraintFact[].class));
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return constraintFacts;
  }
}
