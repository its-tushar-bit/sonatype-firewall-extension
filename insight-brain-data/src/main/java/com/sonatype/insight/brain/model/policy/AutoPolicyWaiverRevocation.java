/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.model.HasStringId;

/*
 * @since 1.183
 */
@Entity
@Table(name = "auto_policy_waiver_revocation")
public class AutoPolicyWaiverRevocation
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

  @Column(name = "associated_package_url")
  private String associatedPackageUrl;

  @Column(name = "scan_id")
  private String scanId;

  @Column(name = "component_match_strategy")
  private ComponentMatcherStrategyForRevocation componentMatchStrategy;

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

  @Transient
  private ComponentIdentifier componentIdentifier;

  public AutoPolicyWaiverRevocation() {
  }

  public AutoPolicyWaiverRevocation(
      String ownerId,
      String creatorId,
      String creatorName,
      Date createTime,
      String autoPolicyWaiverId,
      String scanId,
      String hash,
      String associatedPackageUrl,
      ComponentMatcherStrategyForRevocation componentMatchStrategy
  )
  {
    this(ownerId, creatorId, creatorName, createTime, autoPolicyWaiverId, scanId, hash, associatedPackageUrl);
    setComponentMatchStrategy(componentMatchStrategy);
  }

  public AutoPolicyWaiverRevocation(
      String ownerId,
      String creatorId,
      String creatorName,
      Date createTime,
      String autoPolicyWaiverId,
      String scanId,
      String hash,
      String associatedPackageUrl)
  {
    this.ownerId = ownerId;
    this.creatorId = creatorId;
    this.creatorName = creatorName;
    this.createTime = createTime;
    this.autoPolicyWaiverId = autoPolicyWaiverId;
    this.scanId = scanId;
    this.hash = hash;
    this.associatedPackageUrl = associatedPackageUrl;
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

  public String getAssociatedPackageUrl() {
    return associatedPackageUrl;
  }

  public void setAssociatedPackageUrl(String associatedPackageUrl) {
    this.associatedPackageUrl = associatedPackageUrl;
  }

  public String getScanId() {
    return scanId;
  }

  public void setScanId(String scanId) {
    this.scanId = scanId;
  }

  public ComponentMatcherStrategyForRevocation getComponentMatchStrategy() {
    return componentMatchStrategy;
  }

  public void setComponentMatchStrategy(ComponentMatcherStrategyForRevocation componentMatchStrategy) {
    this.componentMatchStrategy = componentMatchStrategy;
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

  public enum ComponentMatcherStrategyForRevocation
  {
    EXACT_COMPONENT,
    ALL_VERSIONS;

    @Override
    public String toString() {
      return name();
    }
  }
}
  
