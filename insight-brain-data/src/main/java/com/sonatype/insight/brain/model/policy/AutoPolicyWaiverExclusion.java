/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang3.StringUtils;

/*
 * @since 1.183
 */
@Entity
@Table(name = "auto_policy_waiver_revocation")
public class AutoPolicyWaiverExclusion
    implements HasStringId
{
  @Id
  @Column(name = "auto_policy_waiver_revocation_id")
  private String id;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "creator_id")
  private String creatorId;

  @Column(name = "creator_name")
  private String creatorName;

  @Column(name = "create_time")
  private Date createTime;

  @Column(name = "auto_policy_waiver_id")
  private String autoPolicyWaiverId;

  @Column(name = "hash")
  private String hash;

  @Column(name = "scan_id")
  private String scanId;

  @Column(name = "component_match_strategy")
  private ComponentMatcherStrategyForExclusion componentMatchStrategy;

  @Column(name = "policy_violation_id")
  private String policyViolationId;

  @Column(name = "threat_level")
  private Integer threatLevel;

  @Column(name = "vulnerability_identifiers")
  private String vulnerabilityIdentifiers;

  @Column(name = "policy_name")
  private String policyName;

  @Column(name = "component_display_name")
  private String componentDisplayName;

  @Column(name = "policy_id")
  private String policyId;

  @Column(name = "associated_package_url")
  private String associatedPackageUrl;

  @Column(name = "constraint_facts_json")
  private String constraintFactsJson;

  @Transient
  private List<ConstraintFact> constraintFacts;

  @Transient
  private ComponentIdentifier componentIdentifier;

  public AutoPolicyWaiverExclusion() {
  }

  public AutoPolicyWaiverExclusion(
      String ownerId,
      String creatorId,
      String creatorName,
      Date createTime,
      String autoPolicyWaiverId,
      String scanId,
      String hash,
      ComponentMatcherStrategyForExclusion componentMatchStrategy,
      String policyViolationId,
      Integer threatLevel,
      String vulnerabilityIdentifiers,
      String policyName,
      String componentDisplayName,
      String policyId,
      ComponentIdentifier componentIdentifier,
      List<ConstraintFact> constraintFacts)
  {
    this(ownerId, creatorId, creatorName, createTime, autoPolicyWaiverId, scanId, hash);
    setComponentMatchStrategy(componentMatchStrategy);
    setPolicyViolationId(policyViolationId);
    setThreatLevel(threatLevel);
    setVulnerabilityIdentifiers(vulnerabilityIdentifiers);
    setPolicyName(policyName);
    setComponentDisplayName(componentDisplayName);
    setPolicyId(policyId);
    setComponentIdentifier(componentIdentifier);
    setConstraintFacts(constraintFacts);
  }

  public AutoPolicyWaiverExclusion(
      String ownerId,
      String creatorId,
      String creatorName,
      Date createTime,
      String autoPolicyWaiverId,
      String scanId,
      String hash,
      ComponentMatcherStrategyForExclusion componentMatchStrategy)
  {
    this(ownerId, creatorId, creatorName, createTime, autoPolicyWaiverId, scanId, hash);
    setComponentMatchStrategy(componentMatchStrategy);
  }

  public AutoPolicyWaiverExclusion(
      String ownerId,
      String creatorId,
      String creatorName,
      Date createTime,
      String autoPolicyWaiverId,
      String scanId,
      String hash)
  {
    this.ownerId = ownerId;
    this.creatorId = creatorId;
    this.creatorName = creatorName;
    this.createTime = createTime;
    this.autoPolicyWaiverId = autoPolicyWaiverId;
    this.scanId = scanId;
    this.hash = hash;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String id) {
    this.ownerId = id;
  }

  public String getCreatorId() {
    return creatorId;
  }

  public void setCreatorId(String creatorId) {
    this.creatorId = creatorId;
  }

  public String getCreatorName() {
    return creatorName;
  }

  public void setCreatorName(String creatorName) {
    this.creatorName = creatorName;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public void setAutoPolicyWaiverId(String autoPolicyWaiverId) {
    this.autoPolicyWaiverId = autoPolicyWaiverId;
  }

  public String getAutoPolicyWaiverId() {
    return autoPolicyWaiverId;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public String getScanId() {
    return scanId;
  }

  public void setScanId(String scanId) {
    this.scanId = scanId;
  }

  public ComponentMatcherStrategyForExclusion getComponentMatchStrategy() {
    return componentMatchStrategy;
  }

  public void setComponentMatchStrategy(ComponentMatcherStrategyForExclusion componentMatchStrategy) {
    this.componentMatchStrategy = componentMatchStrategy;
  }

  public String getAssociatedPackageUrl() {
    return associatedPackageUrl;
  }

  public void setAssociatedPackageUrl(String associatedPackageUrl) {
    this.associatedPackageUrl = associatedPackageUrl;
  }

  public ComponentIdentifier getComponentIdentifier() {
    if (componentIdentifier == null) {
      if (associatedPackageUrl == null) {
        return null;
      }
      componentIdentifier = ComponentIdentifierAdapter.toComponentIdentifier(associatedPackageUrl);
    }
    return componentIdentifier;
  }

  public void setComponentIdentifier(ComponentIdentifier componentIdentifier) {
    this.componentIdentifier = componentIdentifier;
    this.associatedPackageUrl = PackageUrlIdentifier.toPackageUrl(componentIdentifier);
  }

  public String getPolicyViolationId() {
    return policyViolationId;
  }

  public void setPolicyViolationId(String policyViolationId) {
    this.policyViolationId = policyViolationId;
  }

  public Integer getThreatLevel() {
    return threatLevel;
  }

  public void setThreatLevel(Integer threatLevel) {
    this.threatLevel = threatLevel;
  }

  public String getVulnerabilityIdentifiers() {
    return vulnerabilityIdentifiers;
  }

  public void setVulnerabilityIdentifiers(String vulnerabilityIdentifiers) {
    this.vulnerabilityIdentifiers = vulnerabilityIdentifiers;
  }

  public String getPolicyName() {
    return policyName;
  }

  public void setPolicyName(String policyName) {
    this.policyName = policyName;
  }

  public String getComponentDisplayName() {
    return componentDisplayName;
  }

  public void setComponentDisplayName(String componentDisplayName) {
    this.componentDisplayName = componentDisplayName;
  }

  public String getPolicyId() {
    return policyId;
  }

  public void setPolicyId(String policyId) {
    this.policyId = policyId;
  }

  public String getConstraintFactsJson() {
    return constraintFactsJson;
  }

  public void setConstraintFactsJson(String constraintFactsJson) {
    if (StringUtils.isBlank(constraintFactsJson)) {
      constraintFactsJson = null;
    }
    this.constraintFactsJson = constraintFactsJson;
    constraintFacts = null;
  }

  public void setConstraintFacts(List<ConstraintFact> constraintFacts) {
    if (constraintFacts == null || constraintFacts.isEmpty()) {
      this.constraintFacts = null;
      constraintFactsJson = null;
    }
    else {
      this.constraintFacts = constraintFacts;
      constraintFactsJson = JsonUtils.writeUnformatted(constraintFacts);
    }
  }

  public List<ConstraintFact> getConstraintFacts() {
    if (constraintFacts == null && !StringUtils.isBlank(constraintFactsJson)) {
      try {
        constraintFacts = Arrays.asList(JsonUtils.parse(constraintFactsJson, ConstraintFact[].class));
      }
      catch (IOException e) {
        throw new UncheckedIOException("Failed to read constraint facts for policy waiver " + id, e);
      }
    }
    return constraintFacts;
  }

  public enum ComponentMatcherStrategyForExclusion
  {
    EXACT_COMPONENT,
    ALL_VERSIONS,
    POLICY_VIOLATION;

    @Override
    public String toString() {
      return name();
    }
  }
}
