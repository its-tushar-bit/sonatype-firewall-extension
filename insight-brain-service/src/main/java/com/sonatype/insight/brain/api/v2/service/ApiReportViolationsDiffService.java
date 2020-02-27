/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.ApiApplicationAdapter;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationEvaluationCommitDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationDiffDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyViolationForDiffDTO;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.landing.UserInterfaceLinksResource;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.policy.PolicyEvaluationDiffService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.security.AuthzContext.Key;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.82.0
 */
public class ApiReportViolationsDiffService
{
  private static final Logger log = LoggerFactory.getLogger(ApiReportViolationsDiffService.class);

  private static final String COMMIT_HASH_REGEX = "\\b[0-9a-f]{40}\\b";

  private static final String ABBREVIATED_COMMIT_HASH_REGEX = "\\b[0-9a-f]{4,39}\\b";

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ApiApplicationAdapter applicationAdapter;

  private final ApplicationDAO applicationDAO;

  private final ApplicationComponentDAO applicationComponentDAO;

  private final PolicyViolationAdapter policyViolationAdapter;

  private final PolicyEvaluationDiffService policyEvaluationDiffService;

  private final ProductLicense productLicense;

  @Inject
  public ApiReportViolationsDiffService(final ApplicationDAO applicationDAO,
                                        final PolicyEvaluationDAO policyEvaluationDAO,
                                        final ApiApplicationAdapter applicationAdapter,
                                        final ApplicationComponentDAO applicationComponentDAO,
                                        final PolicyViolationAdapter policyViolationAdapter,
                                        final PolicyEvaluationDiffService policyEvaluationDiffService,
                                        final ProductLicense productLicense)
  {
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.applicationAdapter = applicationAdapter;
    this.applicationComponentDAO = applicationComponentDAO;
    this.policyViolationAdapter = policyViolationAdapter;
    this.policyEvaluationDiffService = policyEvaluationDiffService;
    this.productLicense = productLicense;
  }

  @Authorize(permission = Permission.READ)
  public ApiPolicyViolationDiffDTO getPolicyViolationDiff(
      @AuthzContext(Key.APPLICATION_PUBLIC_ID) final String applicationPublicId,
      final String fromCommit,
      final String toCommit)
  {
    checkLicense();
    validateCommits(fromCommit, toCommit);

    final Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    final PolicyEvaluation fromPolicyEvaluation =
        getPolicyEvaluationForApplicationAndHash(application.getId(), fromCommit);
    final PolicyEvaluation toPolicyEvaluation =
        getPolicyEvaluationForApplicationAndHash(application.getId(), toCommit);

    final PolicyViolationDiff<PolicyViolation> policyViolationDiff =
        getViolationDiffFromEvaluations(fromPolicyEvaluation, toPolicyEvaluation);

    return buildPolicyViolationDiffDto(application, fromPolicyEvaluation, toPolicyEvaluation,
        policyViolationDiff);
  }

  private void validateCommits(final String fromCommit, final String toCommit) {
    if (Strings.isNullOrEmpty(fromCommit)) {
      throw new BadRequestException("The commit identifier for `fromCommit` must be specified.");
    }
    if (Strings.isNullOrEmpty(toCommit)) {
      throw new BadRequestException("The commit identifier for `toCommit` must be specified.");
    }
    if (toCommit.equals(fromCommit)) {
      throw new BadRequestException("The specified commits cannot be identical.");
    }
  }

