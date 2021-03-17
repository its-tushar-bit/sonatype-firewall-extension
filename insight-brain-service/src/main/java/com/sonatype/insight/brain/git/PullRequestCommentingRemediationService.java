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
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.remediation.ApiComponentRemediationValueDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionDTO;
import com.sonatype.insight.brain.api.v2.dto.remediation.options.ApiVersionChangeOptionType;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.hds.ComponentDetailsDTO;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.hds.ComponentRemediationService;
import com.sonatype.insight.brain.model.HasComponentId;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.license.model.LicensedFeature;

@Named
@Singleton
public class PullRequestCommentingRemediationService
{
  private static final String VERSION_KEY = "version";

  private final ApplicationDAO applicationDAO;

  private final ComponentInfoService componentInfoService;

  private final ComponentRemediationService componentRemediationService;

  private final ProductLicense productLicense;

  @Inject
  public PullRequestCommentingRemediationService(
      final ApplicationDAO applicationDAO,
      final ComponentInfoService componentInfoService,
      final ComponentRemediationService componentRemediationService,
      final ProductLicense productLicense)
  {
    this.applicationDAO = applicationDAO;
    this.componentInfoService = componentInfoService;
    componentInfoService.setToolName("ci");
    this.componentRemediationService = componentRemediationService;
    this.productLicense = productLicense;
  }

  /**
   * Returns a map of component identifier and remediation versions for a given set of component identifiers.
   * The map will contains entries only for the components for which a remediation version is found.
   */
  public SortedMap<ComponentIdentifier, RemediationVersionDTO> getRemediationVersionMap(
      final List<PolicyViolation> policyViolations,
      final String ownerId)
  {
    SortedMap<ComponentIdentifier, RemediationVersionDTO> remediationVersionMap = new TreeMap<>();
    if (policyViolations != null && !policyViolations.isEmpty()) {
      Set<ComponentIdentifier> componentIdentifiers = policyViolations.stream()
          .filter(pv -> pv.getComponentIdentifier() != null)
          .map(HasComponentId::getComponentIdentifier)
          .collect(Collectors.toSet());

      for (ComponentIdentifier componentIdentifier : componentIdentifiers) {
        Optional<RemediationVersionDTO> suggestedVersion = getRemediationVersion(componentIdentifier, ownerId);
        suggestedVersion.ifPresent(s -> remediationVersionMap.put(componentIdentifier, s));
      }
    }
    return remediationVersionMap;
  }

  /**
   * Gets remediation versions, if any, for a given component identifier
   */
  public Optional<RemediationVersionDTO> getRemediationVersion(
      final ComponentIdentifier componentIdentifier,
      final String ownerId)
  {
    RemediationVersionDTO remediationVersionDTO = null;

    String publicOwnerId = applicationDAO.getByIdNotNull(ownerId).getPublicId();

    List<ComponentDetailsDTO> componentDetailsDTOs = componentInfoService
        .getComponentDetailsForAllVersionsNoAuth(OwnerType.APPLICATION, publicOwnerId, componentIdentifier,
            null, null, null, null);

    ApiComponentRemediationValueDTO remediationValueDto =
        componentRemediationService.getSuggestedRemediation(componentIdentifier, componentDetailsDTOs,
            OwnerType.APPLICATION, ownerId, null);

    if (remediationValueDto != null) {
      ComponentIdentifier remediationComponentIdentifier = getRemediationComponentIdentifier(remediationValueDto);
      remediationVersionDTO = getRemediationVersionDTO(componentDetailsDTOs, remediationComponentIdentifier);
    }
    return Optional.ofNullable(remediationVersionDTO);
  }

  private RemediationVersionDTO getRemediationVersionDTO(
      final List<ComponentDetailsDTO> componentDetailsDTOs,
      final ComponentIdentifier remediationComponentIdentifier)
  {
    if (remediationComponentIdentifier != null) {
      if (productLicense.hasFeature(LicensedFeature.BREAKING_CHANGE)) {
        // Collect breaking changes info
        Optional<ComponentDetailsDTO> componentDetailsDTO = componentDetailsDTOs.stream()
            .filter(dto -> dto.componentIdentifier.compareTo(remediationComponentIdentifier) == 0)
            .findFirst();

        if (componentDetailsDTO.isPresent()) {
          return
              new RemediationVersionDTO(remediationComponentIdentifier.getCoordinates().get(VERSION_KEY),
                  componentDetailsDTO.get().breakingChangesCount);
        }
      }
      return new RemediationVersionDTO(remediationComponentIdentifier.getCoordinates().get(VERSION_KEY));
    }
    return null;
  }

  private ComponentIdentifier getRemediationComponentIdentifier(ApiComponentRemediationValueDTO remediationValueDto) {
    ComponentIdentifier remediationComponentIdentifier = null;

    if (remediationValueDto != null) {
      List<ApiVersionChangeOptionDTO> versionChanges = remediationValueDto.versionChanges;
      if (!versionChanges.isEmpty()) {
        for (ApiVersionChangeOptionDTO versionChange : versionChanges) {
          if (versionChange.getType() == ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS) {
            ApiComponentIdentifierDTOV2 identifierDTOV2 =
                versionChange.getData().getComponent().componentIdentifier;
            remediationComponentIdentifier =
                new ComponentIdentifier(identifierDTOV2.getFormat(), identifierDTOV2.getCoordinates());
            break;
          }
        }
      }
    }
    return remediationComponentIdentifier;
  }
}
