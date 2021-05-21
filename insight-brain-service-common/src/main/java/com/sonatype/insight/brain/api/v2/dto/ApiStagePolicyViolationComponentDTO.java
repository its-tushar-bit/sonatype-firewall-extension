/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.commons.lang3.tuple.Pair;

@JsonInclude(Include.NON_NULL)
public class ApiStagePolicyViolationComponentDTO
{
  public String policyId;

  public String policyName;

  public int threatLevel;

  public String threatCategory;

  public String policyViolationId;

  // Action is for a specific stage
  public String action;

  public ApiComponentIdentifierDTOV2 componentIdentifier;

  public String packageUrl;

  public String hash;

  public String displayName;

  public static ApiStagePolicyViolationComponentDTO fromPolicyViolationAndComponent(
      Pair<PolicyViolation, Component> policyViolationAndComponent)
  {
    ApiStagePolicyViolationComponentDTO result = new ApiStagePolicyViolationComponentDTO();
    result.policyId = policyViolationAndComponent.getLeft().getPolicyId();
    result.policyName = policyViolationAndComponent.getLeft().getPolicyName();
    result.threatLevel = policyViolationAndComponent.getLeft().getThreatLevel();
    result.threatCategory = policyViolationAndComponent.getLeft().getThreatCategory().getName();
    result.policyViolationId = policyViolationAndComponent.getLeft().getId();
    result.action = policyViolationAndComponent.getLeft().getActionTypeId();
    ComponentIdentifier componentIdentifier = policyViolationAndComponent.getLeft().getComponentIdentifier();
    if (componentIdentifier != null) {
      result.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
      result.packageUrl = PackageUrlIdentifier.toPackageUrl(componentIdentifier);
    }
    result.hash = policyViolationAndComponent.getRight().getHash();
    result.displayName = policyViolationAndComponent.getRight().getDisplayName();
    return result;
  }
}
