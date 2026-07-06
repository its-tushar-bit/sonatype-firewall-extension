/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.slo;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.api.v2.dto.ApiPageResult;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;

@Named
public class SloViolationFeedService
{
  static final int MAX_PAGE_SIZE = 1000;

  // Upper bound on page so (page - 1) * pageSize can never overflow the int offset passed to the DAO
  // (worst case 1_000_000 * 1000 = 1e9, well within Integer.MAX_VALUE). Deep offset pagination beyond this is
  // both nonsensical and prohibitively slow; callers walking the full set should use the updatedSince watermark.
  static final int MAX_PAGE = 1_000_000;

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final SloViolationEnricher enricher;

  @Inject
  public SloViolationFeedService(
      final ApplicationDAO applicationDAO,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final PolicyViolationDAO policyViolationDAO,
      final SloViolationEnricher enricher)
  {
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.enricher = enricher;
  }

  /**
   * Returns the SLO violation feed for an application/stage across all states (open, fixed, waived).
   * <p>
   * Note on enrichment provenance: {@code dependencyType} and {@code recommendedRemediationVersion} are derived
   * from the <em>latest</em> policy evaluation's scan report for the stage, not from the scan in which each
   * violation was originally opened. Consequently a violation opened in an older scan may report
   * {@code dependencyType = "Unknown"} (and no recommended version) if its component version is no longer present
   * in the latest report. Time-based SLO fields ({@code openTime}/{@code fixTime}/{@code waiveTime}) are always
   * per-violation and unaffected.
   * </p>
   * <p>
   * Note on {@code total} staleness: the count and the page are read in <em>separate</em> transactions
   * ({@link PolicyViolationDAO#countByApplicationIdAndStage} then
   * {@link PolicyViolationDAO#getByApplicationIdAndStagePaged}), so under a concurrent scan the reported
   * {@code total} may be slightly stale relative to the returned page. This is benign for a polling feed whose
   * consumers dedupe by {@code violationId} and reconcile across polls; it mirrors the Priorities pattern. We
   * intentionally do not "fix" this with a costlier single-query/serializable read.
   * </p>
   * <p>
   * Feature gating for this endpoint is enforced at the REST tier via
   * {@code @HasFeature(SLO_VIOLATION_FEED)} on {@link SloViolationsRestResource} (404 when disabled), so this
   * service method performs no feature-flag check of its own.
   * </p>
   */
  @Authorize(permission = Permission.READ)
  public SloViolationFeedResults getSloViolations(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) final String applicationPublicId,
      final String stageIdParam,
      final Date updatedSince,
      final int page,
      final int pageSize)
  {
    validatePagination(page, pageSize);

    final String stageId = resolveAndValidateStage(stageIdParam);

    final Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);

    final PolicyEvaluation evaluation =
        policyEvaluationDAO.getLastByApplicationIdAndStageId(application.getId(), stageId);
    if (evaluation == null) {
      throw new NotFoundException(
          "No policy evaluation found for application " + applicationPublicId + " at stage " + stageId + ".");
    }
    final String scanId = evaluation.getScanId();

    final long total = policyViolationDAO.countByApplicationIdAndStage(application.getId(), stageId, updatedSince);
    final int offset = (page - 1) * pageSize;

    final List<PolicyViolation> violations =
        policyViolationDAO.getByApplicationIdAndStagePaged(application.getId(), stageId, updatedSince, offset,
            pageSize);

    final List<SloViolation> mapped = enricher.enrich(application, stageId, scanId, violations);

    return new SloViolationFeedResults(stageId, scanId, new ApiPageResult<>(total, page, pageSize, mapped));
  }

  private void validatePagination(final int page, final int pageSize) {
    if (page < 1 || page > MAX_PAGE) {
      throw new BadRequestException("Invalid page: " + page + ". Page must be between 1 and " + MAX_PAGE + ".");
    }
    if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
      throw new BadRequestException(
          "Invalid page size: " + pageSize + ". Page size must be between 1 and " + MAX_PAGE_SIZE + ".");
    }
  }

  private String resolveAndValidateStage(final String stageIdParam) {
    if (StringUtils.isBlank(stageIdParam)) {
      return Stage.ID_RELEASE;
    }
    final String normalized = stageIdParam.toLowerCase(Locale.ROOT);
    if (!Stage.isValidStageTypeId(normalized)) {
      throw new InvalidStageException(stageIdParam);
    }
    return normalized;
  }
}
