/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.sonatype.insight.brain.api.v2.ApiApplicationAdapter;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationEvaluationCommitDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDiffDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationForDiffDTO;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.tag.ApplicationTagDAO;
import com.sonatype.insight.brain.git.IqForScmLicenseChecker;
import com.sonatype.insight.brain.landing.UserInterfaceLinksHelper;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.PolicyEvaluationDiffService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.82.0
 */
@Named
@Singleton
public class ApiReportViolationsDiffService
{
  private static final Logger log = LoggerFactory.getLogger(ApiReportViolationsDiffService.class);

  private static final String COMMIT_HASH_REGEX = "\\b[0-9a-f]{40}\\b";

  private static final String ABBREVIATED_COMMIT_HASH_REGEX = "\\b[0-9a-f]{4,39}\\b";

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ApplicationDAO applicationDAO;

  private final OwnerComponentDAO applicationComponentDAO;

  private final ApplicationTagDAO applicationTagDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final PolicyEvaluationDiffService policyEvaluationDiffService;

  private final IqForScmLicenseChecker licenseChecker;

  private final ReportService reportService;

  public static final String CANT_CALCULATE_DIFF_MESSAGE =
      "The policy violation diff could not be determined for the given request.";

  @Inject
  public ApiReportViolationsDiffService(
      final ApplicationDAO applicationDAO,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final OwnerComponentDAO applicationComponentDAO,
      final ApplicationTagDAO applicationTagDAO,
      final PolicyViolationDAO policyViolationDAO,
      final PolicyEvaluationDiffService policyEvaluationDiffService,
      final IqForScmLicenseChecker licenseChecker,
      final ReportService reportService)
  {
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.applicationComponentDAO = applicationComponentDAO;
    this.applicationTagDAO = applicationTagDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.policyEvaluationDiffService = policyEvaluationDiffService;
    this.licenseChecker = licenseChecker;
    this.reportService = reportService;
  }

  @Authorize(permission = Permission.READ)
  public ApiPolicyViolationDiffDTO getPolicyViolationDiff(
      @AuthzContext(Key.APPLICATION_PUBLIC_ID) final String applicationPublicId,
      final String fromCommit,
      final String toCommit,
      final String fromEvaluationId,
      final String toEvaluationId,
      final boolean includeViolationTimes)
  {
    checkLicense();
    validateInputs(fromCommit, toCommit, fromEvaluationId, toEvaluationId);

    final Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    final PolicyEvaluation fromPolicyEvaluation =
        getPolicyEvaluationForInput(application.getId(), fromCommit, fromEvaluationId);
    final PolicyEvaluation toPolicyEvaluation =
        getPolicyEvaluationForInput(application.getId(), toCommit, toEvaluationId);

    ApiPolicyViolationDiffDTO policyViolationDiffFromEvaluations =
        getPolicyViolationDiffFromEvaluations(application, fromPolicyEvaluation, toPolicyEvaluation);

    if (includeViolationTimes) {
      includeViolationTimes(policyViolationDiffFromEvaluations);
    }

    return policyViolationDiffFromEvaluations;
  }

  private ApiPolicyViolationDiffDTO getPolicyViolationDiffFromEvaluations(
      final Application application,
      final PolicyEvaluation fromPolicyEvaluation,
      final PolicyEvaluation toPolicyEvaluation)
  {
    final PolicyViolationDiff<PolicyViolation> policyViolationDiff =
        getViolationDiffFromEvaluations(fromPolicyEvaluation, toPolicyEvaluation);

    return buildPolicyViolationDiffDto(application, fromPolicyEvaluation, toPolicyEvaluation,
        policyViolationDiff);
  }

