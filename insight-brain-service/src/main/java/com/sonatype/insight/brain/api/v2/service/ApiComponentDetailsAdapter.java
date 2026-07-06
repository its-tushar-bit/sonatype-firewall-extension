/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationData;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList.RepositoryComponentEvaluationDataRequest;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.PolicyFact;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDetailsDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentPolicyViolationListDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiConstraintViolationReasonDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationRequestList;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationRequestList.ApiRepositoryComponentEvaluationRequest;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationResultList;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryComponentEvaluationResultList.ApiRepositoryComponentEvaluationResult;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.model.HashHelper;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.utils.RepositoryPathnameSerializer;
import com.sonatype.insight.purl.PackageUrlIdentifier;

/**
 * @since 1.13.0
 */
@Named
public class ApiComponentDetailsAdapter
{
  private final ApiLicenseDataAdapter licenseDataAdapter;

  private final ApiSecurityDataAdapter securityDataAdapter;

  private final ApiComponentProjectDetailsAdapter componentProjectDetailsAdapter;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  @Inject
  public ApiComponentDetailsAdapter(
      final ApiLicenseDataAdapter licenseDataAdapter,
      final ApiSecurityDataAdapter securityDataAdapter,
      final ApiComponentProjectDetailsAdapter componentProjectDetailsAdapter,
      final RepositoryComponentDAO repositoryComponentDAO,
      final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO)
  {
    this.licenseDataAdapter = licenseDataAdapter;
    this.securityDataAdapter = securityDataAdapter;
    this.componentProjectDetailsAdapter = componentProjectDetailsAdapter;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
  }

  public ApiComponentDetailsDTOV2 convertToDTO(final Component component, final Collection<PolicyAlert> policyAlerts) {
    ApiComponentDetailsDTOV2 componentDetailsDTO = new ApiComponentDetailsDTOV2();
    componentDetailsDTO.component = new ApiComponentDTOV2();
    componentDetailsDTO.component.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(component
        .getComponentIdentifier());
    componentDetailsDTO.component.hash = component.getHash();
    componentDetailsDTO.component.packageUrl = PackageUrlIdentifier.toPackageUrl(component.getComponentIdentifier());

    ComponentDisplayName componentDisplayName =
        ComponentDisplayNameUtil.fromIdentifier(component.getComponentIdentifier());
    componentDetailsDTO.component.displayName = componentDisplayName != null ? componentDisplayName.toString() : null;

    componentDetailsDTO.component.proprietary = component.isProprietary();
    componentDetailsDTO.matchState = component.getMatchState() == null
        ? MatchState.UNKNOWN.getId()
        : component
            .getMatchState()
            .getId();

    if (component.getCatalogDate() != null) {
      componentDetailsDTO.catalogDate = new Date(component.getCatalogDate());
    }
    componentDetailsDTO.relativePopularity = component.getRelativePopularity();

    componentDetailsDTO.licenseData = licenseDataAdapter.convertToDTO(component);
    componentDetailsDTO.securityData = securityDataAdapter.convertToDTO(component);

    componentDetailsDTO.policyData = new ApiComponentPolicyViolationListDTOV2();
    for (PolicyAlert policyAlert : policyAlerts) {
      componentDetailsDTO.policyData.policyViolations.add(convert(policyAlert));
    }
    return componentDetailsDTO;
  }

