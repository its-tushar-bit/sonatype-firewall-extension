/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.Date;

import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.utils.ScopeOwnerUtils;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @since 1.76
 */
public class ApiPolicyWaiverDTO
{
  @JsonInclude(Include.NON_EMPTY)
  public String policyWaiverId;

  public String comment;

  @JsonInclude(Include.NON_EMPTY)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
  public Date createTime;

  @JsonInclude(Include.NON_NULL)
  public Boolean isObsolete;

  /**
   * @since 1.79
   */
  @JsonInclude(Include.NON_EMPTY)
  public String scopeOwnerType;

  /**
   * @since 1.79
   */
  @JsonInclude(Include.NON_EMPTY)
  public String scopeOwnerId;

  /**
   * @since 1.79
   */
  @JsonInclude(Include.NON_EMPTY)
  public String scopeOwnerName;

  /**
   * @since 1.92
   */
  @JsonInclude(Include.NON_NULL)
  public String hash;

  /**
   * @since 1.92
   */
  public String policyId;

  public static ApiPolicyWaiverDTO toDto(PolicyWaiver policyWaiver, Owner owner) {
    ApiPolicyWaiverDTO dto = new ApiPolicyWaiverDTO();

    dto.policyWaiverId = policyWaiver.getId();
    dto.comment = policyWaiver.getComment();
    dto.createTime = policyWaiver.getCreateTime();
    dto.hash = policyWaiver.getHash();
    dto.policyId = policyWaiver.getPolicyId();

    if (owner != null) {
      dto.scopeOwnerId = owner.getId();
      dto.scopeOwnerType = ScopeOwnerUtils.getScopeOwnerType(owner.getType(), owner.getId());
      dto.scopeOwnerName = owner.getName();
    }

    return dto;
  }
}