  private void validateInputs(
      final String fromCommit,
      final String toCommit,
      final String fromEvaluationId,
      final String toEvaluationId)
  {
    if (Strings.isNullOrEmpty(fromCommit) && Strings.isNullOrEmpty(fromEvaluationId)) {
      throw new BadRequestException(
          "The commit identifier or policy evaluation id for the `from` evaluation needs to be specified");
    }
    if (Strings.isNullOrEmpty(toCommit) && Strings.isNullOrEmpty(toEvaluationId)) {
      throw new BadRequestException(
          "The commit identifier or policy evaluation id for the `to` evaluation needs to be specified");
    }
    if ((toCommit != null && toCommit.equals(fromCommit)) ||
        (toEvaluationId != null && toEvaluationId.equals(fromEvaluationId)))
    {
      throw new BadRequestException("The specified commits or evaluation ids cannot be identical.");
    }
    if (!Strings.isNullOrEmpty(fromCommit) && !Strings.isNullOrEmpty(fromEvaluationId)) {
      throw new BadRequestException("Cannot specify both commit identifier and evaluation id for `from` evaluation.");
    }
    if (!Strings.isNullOrEmpty(toCommit) && !Strings.isNullOrEmpty(toEvaluationId)) {
      throw new BadRequestException("Cannot specify both commit identifier and evaluation id for `to` evaluation.");
    }
  }

  private PolicyEvaluation getPolicyEvaluationForInput(String ownerId, String commitHash, String evaluationId) {
    if (!Strings.isNullOrEmpty(commitHash)) {
      return getPolicyEvaluationForApplicationAndHash(ownerId, commitHash);
    }
    final PolicyEvaluation policyEvaluation = policyEvaluationDAO.getById(evaluationId);
    if (policyEvaluation == null || !policyEvaluation.getOwnerId().equals(ownerId)) {
      throw new NotFoundException(CANT_CALCULATE_DIFF_MESSAGE);
    }
    return policyEvaluation;
  }

  private PolicyEvaluation getPolicyEvaluationForApplicationAndHash(String ownerId, String commitHash) {

    PolicyEvaluation policyEvaluation;
    if (commitHash.matches(COMMIT_HASH_REGEX)) {
      policyEvaluation = policyEvaluationDAO.getLastByApplicationAndCommitHash(ownerId, commitHash);
    }
    else if (commitHash.matches(ABBREVIATED_COMMIT_HASH_REGEX)) {
      policyEvaluation = policyEvaluationDAO.getLastByApplicationAndAbbreviatedCommitHash(ownerId, commitHash);
    }
    else {
      throw new BadRequestException(
          String.format("The commit identifier `%s` supplied is not a valid commit hash", commitHash));
    }

    if (policyEvaluation == null) {
      throw new NotFoundException(CANT_CALCULATE_DIFF_MESSAGE);
    }
    return policyEvaluation;
  }

  private PolicyViolationDiff<PolicyViolation> getViolationDiffFromEvaluations(
      final PolicyEvaluation originalPolicyEvaluation,
      final PolicyEvaluation updatedPolicyEvaluation)
  {
    Optional<PolicyViolationDiff<PolicyViolation>> policyViolationDiff =
        policyEvaluationDiffService.createPolicyViolationDiff(originalPolicyEvaluation, updatedPolicyEvaluation);

    if (!policyViolationDiff.isPresent()) {
      throw new NotFoundException(CANT_CALCULATE_DIFF_MESSAGE);
    }

    return policyViolationDiff.get();
  }