  public ApiComponentDetailsDTOV2 convertToDTO(ComponentEvaluationData componentDetailsFromHds) {
    ApiComponentDetailsDTOV2 componentDetailsDTO = new ApiComponentDetailsDTOV2();
    componentDetailsDTO.component = new ApiComponentDTOV2();
    componentDetailsDTO.component.componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(componentDetailsFromHds.componentIdentifier);
    componentDetailsDTO.component.hash = componentDetailsFromHds.hash;
    componentDetailsDTO.component.packageUrl =
        PackageUrlIdentifier.toPackageUrl(componentDetailsFromHds.componentIdentifier);

    ComponentDisplayName componentDisplayName =
        ComponentDisplayNameUtil.fromIdentifier(componentDetailsFromHds.componentIdentifier);
    componentDetailsDTO.component.displayName = componentDisplayName != null ? componentDisplayName.toString() : null;

    if (componentDetailsFromHds.matchState == null) {
      componentDetailsDTO.matchState = MatchState.UNKNOWN.getId();
    }
    else {
      // The matchState (which is an id) from HDS may be unknown to the CLM server and we don't want the CLM server to
      // fail in this case.
      MatchState matchState = MatchState.getById(componentDetailsFromHds.matchState);
      componentDetailsDTO.matchState = matchState == null ? MatchState.UNKNOWN.getId() : matchState.getId();
    }

    if (componentDetailsFromHds.catalogDate != null) {
      componentDetailsDTO.catalogDate = new Date(componentDetailsFromHds.catalogDate);
    }
    componentDetailsDTO.relativePopularity = componentDetailsFromHds.relativePopularity;

    if (componentDetailsFromHds.integrityRating != null) {
      componentDetailsDTO.integrityRating = componentDetailsFromHds.integrityRating.getLabel();
    }

    if (componentDetailsFromHds.hygieneRating != null) {
      componentDetailsDTO.hygieneRating = componentDetailsFromHds.hygieneRating.getLabel();
    }

    componentDetailsDTO.licenseData = licenseDataAdapter.convertToDTO(componentDetailsFromHds);
    componentDetailsDTO.securityData = securityDataAdapter.convertToDTO(componentDetailsFromHds);
    componentDetailsDTO.projectData = componentProjectDetailsAdapter.convertToDTO(componentDetailsFromHds);

    return componentDetailsDTO;
  }

  public RepositoryComponentEvaluationDataRequestList convertFromDTO(
      ApiRepositoryComponentEvaluationRequestList apiRepositoryComponentEvaluationRequestList)
  {
    RepositoryComponentEvaluationDataRequestList requestList = new RepositoryComponentEvaluationDataRequestList();

    for (ApiRepositoryComponentEvaluationRequest componentRequest : apiRepositoryComponentEvaluationRequestList.components) {
      String pathname = componentRequest.pathname;
      String hash = componentRequest.hash;

      if (pathname == null && componentRequest.packageUrl != null) {
        pathname = RepositoryPathnameSerializer.toPathname(componentRequest.packageUrl);

        // For coordinate-based formats (golang, conan, cargo, etc.), generate a synthetic hash when not provided.
        // Coordinate-based formats identify components by coordinates (name+version) rather than file hash.
        // The synthetic hash satisfies HDS validation while the actual lookup is done by coordinates (NEXUS-49174).
        // For hash-based formats (maven, npm, pypi, etc.), hash must be provided by the caller.
        if (hash == null &&
            ComponentFormatConstants.isCoordinateBasedFormat(apiRepositoryComponentEvaluationRequestList.format))
        {
          hash = generateSyntheticHash(componentRequest.packageUrl);
        }
      }

      requestList.components.add(
          new RepositoryComponentEvaluationDataRequest(apiRepositoryComponentEvaluationRequestList.format, pathname,
              HashHelper.truncateHash(hash)));
    }
    return requestList;
  }

