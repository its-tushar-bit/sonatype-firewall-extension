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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

import org.apache.commons.lang3.StringUtils;

/**
 * @since 1.6
 */
@Entity
@Table(name = "policy_waiver")
public class PolicyWaiver
    implements HasStringId
{
  @Id
  @Column(name = "policy_waiver_id")
  private String id;

  @Column(name = "hash")
  private String hash;

  @Column(name = "policy_id")
  private String policyId;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "comment")
  private String comment;

  @Column(name = "create_time")
  private Date createTime;

  @Column(name = "expiry_time")
  private Date expiryTime;

  @Column(name = "creator_id")
  private String creatorId;

  @Column(name = "creator_name")
  private String creatorName;

  /**
   * @since 1.53
   */
  @Column(name = "constraint_facts_json")
  private String constraintFactsJson;

  @Transient
  private List<ConstraintFact> constraintFacts;

  /**
   * @since 1.140
   */
  @Column(name = "associated_package_url")
  private String associatedPackageUrl;

  /**
   * @since 1.140
   */
  @Column(name = "component_match_strategy")
  @Enumerated(EnumType.STRING)
  private ComponentMatcherStrategyForWaiver componentMatchStrategy;

  /**
   * @since 1.140
   */
  @Transient
  private ComponentIdentifier componentIdentifier;

  public PolicyWaiver() {
  }

  public PolicyWaiver(String policyId, String ownerId, String comment) {
    this.policyId = policyId;
    this.ownerId = ownerId;
    this.comment = comment;
  }

  public PolicyWaiver(String hash, String policyId, String ownerId, String comment) {
    this(policyId, ownerId, comment);
    setHash(hash);
  }

  public PolicyWaiver(String hash,
                      String policyId,
                      String ownerId,
                      List<ConstraintFact> constraintFacts,
                      String comment)
  {
    this(policyId, ownerId, comment);
    setHash(hash);
    setConstraintFacts(constraintFacts);
  }

  public PolicyWaiver(String hash,
                      String policyId,
                      String ownerId,
                      List<ConstraintFact> constraintFacts,
                      String associatedPackageUrl,
                      ComponentMatcherStrategyForWaiver componentMatchStrategy,
                      String comment)
  {
    this(hash, policyId, ownerId, constraintFacts, comment);
    setAssociatedPackageUrl(associatedPackageUrl);
    setComponentMatchStrategy(componentMatchStrategy);
  }

  public PolicyWaiver(String hash,
                      String policyId,
                      String ownerId,
                      String associatedPackageUrl,
                      ComponentMatcherStrategyForWaiver componentMatchStrategy,
                      String comment)
  {
    this(hash, policyId, ownerId, null, associatedPackageUrl, componentMatchStrategy, comment);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = HashHelper.truncateHash(hash);
  }

  public String getPolicyId() {
    return policyId;
  }

  public void setPolicyId(String policyId) {
    this.policyId = policyId;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
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

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public Date getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Date createTime) {
    this.createTime = createTime;
  }

  public Date getExpiryTime() {
    return expiryTime;
  }

  public void setExpiryTime(Date expiryTime) {
    this.expiryTime = expiryTime;
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

  public String getAssociatedPackageUrl() {
    return associatedPackageUrl;
  }

  public void setAssociatedPackageUrl(String associatedPackageUrl) {
    this.associatedPackageUrl = associatedPackageUrl;
  }

  public ComponentMatcherStrategyForWaiver getComponentMatchStrategy() {
    return componentMatchStrategy;
  }

  public void setComponentMatchStrategy(ComponentMatcherStrategyForWaiver componentMatchStrategy) {
    this.componentMatchStrategy = componentMatchStrategy;
  }

  public ComponentIdentifier getComponentIdentifier() {
    if (componentIdentifier == null) {
      if (associatedPackageUrl == null) {
        return null;
      }
      componentIdentifier = ComponentIdentifierAdapter.toComponentIdentifier(associatedPackageUrl);
      componentIdentifier.ensureComplete();
    }
    return componentIdentifier;
  }

  public enum ComponentMatcherStrategyForWaiver
  {
    DEFAULT,
    EXACT_COMPONENT,
    ALL_COMPONENTS,
    ALL_VERSIONS;

    @Override
    public String toString() {
      return values()[this.ordinal()].name();
    }
  }
}