  private ApiPolicyViolationDiffDTO buildPolicyViolationDiffDto(
      final Application application,
      final PolicyEvaluation fromPolicyEvaluation,
      final PolicyEvaluation toPolicyEvaluation,
      final PolicyViolationDiff<PolicyViolation> policyViolationDiff)
  {
    final ApiPolicyViolationDiffDTO dto = new ApiPolicyViolationDiffDTO();
    Map<String, String> toEvaluationComponentNames = null;
    Map<String, String> fromEvaluationComponentNames = null;
    try {
      toEvaluationComponentNames =
          getDisplayNamesMapFromBom(reportService.getBomForPolicyEvaluation(toPolicyEvaluation));
      fromEvaluationComponentNames =
          getDisplayNamesMapFromBom(reportService.getBomForPolicyEvaluation(fromPolicyEvaluation));
    }
    catch (IOException e) {
      log.error("Failed to retrieve required BOM files", e);
      throw new NotFoundException(CANT_CALCULATE_DIFF_MESSAGE);
    }

    Set<String> allHashes = Stream.of(
        policyViolationDiff.getAppeared().stream(),
        policyViolationDiff.getSame().values().stream(),
        policyViolationDiff.getCleared().stream())
        .flatMap(Function.identity())
        .map(PolicyViolation::getHash)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    Set<String> stageTypeIds = Stream.of(
        fromPolicyEvaluation.getStageTypeId(),
        toPolicyEvaluation.getStageTypeId())
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    Map<OwnerComponentDAO.OwnerComponentKey, OwnerComponent> componentsByKey =
        allHashes.isEmpty()
            ? Map.of()
            : applicationComponentDAO.getMapByOwnerIdsAndStageTypeIdsAndHashes(
                Set.of(application.getId()), stageTypeIds, allHashes);

    dto.addedViolations = buildPolicyViolationsDtos(policyViolationDiff.getAppeared(), application.getId(),
        toPolicyEvaluation.getStageTypeId(), toEvaluationComponentNames, componentsByKey);
    dto.sameViolations = buildPolicyViolationsDtos(policyViolationDiff.getSame().values(), application.getId(),
        toPolicyEvaluation.getStageTypeId(), toEvaluationComponentNames, componentsByKey);
    dto.removedViolations = buildPolicyViolationsDtos(policyViolationDiff.getCleared(), application.getId(),
        fromPolicyEvaluation.getStageTypeId(), fromEvaluationComponentNames, componentsByKey);
    dto.fromCommit = buildEvaluationCommit(application.getPublicId(), fromPolicyEvaluation);
    dto.toCommit = buildEvaluationCommit(application.getPublicId(), toPolicyEvaluation);
    dto.application =
        ApiApplicationAdapter.convertToDTO(application, applicationTagDAO.getByApplicationId(application.getId()));
    dto.diffTime = new Date();
    return dto;
  }

  private Set<ApiPolicyViolationForDiffDTO> buildPolicyViolationsDtos(
      final Collection<PolicyViolation> policyViolations,
      final String ownerId,
      final String stageTypeId,
      final Map<String, String> componentNamesMap,
      final Map<OwnerComponentDAO.OwnerComponentKey, OwnerComponent> componentsByKey)
  {
    final Set<ApiPolicyViolationForDiffDTO> set = new HashSet<>(policyViolations.size());
    policyViolations.forEach(violation -> {
      if (violation.isActive()) {
        set.add(buildDiffPolicyViolationDTO(ownerId, stageTypeId, violation, componentNamesMap, componentsByKey));
      }
    });
    return set;
  }

  private ApiPolicyViolationForDiffDTO buildDiffPolicyViolationDTO(
      String ownerId,
      String stageTypeId,
      PolicyViolation policyViolation,
      final Map<String, String> componentNamesMap,
      final Map<OwnerComponentDAO.OwnerComponentKey, OwnerComponent> componentsByKey)
  {
    final ApiPolicyViolationForDiffDTO apiPolicyViolationForDiffDTO = new ApiPolicyViolationForDiffDTO();
    apiPolicyViolationForDiffDTO.policyId = policyViolation.getPolicyId();
    apiPolicyViolationForDiffDTO.policyName = policyViolation.getPolicyName();
    apiPolicyViolationForDiffDTO.policyViolationId = policyViolation.getId();
    apiPolicyViolationForDiffDTO.threatLevel = policyViolation.getThreatLevel();
    final OwnerComponent applicationComponent = componentsByKey.get(
        new OwnerComponentDAO.OwnerComponentKey(ownerId, stageTypeId, policyViolation.getHash()));
    apiPolicyViolationForDiffDTO.component = new ApiComponentDTOV2();
    apiPolicyViolationForDiffDTO.component.hash = policyViolation.getHash();
    apiPolicyViolationForDiffDTO.component.proprietary = applicationComponent != null
        && applicationComponent.isProprietary();
    apiPolicyViolationForDiffDTO.component.componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(policyViolation.getComponentIdentifier());
    apiPolicyViolationForDiffDTO.component.packageUrl =
        PackageUrlIdentifier.toPackageUrl(policyViolation.getComponentIdentifier());
    apiPolicyViolationForDiffDTO.component.displayName = componentNamesMap.get(policyViolation.getHash());
    apiPolicyViolationForDiffDTO.constraintViolations = PolicyViolationAdapter.convert(policyViolation);

    return apiPolicyViolationForDiffDTO;
  }

