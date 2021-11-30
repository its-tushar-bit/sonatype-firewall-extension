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
   * Returns a map of component identifier and remediation versions for a given set of component identifiers. The map
   * will contains entries only for the components for which a remediation version is found.
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
            null, null, null, null).getLeft();

    ApiComponentRemediationValueDTO remediationValueDto =
        componentRemediationService.getSuggestedRemediation(componentIdentifier, componentDetailsDTOs,
            OwnerType.APPLICATION, ownerId, null);

    if (remediationValueDto != null) {
      Optional<ApiVersionChangeOptionDTO> versionChangeDTO =
          getApplicableVersionChange(remediationValueDto.versionChanges);
      remediationVersionDTO = getRemediationVersionDTO(componentDetailsDTOs, versionChangeDTO);
    }
    return Optional.ofNullable(remediationVersionDTO);
  }

  private RemediationVersionDTO getRemediationVersionDTO(
      final List<ComponentDetailsDTO> componentDetailsDTOs,
      Optional<ApiVersionChangeOptionDTO> versionChangeDTO)
  {
    if (!versionChangeDTO.isPresent()) {
      return null;
    }

    RemediationVersionDTO remediationVersionDTO = null;
    ComponentIdentifier remediationComponentIdentifier = getRemediationComponentIdentifier(versionChangeDTO.get());

    if (productLicense.hasFeature(LicensedFeature.BREAKING_CHANGE)) {
      // Collect breaking changes info
      Optional<ComponentDetailsDTO> componentDetailsDTO = componentDetailsDTOs.stream()
          .filter(dto -> dto.componentIdentifier.compareTo(remediationComponentIdentifier) == 0)
          .findFirst();

      if (componentDetailsDTO.isPresent()) {
        remediationVersionDTO =
            new RemediationVersionDTO(remediationComponentIdentifier.getCoordinates().get(VERSION_KEY),
                versionChangeDTO.get().getType(), componentDetailsDTO.get().breakingChangesCount);
      }
    }
    else {
      remediationVersionDTO =
          new RemediationVersionDTO(remediationComponentIdentifier.getCoordinates().get(VERSION_KEY),
              versionChangeDTO.get().getType());
    }

    return remediationVersionDTO;
  }

  private ComponentIdentifier getRemediationComponentIdentifier(ApiVersionChangeOptionDTO versionChangeDTO) {
    ApiComponentIdentifierDTOV2 identifierDTOV2 =
        versionChangeDTO.getData().getComponent().componentIdentifier;
    return new ComponentIdentifier(identifierDTOV2.getFormat(), identifierDTOV2.getCoordinates());
  }

  private Optional<ApiVersionChangeOptionDTO> getApplicableVersionChange(
      List<ApiVersionChangeOptionDTO> versionChanges)
  {
    if (versionChanges.isEmpty()) {
      return Optional.empty();
    }

    Optional<ApiVersionChangeOptionDTO> versionChange = Optional.empty();
    versionChange = versionChanges.stream().filter(
        vChange -> vChange.getType() ==
            ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS_WITH_DEPENDENCIES).findFirst();

    if (!versionChange.isPresent()) {
      versionChange = versionChanges.stream().filter(
          vChange -> vChange.getType() ==
              ApiVersionChangeOptionType.NEXT_NO_VIOLATIONS).findFirst();
    }

    return versionChange;
  }
}