  private PolicyEvaluation getPolicyEvaluationForApplicationAndHash(String applicationId, String commitHash) {

    PolicyEvaluation policyEvaluation;
    if (commitHash.matches(COMMIT_HASH_REGEX)) {
      policyEvaluation = policyEvaluationDAO.getLastByApplicationAndCommitHash(applicationId, commitHash);
    }
    else if (commitHash.matches(ABBREVIATED_COMMIT_HASH_REGEX)) {
      policyEvaluation = policyEvaluationDAO.getLastByApplicationAndAbbreviatedCommitHash(applicationId, commitHash);
    }
    else {
      throw new BadRequestException(
          String.format("The commit identifier `%s` supplied is not a valid commit hash", commitHash));
    }

    if (policyEvaluation == null) {
      throw new NotFoundException("The policy violation diff could not be determined for the given request.");
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
      throw new NotFoundException("The policy violation diff could not be determined for the given request.");
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
    dto.addedViolations = buildPolicyViolationsDtos(policyViolationDiff.getAppeared(), application.getId(),
        toPolicyEvaluation.getStageTypeId());
    dto.sameViolations = buildPolicyViolationsDtos(policyViolationDiff.getSame().values(), application.getId(),
        toPolicyEvaluation.getStageTypeId());
    dto.removedViolations = buildPolicyViolationsDtos(policyViolationDiff.getCleared(), application.getId(),
        fromPolicyEvaluation.getStageTypeId());
    dto.fromCommit = buildEvaluationCommit(application.getPublicId(), fromPolicyEvaluation);
    dto.toCommit = buildEvaluationCommit(application.getPublicId(), toPolicyEvaluation);
    dto.application = applicationAdapter.convertToDTO(application);
    dto.diffTime = new Date();
    return dto;
  }

  private Set<ApiPolicyViolationForDiffDTO> buildPolicyViolationsDtos(
      final Collection<PolicyViolation> policyViolations,
      final String applicationId,
      final String stageTypeId)
  {
    final Set<ApiPolicyViolationForDiffDTO> set = new HashSet<>(policyViolations.size());
    policyViolations.forEach(violation -> {
      if (violation.isActive()) {
        set.add(buildDiffPolicyViolationDTO(applicationId, stageTypeId, violation));
      }
    });
    return set;
  }

  private ApiPolicyViolationForDiffDTO buildDiffPolicyViolationDTO(String applicationId,
                                                                   String stageTypeId,
                                                                   PolicyViolation policyViolation)
  {

    final ApiPolicyViolationForDiffDTO apiPolicyViolationForDiffDTO = new ApiPolicyViolationForDiffDTO();
    apiPolicyViolationForDiffDTO.policyId = policyViolation.getPolicyId();
    apiPolicyViolationForDiffDTO.policyName = policyViolation.getPolicyName();
    apiPolicyViolationForDiffDTO.policyViolationId = policyViolation.getId();
    apiPolicyViolationForDiffDTO.threatLevel = policyViolation.getThreatLevel();
    final ApplicationComponent applicationComponent = applicationComponentDAO.getByApplicationIdAndStageTypeIdAndHash(
        applicationId, stageTypeId, policyViolation.getHash());
    apiPolicyViolationForDiffDTO.component = new ApiComponentDTOV2();
    apiPolicyViolationForDiffDTO.component.hash = policyViolation.getHash();
    apiPolicyViolationForDiffDTO.component.proprietary = applicationComponent != null
        && applicationComponent.isProprietary();
    apiPolicyViolationForDiffDTO.component.componentIdentifier = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(policyViolation.getComponentIdentifier());
    apiPolicyViolationForDiffDTO.component.packageUrl =
        PackageUrlIdentifier.toPackageUrl(policyViolation.getComponentIdentifier());
    apiPolicyViolationForDiffDTO.constraintViolations = policyViolationAdapter.convert(policyViolation);
    return apiPolicyViolationForDiffDTO;
  }

  private ApiApplicationEvaluationCommitDTO buildEvaluationCommit(String applicationPublicId,
                                                                  PolicyEvaluation policyEvaluation)
  {
    final ApiApplicationEvaluationCommitDTO apiApplicationEvaluationCommitDTO = new ApiApplicationEvaluationCommitDTO();
    apiApplicationEvaluationCommitDTO.commitHash = policyEvaluation.getCommitHash();
    apiApplicationEvaluationCommitDTO.reportUrl =
        UserInterfaceLinksResource.getReportUrl(applicationPublicId, policyEvaluation.getScanId());
    apiApplicationEvaluationCommitDTO.scanId = policyEvaluation.getScanId();
    apiApplicationEvaluationCommitDTO.scanTime = policyEvaluation.getTime();
    return apiApplicationEvaluationCommitDTO;
  }

  private void checkLicense() {
    if (!productLicense.hasFeature(LicensedFeature.AUTOMATION)) {
      log.debug("License does not support SourceControl automation features");
      throw new InvalidLicenseException();
    }
  }
}