  private ApiApplicationEvaluationCommitDTO buildEvaluationCommit(
      String applicationPublicId,
      PolicyEvaluation policyEvaluation)
  {
    final ApiApplicationEvaluationCommitDTO apiApplicationEvaluationCommitDTO = new ApiApplicationEvaluationCommitDTO();
    apiApplicationEvaluationCommitDTO.commitHash = policyEvaluation.getCommitHash();
    apiApplicationEvaluationCommitDTO.reportUrl =
        UserInterfaceLinksHelper.getReportUrl(applicationPublicId, policyEvaluation.getScanId());
    apiApplicationEvaluationCommitDTO.scanId = policyEvaluation.getScanId();
    apiApplicationEvaluationCommitDTO.scanTime = policyEvaluation.getTime();
    return apiApplicationEvaluationCommitDTO;
  }

  private void checkLicense() {
    if (!licenseChecker.isPullRequestCommentingSupported()) {
      log.debug("License does not support source control automation features");
      throw new InvalidLicenseException();
    }
  }

  private Map<String, String> getDisplayNamesMapFromBom(final ReportEntry bomReportEntry) throws IOException {
    final Map<String, String> componentDisplayNamesMap = new HashMap<>();
    JsonNode bomJson = JsonUtils.parse(bomReportEntry.buf);
    if (bomJson != null) {
      bomJson = bomJson.get("aaData");
      if (bomJson != null) {
        final ArrayNode bomJsonArray = (ArrayNode) bomJson;
        bomJsonArray.forEach(jsonNode -> {
          final String hash = JsonUtils.getNullableString(jsonNode.get("hash"));
          componentDisplayNamesMap.put(hash, ComponentDisplayNameUtil.fromJsonNode((ObjectNode) jsonNode).toString());
        });
      }
    }
    return componentDisplayNamesMap;
  }

  private void includeViolationTimes(final ApiPolicyViolationDiffDTO apiPolicyViolationDiffDTO) {
    Set<ApiPolicyViolationForDiffDTO> policyViolations = Stream.concat(
        apiPolicyViolationDiffDTO.addedViolations.stream(),
        Stream.concat(
            apiPolicyViolationDiffDTO.sameViolations.stream(),
            apiPolicyViolationDiffDTO.removedViolations.stream()))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    Set<String> policyViolationIds = policyViolations.stream()
        .map(violation -> violation.policyViolationId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    Map<String, PolicyViolation> policyViolationById =
        policyViolationDAO.getByIds(policyViolationIds)
            .stream()
            .collect(Collectors.toMap(PolicyViolation::getId, Function.identity()));

    for (ApiPolicyViolationForDiffDTO policyViolation : policyViolations) {
      PolicyViolation policyViolationFromDb = policyViolationById.get(policyViolation.policyViolationId);
      if (policyViolationFromDb != null) {
        policyViolation.openTime = policyViolationFromDb.getOpenTime();
        policyViolation.waiveTime = policyViolationFromDb.getWaiveTime();
        policyViolation.fixTime = policyViolationFromDb.getFixTime();
        policyViolation.legacyViolationTime = policyViolationFromDb.getLegacyViolationTime();
      }
    }
  }
}
