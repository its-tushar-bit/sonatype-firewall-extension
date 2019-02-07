/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

/**
 * Class used only for persistence of policies to the SQL database.
 * 
 * It provides conversions from/to {@link Policy}. The Policy class is not mapped directly to the database because its
 * structure does not match the database table structure. The Policy class has fields for policy constraints, actions,
 * notifications, while PolicyInternal stores all that in one content field in json format (mapped to CLOB in the
 * database).
 * 
 * @since 1.9
 */
@Entity
@Table(name = "policy")
public class PolicyInternal
    implements HasStringId
{
  @Id
  @Column(name = "policy_id")
  private String id;

  @Column(name = "name")
  private String name;

  @Column(name = "name_lowercase_no_whitespace")
  private String nameLowercaseNoWhitespace;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "threat_level")
  private int threatLevel = 5;

  /**
   * @since 1.50
   */
  @Column(name = "policy_violation_grandfathering_allowed")
  private boolean policyViolationGrandfatheringAllowed;

  @Column(name = "content")
  private String content;

  @Column(name = "drools_code")
  private String droolsCode;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    nameLowercaseNoWhitespace = NameHelper.normalize(name);
    this.name = name;
  }

  public String getNameLowercaseNoWhitespace() {
    return nameLowercaseNoWhitespace;
  }

  /**
   * This method is defined here only to trick jackson into "thinking" that it de-serialized the value of the
   * nameLowercaseNoWhitespace field. If this method is not defined, jackson will set/access the
   * nameLowercaseNoWhitespace field directly via reflection, possibly setting it to an incorrect value.
   * 
   * @deprecated This method should not be used explicitly.
   */
  @Deprecated
  @SuppressWarnings("unused")
  private void setNameLowercaseNoWhitespace(String nameLowercaseNoWhitespace) {
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public int getThreatLevel() {
    return threatLevel;
  }

  public void setThreatLevel(int threatLevel) {
    this.threatLevel = threatLevel;
  }

  public boolean isPolicyViolationGrandfatheringAllowed() {
    return policyViolationGrandfatheringAllowed;
  }

  public void setPolicyViolationGrandfatheringAllowed(boolean policyViolationGrandfatheringAllowed) {
    this.policyViolationGrandfatheringAllowed = policyViolationGrandfatheringAllowed;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getDroolsCode() {
    return droolsCode;
  }

  public void setDroolsCode(String droolsCode) {
    this.droolsCode = droolsCode;
  }

  public static PolicyInternal fromPolicy(Policy policy) {
    if (policy == null) {
      return null;
    }

    PolicyInternal result = new PolicyInternal();
    result.setId(policy.getId());
    result.setName(policy.getName());
    result.setOwnerId(policy.getOwnerId());
    result.setThreatLevel(policy.getThreatLevel());
    result.setPolicyViolationGrandfatheringAllowed(policy.isPolicyViolationGrandfatheringAllowed());
    result.setContent(JsonUtils.format(policy));
    result.setDroolsCode(policy.getDroolsCode());

    return result;
  }

  Policy toPolicy() {
    Policy result;
    try {
      result = JsonUtils.parse(content, Policy.class);
    }
    catch (IOException e) {
      throw new UncheckedIOException("Failed to parse policy content for policy '" + name + "' for owner id " + ownerId
          + ": " + e.getMessage(), e);
    }
    result.setId(id);
    result.setName(name);
    result.setOwnerId(ownerId);
    result.setThreatLevel(threatLevel);
    result.setPolicyViolationGrandfatheringAllowed(policyViolationGrandfatheringAllowed);
    result.setDroolsCode(droolsCode);

    return result;
  }

  static Policy toPolicy(PolicyInternal policyInternal) {
    if (policyInternal == null) {
      return null;
    }
    return policyInternal.toPolicy();
  }

  static List<Policy> toPolicies(List<PolicyInternal> policyInternals) {
    List<Policy> result = new ArrayList<>();
    for (PolicyInternal policyInternal : policyInternals) {
      result.add(toPolicy(policyInternal));
    }
    return result;
  }
}
