/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.lang3.tuple.Pair;

public class ApiComponentTransitivePolicyViolationsDTO
{
  public ApiComponentIdentifierDTOV2 componentIdentifier;

  public String packageUrl;

  public String hash;

  public String displayName;

  public boolean isInnerSource;

  public List<ApiStagePolicyViolationComponentDTO> transitivePolicyViolations;

  public ApiComponentTransitivePolicyViolationsDTO() {
    // for Jackson
  }

  public ApiComponentTransitivePolicyViolationsDTO(
      Component component,
      List<Pair<PolicyViolation, Component>> transitivePolicyViolations)
  {
    this.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(component.getComponentIdentifier());
    this.packageUrl = PackageUrlIdentifier.toPackageUrl(component.getComponentIdentifier());
    this.hash = component.getHash();
    this.displayName = component.getDisplayName();
    this.isInnerSource = component.getInnerSource() != null && component.getInnerSource();
    this.transitivePolicyViolations = transitivePolicyViolations.stream()
        .map(ApiStagePolicyViolationComponentDTO::fromPolicyViolationAndComponent)
        .collect(Collectors.toList());
  }
}