  /**
   * Generates a synthetic SHA1 hash for coordinate-based formats when packageUrl is provided without hash.
   *
   * <p>
   * Coordinate-based formats (golang, conan, cargo, cocoapods, cran, conda, composer, hf-model) identify
   * components by their coordinates (name + version) rather than file content hash. Unlike hash-based formats
   * (maven, npm, pypi), these formats do not have a single canonical file to hash.
   *
   * <p>
   * This synthetic hash:
   * <ul>
   * <li>Satisfies HDS validation requirements that expect a hash field</li>
   * <li>Is generated deterministically from the packageUrl for consistency</li>
   * <li>Is not used for component lookup (coordinates are used instead)</li>
   * <li>Allows the API to make hash optional for coordinate-based formats</li>
   * </ul>
   *
   * <p>
   * <b>Edge Case - Hash Collisions:</b>
   * <br>
   * While theoretically possible, synthetic hash collisions with real component hashes are extremely unlikely
   * due to SHA-1's cryptographic properties. Even if a collision occurred, it would not impact correctness because:
   * <ul>
   * <li>Coordinate-based formats use coordinates (name+version) for component lookup, not hash</li>
   * <li>Hash-based formats always require actual file hashes (synthetic hashes are never generated for them)</li>
   * <li>The synthetic hash is only used to satisfy HDS validation, not for component identification</li>
   * </ul>
   *
   * @param packageUrl the package URL (e.g., pkg:golang/github.com/gin-gonic/gin@v1.7.4)
   * @return a synthetic SHA1 hash derived from the packageUrl
   * @see <a href="https://sonatype.atlassian.net/browse/NEXUS-49174">NEXUS-49174</a>
   */
  private String generateSyntheticHash(String packageUrl) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      byte[] hashBytes = digest.digest(packageUrl.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder(40); // SHA-1 produces 40 hex characters
      for (byte b : hashBytes) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    }
    catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-1 algorithm not available", e);
    }
  }

  public ApiRepositoryComponentEvaluationResultList convertToDTO(
      RepositoryManager repositoryManager,
      Repository repository,
      ApiRepositoryComponentEvaluationRequestList apiRepositoryComponentEvaluationRequestList,
      RepositoryComponentEvaluationDataList repositoryComponentEvaluationDataList)
  {
    ApiRepositoryComponentEvaluationResultList resultDTO = new ApiRepositoryComponentEvaluationResultList();
    resultDTO.repositoryManagerId = repositoryManager.getId();
    resultDTO.repositoryId = repository.getId();
    resultDTO.repositoryPublicId = repository.getPublicId();
    resultDTO.repositoryType = repository.getRepositoryType().name();

    List<RepositoryComponentEvaluationData> evalResults =
        repositoryComponentEvaluationDataList.componentEvalResults;

    // Derive pathname per eval entry, preserving alignment with evalResults.
    List<String> pathnamesInOrder = new ArrayList<>(evalResults.size());
    for (RepositoryComponentEvaluationData d : evalResults) {
      pathnamesInOrder.add(resolvePathname(
          apiRepositoryComponentEvaluationRequestList.components.get(d.requestIndex)));
    }

    // Distinct non-null pathnames feed the IN-clause batch reads.
    List<String> distinctPathnames = pathnamesInOrder.stream()
        .filter(Objects::nonNull)
        .distinct()
        .collect(Collectors.toList());

    Map<String, RepositoryComponent> componentByPathname = distinctPathnames.isEmpty()
        ? Collections.emptyMap()
        : repositoryComponentDAO.getByRepositoryIdAndPathnames(repository.getId(), distinctPathnames)
            .stream()
            .collect(Collectors.toMap(RepositoryComponent::getPathname, Function.identity(), (a, b) -> a));

    List<RepositoryPolicyViolation> allViolations = distinctPathnames.isEmpty()
        ? Collections.emptyList()
        : repositoryPolicyViolationDAO.getActiveByRepositoryIdAndPathnames(repository.getId(), distinctPathnames);

    Map<String, List<RepositoryPolicyViolation>> violationsByPathname = allViolations.stream()
        .collect(Collectors.groupingBy(RepositoryPolicyViolation::getPathname));

    if (!allViolations.isEmpty()) {
      repositoryPolicyViolationDAO.loadConstraintFacts(allViolations);
    }

    for (int i = 0; i < evalResults.size(); i++) {
      RepositoryComponentEvaluationData repositoryComponentEvaluationData = evalResults.get(i);
      ApiRepositoryComponentEvaluationResult componentEvaluationResult =
          new ApiRepositoryComponentEvaluationResult();
      ApiRepositoryComponentEvaluationRequest apiRepositoryComponentEvaluationRequest =
          apiRepositoryComponentEvaluationRequestList.components.get(repositoryComponentEvaluationData.requestIndex);

      componentEvaluationResult.quarantined = repositoryComponentEvaluationData.quarantine;
      String pathname = pathnamesInOrder.get(i);
      RepositoryComponent repositoryComponent = pathname == null ? null : componentByPathname.get(pathname);
      if (repositoryComponent != null) {
        componentEvaluationResult.quarantineDate = repositoryComponent.getQuarantineTime();
      }
      componentEvaluationResult.component = apiRepositoryComponentEvaluationRequest;
      componentEvaluationResult.catalogDate = repositoryComponentEvaluationData.catalogDate;
      List<RepositoryPolicyViolation> repositoryPolicyViolations = pathname == null
          ? Collections.emptyList()
          : violationsByPathname.getOrDefault(pathname, Collections.emptyList());
      componentEvaluationResult.policyViolations.addAll(
          repositoryPolicyViolations.stream().map(this::convert).collect(Collectors.toList()));
      resultDTO.results.add(componentEvaluationResult);
    }
    return resultDTO;
  }

  private String resolvePathname(ApiRepositoryComponentEvaluationRequest request) {
    if (request.pathname != null) {
      return request.pathname;
    }
    if (request.packageUrl != null) {
      return RepositoryPathnameSerializer.toPathname(request.packageUrl);
    }
    return null;
  }

  private ApiPolicyViolationDTOV2 convert(RepositoryPolicyViolation repositoryPolicyViolation) {
    ApiPolicyViolationDTOV2 dto = new ApiPolicyViolationDTOV2();
    dto.policyId = repositoryPolicyViolation.getPolicyId();
    dto.policyName = repositoryPolicyViolation.getPolicyName();
    dto.policyViolationId = repositoryPolicyViolation.getId();
    dto.openTime = repositoryPolicyViolation.getOpenTime();
    dto.waiveTime = repositoryPolicyViolation.getWaiveTime();
    dto.threatLevel = repositoryPolicyViolation.getThreatLevel();
    for (ConstraintFact constraintFact : repositoryPolicyViolation.getConstraintFacts()) {
      dto.constraintViolations.add(convert(constraintFact));
    }
    return dto;
  }

  private ApiPolicyViolationDTOV2 convert(final PolicyAlert policyAlert) {
    ApiPolicyViolationDTOV2 componentPolicyViolationDTO = new ApiPolicyViolationDTOV2();
    PolicyFact policyFact = policyAlert.getTrigger();
    componentPolicyViolationDTO.policyId = policyFact.getPolicyId();
    componentPolicyViolationDTO.policyName = policyFact.getPolicyName();
    componentPolicyViolationDTO.threatLevel = policyFact.getThreatLevel();

    for (ComponentFact componentFact : policyFact.getComponentFacts()) {
      for (ConstraintFact constraintFact : componentFact.getConstraintFacts()) {
        componentPolicyViolationDTO.constraintViolations.add(convert(constraintFact));
      }
    }
    return componentPolicyViolationDTO;
  }

  private ApiConstraintViolationDTO convert(final ConstraintFact constraintFact) {
    ApiConstraintViolationDTO constraintViolationDTO = new ApiConstraintViolationDTO();
    constraintViolationDTO.constraintId = constraintFact.getConstraintId();
    constraintViolationDTO.constraintName = constraintFact.getConstraintName();
    for (ConditionFact conditionFact : constraintFact.getConditionFacts()) {
      ApiConstraintViolationReasonDTO constraintViolationReasonDTO = new ApiConstraintViolationReasonDTO();
      constraintViolationReasonDTO.reason = conditionFact.getReason();
      constraintViolationDTO.reasons.add(constraintViolationReasonDTO);
    }
    return constraintViolationDTO;
  }
}
