/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.api.v2.service.ApiComponentRemediationService;
import com.sonatype.insight.brain.model.HasComponentId;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyViolation;

@Named
@Singleton
public class PullRequestCommentingRemediationService
{
  private static final String VERSION_KEY = "version";
  
  private final ApiComponentRemediationService remediationService;
  
  @Inject
  public PullRequestCommentingRemediationService(final ApiComponentRemediationService remediationService) {
    this.remediationService = remediationService;
  }
  
  /**
   * Returns a map of component identifier and remediation versions for a given set of component identifiers.
   * The map will contains entries only for the components for which a remediation version is found.
   */
  public SortedMap<ComponentIdentifier, String> getRemediationVersionMap(
      final List<PolicyViolation> policyViolations,
      final String ownerId)
  {
    SortedMap<ComponentIdentifier, String> remediationVersionMap = new TreeMap<>();
    if (policyViolations != null && !policyViolations.isEmpty()) {
      Set<ComponentIdentifier> componentIdentifiers = policyViolations.stream()
          .filter(pv -> pv.getComponentIdentifier() != null)
          .map(HasComponentId::getComponentIdentifier)
          .collect(Collectors.toSet());

      for (ComponentIdentifier componentIdentifier : componentIdentifiers) {
        Optional<String> suggestedVersion = getRemediationVersion(componentIdentifier, ownerId);
        suggestedVersion.ifPresent(s -> remediationVersionMap.put(componentIdentifier, s));
      }
    }
    return remediationVersionMap;
  }

  /**
   * Gets remediation versions, if any, for a given component identifier
   */
  private Optional<String> getRemediationVersion(final ComponentIdentifier componentIdentifier, final String ownerId) {
    String nextVersion = null;
    final ApiComponentDTOV2 componentDto = new ApiComponentDTOV2();
    componentDto.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    ApiComponentRemediationDTO remediationDTO = remediationService.getSuggestedRemediationForComponentNoAuth(
        componentDto, OwnerType.APPLICATION, ownerId, null, null, null);
    if (remediationDTO != null) {
      List<ApiVersionChangeOptionDTO> versionChanges = remediationDTO.remediation.versionChanges;
      if (!versionChanges.isEmpty()) {
        for (ApiVersionChangeOptionDTO versionChange : versionChanges) {
          if (versionChange.getType() == ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS) {
            nextVersion =
                versionChange.getData().getComponent().componentIdentifier.getCoordinates().get(VERSION_KEY);
            break;
          }
        }
      }
    }
    return Optional.ofNullable(nextVersion);
  }
}
