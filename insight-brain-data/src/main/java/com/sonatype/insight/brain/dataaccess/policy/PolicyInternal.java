/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.policy;

import java.io.IOException;
import java.nio.charset.Charset;
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
class PolicyInternal
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

  @Column(name = "content")
  private String content;

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

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  static PolicyInternal fromPolicy(Policy policy) {
    if (policy == null) {
      return null;
    }

    PolicyInternal result = new PolicyInternal();
    result.setId(policy.getId());
    result.setName(policy.getName());
    result.setOwnerId(policy.getOwnerId());
    result.setThreatLevel(policy.getThreatLevel());
    try {
      result.setContent(new String(JsonUtils.generate(policy), Charset.forName("UTF-8")));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    return result;
  }

  Policy toPolicy() {
    Policy result;
    try {
      result = JsonUtils.parse(content, Policy.class);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    result.setId(id);
    result.setName(name);
    result.setOwnerId(ownerId);
    result.setThreatLevel(threatLevel);

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
