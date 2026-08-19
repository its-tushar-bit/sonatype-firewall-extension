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

import com.sonatype.insight.brain.model.Nameable;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

import jakarta.persistence.Column;

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
public class PolicyInternal
    extends Nameable
    implements HasStringId
{
  @Column(name = "policy_id")
  private String id;

  @Column(name = "owner_id")
  private String ownerId;

  @Column(name = "threat_level")
  private int threatLevel = 5;

  @Column(name = "legacy_violation_allowed")
  private boolean legacyViolationAllowed;

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

  public boolean isLegacyViolationAllowed() {
    return legacyViolationAllowed;
  }

  public void setLegacyViolationAllowed(boolean legacyViolationAllowed) {
    this.legacyViolationAllowed = legacyViolationAllowed;
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
    result.setLegacyViolationAllowed(policy.isLegacyViolationAllowed());
    result.setContent(toJson(policy));
    result.setDroolsCode(policy.getDroolsCode());

    return result;
  }

  Policy toPolicy() {
    Policy result = fromJson(content, name, ownerId);
    result.setId(id);
    result.setName(name);
    result.setOwnerId(ownerId);
    result.setThreatLevel(threatLevel);
    result.setLegacyViolationAllowed(legacyViolationAllowed);
    result.setDroolsCode(droolsCode);

    return result;
  }

  public static Policy fromJson(String policyJson, String policyName, String policyOwnerid) {
    try {
      return JsonUtils.parse(policyJson, Policy.class);
    }
    catch (IOException e) {
      throw new UncheckedIOException("Failed to parse policy content for policy '" + policyName + "' for owner id "
          + policyOwnerid + ": " + e.getMessage(), e);
    }
  }

  public static String toJson(Policy policy) {
    return JsonUtils.format(policy);
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
