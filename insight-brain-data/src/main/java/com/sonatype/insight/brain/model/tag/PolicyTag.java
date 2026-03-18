/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.tag;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.sonatype.insight.model.HasStringId;

/**
 * Represents a tag that has been applied to a policy (many-to-many association between policies and tags).
 *
 * @since 1.9
 */
@Entity
@Table(name = "policy_tag")
public class PolicyTag
    implements HasStringId
{
  @Id
  @Column(name = "policy_tag_id")
  private String id;

  @Column(name = "policy_id")
  private String policyId;

  @Column(name = "tag_id")
  private String tagId;

  public PolicyTag() {
  }

  public PolicyTag(String policyId, String tagId) {
    this.policyId = policyId;
    this.tagId = tagId;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  public String getPolicyId() {
    return policyId;
  }

  public void setPolicyId(String policyId) {
    this.policyId = policyId;
  }

  public String getTagId() {
    return tagId;
  }

  public void setTagId(String tagId) {
    this.tagId = tagId;
  }
}
